package com.betfriends.app.domain.model

import java.time.LocalDateTime

data class BetCheckIn(
    val participantName: String,
    val checkedAt: LocalDateTime,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null,
    val distanceMeters: Double? = null
)