package com.betfriends.app.ui.bets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.betfriends.app.domain.model.Bet
import com.betfriends.app.domain.model.BetInvitation
import com.betfriends.app.domain.model.UserProfile
import com.betfriends.app.domain.repository.BetRepository
import com.betfriends.app.domain.repository.RepositorySubscription

class BetsViewModel(
    private val repository: BetRepository
) : ViewModel() {

    var uiState by mutableStateOf(BetsUiState())
        private set

    private var activeUserId: String? = null
    private var balanceSubscription: RepositorySubscription? = null
    private var betsSubscription: RepositorySubscription? = null
    private var invitationsSubscription: RepositorySubscription? = null

    fun start(user: UserProfile) {
        if (activeUserId == user.uid) {
            return
        }

        stopSubscriptions()
        activeUserId = user.uid
        uiState = BetsUiState(
            currentBalance = user.saldo
        )

        repository.ensurePublicProfile(
            user = user,
            onError = ::showMessage
        )

        balanceSubscription = repository.observeBalance(
            userId = user.uid,
            onChanged = { balance ->
                uiState = uiState.copy(
                    currentBalance = balance
                )
            },
            onError = ::showMessage
        )

        betsSubscription = repository.observeBets(
            userId = user.uid,
            onChanged = { remoteBets ->
                uiState = uiState.copy(
                    bets = mergeLocalCheckIns(remoteBets)
                )
            },
            onError = ::showMessage
        )

        invitationsSubscription = repository.observeInvitations(
            userId = user.uid,
            onChanged = { invitations ->
                uiState = uiState.copy(
                    invitations = invitations
                )
            },
            onError = ::showMessage
        )
    }

    fun stop() {
        stopSubscriptions()
        activeUserId = null
        uiState = BetsUiState()
    }

    fun searchUser(email: String) {
        val currentUserId = activeUserId

        if (currentUserId == null) {
            showMessage("Inicia sesión para buscar participantes.")
            return
        }

        uiState = uiState.copy(
            isSearchingUser = true,
            searchResult = null,
            searchMessage = null
        )

        repository.searchUserByEmail(
            email = email,
            onResult = { result ->
                uiState = if (result == null) {
                    uiState.copy(
                        isSearchingUser = false,
                        searchMessage =
                            "No se encontró un usuario con ese correo."
                    )
                } else if (result.uid == currentUserId) {
                    uiState.copy(
                        isSearchingUser = false,
                        searchMessage =
                            "Tu cuenta ya está incluida como creador."
                    )
                } else {
                    uiState.copy(
                        isSearchingUser = false,
                        searchResult = result,
                        searchMessage = null
                    )
                }
            },
            onError = { error ->
                uiState = uiState.copy(
                    isSearchingUser = false,
                    searchMessage = error
                )
            }
        )
    }

    fun clearUserSearch() {
        uiState = uiState.copy(
            searchResult = null,
            searchMessage = null
        )
    }

    fun createBet(
        bet: Bet,
        creator: UserProfile,
        onSuccess: () -> Unit
    ) {
        if (uiState.isCreatingBet) {
            return
        }

        uiState = uiState.copy(
            isCreatingBet = true,
            message = null
        )

        repository.createBet(
            bet = bet,
            creator = creator,
            onSuccess = {
                uiState = uiState.copy(
                    isCreatingBet = false,
                    searchResult = null,
                    searchMessage = null,
                    message = "Apuesta e invitaciones creadas."
                )
                onSuccess()
            },
            onError = { error ->
                uiState = uiState.copy(
                    isCreatingBet = false,
                    message = error
                )
            }
        )
    }

    fun acceptInvitation(invitation: BetInvitation) {
        val currentUserId = activeUserId ?: return

        uiState = uiState.copy(
            processingInvitationId = invitation.id,
            message = null
        )

        repository.acceptInvitation(
            invitation = invitation,
            currentUserId = currentUserId,
            onSuccess = {
                uiState = uiState.copy(
                    processingInvitationId = null,
                    message = "Invitación aceptada y saldo actualizado."
                )
            },
            onError = { error ->
                uiState = uiState.copy(
                    processingInvitationId = null,
                    message = error
                )
            }
        )
    }

    fun declineInvitation(invitation: BetInvitation) {
        val currentUserId = activeUserId ?: return

        uiState = uiState.copy(
            processingInvitationId = invitation.id,
            message = null
        )

        repository.declineInvitation(
            invitation = invitation,
            currentUserId = currentUserId,
            onSuccess = {
                uiState = uiState.copy(
                    processingInvitationId = null,
                    message = "Invitación rechazada."
                )
            },
            onError = { error ->
                uiState = uiState.copy(
                    processingInvitationId = null,
                    message = error
                )
            }
        )
    }

    fun updateBet(updatedBet: Bet) {
        uiState = uiState.copy(
            bets = uiState.bets.map { currentBet ->
                if (currentBet.id == updatedBet.id) {
                    updatedBet
                } else {
                    currentBet
                }
            }
        )

        repository.updateBetOutcome(
            bet = updatedBet,
            onError = ::showMessage
        )
    }

    fun clearMessage() {
        uiState = uiState.copy(message = null)
    }

    private fun mergeLocalCheckIns(
        remoteBets: List<Bet>
    ): List<Bet> {
        val localBetsById = uiState.bets.associateBy { it.id }

        return remoteBets.map { remoteBet ->
            val localBet = localBetsById[remoteBet.id]

            if (
                localBet != null &&
                localBet.checkIns.isNotEmpty() &&
                remoteBet.checkIns.isEmpty()
            ) {
                remoteBet.copy(
                    checkIns = localBet.checkIns,
                    status = localBet.status,
                    winnerName =
                        localBet.winnerName ?: remoteBet.winnerName
                )
            } else {
                remoteBet
            }
        }
    }

    private fun showMessage(message: String) {
        uiState = uiState.copy(message = message)
    }

    private fun stopSubscriptions() {
        balanceSubscription?.cancel()
        betsSubscription?.cancel()
        invitationsSubscription?.cancel()

        balanceSubscription = null
        betsSubscription = null
        invitationsSubscription = null
    }

    override fun onCleared() {
        stopSubscriptions()
        super.onCleared()
    }
}

class BetsViewModelFactory(
    private val repository: BetRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(BetsViewModel::class.java)) {
            return BetsViewModel(repository) as T
        }

        error("ViewModel no reconocido: ${modelClass.name}")
    }
}
