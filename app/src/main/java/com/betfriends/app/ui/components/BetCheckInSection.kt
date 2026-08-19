package com.betfriends.app.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.betfriends.app.domain.model.Bet
import com.betfriends.app.domain.model.BetStatus
import com.betfriends.app.domain.model.BetType
import com.betfriends.app.domain.rules.BetRules
import com.betfriends.app.domain.rules.CheckInResult
import com.betfriends.app.location.CurrentLocationProvider
import com.betfriends.app.location.DeviceLocation
import java.time.LocalDateTime
import java.util.Locale

@Composable
fun BetCheckInSection(
    bet: Bet,
    onBetUpdated: (Bet) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val locationProvider = remember {
        CurrentLocationProvider(
            context.applicationContext
        )
    }

    var selectedParticipantId by remember(bet.id) {
        mutableStateOf<String?>(null)
    }

    var message by remember {
        mutableStateOf<String?>(null)
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    val selectedParticipant =
        bet.participants.firstOrNull {
            it.id == selectedParticipantId
        }

    val processCheckIn: (DeviceLocation?) -> Unit = { location ->
        val result = BetRules.registerCheckIn(
            bet = bet,
            participantName =
                selectedParticipant?.name.orEmpty(),
            checkedAt = LocalDateTime.now(),
            currentLocation = location
        )

        when (result) {
            is CheckInResult.Accepted -> {
                message = result.message
                onBetUpdated(result.updatedBet)
            }

            is CheckInResult.Rejected -> {
                message = result.message
            }
        }
    }

    val captureCurrentLocation: () -> Unit = {
        isLoading = true
        message = "Comprobando ubicación..."

        locationProvider.getCurrentLocation(
            onSuccess = { location ->
                isLoading = false
                processCheckIn(location)
            },
            onError = { error ->
                isLoading = false
                message = error
            }
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val permissionGranted =
                permissions.values.any { it }

            if (permissionGranted) {
                captureCurrentLocation()
            } else {
                message =
                    "Debes permitir el acceso a la ubicación."
            }
        }

    Column(
        modifier = modifier
    ) {
        bet.winnerName?.let { winner ->
            Text(
                text = "Ganador: $winner",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Premio: ${
                    formatCheckInCurrency(
                        bet.totalPrize
                    )
                }",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )
        }

        Text(
            text = "Selecciona quién realizará el check-in",
            style = MaterialTheme.typography.labelLarge
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        if (bet.participants.isEmpty()) {
            Text(
                text = "Esta apuesta no tiene participantes.",
                color = MaterialTheme.colorScheme.error
            )
        } else {
            LazyRow(
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = bet.participants,
                    key = { participant ->
                        participant.id
                    }
                ) { participant ->
                    FilterChip(
                        selected =
                            participant.id ==
                                    selectedParticipantId,
                        onClick = {
                            selectedParticipantId =
                                participant.id

                            message = null
                        },
                        enabled =
                            bet.status != BetStatus.FINISHED &&
                                    bet.status != BetStatus.CANCELLED,
                        label = {
                            Text(participant.name)
                        }
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Button(
            enabled =
                selectedParticipant != null &&
                        !isLoading &&
                        bet.status != BetStatus.FINISHED &&
                        bet.status != BetStatus.CANCELLED,
            onClick = {
                if (bet.type == BetType.TIME) {
                    processCheckIn(null)
                } else {
                    if (hasLocationPermission(context)) {
                        captureCurrentLocation()
                    } else {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (isLoading) {
                    "Verificando..."
                } else {
                    "Realizar check-in"
                }
            )
        }

        message?.let {
            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(text = it)
        }
    }
}

private fun hasLocationPermission(
    context: Context
): Boolean {
    val finePermission =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    val coarsePermission =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    return finePermission || coarsePermission
}

private fun formatCheckInCurrency(
    amount: Double
): String {
    return "${'$'}${
        String.format(
            Locale.US,
            "%.2f",
            amount
        )
    } MXN"
}