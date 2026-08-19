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
    val stakePerParticipant: Double = 0.0,
    val confirmedPot: Double = 0.0,

    val status: BetStatus = BetStatus.WAITING,
    val checkIns: List<BetCheckIn> = emptyList(),
    val winnerName: String? = null
) {
    val acceptedParticipants: List<BetParticipant>
        get() = participants.filter {
            it.invitationStatus ==
                    ParticipantInvitationStatus.ACCEPTED
        }

    val expectedPrize: Double
        get() = stakePerParticipant * participants.count {
            it.invitationStatus !=
                    ParticipantInvitationStatus.DECLINED
        }

    val totalPrize: Double
        get() = if (confirmedPot > 0.0) {
            confirmedPot
        } else {
            stakePerParticipant * acceptedParticipants.size
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
