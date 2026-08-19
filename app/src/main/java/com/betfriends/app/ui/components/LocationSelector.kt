package com.betfriends.app.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.betfriends.app.location.CurrentLocationProvider
import com.betfriends.app.location.DeviceLocation
import java.util.Locale

@Composable
fun LocationSelector(
    selectedLocation: DeviceLocation?,
    onLocationSelected: (DeviceLocation) -> Unit
) {
    val context = LocalContext.current

    val locationProvider = remember(context) {
        CurrentLocationProvider(
            context.applicationContext
        )
    }

    var isLoading by rememberSaveable {
        mutableStateOf(false)
    }

    var message by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val obtainCurrentLocation: () -> Unit = {
        isLoading = true
        message = null

        locationProvider.getCurrentLocation(
            onSuccess = { location ->
                isLoading = false

                if (location.accuracyMeters > 100f) {
                    message =
                        "La ubicación no es suficientemente precisa. " +
                                "Precisión obtenida: " +
                                "${location.accuracyMeters.toInt()} metros. " +
                                "Inténtalo nuevamente en un lugar abierto."
                } else {
                    onLocationSelected(location)
                    message = null
                }
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

            val fineLocationGranted =
                permissions[
                    Manifest.permission.ACCESS_FINE_LOCATION
                ] == true

            val coarseLocationGranted =
                permissions[
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ] == true

            when {
                fineLocationGranted -> {
                    obtainCurrentLocation()
                }

                coarseLocationGranted -> {
                    message =
                        "BetFriends necesita ubicación precisa " +
                                "para validar una apuesta. " +
                                "Concede la opción «Precisa»."
                }

                else -> {
                    message =
                        "El permiso de ubicación fue rechazado."
                }
            }
        }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Coordenadas del destino",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text =
                "Debes encontrarte en el lugar que quieres " +
                        "registrar como destino.",
            style = MaterialTheme.typography.bodyMedium
        )

        Button(
            onClick = {
                if (hasFineLocationPermission(context)) {
                    obtainCurrentLocation()
                } else {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )

                    Text("Obteniendo ubicación...")
                }
            } else {
                Text(
                    text = if (selectedLocation == null) {
                        "Usar mi ubicación actual"
                    } else {
                        "Actualizar ubicación"
                    }
                )
            }
        }

        if (selectedLocation != null) {
            Text(
                text = "Latitud: ${
                    formatCoordinate(selectedLocation.latitude)
                }",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Longitud: ${
                    formatCoordinate(selectedLocation.longitude)
                }",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text =
                    "Precisión: " +
                            "${selectedLocation.accuracyMeters.toInt()} metros",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )
        }

        if (message != null) {
            Text(
                text = message.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun hasFineLocationPermission(
    context: Context
): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

private fun formatCoordinate(
    coordinate: Double
): String {
    return String.format(
        Locale.US,
        "%.6f",
        coordinate
    )
}