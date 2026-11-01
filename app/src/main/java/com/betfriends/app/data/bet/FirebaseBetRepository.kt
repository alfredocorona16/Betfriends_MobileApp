package com.betfriends.app.data.bet

import com.betfriends.app.domain.model.Bet
import com.betfriends.app.domain.model.BetInvitation
import com.betfriends.app.domain.model.BetParticipant
import com.betfriends.app.domain.model.BetStatus
import com.betfriends.app.domain.model.InvitationStatus
import com.betfriends.app.domain.model.ParticipantInvitationStatus
import com.betfriends.app.domain.model.PublicUserProfile
import com.betfriends.app.domain.model.UserProfile
import com.betfriends.app.domain.repository.BetRepository
import com.betfriends.app.domain.repository.RepositorySubscription
import com.betfriends.app.domain.rules.PointsRules
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.Locale

class FirebaseBetRepository(
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance(),
    private val adapter: BetDocumentAdapter = BetDocumentAdapter()
) : BetRepository {

    override fun ensurePublicProfile(
        user: UserProfile,
        onError: (String) -> Unit
    ) {
        val publicProfile = mapOf(
            "uid" to user.uid,
            "nombre" to user.nombre,
            "correo" to user.correo,
            "correoBusqueda" to normalizeEmail(user.correo),
            "actualizadoEn" to System.currentTimeMillis()
        )

        firestore.collection(PUBLIC_PROFILES_COLLECTION)
            .document(user.uid)
            .set(publicProfile, SetOptions.merge())
            .addOnFailureListener { exception ->
                onError(
                    exception.message
                        ?: "No se pudo publicar el perfil del usuario."
                )
            }
    }

    override fun observePoints(
        userId: String,
        onChanged: (Int) -> Unit,
        onError: (String) -> Unit
    ): RepositorySubscription {
        val registration = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    onError(
                        exception.message
                            ?: "No se pudieron actualizar los puntos."
                    )
                    return@addSnapshotListener
                }

                val points =
                    (snapshot?.get(FIELD_POINTS) as? Number)?.toInt()
                        ?: 0

                onChanged(points)
            }

        return RepositorySubscription {
            registration.remove()
        }
    }

    override fun observeBets(
        userId: String,
        onChanged: (List<Bet>) -> Unit,
        onError: (String) -> Unit
    ): RepositorySubscription {
        val registration = firestore.collection(BETS_COLLECTION)
            .whereArrayContains("participantIds", userId)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    onError(
                        exception.message
                            ?: "No se pudieron cargar las apuestas."
                    )
                    return@addSnapshotListener
                }

                val bets = snapshot
                    ?.documents
                    .orEmpty()
                    .mapNotNull(adapter::toBet)
                    .sortedByDescending { it.startsAt }

                onChanged(bets)
            }

        return RepositorySubscription {
            registration.remove()
        }
    }

    override fun observeInvitations(
        userId: String,
        onChanged: (List<BetInvitation>) -> Unit,
        onError: (String) -> Unit
    ): RepositorySubscription {
        val registration = firestore
            .collection(INVITATIONS_COLLECTION)
            .whereEqualTo("inviteeId", userId)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    onError(
                        exception.message
                            ?: "No se pudieron cargar las invitaciones."
                    )
                    return@addSnapshotListener
                }

                val invitations = snapshot
                    ?.documents
                    .orEmpty()
                    .mapNotNull(adapter::toInvitation)
                    .filter {
                        it.status == InvitationStatus.PENDING
                    }

                onChanged(invitations)
            }

        return RepositorySubscription {
            registration.remove()
        }
    }

    override fun searchUserByEmail(
        email: String,
        onResult: (PublicUserProfile?) -> Unit,
        onError: (String) -> Unit
    ) {
        val normalizedEmail = normalizeEmail(email)

        if (normalizedEmail.isBlank()) {
            onError("Escribe el correo del usuario.")
            return
        }

        firestore.collection(PUBLIC_PROFILES_COLLECTION)
            .whereEqualTo("correoBusqueda", normalizedEmail)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                val document = snapshot.documents.firstOrNull()

                if (document == null) {
                    onResult(null)
                    return@addOnSuccessListener
                }

                onResult(
                    PublicUserProfile(
                        uid = document.getString("uid")
                            ?: document.id,
                        name = document.getString("nombre")
                            ?: "Usuario",
                        email = document.getString("correo")
                            ?: normalizedEmail
                    )
                )
            }
            .addOnFailureListener { exception ->
                onError(
                    exception.message
                        ?: "No se pudo buscar al usuario."
                )
            }
    }

    override fun createBet(
        bet: Bet,
        creator: UserProfile,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val creatorParticipant = BetParticipant(
            id = creator.uid,
            name = creator.nombre,
            email = creator.correo,
            invitationStatus =
                ParticipantInvitationStatus.ACCEPTED
        )

        val invitedParticipants = bet.participants
            .filter { it.id != creator.uid }
            .distinctBy { it.id }
            .map {
                it.copy(
                    invitationStatus =
                        ParticipantInvitationStatus.PENDING
                )
            }

        val participants = listOf(creatorParticipant) +
                invitedParticipants

        if (participants.size < 2) {
            onError("Invita al menos a otro usuario.")
            return
        }

        val preparedBet = bet.copy(
            creatorId = creator.uid,
            creatorName = creator.nombre,
            participants = participants
        )

        val betReference = firestore.collection(BETS_COLLECTION)
            .document(preparedBet.id)

        firestore.runTransaction { transaction ->
            transaction.set(
                betReference,
                adapter.toDocument(preparedBet)
            )

            invitedParticipants.forEach { participant ->
                val invitationId =
                    "${preparedBet.id}_${participant.id}"

                val invitationReference = firestore
                    .collection(INVITATIONS_COLLECTION)
                    .document(invitationId)

                transaction.set(
                    invitationReference,
                    mapOf(
                        "id" to invitationId,
                        "betId" to preparedBet.id,
                        "betTitle" to preparedBet.title,
                        "inviterId" to creator.uid,
                        "inviterName" to creator.nombre,
                        "inviteeId" to participant.id,
                        "inviteeName" to participant.name,
                        "status" to InvitationStatus.PENDING.name,
                        "createdAtMillis" to
                                System.currentTimeMillis(),
                        "updatedAtMillis" to
                                System.currentTimeMillis()
                    )
                )
            }
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { exception ->
            onError(
                exception.message
                    ?: "No se pudo crear la apuesta."
            )
        }
    }

    override fun acceptInvitation(
        invitation: BetInvitation,
        currentUserId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (invitation.inviteeId != currentUserId) {
            onError("Esta invitación pertenece a otro usuario.")
            return
        }

        val invitationReference = firestore
            .collection(INVITATIONS_COLLECTION)
            .document(invitation.id)

        val betReference = firestore.collection(BETS_COLLECTION)
            .document(invitation.betId)

        firestore.runTransaction { transaction ->
            val invitationSnapshot =
                transaction.get(invitationReference)
            val betSnapshot = transaction.get(betReference)

            if (!invitationSnapshot.exists()) {
                error("La invitación ya no existe.")
            }

            if (
                invitationSnapshot.getString("status") !=
                InvitationStatus.PENDING.name
            ) {
                error("La invitación ya fue respondida.")
            }

            if (
                invitationSnapshot.getString("inviteeId") !=
                currentUserId
            ) {
                error("No puedes responder esta invitación.")
            }

            if (!betSnapshot.exists()) {
                error("La apuesta ya no existe.")
            }

            val updatedParticipants = updateParticipantStatus(
                betSnapshot = betSnapshot,
                participantId = currentUserId,
                newStatus = ParticipantInvitationStatus.ACCEPTED
            )

            val acceptedIds =
                (betSnapshot.get("acceptedParticipantIds")
                        as? List<*>)
                    .orEmpty()
                    .filterIsInstance<String>()
                    .toMutableSet()
                    .apply { add(currentUserId) }
                    .toList()

            val now = System.currentTimeMillis()

            transaction.update(
                invitationReference,
                mapOf(
                    "status" to InvitationStatus.ACCEPTED.name,
                    "updatedAtMillis" to now
                )
            )

            transaction.update(
                betReference,
                mapOf(
                    "participants" to updatedParticipants,
                    "acceptedParticipantIds" to acceptedIds,
                    "updatedAtMillis" to now
                )
            )
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { exception ->
            onError(
                exception.message
                    ?: "No se pudo aceptar la invitación."
            )
        }
    }

    override fun declineInvitation(
        invitation: BetInvitation,
        currentUserId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (invitation.inviteeId != currentUserId) {
            onError("Esta invitación pertenece a otro usuario.")
            return
        }

        val invitationReference = firestore
            .collection(INVITATIONS_COLLECTION)
            .document(invitation.id)

        val betReference = firestore.collection(BETS_COLLECTION)
            .document(invitation.betId)

        firestore.runTransaction { transaction ->
            val invitationSnapshot =
                transaction.get(invitationReference)
            val betSnapshot = transaction.get(betReference)

            if (
                invitationSnapshot.getString("status") !=
                InvitationStatus.PENDING.name
            ) {
                error("La invitación ya fue respondida.")
            }

            if (
                invitationSnapshot.getString("inviteeId") !=
                currentUserId
            ) {
                error("No puedes responder esta invitación.")
            }

            val updatedParticipants = updateParticipantStatus(
                betSnapshot = betSnapshot,
                participantId = currentUserId,
                newStatus = ParticipantInvitationStatus.DECLINED
            )

            val now = System.currentTimeMillis()

            transaction.update(
                invitationReference,
                mapOf(
                    "status" to InvitationStatus.DECLINED.name,
                    "updatedAtMillis" to now
                )
            )

            transaction.update(
                betReference,
                mapOf(
                    "participants" to updatedParticipants,
                    "updatedAtMillis" to now
                )
            )
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { exception ->
            onError(
                exception.message
                    ?: "No se pudo rechazar la invitación."
            )
        }
    }

    override fun updateBetOutcome(
        bet: Bet,
        onError: (String) -> Unit
    ) {
        val betReference = firestore.collection(BETS_COLLECTION)
            .document(bet.id)

        firestore.runTransaction { transaction ->
            val betSnapshot = transaction.get(betReference)

            if (!betSnapshot.exists()) {
                error("La apuesta ya no existe.")
            }

            val remoteBet = adapter.toBet(betSnapshot)
                ?: error("La apuesta tiene datos incompletos.")

            val now = System.currentTimeMillis()
            val updates = hashMapOf<String, Any>(
                "updatedAtMillis" to now
            )

            // El primer resultado registrado es el definitivo.
            val resultBet = if (remoteBet.status == BetStatus.FINISHED) {
                remoteBet
            } else {
                updates["status"] = bet.status.name
                bet.winnerName?.let { updates["winnerName"] = it }

                remoteBet.copy(
                    status = bet.status,
                    winnerName = bet.winnerName
                )
            }

            // Si ya se procesaron los puntos, no se vuelven a asignar.
            if (!remoteBet.pointsProcessed) {
                val awards = PointsRules.calculate(resultBet)

                if (awards.isNotEmpty()) {
                    awards.forEach { award ->
                        val userReference = firestore
                            .collection(USERS_COLLECTION)
                            .document(award.userId)

                        val movementReference = firestore
                            .collection(POINTS_COLLECTION)
                            .document("${bet.id}_${award.userId}")

                        transaction.update(
                            userReference,
                            FIELD_POINTS,
                            FieldValue.increment(award.points.toLong())
                        )

                        transaction.set(
                            movementReference,
                            mapOf(
                                "id" to movementReference.id,
                                "userId" to award.userId,
                                "betId" to bet.id,
                                "betTitle" to resultBet.title,
                                "points" to award.points,
                                "wasWinner" to award.wasWinner,
                                "createdAtMillis" to now
                            )
                        )
                    }

                    updates[FIELD_POINTS_PROCESSED] = true
                }
            }

            transaction.update(betReference, updates)
        }.addOnFailureListener { exception ->
            onError(
                exception.message
                    ?: "No se pudo actualizar la apuesta."
            )
        }
    }

    private fun updateParticipantStatus(
        betSnapshot: DocumentSnapshot,
        participantId: String,
        newStatus: ParticipantInvitationStatus
    ): List<Map<String, Any>> {
        val rawParticipants =
            (betSnapshot.get("participants") as? List<*>)
                .orEmpty()

        var participantWasFound = false

        val updatedParticipants = rawParticipants.mapNotNull { item ->
            val rawMap = item as? Map<*, *>
                ?: return@mapNotNull null

            val id = rawMap["id"] as? String
                ?: return@mapNotNull null

            val status = if (id == participantId) {
                participantWasFound = true
                newStatus.name
            } else {
                rawMap["invitationStatus"] as? String
                    ?: ParticipantInvitationStatus.PENDING.name
            }

            mapOf(
                "id" to id,
                "name" to (rawMap["name"] as? String ?: "Usuario"),
                "email" to (rawMap["email"] as? String ?: ""),
                "invitationStatus" to status
            )
        }

        if (!participantWasFound) {
            error("El usuario no pertenece a esta apuesta.")
        }

        return updatedParticipants
    }

    private fun normalizeEmail(email: String): String {
        return email.trim().lowercase(Locale.ROOT)
    }

    private companion object {
        const val USERS_COLLECTION = "usuarios"
        const val PUBLIC_PROFILES_COLLECTION = "perfilesPublicos"
        const val BETS_COLLECTION = "apuestas"
        const val INVITATIONS_COLLECTION = "invitaciones"
        const val POINTS_COLLECTION = "movimientosPuntos"
        const val FIELD_POINTS = "puntos"
        const val FIELD_POINTS_PROCESSED =
            BetDocumentAdapter.FIELD_POINTS_PROCESSED
    }
}
