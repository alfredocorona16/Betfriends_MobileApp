package com.betfriends.app.ui.auth

import com.betfriends.app.domain.model.ProfileCompletionRequired
import com.betfriends.app.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileRecoveryViewModelTest {
    @Test
    fun registrationCanRetryWithTheSameUidAfterTheFirstProfileWriteFails() {
        val user = UserProfile("uid-80", "Ana", "ana@example.test", 0)
        val repo = object : StubAuthRepository() {
            override fun register(nombre: String, correo: String, password: String, onResult: (Result<UserProfile>) -> Unit) {
                onResult(Result.failure(ProfileCompletionRequired(user.uid, nombre, IllegalStateException())))
            }

            override fun completeProfile(uid: String, nombre: String, onResult: (Result<UserProfile>) -> Unit) {
                assertEquals(user.uid, uid)
                assertEquals(user.nombre, nombre)
                onResult(Result.success(user))
            }
        }
        val vm = AuthViewModel(repo)
        vm.register("Ana", user.correo, "prueba123")
        assertEquals(PendingProfile(user.uid, "Ana"), vm.uiState.pendingProfile)
        assertFalse(vm.uiState.isAuthenticated)
        vm.completeProfile("   ")
        assertNotNull(vm.uiState.errorMessage)
        vm.completeProfile(" Ana ")
        assertEquals(user, vm.uiState.user)
        assertNull(vm.uiState.pendingProfile)
    }

    @Test
    fun sessionRestoreOnlyOffersCompletionForAConfirmedMissingProfile() {
        fun restoreWith(error: Exception): AuthUiState {
            val repo = object : StubAuthRepository() {
                override val hasActiveSession = true
                override fun getCurrentUserProfile(onResult: (Result<UserProfile>) -> Unit) {
                    onResult(Result.failure(error))
                }
            }
            return AuthViewModel(repo).uiState
        }
        val missing = restoreWith(ProfileCompletionRequired("uid-80"))
        assertEquals(PendingProfile("uid-80"), missing.pendingProfile)
        val offline = restoreWith(IllegalStateException("Sin conexión"))
        assertNull(offline.pendingProfile)
        assertEquals("Sin conexión", offline.errorMessage)
    }
}
