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
import com.betfriends.app.domain.rules.PointsRules
import com.betfriends.app.domain.model.Bet
import com.betfriends.app.domain.model.BetInvitation
import com.betfriends.app.domain.model.BetStatus
import com.betfriends.app.domain.model.BetType
import com.betfriends.app.domain.model.ParticipantInvitationStatus
import com.betfriends.app.ui.components.BetCheckInSection
import com.betfriends.app.location.DeviceLocation
import java.time.format.DateTimeFormatter

private val dateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

class BetActions(
    val checkIn: (String, DeviceLocation?) -> Unit,
    val assign: (String, String) -> Unit,
    val approve: (String, String) -> Unit,
    val syncPoints: (Bet) -> Unit
)

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
    actions: BetActions,
    processingBets: Set<String>,
    onBack: () -> Unit
) {
    val visibleBets = bets.filter { bet ->
        bet.creatorId == currentUserId || bet.participants.any {
            it.id == currentUserId && it.invitationStatus != ParticipantInvitationStatus.DECLINED
        }
    }
    val betsById = visibleBets.associateBy { it.id }
    val pendingInvitations = invitations.filter {
        it.inviteeId == currentUserId && betsById[it.betId]?.isOpen != false
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
        if (visibleBets.isEmpty() && pendingInvitations.isEmpty()) {
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
                if (pendingInvitations.isNotEmpty()) {
                    item(key = "invitations-title") {
                        Text(
                            text = "Invitaciones pendientes",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    items(
                        items = pendingInvitations,
                        key = { "invitation-${it.id}" }
                    ) { invitation ->
                        InvitationCard(
                            invitation = invitation,
                            moderatorId = betsById[invitation.betId]?.moderatorId,
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
                            actions = actions,
                            busy = bet.id in processingBets
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
    moderatorId: String?,
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
                text = when (moderatorId) {
                    invitation.inviteeId -> "Te invitan como moderador para confirmar al ganador."
                    null -> "Consulta el reto para ver el rol asignado."
                    else -> "Te invitan como competidor. Registra tu propia llegada."
                },
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
    actions: BetActions,
    busy: Boolean
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
                        text = if (participant.id == bet.moderatorId) "${participant.name} · Moderador · $statusLabel"
                            else "${participant.name} · $statusLabel",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    text = "Puntos: ${PointsRules.PARTICIPATION_POINTS} por participar, " +
                            "+${PointsRules.VICTORY_BONUS_POINTS} por ganar",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleSmall
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
                text = if (bet.isOpen && bet.checkIns.isNotEmpty()) "Pendiente de confirmación" else when (bet.status) {
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

            if (currentParticipant?.invitationStatus == ParticipantInvitationStatus.PENDING && bet.isOpen) {
                Text("Acepta la invitación para ejercer tu rol.")
            } else if (!bet.isOpen || currentParticipant?.invitationStatus == ParticipantInvitationStatus.ACCEPTED) {
                BetCheckInSection(bet, currentUserId, busy, actions)
            }
        }
    }
}
