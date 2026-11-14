package com.betfriends.app.ui.bets

import com.betfriends.app.domain.model.Bet
import com.betfriends.app.domain.model.BetInvitation
import com.betfriends.app.domain.model.PublicUserProfile

data class BetsUiState(
    val bets: List<Bet> = emptyList(),
    val invitations: List<BetInvitation> = emptyList(),
    val currentBalance: Double? = null,
    val searchResult: PublicUserProfile? = null,
    val isSearchingUser: Boolean = false,
    val isCreatingBet: Boolean = false,
    val processingInvitationId: String? = null,
    val searchMessage: String? = null,
    val message: String? = null
)