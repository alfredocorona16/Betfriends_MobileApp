package com.betfriends.app.ui.bets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.betfriends.app.domain.model.Bet
import com.betfriends.app.domain.model.BetInvitation
import com.betfriends.app.domain.model.UserProfile
import com.betfriends.app.location.DeviceLocation
import com.betfriends.app.domain.repository.BetRepository
import com.betfriends.app.domain.repository.RepositorySubscription

class BetsViewModel(
    private val repository: BetRepository
) : ViewModel() {

    var uiState by mutableStateOf(BetsUiState())
        private set

    private var activeUserId: String? = null
    private val settling = mutableSetOf<String>()
    private var pointsSubscription: RepositorySubscription? = null
    private var betsSubscription: RepositorySubscription? = null
    private var invitationsSubscription: RepositorySubscription? = null

    fun start(user: UserProfile) {
        if (activeUserId == user.uid) {
            return
        }

        stopSubscriptions()
        activeUserId = user.uid
        uiState = BetsUiState(
            currentPoints = user.puntos
        )

        repository.ensurePublicProfile(
            user = user,
            onError = ::showMessage
        )

        pointsSubscription = repository.observePoints(
            userId = user.uid,
            onChanged = { points ->
                uiState = uiState.copy(
                    currentPoints = points
                )
            },
            onError = ::showMessage
        )

        betsSubscription = repository.observeBets(
            userId = user.uid,
            onChanged = { remoteBets ->
                if (activeUserId == user.uid) {
                    uiState = uiState.copy(bets = remoteBets)
                    remoteBets.filter { it.approvedBy != null && !it.pointsProcessed }.forEach(::syncPoints)
                }
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
                    message = "Invitación aceptada."
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

    fun assignModerator(betId: String, moderatorId: String) =
        perform(betId, "Moderador asignado.") { _, done -> repository.assignModerator(betId, moderatorId, done) }

    fun submitCheckIn(betId: String, location: DeviceLocation?) =
        perform(betId, "Llegada registrada. El moderador revisará el resultado.") { uid, done ->
            repository.submitCheckIn(betId, uid, location, done)
        }

    fun approveWinner(betId: String, candidate: String) =
        perform(betId, "Ganador confirmado.") { uid, done -> repository.approveWinner(betId, uid, candidate, done) }

    private fun perform(betId: String, success: String, action: (String, (Result<Unit>) -> Unit) -> Unit) {
        val uid = activeUserId ?: return
        if (betId in uiState.processingBets) return
        uiState = uiState.copy(processingBets = uiState.processingBets + betId, message = null)
        action(uid) { result ->
            if (uid == activeUserId) {
                uiState = uiState.copy(processingBets = uiState.processingBets - betId,
                    message = if (result.isSuccess) success else
                        result.exceptionOrNull()?.localizedMessage ?: "No se pudo guardar. Inténtalo de nuevo.")
            }
        }
    }

    fun syncPoints(bet: Bet) {
        val uid = activeUserId ?: return
        if (bet.approvedBy == null || bet.pointsProcessed ||
            (uid != bet.moderatorId && (!bet.isAcceptedCompetitor(uid) || uid in bet.creditedIds))) return
        val key = "${uid}/${bet.id}"
        if (!settling.add(key)) return
        repository.settlePoints(bet, uid) { result ->
            settling.remove(key)
            if (result.isFailure && activeUserId == uid) {
                showMessage("El resultado está guardado. Puedes reintentar la acreditación de puntos.")
            }
        }
    }

    fun clearMessage() {
        uiState = uiState.copy(message = null)
    }

    private fun showMessage(message: String) {
        uiState = uiState.copy(message = message)
    }

    private fun stopSubscriptions() {
        pointsSubscription?.cancel()
        betsSubscription?.cancel()
        invitationsSubscription?.cancel()

        pointsSubscription = null
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
