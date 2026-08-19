package com.betfriends.app.domain.repository

import com.betfriends.app.domain.model.Bet
import com.betfriends.app.domain.model.BetInvitation
import com.betfriends.app.domain.model.PublicUserProfile
import com.betfriends.app.domain.model.UserProfile

fun interface RepositorySubscription {
    fun cancel()
}

interface BetRepository {
    fun ensurePublicProfile(
        user: UserProfile,
        onError: (String) -> Unit
    )

    fun observeBalance(
        userId: String,
        onChanged: (Double) -> Unit,
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

    fun updateBetOutcome(
        bet: Bet,
        onError: (String) -> Unit
    )
}
