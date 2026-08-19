package com.betfriends.app.data.bet

import com.betfriends.app.domain.model.Bet
import com.betfriends.app.domain.model.BetInvitation
import com.betfriends.app.domain.model.BetParticipant
import com.betfriends.app.domain.model.BetStatus
import com.betfriends.app.domain.model.BetType
import com.betfriends.app.domain.model.InvitationStatus
import com.betfriends.app.domain.model.ParticipantInvitationStatus
import com.betfriends.app.domain.model.PublicUserProfile
import com.betfriends.app.domain.model.UserProfile
import com.betfriends.app.domain.repository.BetRepository
import com.betfriends.app.domain.repository.RepositorySubscription
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

class FirebaseBetRepository(
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
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

    override fun observeBalance(
        userId: String,
        onChanged: (Double) -> Unit,
        onError: (String) -> Unit
    ): RepositorySubscription {
        val registration = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    onError(
                        exception.message
                            ?: "No se pudo actualizar el saldo."
                    )
                    return@addSnapshotListener
                }

                val balance =
                    (snapshot?.get("saldo") as? Number)?.toDouble()

                if (balance != null) {
                    onChanged(balance)
                }
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
                    .mapNotNull(::documentToBet)
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
                    .mapNotNull(::documentToInvitation)
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
            participants = participants,
            confirmedPot = bet.stakePerParticipant
        )

        val userReference = firestore.collection(USERS_COLLECTION)
            .document(creator.uid)

        val betReference = firestore.collection(BETS_COLLECTION)
            .document(preparedBet.id)

        firestore.runTransaction { transaction ->
            val userSnapshot = transaction.get(userReference)

            if (!userSnapshot.exists()) {
                error("No se encontró el perfil del creador.")
            }

            val currentBalance =
                (userSnapshot.get("saldo") as? Number)?.toDouble()
                    ?: 0.0

            if (currentBalance < preparedBet.stakePerParticipant) {
                error("Saldo insuficiente para crear la apuesta.")
            }

            transaction.update(
                userReference,
                "saldo",
                currentBalance - preparedBet.stakePerParticipant
            )

            transaction.set(
                betReference,
                betToMap(preparedBet)
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
                        "stakeAmount" to
                                preparedBet.stakePerParticipant,
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

        val userReference = firestore.collection(USERS_COLLECTION)
            .document(currentUserId)

        firestore.runTransaction { transaction ->
            val invitationSnapshot =
                transaction.get(invitationReference)
            val betSnapshot = transaction.get(betReference)
            val userSnapshot = transaction.get(userReference)

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

            val stake =
                (invitationSnapshot.get("stakeAmount") as? Number)
                    ?.toDouble()
                    ?: error("La invitación no tiene un monto válido.")

            val currentBalance =
                (userSnapshot.get("saldo") as? Number)?.toDouble()
                    ?: 0.0

            if (currentBalance < stake) {
                error("Saldo insuficiente para aceptar la apuesta.")
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

            val currentPot =
                (betSnapshot.get("confirmedPot") as? Number)
                    ?.toDouble()
                    ?: 0.0

            val now = System.currentTimeMillis()

            transaction.update(
                userReference,
                "saldo",
                currentBalance - stake
            )

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
                    "confirmedPot" to currentPot + stake,
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
        val updates = hashMapOf<String, Any>(
            "status" to bet.status.name,
            "updatedAtMillis" to System.currentTimeMillis()
        )

        bet.winnerName?.let {
            updates["winnerName"] = it
        }

        firestore.collection(BETS_COLLECTION)
            .document(bet.id)
            .update(updates)
            .addOnFailureListener { exception ->
                onError(
                    exception.message
                        ?: "No se pudo actualizar la apuesta."
                )
            }
    }

    private fun betToMap(bet: Bet): Map<String, Any?> {
        val participantMaps = bet.participants.map {
            participantToMap(it)
        }

        return mapOf(
            "id" to bet.id,
            "title" to bet.title,
            "description" to bet.description,
            "type" to bet.type.name,
            "startsAtMillis" to bet.startsAt.toEpochMillis(),
            "endsAtMillis" to bet.endsAt.toEpochMillis(),
            "creatorId" to bet.creatorId,
            "creatorName" to bet.creatorName,
            "locationName" to bet.locationName,
            "radiusMeters" to bet.radiusMeters,
            "targetLatitude" to bet.targetLatitude,
            "targetLongitude" to bet.targetLongitude,
            "targetAccuracyMeters" to bet.targetAccuracyMeters,
            "participants" to participantMaps,
            "participantIds" to bet.participants.map { it.id },
            "acceptedParticipantIds" to bet.acceptedParticipants
                .map { it.id },
            "stakePerParticipant" to bet.stakePerParticipant,
            "confirmedPot" to bet.confirmedPot,
            "status" to bet.status.name,
            "winnerName" to bet.winnerName,
            "createdAtMillis" to System.currentTimeMillis(),
            "updatedAtMillis" to System.currentTimeMillis()
        )
    }

    private fun participantToMap(
        participant: BetParticipant
    ): Map<String, Any> {
        return mapOf(
            "id" to participant.id,
            "name" to participant.name,
            "email" to participant.email,
            "invitationStatus" to
                    participant.invitationStatus.name
        )
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

    private fun documentToBet(
        document: DocumentSnapshot
    ): Bet? {
        val startsAtMillis =
            (document.get("startsAtMillis") as? Number)?.toLong()
                ?: return null

        val endsAtMillis =
            (document.get("endsAtMillis") as? Number)?.toLong()
                ?: return null

        val participants =
            (document.get("participants") as? List<*>)
                .orEmpty()
                .mapNotNull { item ->
                    val rawMap = item as? Map<*, *>
                        ?: return@mapNotNull null

                    val id = rawMap["id"] as? String
                        ?: return@mapNotNull null

                    BetParticipant(
                        id = id,
                        name = rawMap["name"] as? String
                            ?: "Usuario",
                        email = rawMap["email"] as? String
                            ?: "",
                        invitationStatus = enumValueOrDefault(
                            rawMap["invitationStatus"] as? String,
                            ParticipantInvitationStatus.PENDING
                        )
                    )
                }

        return Bet(
            id = document.getString("id") ?: document.id,
            title = document.getString("title") ?: "Apuesta",
            description = document.getString("description") ?: "",
            type = enumValueOrDefault(
                document.getString("type"),
                BetType.TIME
            ),
            startsAt = startsAtMillis.toLocalDateTime(),
            endsAt = endsAtMillis.toLocalDateTime(),
            creatorId = document.getString("creatorId") ?: "",
            creatorName = document.getString("creatorName") ?: "",
            locationName = document.getString("locationName"),
            radiusMeters =
                (document.get("radiusMeters") as? Number)?.toInt(),
            targetLatitude =
                (document.get("targetLatitude") as? Number)
                    ?.toDouble(),
            targetLongitude =
                (document.get("targetLongitude") as? Number)
                    ?.toDouble(),
            targetAccuracyMeters =
                (document.get("targetAccuracyMeters") as? Number)
                    ?.toFloat(),
            participants = participants,
            stakePerParticipant =
                (document.get("stakePerParticipant") as? Number)
                    ?.toDouble()
                    ?: 0.0,
            confirmedPot =
                (document.get("confirmedPot") as? Number)
                    ?.toDouble()
                    ?: 0.0,
            status = enumValueOrDefault(
                document.getString("status"),
                BetStatus.WAITING
            ),
            winnerName = document.getString("winnerName")
        )
    }

    private fun documentToInvitation(
        document: DocumentSnapshot
    ): BetInvitation? {
        val betId = document.getString("betId") ?: return null
        val inviteeId = document.getString("inviteeId")
            ?: return null

        return BetInvitation(
            id = document.getString("id") ?: document.id,
            betId = betId,
            betTitle = document.getString("betTitle") ?: "Apuesta",
            inviterId = document.getString("inviterId") ?: "",
            inviterName = document.getString("inviterName")
                ?: "Usuario",
            inviteeId = inviteeId,
            inviteeName = document.getString("inviteeName")
                ?: "Usuario",
            stakeAmount =
                (document.get("stakeAmount") as? Number)
                    ?.toDouble()
                    ?: 0.0,
            status = enumValueOrDefault(
                document.getString("status"),
                InvitationStatus.PENDING
            )
        )
    }

    private fun LocalDateTime.toEpochMillis(): Long {
        return atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private fun Long.toLocalDateTime(): LocalDateTime {
        return Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(
        value: String?,
        defaultValue: T
    ): T {
        return runCatching {
            enumValueOf<T>(value.orEmpty())
        }.getOrDefault(defaultValue)
    }

    private fun normalizeEmail(email: String): String {
        return email.trim().lowercase(Locale.ROOT)
    }

    private companion object {
        const val USERS_COLLECTION = "usuarios"
        const val PUBLIC_PROFILES_COLLECTION = "perfilesPublicos"
        const val BETS_COLLECTION = "apuestas"
        const val INVITATIONS_COLLECTION = "invitaciones"
    }
}
