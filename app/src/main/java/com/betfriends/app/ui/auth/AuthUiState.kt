package com.betfriends.app.ui.auth

import com.betfriends.app.domain.model.UserProfile

data class PendingProfile(val uid: String, val suggestedName: String = "")

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSavingProfile: Boolean = false,
    val user: UserProfile? = null,
    val errorMessage: String? = null,
    val registrationSuccessful: Boolean = false,
    val pendingProfile: PendingProfile? = null
) {
    val isAuthenticated: Boolean
        get() = user != null
}

sealed interface PasswordResetState {
    data object Idle : PasswordResetState
    data object Sending : PasswordResetState
    data object Sent : PasswordResetState
    data class Error(val message: String) : PasswordResetState
}
