
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
import com.betfriends.app.domain.rules.BetRules
import com.betfriends.app.domain.rules.PointsRules
import com.betfriends.app.location.DeviceLocation
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.FieldPath
import java.time.LocalDateTime
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.Locale
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Transaction

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
                    (snapshot?.get(FIELD_POINTS) as? Number)?.toInt() ?: 0

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
                        uid = document.getString("uid") ?: document.id,
                        name = document.getString("nombre") ?: "Usuario",
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
            invitationStatus = ParticipantInvitationStatus.ACCEPTED
        )

        val invitedParticipants = bet.participants
            .filter { it.id != creator.uid }
            .distinctBy { it.id }
            .map {
                it.copy(
                    invitationStatus = ParticipantInvitationStatus.PENDING
                )
            }

        val participants = listOf(creatorParticipant) +
            invitedParticipants

        if (participants.size !in 3..10 || participants.none { it.id == bet.moderatorId }) {
            onError("Elige un moderador e invita al menos a dos competidores.")
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
                        "createdAtMillis" to System.currentTimeMillis(),
                        "updatedAtMillis" to System.currentTimeMillis()
                    )
                )
            }
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { exception ->
            onError(
                exception.message ?: "No se pudo crear la apuesta."
            )
        }
    }
    private fun getBetForPendingInvitation(
        transaction: Transaction,
        invitationReference: DocumentReference,
        betReference: DocumentReference,
        currentUserId: String
    ): DocumentSnapshot {

        val invitationSnapshot = transaction.get(invitationReference)
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
            error(BET_NOT_FOUND_MESSAGE)
        }

        return betSnapshot
    }

    override fun acceptInvitation(
        invitation: BetInvitation,
        currentUserId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        respondToInvitation(
            invitation = invitation,
            currentUserId = currentUserId,
            invitationStatus = InvitationStatus.ACCEPTED,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    override fun declineInvitation(
        invitation: BetInvitation,
        currentUserId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        respondToInvitation(
            invitation = invitation,
            currentUserId = currentUserId,
            invitationStatus = InvitationStatus.DECLINED,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    private fun respondToInvitation(
        invitation: BetInvitation,
        currentUserId: String,
        invitationStatus: InvitationStatus,
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

        val betReference = firestore
            .collection(BETS_COLLECTION)
            .document(invitation.betId)

        firestore.runTransaction { transaction ->
            val betSnapshot = getBetForPendingInvitation(
                transaction = transaction,
                invitationReference = invitationReference,
                betReference = betReference,
                currentUserId = currentUserId
            )

            require(adapter.toBet(betSnapshot)?.isOpen == true) { "El reto ya está cerrado." }
            val now = System.currentTimeMillis()
            val idsField = if (invitationStatus == InvitationStatus.ACCEPTED)
                "acceptedParticipantIds" else "declinedParticipantIds"
            val betUpdates = mapOf(idsField to FieldValue.arrayUnion(currentUserId))

            transaction.update(
                invitationReference,
                mapOf(
                    "status" to invitationStatus.name,
                    "updatedAtMillis" to now
                )
            )

            transaction.update(
                betReference,
                betUpdates
            )
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { exception ->
            val message = when (invitationStatus) {
                InvitationStatus.ACCEPTED ->
                    "No se pudo aceptar la invitación."

                InvitationStatus.DECLINED ->
                    "No se pudo rechazar la invitación."

                else -> "No se pudo responder la invitación."
            }

            onError(exception.message ?: message)
        }
    }

    override fun assignModerator(betId: String, moderatorId: String, done: (Result<Unit>) -> Unit) {
        firestore.collection(BETS_COLLECTION).document(betId)
            .update("moderatorId", moderatorId).report(done)
    }

    override fun submitCheckIn(betId: String, uid: String, location: DeviceLocation?, done: (Result<Unit>) -> Unit) {
        val ref = firestore.collection(BETS_COLLECTION).document(betId)
        firestore.runTransaction { tx ->
            val bet = adapter.toBet(tx.get(ref)) ?: error(BET_NOT_FOUND_MESSAGE)
            if (bet.checkIns.none { it.participantId == uid }) {
                val arrival = BetRules.checkIn(bet, uid, LocalDateTime.now(), location)
                val profile = tx.get(firestore.collection(USERS_COLLECTION).document(uid))
                val data = mutableMapOf<String, Any>(
                    "name" to requireNotNull(profile.getString("nombre")) { "Completa tu perfil." },
                    "email" to requireNotNull(profile.getString("correo")) { "Completa tu perfil." },
                    "at" to FieldValue.serverTimestamp()
                )
                if (arrival.latitude != null && arrival.longitude != null) {
                    data["position"] = GeoPoint(arrival.latitude, arrival.longitude)
                    data["accuracy"] = requireNotNull(arrival.accuracyMeters).toDouble()
                }
                tx.update(ref, FieldPath.of("checkInsByUid", uid), data)
            }
            Unit
        }.reportVerified(ref, done) { saved -> saved.checkIns.any { it.participantId == uid } }
    }

    override fun approveWinner(betId: String, actor: String, candidate: String, done: (Result<Unit>) -> Unit) {
        val ref = firestore.collection(BETS_COLLECTION).document(betId)
        firestore.runTransaction { tx ->
            val bet = adapter.toBet(tx.get(ref)) ?: error(BET_NOT_FOUND_MESSAGE)
            if (bet.status != BetStatus.FINISHED || bet.winnerId != candidate || bet.approvedBy != actor) {
                BetRules.requireApproval(bet, actor, candidate)
                tx.update(ref, mapOf(
                    "status" to BetStatus.FINISHED.name,
                    "winnerId" to candidate,
                    "winnerName" to bet.checkIns.first { it.participantId == candidate }.participantName,
                    "approvedBy" to actor,
                    "approvedAt" to FieldValue.serverTimestamp()
                ))
            }
            Unit
        }.reportVerified(ref, done) { saved ->
            saved.status == BetStatus.FINISHED && saved.approvedBy == actor && saved.winnerId == candidate
        }
    }

    override fun settlePoints(bet: Bet, actor: String, done: (Result<Unit>) -> Unit) {
        val recipients = PointsRules.calculate(bet).filter {
            it.userId !in bet.creditedIds && (actor == bet.moderatorId || actor == it.userId)
        }
        val ref = firestore.collection(BETS_COLLECTION).document(bet.id)

        // Una transacción por beneficiario mantiene cada saldo unido a su comprobante.
        // Si se interrumpe el reparto, el siguiente intento retoma a quienes faltan.
        fun credit(index: Int) {
            val award = recipients.getOrNull(index) ?: return done(Result.success(Unit))
            var observedCount = -1
            firestore.runTransaction { tx ->
                val snap = tx.get(ref)
                val current = adapter.toBet(snap) ?: error(BET_NOT_FOUND_MESSAGE)
                require(current.status == BetStatus.FINISHED && current.approvedBy == current.moderatorId &&
                    current.winnerId != null) { "El resultado aún no está confirmado." }
                observedCount = current.creditedIds.size
                if (award.userId !in current.creditedIds) {
                    val awards = PointsRules.calculate(current)
                    val item = awards.first { it.userId == award.userId }
                    val credited = current.creditedIds + item.userId
                    val movement = firestore.collection(POINTS_COLLECTION).document("${bet.id}_${item.userId}")
                    val profile = firestore.collection(USERS_COLLECTION).document(item.userId)
                    tx.update(profile, mapOf(
                        FIELD_POINTS to FieldValue.increment(item.points.toLong()),
                        "ultimoRetoBF" to bet.id
                    ))
                    tx.set(movement, mapOf(
                        "id" to movement.id, "userId" to item.userId, "betId" to bet.id,
                        "betTitle" to current.title, "points" to item.points, "wasWinner" to item.wasWinner,
                        "createdAtMillis" to requireNotNull(snap.getTimestamp("approvedAt")).toDate().time
                    ))
                    tx.update(ref, mapOf("creditedIds" to credited, FIELD_POINTS_PROCESSED to
                        awards.all { it.userId in credited }))
                }
                Unit
            }.addOnSuccessListener { credit(index + 1) }.addOnFailureListener { error ->
                // Una escritura concurrente puede confirmarse antes de que se evalúen las reglas.
                ref.get(Source.SERVER).addOnSuccessListener { snapshot ->
                    val paid = (snapshot.get("creditedIds") as? List<*>).orEmpty()
                    when {
                        award.userId in paid -> credit(index + 1)
                        observedCount >= 0 && paid.size > observedCount -> credit(index)
                        else -> done(Result.failure(error))
                    }
                }.addOnFailureListener { done(Result.failure(error)) }
            }
        }
        credit(0)
    }

    private fun Task<*>.reportVerified(
        ref: DocumentReference,
        done: (Result<Unit>) -> Unit,
        isSaved: (Bet) -> Boolean
    ) {
        addOnSuccessListener { done(Result.success(Unit)) }
        addOnFailureListener { error ->
            ref.get(Source.SERVER).addOnSuccessListener { snapshot ->
                val stored = adapter.toBet(snapshot)
                done(if (stored != null && isSaved(stored)) Result.success(Unit) else Result.failure(error))
            }.addOnFailureListener { done(Result.failure(error)) }
        }
    }

    private fun Task<*>.report(done: (Result<Unit>) -> Unit) {
        addOnSuccessListener { done(Result.success(Unit)) }
        addOnFailureListener { done(Result.failure(it)) }
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

        const val BET_NOT_FOUND_MESSAGE = "La apuesta ya no existe."
    }
}
