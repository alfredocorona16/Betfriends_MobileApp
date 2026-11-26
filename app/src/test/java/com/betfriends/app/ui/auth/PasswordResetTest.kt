package com.betfriends.app.ui.auth

import com.betfriends.app.data.auth.passwordResetResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordResetTest {
    private val repo = ResetRepository()
    private val vm = AuthViewModel(repo)

    @Test
    fun invalidAddressDoesNotSendARequest() {
        for (value in listOf("", "ana", "ana@", "ana @example.test")) {
            vm.requestPasswordReset(value)
            assertTrue(vm.passwordReset is PasswordResetState.Error)
        }
        assertTrue(repo.emails.isEmpty())
    }

    @Test
    fun sendsOnceAndShowsTheSameConfirmationForKnownUnknownAndDisabledAccounts() {
        for (code in listOf(null, "ERROR_USER_NOT_FOUND", "ERROR_USER_DISABLED")) {
            vm.requestPasswordReset("  ana+prueba@example.test  ")
            assertEquals(PasswordResetState.Sending, vm.passwordReset)
            vm.requestPasswordReset("otro@example.test")
            repo.respond(passwordResetResult(success = code == null, errorCode = code))
            assertEquals(PasswordResetState.Sent, vm.passwordReset)
            vm.clearPasswordReset()
            assertEquals(PasswordResetState.Idle, vm.passwordReset)
        }
        assertEquals(List(3) { "ana+prueba@example.test" }, repo.emails)
    }

    @Test
    fun technicalFailureDisplaysAnErrorAndAllowsRetry() {
        vm.requestPasswordReset("ana@example.test")
        repo.respond(passwordResetResult(success = false, errorCode = null))
        assertTrue(vm.passwordReset is PasswordResetState.Error)
        vm.requestPasswordReset("ana@example.test")
        repo.respond(Result.success(Unit))
        assertEquals(PasswordResetState.Sent, vm.passwordReset)
    }

    private class ResetRepository : StubAuthRepository() {
        val emails = mutableListOf<String>()
        lateinit var respond: (Result<Unit>) -> Unit

        override fun requestPasswordReset(correo: String, onResult: (Result<Unit>) -> Unit) {
            emails.add(correo)
            respond = onResult
        }
    }
}
