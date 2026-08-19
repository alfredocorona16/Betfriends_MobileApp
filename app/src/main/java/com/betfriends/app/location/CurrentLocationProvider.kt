package com.betfriends.app.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

class CurrentLocationProvider(
    context: Context
) {
    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(
        onSuccess: (DeviceLocation) -> Unit,
        onError: (String) -> Unit
    ) {
        val request = CurrentLocationRequest.Builder()
            .setPriority(
                Priority.PRIORITY_HIGH_ACCURACY
            )
            .setMaxUpdateAgeMillis(5_000L)
            .setDurationMillis(15_000L)
            .build()

        val cancellationTokenSource =
            CancellationTokenSource()

        try {
            fusedLocationClient
                .getCurrentLocation(
                    request,
                    cancellationTokenSource.token
                )
                .addOnSuccessListener { location ->
                    if (location == null) {
                        onError(
                            "No se pudo obtener la ubicación. " +
                                    "Verifica que el GPS esté activado."
                        )

                        return@addOnSuccessListener
                    }

                    onSuccess(
                        DeviceLocation(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracyMeters = location.accuracy
                        )
                    )
                }
                .addOnFailureListener { exception ->
                    onError(
                        exception.message
                            ?: "Ocurrió un error al obtener la ubicación."
                    )
                }
        } catch (_: SecurityException) {
            onError(
                "No se concedió el permiso de ubicación precisa."
            )
        }
    }
}