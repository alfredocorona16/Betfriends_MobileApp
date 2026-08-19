package com.betfriends.app.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.betfriends.app.ui.theme.BetFriendsTheme

@Composable
fun RegisterScreen(
    uiState: AuthUiState,
    onRegister: (
        nombre: String,
        correo: String,
        password: String
    ) -> Unit,
    onBackToLogin: () -> Unit
) {
    var nombre by rememberSaveable {
        mutableStateOf("")
    }

    var correo by rememberSaveable {
        mutableStateOf("")
    }

    var password by rememberSaveable {
        mutableStateOf("")
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            AuthBrandHeader()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            horizontal = 16.dp,
                            vertical = 32.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 480.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 8.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment =
                                Alignment.CenterHorizontally,
                            verticalArrangement =
                                Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Crear cuenta",
                                color =
                                    MaterialTheme.colorScheme.onSurface,
                                style =
                                    MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "Regístrate para crear apuestas con tus amigos.",
                                color =
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                style =
                                    MaterialTheme.typography.bodyMedium
                            )

                            OutlinedTextField(
                                value = nombre,
                                onValueChange = {
                                    nombre = it
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = {
                                    Text("Nombre completo")
                                },
                                singleLine = true,
                                enabled = !uiState.isLoading,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType =
                                        KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                )
                            )

                            OutlinedTextField(
                                value = correo,
                                onValueChange = {
                                    correo = it
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = {
                                    Text("Correo electrónico")
                                },
                                singleLine = true,
                                enabled = !uiState.isLoading,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType =
                                        KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                )
                            )

                            OutlinedTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = {
                                    Text("Contraseña")
                                },
                                supportingText = {
                                    Text(
                                        "Debe contener al menos 6 caracteres."
                                    )
                                },
                                singleLine = true,
                                enabled = !uiState.isLoading,
                                visualTransformation =
                                    PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType =
                                        KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (!uiState.isLoading) {
                                            onRegister(
                                                nombre,
                                                correo,
                                                password
                                            )
                                        }
                                    }
                                )
                            )

                            uiState.errorMessage?.let { message ->
                                Text(
                                    text = message,
                                    modifier =
                                        Modifier.fillMaxWidth(),
                                    color =
                                        MaterialTheme.colorScheme.error,
                                    style =
                                        MaterialTheme.typography.bodySmall
                                )
                            }

                            Button(
                                onClick = {
                                    onRegister(
                                        nombre,
                                        correo,
                                        password
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor =
                                        MaterialTheme.colorScheme.primary,
                                    contentColor =
                                        MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color =
                                            MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Crear cuenta")
                                }
                            }

                            TextButton(
                                onClick = onBackToLogin,
                                enabled = !uiState.isLoading
                            ) {
                                Text("Ya tengo una cuenta")
                            }
                        }
                    }

                    Spacer(
                        modifier = Modifier.padding(
                            bottom = 16.dp
                        )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RegisterScreenPreview() {
    BetFriendsTheme {
        RegisterScreen(
            uiState = AuthUiState(),
            onRegister = { _, _, _ -> },
            onBackToLogin = {}
        )
    }
}