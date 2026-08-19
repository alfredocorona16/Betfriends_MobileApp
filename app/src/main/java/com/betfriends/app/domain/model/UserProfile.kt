package com.betfriends.app.domain.model

data class UserProfile(
    val uid: String = "",
    val nombre: String = "",
    val correo: String = "",
    val saldo: Double = 0.0
)