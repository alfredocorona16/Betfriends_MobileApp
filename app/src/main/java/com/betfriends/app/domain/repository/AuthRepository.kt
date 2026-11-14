package com.betfriends.app.domain.repository

import com.betfriends.app.domain.model.UserProfile

interface AuthRepository {

    val hasActiveSession: Boolean

    fun login(
        correo: String,
        password: String,
        onResult: (Result<UserProfile>) -> Unit
    )

    fun register(
        nombre: String,
        correo: String,
        password: String,
        onResult: (Result<UserProfile>) -> Unit
    )

    fun getCurrentUserProfile(
        onResult: (Result<UserProfile>) -> Unit
    )

    fun logout()
}