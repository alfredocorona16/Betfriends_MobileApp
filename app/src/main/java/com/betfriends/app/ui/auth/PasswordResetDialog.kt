package com.betfriends.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
internal fun PasswordResetDialog(
    initialEmail: String,
    state: PasswordResetState,
    onSend: (String) -> Unit,
    onEdit: () -> Unit,
    onClose: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf(initialEmail) }
    val sending = state == PasswordResetState.Sending
    val sent = state == PasswordResetState.Sent

    AlertDialog(
        onDismissRequest = { if (!sending) onClose() },
        title = { Text("Restablecer contraseña") },
        text = {
            Column(
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (sent) {
                    Text("Si existe una cuenta con ese correo, recibirás un enlace para restablecer tu contraseña.")
                } else {
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            if (state is PasswordResetState.Error) onEdit()
                        },
                        label = { Text("Correo electrónico") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        enabled = !sending,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (sending) CircularProgressIndicator()
                    if (state is PasswordResetState.Error) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (sent) onClose() else onSend(email) },
                enabled = !sending
            ) {
                Text(if (sent) "Volver al inicio de sesión" else "Enviar enlace")
            }
        },
        dismissButton = {
            if (!sent) TextButton(onClick = onClose, enabled = !sending) { Text("Cancelar") }
        }
    )
}
