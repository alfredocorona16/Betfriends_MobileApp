package com.betfriends.app.data.bet

import com.betfriends.app.domain.model.Bet
import com.betfriends.app.domain.model.BetCheckIn
import com.betfriends.app.domain.model.BetInvitation
import com.betfriends.app.domain.model.BetParticipant
import com.betfriends.app.domain.model.BetStatus
import com.betfriends.app.domain.model.BetType
import com.betfriends.app.domain.model.InvitationStatus
import com.betfriends.app.domain.model.ParticipantInvitationStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.DocumentSnapshot
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * PATRÓN ADAPTER
 *
 * - Adaptee (lo que no podemos cambiar): los documentos de Firestore
 *   (DocumentSnapshot y mapas con valores como Number, String, List).
 * - Target (lo que la app necesita): los modelos de dominio
 *   Bet y BetInvitation, con LocalDateTime y enums.
 * - Adapter (esta clase): traduce en ambos sentidos.
 *
 * Así FirebaseBetRepository solo se encarga de consultar y guardar,
 * y ya no sabe cómo se ve un documento por dentro.
 */
class BetDocumentAdapter {

    // ---------- Firestore -> dominio ----------

    fun toBet(document: DocumentSnapshot): Bet? {
        val startsAtMillis =
            (document.get("startsAtMillis") as? Number)?.toLong()
                ?: return null

        val endsAtMillis =
            (document.get("endsAtMillis") as? Number)?.toLong()
                ?: return null

        val accepted = document.get("acceptedParticipantIds") as? List<*>
        val declined = (document.get("declinedParticipantIds") as? List<*>).orEmpty()
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
                        invitationStatus = when {
                            id in declined -> ParticipantInvitationStatus.DECLINED
                            accepted?.contains(id) == true -> ParticipantInvitationStatus.ACCEPTED
                            document.contains("declinedParticipantIds") &&
                                rawMap["invitationStatus"] != "DECLINED" -> ParticipantInvitationStatus.PENDING
                            else -> enumValueOrDefault(rawMap["invitationStatus"] as? String,
                                ParticipantInvitationStatus.PENDING)
                        }
                    )
                }

        val arrivals = (document.get("checkInsByUid") as? Map<*, *>).orEmpty().mapNotNull { (key, value) ->
            val uid = key as? String ?: return@mapNotNull null
            val entry = value as? Map<*, *> ?: return@mapNotNull null
            val at = entry["at"] as? Timestamp ?: return@mapNotNull null
            val name = entry["name"] as? String ?: return@mapNotNull null
            val position = entry["position"] as? GeoPoint
            BetCheckIn(uid, name, at.toDate().time.toLocalDateTime(), position?.latitude,
                position?.longitude, (entry["accuracy"] as? Number)?.toFloat(),
                participantEmail = entry["email"] as? String ?: "")
        }.sortedWith(compareBy({ it.checkedAt }, { it.participantId }))

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
            status = enumValueOrDefault(
                document.getString("status"),
                BetStatus.WAITING
            ),
            moderatorId = document.getString("moderatorId"),
            checkIns = arrivals,
            winnerId = document.getString("winnerId"),
            winnerName = document.getString("winnerName"),
            approvedBy = document.getString("approvedBy"),
            creditedIds = (document.get("creditedIds") as? List<*>).orEmpty().filterIsInstance<String>(),
            pointsProcessed =
                document.getBoolean(FIELD_POINTS_PROCESSED) == true
        )
    }

    fun toInvitation(document: DocumentSnapshot): BetInvitation? {
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
            status = enumValueOrDefault(
                document.getString("status"),
                InvitationStatus.PENDING
            )
        )
    }

    // ---------- dominio -> Firestore ----------

    fun toDocument(bet: Bet): Map<String, Any?> {
        val participantMaps = bet.participants.map {
            toDocument(it)
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
            "moderatorId" to bet.moderatorId,
            "declinedParticipantIds" to emptyList<String>(),
            "locationName" to bet.locationName,
            "radiusMeters" to bet.radiusMeters,
            "targetLatitude" to bet.targetLatitude,
            "targetLongitude" to bet.targetLongitude,
            "targetAccuracyMeters" to bet.targetAccuracyMeters,
            "participants" to participantMaps,
            "participantIds" to bet.participants.map { it.id },
            "acceptedParticipantIds" to bet.acceptedParticipants
                .map { it.id },
            "status" to bet.status.name,
            "winnerName" to bet.winnerName,
            FIELD_POINTS_PROCESSED to false,
            "createdAtMillis" to System.currentTimeMillis(),
            "updatedAtMillis" to System.currentTimeMillis()
        )
    }

    fun toDocument(participant: BetParticipant): Map<String, Any> {
        return mapOf(
            "id" to participant.id,
            "name" to participant.name,
            "email" to participant.email,
            "invitationStatus" to participant.invitationStatus.name
        )
    }

    // ---------- conversiones auxiliares ----------

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

    companion object {
        const val FIELD_POINTS_PROCESSED = "pointsProcessed"
    }
}
