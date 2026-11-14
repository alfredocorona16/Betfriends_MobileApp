package com.betfriends.app.domain.model

import java.util.UUID

data class BetParticipant(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String = "",
    val invitationStatus: ParticipantInvitationStatus =
        ParticipantInvitationStatus.ACCEPTED,
    val role: ParticipantRole = ParticipantRole.COMPETITOR
)

enum class ParticipantRole {
    COMPETITOR,
    MODERATOR
}

enum class ParticipantInvitationStatus {
    PENDING,
    ACCEPTED,
    DECLINED
}