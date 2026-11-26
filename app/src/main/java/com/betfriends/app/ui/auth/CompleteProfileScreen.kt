package com.betfriends.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun CompleteProfileScreen(
    pending: PendingProfile,
    isSaving: Boolean,
    error: String?,
    onSave: (String) -> Unit,
    onLogout: () -> Unit
) {
    var nombre by rememberSaveable(pending.uid) { mutableStateOf(pending.suggestedName) }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Completa tu perfil", style = MaterialTheme.typography.headlineMedium)
            Text("Tu cuenta ya existe. Guarda tu nombre para entrar a Challenge Friends.")
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre") },
                singleLine = true,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            )
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            if (isSaving) CircularProgressIndicator()
            Button(onClick = { onSave(nombre) }, enabled = !isSaving && nombre.isNotBlank()) {
                Text("Guardar perfil")
            }
            TextButton(onClick = onLogout, enabled = !isSaving) {
                Text("Cerrar sesión")
            }
        }
    }
}
