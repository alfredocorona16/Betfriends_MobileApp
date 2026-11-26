package com.betfriends.app.domain.rules

import com.betfriends.app.domain.model.Bet
import com.betfriends.app.domain.model.BetCheckIn
import com.betfriends.app.domain.model.BetType
import com.betfriends.app.domain.model.ParticipantInvitationStatus
import com.betfriends.app.location.DeviceLocation
import java.time.LocalDateTime
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object BetRules {
    fun checkIn(bet: Bet, uid: String, at: LocalDateTime, location: DeviceLocation?): BetCheckIn {
        require(bet.isOpen) { "El reto ya está cerrado." }
        require(bet.moderator?.invitationStatus == ParticipantInvitationStatus.ACCEPTED) {
            "El moderador debe aceptar el reto primero."
        }
        require(bet.isAcceptedCompetitor(uid)) { "Solo un competidor que aceptó puede registrar su llegada." }
        require(bet.checkIns.none { it.participantId == uid }) { "Ya registraste tu llegada." }
        require(!at.isBefore(bet.startsAt) && !at.isAfter(bet.endsAt)) { "Estás fuera del horario del reto." }
        val name = bet.acceptedParticipants.first { it.id == uid }.name
        if (bet.type == BetType.TIME) return BetCheckIn(uid, name, at)

        val gps = requireNotNull(location) { "Activa la ubicación para registrar tu llegada." }
        val radius = requireNotNull(bet.radiusMeters) { "Falta el radio del reto." }
        require(gps.latitude in -90.0..90.0 && gps.longitude in -180.0..180.0 &&
                gps.accuracyMeters.isFinite() && gps.accuracyMeters >= 0 && gps.accuracyMeters <= radius) {
            "La precisión del GPS es insuficiente."
        }
        val distance = calculateDistanceMeters(
            gps.latitude, gps.longitude, requireNotNull(bet.targetLatitude), requireNotNull(bet.targetLongitude)
        )
        require(distance <= radius) { "Estás fuera del radio de $radius metros." }
        return BetCheckIn(uid, name, at, gps.latitude, gps.longitude, gps.accuracyMeters, distance)
    }

    fun requireApproval(bet: Bet, actor: String, candidate: String) {
        require(bet.isOpen) { "El reto ya está cerrado." }
        require(actor == bet.moderatorId && bet.moderator?.invitationStatus == ParticipantInvitationStatus.ACCEPTED) {
            "Solo el moderador que aceptó este reto puede confirmar el resultado."
        }
        require(bet.isAcceptedCompetitor(candidate) && bet.checkIns.any { it.participantId == candidate }) {
            "Elige a un competidor con una llegada registrada."
        }
    }

    private fun calculateDistanceMeters(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double
    ): Double {
        val earthRadiusMeters = 6_371_000.0

        val startLatRadians =
            Math.toRadians(startLatitude)

        val endLatRadians =
            Math.toRadians(endLatitude)

        val latitudeDifference =
            Math.toRadians(
                endLatitude - startLatitude
            )

        val longitudeDifference =
            Math.toRadians(
                endLongitude - startLongitude
            )

        val latitudeSin =
            sin(latitudeDifference / 2)

        val longitudeSin =
            sin(longitudeDifference / 2)

        val calculation =
            latitudeSin * latitudeSin +
                    cos(startLatRadians) *
                    cos(endLatRadians) *
                    longitudeSin *
                    longitudeSin

        val safeCalculation =
            calculation.coerceIn(0.0, 1.0)

        val angularDistance = 2 * atan2(
            sqrt(safeCalculation),
            sqrt(1 - safeCalculation)
        )

        return earthRadiusMeters * angularDistance
    }
}
