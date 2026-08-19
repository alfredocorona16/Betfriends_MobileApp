package com.betfriends.app.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.betfriends.app.domain.repository.AuthRepository

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    var uiState by mutableStateOf(AuthUiState())
        private set

    init {
        restoreSession()
    }

    fun login(
        correo: String,
        password: String
    ) {
        val normalizedEmail = correo.trim()

        if (normalizedEmail.isBlank()) {
            showError("Escribe tu correo electrónico.")
            return
        }

        if (password.isBlank()) {
            showError("Escribe tu contraseña.")
            return
        }

        uiState = uiState.copy(
            isLoading = true,
            errorMessage = null,
            registrationSuccessful = false
        )

        authRepository.login(
            correo = normalizedEmail,
            password = password
        ) { result ->
            result.fold(
                onSuccess = { userProfile ->
                    uiState = AuthUiState(
                        user = userProfile
                    )
                },
                onFailure = { error ->
                    uiState = AuthUiState(
                        errorMessage = error.message
                            ?: "No fue posible iniciar sesión."
                    )
                }
            )
        }
    }

    fun register(
        nombre: String,
        correo: String,
        password: String
    ) {
        val normalizedName = nombre.trim()
        val normalizedEmail = correo.trim()

        if (normalizedName.isBlank()) {
            showError("Escribe tu nombre.")
            return
        }

        if (normalizedEmail.isBlank()) {
            showError("Escribe tu correo electrónico.")
            return
        }

        if (password.length < MINIMUM_PASSWORD_LENGTH) {
            showError(
                "La contraseña debe tener al menos " +
                        "$MINIMUM_PASSWORD_LENGTH caracteres."
            )
            return
        }

        uiState = uiState.copy(
            isLoading = true,
            errorMessage = null,
            registrationSuccessful = false
        )

        authRepository.register(
            nombre = normalizedName,
            correo = normalizedEmail,
            password = password
        ) { result ->
            result.fold(
                onSuccess = { userProfile ->
                    uiState = AuthUiState(
                        user = userProfile,
                        registrationSuccessful = true
                    )
                },
                onFailure = { error ->
                    uiState = AuthUiState(
                        errorMessage = error.message
                            ?: "No fue posible crear la cuenta."
                    )
                }
            )
        }
    }

    fun logout() {
        authRepository.logout()
        uiState = AuthUiState()
    }

    fun clearError() {
        uiState = uiState.copy(
            errorMessage = null
        )
    }

    fun consumeRegistrationSuccess() {
        uiState = uiState.copy(
            registrationSuccessful = false
        )
    }

    private fun restoreSession() {
        if (!authRepository.hasActiveSession) {
            return
        }

        uiState = uiState.copy(
            isLoading = true,
            errorMessage = null
        )

        authRepository.getCurrentUserProfile { result ->
            result.fold(
                onSuccess = { userProfile ->
                    uiState = AuthUiState(
                        user = userProfile
                    )
                },
                onFailure = { error ->
                    authRepository.logout()

                    uiState = AuthUiState(
                        errorMessage = error.message
                    )
                }
            )
        }
    }

    private fun showError(
        message: String
    ) {
        uiState = uiState.copy(
            isLoading = false,
            errorMessage = message
        )
    }

    private companion object {
        const val MINIMUM_PASSWORD_LENGTH = 6
    }
}

class AuthViewModelFactory(
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (
            modelClass.isAssignableFrom(
                AuthViewModel::class.java
            )
        ) {
            return AuthViewModel(
                authRepository = authRepository
            ) as T
        }

        throw IllegalArgumentException(
            "ViewModel desconocido: ${modelClass.name}"
        )
    }
}