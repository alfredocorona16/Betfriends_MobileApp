package com.betfriends.app.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.betfriends.app.domain.model.Bet
import com.betfriends.app.domain.model.BetStatus
import com.betfriends.app.domain.model.BetType
import com.betfriends.app.domain.model.ParticipantInvitationStatus
import com.betfriends.app.location.CurrentLocationProvider
import com.betfriends.app.location.DeviceLocation
import com.betfriends.app.ui.bets.BetActions
import java.time.format.DateTimeFormatter

@Composable
fun BetCheckInSection(bet: Bet, uid: String, busy: Boolean, actions: BetActions) {
    if (!bet.isOpen) {
        if (bet.status == BetStatus.FINISHED) {
            bet.winnerName?.let { name ->
                val email = bet.checkIns.firstOrNull { it.participantId == bet.winnerId }?.participantEmail
                val label = if (email.isNullOrBlank()) name else "$name ($email)"
                Text("Ganador: $label", style = MaterialTheme.typography.titleMedium)
            }
            if (bet.approvedBy != null) Text("Confirmado por ${bet.moderator?.name ?: "el moderador"}.")
            if (bet.approvedBy != null && !bet.pointsProcessed &&
                (uid == bet.moderatorId || (bet.isAcceptedCompetitor(uid) && uid !in bet.creditedIds))) {
                TextButton(onClick = { actions.syncPoints(bet) }) { Text("Sincronizar puntos") }
            }
        }
        return
    }
    if (bet.needsModerator) {
        if (bet.creatorId == uid) {
            Text("Elige quién moderará este reto.")
            bet.participants.filter { it.invitationStatus != ParticipantInvitationStatus.DECLINED }.forEach { p ->
                TextButton(onClick = { actions.assign(bet.id, p.id) }, enabled = !busy) {
                    Text("Asignar a ${p.name} (${p.email.ifBlank { p.id }})")
                }
            }
        } else Text("El creador debe asignar un moderador.")
        return
    }
    if (bet.moderator?.invitationStatus != ParticipantInvitationStatus.ACCEPTED) {
        Text("Esperando a que el moderador acepte su invitación.")
        return
    }
    if (uid == bet.moderatorId) {
        ReviewArrivals(bet, busy) { actions.approve(bet.id, it) }
    } else if (bet.checkIns.any { it.participantId == uid }) {
        Text("Tu llegada está registrada. El moderador confirmará al ganador.")
    } else if (bet.isAcceptedCompetitor(uid)) {
        ArrivalButton(bet, busy) { location -> actions.checkIn(bet.id, location) }
    }
}

@Composable
private fun ReviewArrivals(bet: Bet, busy: Boolean, onApprove: (String) -> Unit) {
    var selected by rememberSaveable(bet.id) { mutableStateOf<String?>(null) }
    val format = remember { DateTimeFormatter.ofPattern("dd/MM HH:mm:ss") }
    Text("Llegadas para revisar", style = MaterialTheme.typography.titleMedium)
    if (bet.checkIns.isEmpty()) Text("Aún no hay llegadas registradas.")
    bet.checkIns.forEach { arrival ->
        val identity = arrival.participantEmail
        val label = "${arrival.participantName} (${identity.ifBlank { arrival.participantId }})"
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("$label · ${arrival.checkedAt.format(format)}")
            TextButton(onClick = { selected = arrival.participantId }, enabled = !busy) { Text("Elegir ganador") }
        }
        if (selected == arrival.participantId) {
            AlertDialog(onDismissRequest = { selected = null },
                title = { Text("Confirmar ganador") },
                text = { Text("¿Confirmas a $label? El reto se cerrará y se acreditarán los puntos BF.") },
                confirmButton = { TextButton(enabled = !busy, onClick = {
                    selected = null
                    onApprove(arrival.participantId)
                }) { Text("Confirmar ganador") } },
                dismissButton = { TextButton(onClick = { selected = null }) { Text("Volver") } })
        }
    }
}

@Composable
private fun ArrivalButton(bet: Bet, busy: Boolean, onArrival: (DeviceLocation?) -> Unit) {
    val context = LocalContext.current
    val provider = remember(context) { CurrentLocationProvider(context.applicationContext) }
    var finding by remember(bet.id) { mutableStateOf(false) }
    var error by remember(bet.id) { mutableStateOf<String?>(null) }
    val capture = {
        finding = true
        error = null
        provider.getCurrentLocation(
            onSuccess = { location ->
                finding = false
                onArrival(location)
            },
            onError = { message ->
                finding = false
                error = message
            }
        )
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (it.values.any { granted -> granted }) capture() else error = "Permite el acceso a tu ubicación."
    }
    Button(enabled = !busy && !finding, onClick = {
        when {
            bet.type == BetType.TIME -> onArrival(null)
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED -> capture()
            else -> permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }) { Text(if (busy || finding) "Registrando llegada…" else "Registrar mi llegada") }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
}
