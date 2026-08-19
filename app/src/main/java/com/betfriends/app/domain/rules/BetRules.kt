package com.betfriends.app.domain.rules

import com.betfriends.app.domain.model.Bet
import com.betfriends.app.domain.model.BetCheckIn
import com.betfriends.app.domain.model.BetStatus
import com.betfriends.app.domain.model.BetType
import com.betfriends.app.location.DeviceLocation
import java.time.LocalDateTime
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

sealed interface CheckInResult {

    data class Accepted(
        val updatedBet: Bet,
        val message: String
    ) : CheckInResult

    data class Rejected(
        val message: String
    ) : CheckInResult
}

object BetRules {

    fun registerCheckIn(
        bet: Bet,
        participantName: String,
        checkedAt: LocalDateTime,
        currentLocation: DeviceLocation?
    ): CheckInResult {
        val cleanName = participantName.trim()

        if (cleanName.isBlank()) {
            return CheckInResult.Rejected(
                "Selecciona un participante."
            )
        }

        if (bet.status == BetStatus.FINISHED) {
            return CheckInResult.Rejected(
                "Esta apuesta ya terminó. Ganador: ${bet.winnerName}."
            )
        }

        if (bet.status == BetStatus.CANCELLED) {
            return CheckInResult.Rejected(
                "Esta apuesta fue cancelada."
            )
        }

        val registeredParticipant =
            bet.participants.firstOrNull {
                it.name.equals(
                    cleanName,
                    ignoreCase = true
                )
            }

        if (
            bet.participants.isNotEmpty() &&
            registeredParticipant == null
        ) {
            return CheckInResult.Rejected(
                "El usuario $cleanName no participa en esta apuesta."
            )
        }

        val verifiedParticipantName =
            registeredParticipant?.name ?: cleanName

        if (checkedAt.isBefore(bet.startsAt)) {
            return CheckInResult.Rejected(
                "La apuesta todavía no ha comenzado."
            )
        }

        if (checkedAt.isAfter(bet.endsAt)) {
            return CheckInResult.Rejected(
                "El tiempo permitido ya terminó."
            )
        }

        return when (bet.type) {
            BetType.TIME -> {
                acceptCheckIn(
                    bet = bet,
                    participantName = verifiedParticipantName,
                    checkedAt = checkedAt,
                    location = null,
                    distanceMeters = null
                )
            }

            BetType.LOCATION -> {
                validateLocationCheckIn(
                    bet = bet,
                    participantName = verifiedParticipantName,
                    checkedAt = checkedAt,
                    currentLocation = currentLocation
                )
            }
        }
    }

    private fun validateLocationCheckIn(
        bet: Bet,
        participantName: String,
        checkedAt: LocalDateTime,
        currentLocation: DeviceLocation?
    ): CheckInResult {
        if (currentLocation == null) {
            return CheckInResult.Rejected(
                "No fue posible obtener tu ubicación."
            )
        }

        val targetLatitude = bet.targetLatitude
        val targetLongitude = bet.targetLongitude
        val radiusMeters = bet.radiusMeters

        if (
            targetLatitude == null ||
            targetLongitude == null ||
            radiusMeters == null
        ) {
            return CheckInResult.Rejected(
                "La apuesta no tiene una ubicación configurada correctamente."
            )
        }

        if (currentLocation.accuracyMeters > radiusMeters) {
            return CheckInResult.Rejected(
                "La precisión del GPS es insuficiente. " +
                        "Precisión actual: " +
                        "${currentLocation.accuracyMeters.toInt()} m."
            )
        }

        val distance = calculateDistanceMeters(
            startLatitude = currentLocation.latitude,
            startLongitude = currentLocation.longitude,
            endLatitude = targetLatitude,
            endLongitude = targetLongitude
        )

        if (distance > radiusMeters) {
            return CheckInResult.Rejected(
                "Estás a ${distance.toInt()} metros del objetivo. " +
                        "Debes estar dentro de un radio de " +
                        "$radiusMeters metros."
            )
        }

        return acceptCheckIn(
            bet = bet,
            participantName = participantName,
            checkedAt = checkedAt,
            location = currentLocation,
            distanceMeters = distance
        )
    }

    private fun acceptCheckIn(
        bet: Bet,
        participantName: String,
        checkedAt: LocalDateTime,
        location: DeviceLocation?,
        distanceMeters: Double?
    ): CheckInResult.Accepted {
        val checkIn = BetCheckIn(
            participantName = participantName,
            checkedAt = checkedAt,
            latitude = location?.latitude,
            longitude = location?.longitude,
            accuracyMeters = location?.accuracyMeters,
            distanceMeters = distanceMeters
        )

        val updatedBet = bet.copy(
            checkIns = bet.checkIns + checkIn,
            winnerName = participantName,
            status = BetStatus.FINISHED
        )

        val message = if (distanceMeters == null) {
            "Check-in válido. $participantName ganó la apuesta."
        } else {
            "Check-in válido a ${distanceMeters.toInt()} metros. " +
                    "$participantName ganó la apuesta."
        }

        return CheckInResult.Accepted(
            updatedBet = updatedBet,
            message = message
        )
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