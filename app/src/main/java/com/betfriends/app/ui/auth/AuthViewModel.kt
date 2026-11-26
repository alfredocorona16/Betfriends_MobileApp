package com.betfriends.app.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.betfriends.app.domain.model.ProfileCompletionRequired
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
                    handleAuthFailure(error, "No fue posible iniciar sesión.")
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

        if (!validateName(normalizedName)) return

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
                    handleAuthFailure(error, "No fue posible crear la cuenta.")
                }
            )
        }
    }

    fun completeProfile(nombre: String) {
        val pending = uiState.pendingProfile ?: return
        val name = nombre.trim()
        if (uiState.isLoading || !validateName(name)) return

        uiState = uiState.copy(isLoading = true, errorMessage = null)
        authRepository.completeProfile(pending.uid, name) completed@{ result ->
            if (uiState.pendingProfile?.uid != pending.uid) return@completed
            result.fold(
                onSuccess = { uiState = AuthUiState(user = it) },
                onFailure = { handleAuthFailure(it, "No fue posible completar el perfil.") }
            )
        }
    }

    fun updateName(
        nombre: String,
        onSuccess: () -> Unit
    ) {
        val normalizedName = nombre.trim()

        if (!validateName(normalizedName)) return

        if (normalizedName == uiState.user?.nombre) {
            onSuccess()
            return
        }

        uiState = uiState.copy(
            isSavingProfile = true,
            errorMessage = null
        )

        authRepository.updateProfileName(
            nombre = normalizedName
        ) { result ->
            result.fold(
                onSuccess = { updatedProfile ->
                    uiState = uiState.copy(
                        user = updatedProfile,
                        isSavingProfile = false
                    )
                    onSuccess()
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        isSavingProfile = false,
                        errorMessage = error.message
                            ?: "No fue posible guardar los cambios."
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
                    handleAuthFailure(error, "No fue posible consultar tu perfil.")
                }
            )
        }
    }

    private fun handleAuthFailure(error: Throwable, fallback: String) {
        uiState = if (error is ProfileCompletionRequired) {
            AuthUiState(
                pendingProfile = PendingProfile(error.uid, error.suggestedName),
                errorMessage = if (error.cause == null) null else
                    "Tu cuenta ya está creada. Revisa tu conexión y vuelve a guardar el perfil."
            )
        } else {
            AuthUiState(errorMessage = error.message ?: fallback)
        }
    }

    private fun validateName(name: String): Boolean {
        if (name.isBlank() || name.length > MAXIMUM_NAME_LENGTH) {
            showError("Escribe un nombre de 1 a $MAXIMUM_NAME_LENGTH caracteres.")
            return false
        }
        return true
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
        const val MAXIMUM_NAME_LENGTH = 40
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