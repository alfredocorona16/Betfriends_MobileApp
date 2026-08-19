package com.betfriends.app.domain.model

import java.time.LocalDateTime

data class Bet(
    val id: String,
    val title: String,
    val description: String,
    val type: BetType,
    val startsAt: LocalDateTime,
    val endsAt: LocalDateTime,

    val locationName: String? = null,
    val radiusMeters: Int? = null,
    val targetLatitude: Double? = null,
    val targetLongitude: Double? = null,
    val targetAccuracyMeters: Float? = null,

    val participants: List<BetParticipant> = emptyList(),
    val stakePerParticipant: Double = 0.0,

    val status: BetStatus = BetStatus.WAITING,
    val checkIns: List<BetCheckIn> = emptyList(),
    val winnerName: String? = null
) {
    val totalPrize: Double
        get() = stakePerParticipant * participants.size
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