package com.betfriends.app.ui.auth

import com.betfriends.app.domain.model.UserProfile

data class AuthUiState(
    val isLoading: Boolean = false,
    val user: UserProfile? = null,
    val errorMessage: String? = null,
    val registrationSuccessful: Boolean = false
) {
    val isAuthenticated: Boolean
        get() = user != null
}