package com.betfriends.app.domain.model

/**
 * Registro de los puntos que recibió un usuario por una apuesta.
 * Se guarda una sola vez por usuario y por apuesta.
 */
data class PointsTransaction(
    val id: String,
    val userId: String,
    val betId: String,
    val betTitle: String,
    val points: Int,
    val wasWinner: Boolean,
    val createdAtMillis: Long
)
