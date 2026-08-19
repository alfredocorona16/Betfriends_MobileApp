package com.betfriends.app.ui.bets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.betfriends.app.domain.model.Bet
import com.betfriends.app.domain.model.BetParticipant
import com.betfriends.app.domain.model.BetStatus
import com.betfriends.app.domain.model.BetType
import com.betfriends.app.ui.components.BetCheckInSection
import com.betfriends.app.ui.theme.BetFriendsTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBetsScreen(
    bets: List<Bet>,
    onBetUpdated: (Bet) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Mis apuestas")
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

        if (bets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text =
                        "Todavía no tienes apuestas registradas.",
                    style =
                        MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = bets,
                    key = { bet ->
                        bet.id
                    }
                ) { bet ->
                    BetCard(
                        bet = bet,
                        onBetUpdated = onBetUpdated
                    )
                }
            }
        }
    }
}

@Composable
private fun BetCard(
    bet: Bet,
    onBetUpdated: (Bet) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = bet.title,
                style =
                    MaterialTheme.typography.titleMedium
            )

            Text(
                text = when (bet.type) {
                    BetType.TIME ->
                        "Apuesta por tiempo"

                    BetType.LOCATION ->
                        "Apuesta por ubicación"
                },
                color =
                    MaterialTheme.colorScheme.primary,
                style =
                    MaterialTheme.typography.labelLarge
            )

            if (bet.description.isNotBlank()) {
                Text(
                    text = bet.description,
                    style =
                        MaterialTheme.typography.bodyMedium
                )
            }

            if (bet.participants.isNotEmpty()) {
                Text(
                    text =
                        "Participantes (${bet.participants.size}): ${
                            bet.participants.joinToString {
                                it.name
                            }
                        }",
                    style =
                        MaterialTheme.typography.bodyMedium
                )

                Text(
                    text =
                        "Monto por participante: ${
                            formatBetCurrency(
                                bet.stakePerParticipant
                            )
                        }",
                    style =
                        MaterialTheme.typography.bodyMedium
                )

                Text(
                    text =
                        "Bolsa total: ${
                            formatBetCurrency(
                                bet.totalPrize
                            )
                        }",
                    color =
                        MaterialTheme.colorScheme.primary,
                    style =
                        MaterialTheme.typography.titleSmall
                )
            }

            Text(
                text =
                    "Inicio: ${
                        bet.startsAt.format(
                            dateTimeFormatter
                        )
                    }",
                style =
                    MaterialTheme.typography.bodySmall
            )

            Text(
                text =
                    "Límite: ${
                        bet.endsAt.format(
                            dateTimeFormatter
                        )
                    }",
                style =
                    MaterialTheme.typography.bodySmall
            )

            if (bet.type == BetType.LOCATION) {
                bet.locationName
                    ?.takeIf { it.isNotBlank() }
                    ?.let { locationName ->
                        Text(
                            text = "Lugar: $locationName",
                            style =
                                MaterialTheme.typography.bodySmall
                        )
                    }

                bet.radiusMeters?.let { radiusMeters ->
                    Text(
                        text =
                            "Radio permitido: " +
                                    "$radiusMeters metros",
                        style =
                            MaterialTheme.typography.bodySmall
                    )
                }
            }

            Text(
                text = when (bet.status) {
                    BetStatus.WAITING ->
                        "Estado: Pendiente"

                    BetStatus.ACTIVE ->
                        "Estado: En juego"

                    BetStatus.FINISHED ->
                        "Estado: Finalizada"

                    BetStatus.CANCELLED ->
                        "Estado: Cancelada"
                },
                color = when (bet.status) {
                    BetStatus.WAITING ->
                        MaterialTheme.colorScheme.tertiary

                    BetStatus.ACTIVE ->
                        MaterialTheme.colorScheme.primary

                    BetStatus.FINISHED ->
                        MaterialTheme.colorScheme.onSurfaceVariant

                    BetStatus.CANCELLED ->
                        MaterialTheme.colorScheme.error
                },
                style = MaterialTheme.typography.labelLarge
            )

            if (bet.status != BetStatus.CANCELLED) {
                BetCheckInSection(
                    bet = bet,
                    onBetUpdated = onBetUpdated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }
    }
}

private fun formatBetCurrency(
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
private fun MyBetsScreenPreview() {
    BetFriendsTheme {
        MyBetsScreen(
            bets = listOf(
                Bet(
                    id = "1",
                    title = "¿Quién llegará primero?",
                    description =
                        "El primero en llegar a la universidad gana.",
                    type = BetType.LOCATION,
                    startsAt = LocalDateTime.of(
                        2026,
                        8,
                        18,
                        7,
                        0
                    ),
                    endsAt = LocalDateTime.of(
                        2026,
                        8,
                        18,
                        8,
                        0
                    ),
                    locationName =
                        "Universidad Tecnológica de Tlaxcala",
                    radiusMeters = 100,
                    targetLatitude = 19.3121,
                    targetLongitude = -97.9202,
                    targetAccuracyMeters = 10f,
                    participants = listOf(
                        BetParticipant(
                            id = "participant-1",
                            name = "Ángel"
                        ),
                        BetParticipant(
                            id = "participant-2",
                            name = "Ared"
                        ),
                        BetParticipant(
                            id = "participant-3",
                            name = "Carlos"
                        )
                    ),
                    stakePerParticipant = 50.0
                )
            ),
            onBetUpdated = {},
            onBack = {}
        )
    }
}