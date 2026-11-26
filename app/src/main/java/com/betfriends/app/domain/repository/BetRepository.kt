package com.betfriends.app.domain.repository

import com.betfriends.app.domain.model.Bet
import com.betfriends.app.domain.model.BetInvitation
import com.betfriends.app.domain.model.PublicUserProfile
import com.betfriends.app.domain.model.UserProfile
import com.betfriends.app.location.DeviceLocation

fun interface RepositorySubscription {
    fun cancel()
}

interface BetRepository {
    fun ensurePublicProfile(
        user: UserProfile,
        onError: (String) -> Unit
    )

    fun observePoints(
        userId: String,
        onChanged: (Int) -> Unit,
        onError: (String) -> Unit
    ): RepositorySubscription

    fun observeBets(
        userId: String,
        onChanged: (List<Bet>) -> Unit,
        onError: (String) -> Unit
    ): RepositorySubscription

    fun observeInvitations(
        userId: String,
        onChanged: (List<BetInvitation>) -> Unit,
        onError: (String) -> Unit
    ): RepositorySubscription

    fun searchUserByEmail(
        email: String,
        onResult: (PublicUserProfile?) -> Unit,
        onError: (String) -> Unit
    )

    fun createBet(
        bet: Bet,
        creator: UserProfile,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    )

    fun acceptInvitation(
        invitation: BetInvitation,
        currentUserId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    )

    fun declineInvitation(
        invitation: BetInvitation,
        currentUserId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    )

    fun assignModerator(betId: String, moderatorId: String, done: (Result<Unit>) -> Unit)
    fun submitCheckIn(betId: String, uid: String, location: DeviceLocation?, done: (Result<Unit>) -> Unit)
    fun approveWinner(betId: String, actor: String, candidate: String, done: (Result<Unit>) -> Unit)
    fun settlePoints(bet: Bet, actor: String, done: (Result<Unit>) -> Unit)
}
