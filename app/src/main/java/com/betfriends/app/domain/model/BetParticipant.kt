package com.betfriends.app.domain.model

import java.util.UUID

data class BetParticipant(
    val id: String = UUID.randomUUID().toString(),
    val name: String
)