package com.betfriends.app.domain.model

import java.time.LocalDateTime

data class Bet(
    val id: String,
    val title: String,
    val description: String,
    val type: BetType,
    val startsAt: LocalDateTime,
    val endsAt: LocalDateTime,

    val creatorId: String = "",
    val creatorName: String = "",

    val locationName: String? = null,
    val radiusMeters: Int? = null,
    val targetLatitude: Double? = null,
    val targetLongitude: Double? = null,
    val targetAccuracyMeters: Float? = null,

    val participants: List<BetParticipant> = emptyList(),

    val status: BetStatus = BetStatus.WAITING,
    val checkIns: List<BetCheckIn> = emptyList(),
    val winnerName: String? = null,
    val pointsProcessed: Boolean = false,
    val moderatorId: String? = null,
    val winnerId: String? = null,
    val approvedBy: String? = null,
    val creditedIds: List<String> = emptyList()
) {
    val acceptedParticipants: List<BetParticipant>
        get() = participants.filter {
            it.invitationStatus ==
                    ParticipantInvitationStatus.ACCEPTED
        }
    val isOpen: Boolean get() = status == BetStatus.WAITING || status == BetStatus.ACTIVE

    val moderator: BetParticipant? get() = participants.firstOrNull { it.id == moderatorId }

    val needsModerator: Boolean get() = moderator == null ||
        moderator?.invitationStatus == ParticipantInvitationStatus.DECLINED

    fun isAcceptedCompetitor(uid: String): Boolean =
        uid != moderatorId && acceptedParticipants.any { it.id == uid }
}

enum class BetType {
    TIME,
    LOCATION
}

enum class BetStatus {
    WAITING,
    ACTIVE,
    FINISHED,
    CANCELLED
}
