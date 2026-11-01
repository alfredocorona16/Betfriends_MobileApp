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
    val pointsProcessed: Boolean = false
) {
    val acceptedParticipants: List<BetParticipant>
        get() = participants.filter {
            it.invitationStatus ==
                    ParticipantInvitationStatus.ACCEPTED
        }
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
