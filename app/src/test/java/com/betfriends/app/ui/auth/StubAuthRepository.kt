package com.betfriends.app.ui.auth

import com.betfriends.app.domain.model.UserProfile
import com.betfriends.app.domain.repository.AuthRepository

internal open class StubAuthRepository : AuthRepository {
    override val hasActiveSession: Boolean = false
    override fun login(correo: String, password: String, onResult: (Result<UserProfile>) -> Unit): Unit = unexpected()
    override fun register(nombre: String, correo: String, password: String, onResult: (Result<UserProfile>) -> Unit): Unit = unexpected()
    override fun getCurrentUserProfile(onResult: (Result<UserProfile>) -> Unit): Unit = unexpected()
    override fun updateProfileName(nombre: String, onResult: (Result<UserProfile>) -> Unit): Unit = unexpected()
    override fun completeProfile(uid: String, nombre: String, onResult: (Result<UserProfile>) -> Unit): Unit = unexpected()
    override fun logout(): Unit = unexpected()

    protected fun unexpected(): Nothing = error("Llamada imprevista al repositorio de autenticación.")
}
