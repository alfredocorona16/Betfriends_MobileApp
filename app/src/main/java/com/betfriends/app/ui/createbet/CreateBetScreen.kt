package com.betfriends.app.ui.createbet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.betfriends.app.domain.model.Bet
import com.betfriends.app.domain.model.BetParticipant
import com.betfriends.app.domain.model.BetStatus
import com.betfriends.app.domain.model.BetType
import com.betfriends.app.location.DeviceLocation
import com.betfriends.app.ui.components.DateTimeSelector
import com.betfriends.app.ui.components.LocationSelector
import com.betfriends.app.ui.theme.BetFriendsTheme
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBetScreen(
    onBack: () -> Unit,
    onBetCreated: (Bet) -> Unit
) {
    var title by rememberSaveable {
        mutableStateOf("")
    }

    var description by rememberSaveable {
        mutableStateOf("")
    }

    var selectedTypeName by rememberSaveable {
        mutableStateOf(BetType.TIME.name)
    }

    var startDateTimeIso by rememberSaveable {
        mutableStateOf("")
    }

    var endDateTimeIso by rememberSaveable {
        mutableStateOf("")
    }

    var locationName by rememberSaveable {
        mutableStateOf("")
    }

    var radius by rememberSaveable {
        mutableStateOf("100")
    }

    var targetLatitude by rememberSaveable {
        mutableStateOf<Double?>(null)
    }

    var targetLongitude by rememberSaveable {
        mutableStateOf<Double?>(null)
    }

    var targetAccuracyMeters by rememberSaveable {
        mutableStateOf<Float?>(null)
    }

    var participantName by rememberSaveable {
        mutableStateOf("")
    }

    var stakeAmount by rememberSaveable {
        mutableStateOf("50")
    }

    val participants = remember {
        mutableStateListOf<BetParticipant>()
    }

    var errorMessage by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val selectedType = BetType.valueOf(selectedTypeName)

    val startDateTime = startDateTimeIso
        .takeIf { it.isNotBlank() }
        ?.let(LocalDateTime::parse)

    val endDateTime = endDateTimeIso
        .takeIf { it.isNotBlank() }
        ?.let(LocalDateTime::parse)

    val selectedTargetLocation =
        targetLatitude?.let { latitude ->
            targetLongitude?.let { longitude ->
                targetAccuracyMeters?.let { accuracy ->
                    DeviceLocation(
                        latitude = latitude,
                        longitude = longitude,
                        accuracyMeters = accuracy
                    )
                }
            }
        }

    val currentStake =
        stakeAmount.toDoubleOrNull() ?: 0.0

    val currentPrize =
        currentStake * participants.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Crear apuesta")
                },
                navigationIcon = {
                    TextButton(
                        onClick = onBack
                    ) {
                        Text("Volver")
                    }
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Datos generales",
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Título")
                },
                placeholder = {
                    Text("¿Quién llegará primero?")
                },
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Descripción")
                },
                placeholder = {
                    Text("Describe las condiciones de la apuesta")
                },
                minLines = 3
            )

            Text(
                text = "Tipo de apuesta",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = selectedType == BetType.TIME,
                    onClick = {
                        selectedTypeName = BetType.TIME.name
                        errorMessage = null
                    },
                    label = {
                        Text("Tiempo")
                    }
                )

                FilterChip(
                    selected = selectedType == BetType.LOCATION,
                    onClick = {
                        selectedTypeName = BetType.LOCATION.name
                        errorMessage = null
                    },
                    label = {
                        Text("Ubicación")
                    }
                )
            }

            Text(
                text = when (selectedType) {
                    BetType.TIME ->
                        "Ganará quien complete primero la condición dentro del periodo."

                    BetType.LOCATION ->
                        "Ganará el primer participante que llegue al lugar establecido."
                },
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Participantes",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Agrega entre 2 y 10 participantes.",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = participantName,
                onValueChange = {
                    participantName = it
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Nombre del participante")
                },
                placeholder = {
                    Text("Ejemplo: Ángel")
                },
                singleLine = true
            )

            Button(
                onClick = {
                    val cleanName = participantName.trim()

                    when {
                        cleanName.isBlank() -> {
                            errorMessage =
                                "Escribe el nombre del participante."
                        }

                        participants.size >= 10 -> {
                            errorMessage =
                                "Puedes agregar un máximo de 10 participantes."
                        }

                        participants.any {
                            it.name.equals(
                                cleanName,
                                ignoreCase = true
                            )
                        } -> {
                            errorMessage =
                                "Ese participante ya fue agregado."
                        }

                        else -> {
                            participants.add(
                                BetParticipant(
                                    name = cleanName
                                )
                            )

                            participantName = ""
                            errorMessage = null
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Agregar participante")
            }

            participants.forEachIndexed { index, participant ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${index + 1}. ${participant.name}",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    TextButton(
                        onClick = {
                            participants.remove(participant)
                            errorMessage = null
                        }
                    ) {
                        Text("Quitar")
                    }
                }
            }

            OutlinedTextField(
                value = stakeAmount,
                onValueChange = { newValue ->
                    val validNumber = newValue.matches(
                        Regex("""\d*(\.\d{0,2})?""")
                    )

                    if (validNumber) {
                        stakeAmount = newValue
                        errorMessage = null
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Monto por participante")
                },
                prefix = {
                    Text("$")
                },
                suffix = {
                    Text("MXN")
                },
                supportingText = {
                    Text("Monto demostrativo, no se procesa dinero real")
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                singleLine = true
            )

            Text(
                text = "Bolsa total: ${formatCurrency(currentPrize)}",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )

            DateTimeSelector(
                title = "Inicio de la apuesta",
                value = startDateTime,
                onValueChange = { selectedDateTime ->
                    startDateTimeIso = selectedDateTime.toString()
                    errorMessage = null
                }
            )

            DateTimeSelector(
                title = "Fecha y hora límite",
                value = endDateTime,
                onValueChange = { selectedDateTime ->
                    endDateTimeIso = selectedDateTime.toString()
                    errorMessage = null
                }
            )

            if (selectedType == BetType.LOCATION) {
                Text(
                    text = "Regla de ubicación",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = locationName,
                    onValueChange = {
                        locationName = it
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Nombre del lugar")
                    },
                    placeholder = {
                        Text("Universidad Tecnológica de Tlaxcala")
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    value = radius,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() }) {
                            radius = newValue
                            errorMessage = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Radio permitido en metros")
                    },
                    supportingText = {
                        Text("Debe estar entre 20 y 500 metros")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true
                )

                LocationSelector(
                    selectedLocation = selectedTargetLocation,
                    onLocationSelected = { location ->
                        targetLatitude = location.latitude
                        targetLongitude = location.longitude
                        targetAccuracyMeters =
                            location.accuracyMeters

                        errorMessage = null
                    }
                )
            }

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Button(
                onClick = {
                    runCatching {
                        validateAndCreateBet(
                            title = title,
                            description = description,
                            type = selectedType,
                            startDateTime = startDateTime,
                            endDateTime = endDateTime,
                            locationName = locationName,
                            radius = radius,
                            targetLatitude = targetLatitude,
                            targetLongitude = targetLongitude,
                            targetAccuracyMeters =
                                targetAccuracyMeters,
                            participants = participants.toList(),
                            stakeAmount = stakeAmount
                        )
                    }.onSuccess { newBet ->
                        onBetCreated(newBet)
                    }.onFailure { exception ->
                        errorMessage =
                            exception.message
                                ?: "No se pudo crear la apuesta."
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Crear apuesta")
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )
        }
    }
}

private fun validateAndCreateBet(
    title: String,
    description: String,
    type: BetType,
    startDateTime: LocalDateTime?,
    endDateTime: LocalDateTime?,
    locationName: String,
    radius: String,
    targetLatitude: Double?,
    targetLongitude: Double?,
    targetAccuracyMeters: Float?,
    participants: List<BetParticipant>,
    stakeAmount: String
): Bet {
    require(title.isNotBlank()) {
        "Escribe un título para la apuesta."
    }

    val validStartDateTime = requireNotNull(
        startDateTime
    ) {
        "Selecciona la fecha y hora de inicio."
    }

    val validEndDateTime = requireNotNull(
        endDateTime
    ) {
        "Selecciona la fecha y hora límite."
    }

    require(
        validEndDateTime.isAfter(validStartDateTime)
    ) {
        "La fecha límite debe ser posterior al inicio."
    }

    require(participants.size >= 2) {
        "Agrega al menos dos participantes."
    }

    require(participants.size <= 10) {
        "Puedes agregar un máximo de 10 participantes."
    }

    val validStakeAmount =
        stakeAmount.toDoubleOrNull()

    require(
        validStakeAmount != null &&
                validStakeAmount > 0.0
    ) {
        "El monto por participante debe ser mayor a cero."
    }

    require(validStakeAmount <= 100_000.0) {
        "El monto máximo permitido es de $100,000 MXN."
    }

    var validLocationName: String? = null
    var validRadius: Int? = null
    var validTargetLatitude: Double? = null
    var validTargetLongitude: Double? = null
    var validTargetAccuracyMeters: Float? = null

    if (type == BetType.LOCATION) {
        require(locationName.isNotBlank()) {
            "Escribe el lugar de destino."
        }

        val parsedRadius = radius.toIntOrNull()

        require(
            parsedRadius != null &&
                    parsedRadius in 20..500
        ) {
            "El radio debe estar entre 20 y 500 metros."
        }

        val latitude = requireNotNull(
            targetLatitude
        ) {
            "Captura la ubicación objetivo antes de crear la apuesta."
        }

        val longitude = requireNotNull(
            targetLongitude
        ) {
            "Captura la ubicación objetivo antes de crear la apuesta."
        }

        val accuracy = requireNotNull(
            targetAccuracyMeters
        ) {
            "No se pudo determinar la precisión de la ubicación."
        }

        require(latitude in -90.0..90.0) {
            "La latitud obtenida no es válida."
        }

        require(longitude in -180.0..180.0) {
            "La longitud obtenida no es válida."
        }

        validLocationName = locationName.trim()
        validRadius = parsedRadius
        validTargetLatitude = latitude
        validTargetLongitude = longitude
        validTargetAccuracyMeters = accuracy
    }

    return Bet(
        id = UUID.randomUUID().toString(),
        title = title.trim(),
        description = description.trim(),
        type = type,
        startsAt = validStartDateTime,
        endsAt = validEndDateTime,
        locationName = validLocationName,
        radiusMeters = validRadius,
        targetLatitude = validTargetLatitude,
        targetLongitude = validTargetLongitude,
        targetAccuracyMeters = validTargetAccuracyMeters,
        participants = participants.toList(),
        stakePerParticipant = validStakeAmount,
        status = BetStatus.WAITING
    )
}

private fun formatCurrency(
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

@Preview(showBackground = true)
@Composable
private fun CreateBetScreenPreview() {
    BetFriendsTheme {
        CreateBetScreen(
            onBack = {},
            onBetCreated = {}
        )
    }
}