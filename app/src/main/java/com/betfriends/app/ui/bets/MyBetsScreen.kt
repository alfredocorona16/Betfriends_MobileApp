package com.betfriends.app.ui.bets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.betfriends.app.domain.model.Bet
import com.betfriends.app.domain.model.BetInvitation
import com.betfriends.app.domain.model.BetStatus
import com.betfriends.app.domain.model.BetType
import com.betfriends.app.domain.model.ParticipantInvitationStatus
import com.betfriends.app.ui.components.BetCheckInSection
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBetsScreen(
    currentUserId: String,
    bets: List<Bet>,
    invitations: List<BetInvitation>,
    processingInvitationId: String?,
    message: String?,
    onAcceptInvitation: (BetInvitation) -> Unit,
    onDeclineInvitation: (BetInvitation) -> Unit,
    onBetUpdated: (Bet) -> Unit,
    onBack: () -> Unit
) {
    val visibleBets = bets.filter { bet ->
        bet.creatorId == currentUserId ||
                bet.participants.firstOrNull {
                    it.id == currentUserId
                }?.invitationStatus !=
                ParticipantInvitationStatus.DECLINED
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis apuestas") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (visibleBets.isEmpty() && invitations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Todavía no tienes apuestas ni invitaciones.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (invitations.isNotEmpty()) {
                    item(key = "invitations-title") {
                        Text(
                            text = "Invitaciones pendientes",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    items(
                        items = invitations,
                        key = { "invitation-${it.id}" }
                    ) { invitation ->
                        InvitationCard(
                            invitation = invitation,
                            isProcessing =
                                processingInvitationId == invitation.id,
                            onAccept = {
                                onAcceptInvitation(invitation)
                            },
                            onDecline = {
                                onDeclineInvitation(invitation)
                            }
                        )
                    }
                }

                message?.let { currentMessage ->
                    item(key = "bets-message") {
                        Text(
                            text = currentMessage,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (visibleBets.isNotEmpty()) {
                    item(key = "bets-title") {
                        Text(
                            text = "Apuestas",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(
                        items = visibleBets,
                        key = { "bet-${it.id}" }
                    ) { bet ->
                        BetCard(
                            bet = bet,
                            currentUserId = currentUserId,
                            onBetUpdated = onBetUpdated
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InvitationCard(
    invitation: BetInvitation,
    isProcessing: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = invitation.betTitle,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Invitación de ${invitation.inviterName}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Monto al aceptar: ${formatBetCurrency(invitation.stakeAmount)}",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleSmall
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Rechazar")
                }

                Button(
                    onClick = onAccept,
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isProcessing) "Procesando" else "Aceptar")
                }
            }
        }
    }
}

@Composable
private fun BetCard(
    bet: Bet,
    currentUserId: String,
    onBetUpdated: (Bet) -> Unit
) {
    val currentParticipant = bet.participants.firstOrNull {
        it.id == currentUserId
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = bet.title,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = when (bet.type) {
                    BetType.TIME -> "Apuesta por tiempo"
                    BetType.LOCATION -> "Apuesta por ubicación"
                },
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )

            if (bet.creatorName.isNotBlank()) {
                Text(
                    text = "Creada por ${bet.creatorName}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (bet.description.isNotBlank()) {
                Text(
                    text = bet.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (bet.participants.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = "Participantes",
                    style = MaterialTheme.typography.titleSmall
                )

                bet.participants.forEach { participant ->
                    val statusLabel = when (
                        participant.invitationStatus
                    ) {
                        ParticipantInvitationStatus.PENDING ->
                            "Pendiente"
                        ParticipantInvitationStatus.ACCEPTED ->
                            "Aceptó"
                        ParticipantInvitationStatus.DECLINED ->
                            "Rechazó"
                    }

                    Text(
                        text = "• ${participant.name}: $statusLabel",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    text = "Monto por participante: ${formatBetCurrency(bet.stakePerParticipant)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Bolsa confirmada: ${formatBetCurrency(bet.totalPrize)}",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Bolsa esperada: ${formatBetCurrency(bet.expectedPrize)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = "Inicio: ${bet.startsAt.format(dateTimeFormatter)}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Límite: ${bet.endsAt.format(dateTimeFormatter)}",
                style = MaterialTheme.typography.bodySmall
            )

            if (bet.type == BetType.LOCATION) {
                bet.locationName
                    ?.takeIf { it.isNotBlank() }
                    ?.let { locationName ->
                        Text(
                            text = "Lugar: $locationName",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                bet.radiusMeters?.let { radiusMeters ->
                    Text(
                        text = "Radio permitido: $radiusMeters metros",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Text(
                text = when (bet.status) {
                    BetStatus.WAITING -> "Estado: Pendiente"
                    BetStatus.ACTIVE -> "Estado: En juego"
                    BetStatus.FINISHED -> "Estado: Finalizada"
                    BetStatus.CANCELLED -> "Estado: Cancelada"
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

            when {
                currentParticipant?.invitationStatus ==
                        ParticipantInvitationStatus.PENDING -> {
                    Text(
                        text = "Acepta la invitación para participar y hacer check-in.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                currentParticipant?.invitationStatus ==
                        ParticipantInvitationStatus.ACCEPTED &&
                        bet.status != BetStatus.CANCELLED -> {
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
}

private fun formatBetCurrency(amount: Double): String {
    return "${'$'}${String.format(Locale.US, "%.2f", amount)} MXN"
}
