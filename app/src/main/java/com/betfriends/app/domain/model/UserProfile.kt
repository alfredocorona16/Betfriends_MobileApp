package com.betfriends.app.domain.model

data class UserProfile(
    val uid: String = "",
    val nombre: String = "",
    val correo: String = "",
    val puntos: Int = 0
)