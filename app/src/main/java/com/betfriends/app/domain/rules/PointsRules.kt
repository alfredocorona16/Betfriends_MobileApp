package com.betfriends.app.domain.rules

import com.betfriends.app.domain.model.Bet
import com.betfriends.app.domain.model.BetStatus

data class PointsAward(
    val userId: String,
    val points: Int,
    val wasWinner: Boolean
)

/**
 * Reglas para calcular los puntos de una apuesta.
 * Para cambiar los valores solo se modifican las dos constantes.
 */
object PointsRules {

    const val PARTICIPATION_POINTS = 10
    const val VICTORY_BONUS_POINTS = 50

    const val POINTS_FOR_WINNER =
        PARTICIPATION_POINTS + VICTORY_BONUS_POINTS

    /**
     * Devuelve los puntos de cada participante que aceptó.
     * Si la apuesta no terminó o no tiene ganador, no hay puntos.
     */
    fun calculate(bet: Bet): List<PointsAward> {
        if (bet.status != BetStatus.FINISHED) {
            return emptyList()
        }

        val winnerName = bet.winnerName?.trim()

        if (winnerName.isNullOrBlank()) {
            return emptyList()
        }

        val winnerId = bet.acceptedParticipants
            .firstOrNull { it.name.equals(winnerName, ignoreCase = true) }
            ?.id

        return bet.acceptedParticipants.map { participant ->
            val isWinner = participant.id == winnerId

            PointsAward(
                userId = participant.id,
                points = if (isWinner) {
                    POINTS_FOR_WINNER
                } else {
                    PARTICIPATION_POINTS
                },
                wasWinner = isWinner
            )
        }
    }
}
