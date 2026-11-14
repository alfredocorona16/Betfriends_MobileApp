package com.betfriends.app.domain.model

data class BetInvitation(
    val id: String,
    val betId: String,
    val betTitle: String,
    val inviterId: String,
    val inviterName: String,
    val inviteeId: String,
    val inviteeName: String,
    val stakeAmount: Double,
    val status: InvitationStatus = InvitationStatus.PENDING
)

enum class InvitationStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    CANCELLED
}