package com.example.betfriends_mobilapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen() {
    val nombreUsuario = "Alfredo"
    val saldoActual = 1000.00

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(red = 18, green = 18, blue = 18))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 1. EL ENCABEZADO
        HeaderUsuario(nombre = nombreUsuario)

        Spacer(modifier = Modifier.height(24.dp))

        TarjetaSaldo(saldo = saldoActual)

        Spacer(modifier = Modifier.height(24.dp))

        BotonesAccionRapida()

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Mis Apuestas Activas",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        SeccionApuestasActivas()
    }
}

@Composable
fun HeaderUsuario(nombre: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Hola,",
                color = Color.LightGray,
                fontSize = 16.sp
            )
            Text(
                text = nombre,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            IconButton(
                onClick = { },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(30, 30, 30))
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notificaciones",
                    tint = Color.White
                )
            }

            IconButton(
                onClick = { /* Abrir perfil */ },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(red = 170, green = 50, blue = 225))
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Perfil",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun TarjetaSaldo(saldo: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(red = 170, green = 50, blue = 225)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Saldo Disponible",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$${saldo}",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun BotonesAccionRapida() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = { /* Acción recargar */ },
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color(30, 30, 30))
            ) {
                Icon(Icons.Default.Casino, contentDescription = "Apuesta", tint = Color(170, 50, 225), modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Apuesta", color = Color.White, fontSize = 14.sp)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = { /* Acción recargar */ },
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color(30, 30, 30))
            ) {
                Icon(Icons.Default.Add, contentDescription = "Recargar", tint = Color(170, 50, 225), modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Recargar", color = Color.White, fontSize = 14.sp)
        }

        // Botón Retirar
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = { /* Acción retirar */ },
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color(30, 30, 30))
            ) {
                Text("$", color = Color(170, 50, 225), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Retirar", color = Color.White, fontSize = 14.sp)
        }

        // Botón Historial
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = { /* Acción historial */ },
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color(30, 30, 30))
            ) {
                Icon(Icons.Default.List, contentDescription = "Historial", tint = Color(170, 50, 225), modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Historial", color = Color.White, fontSize = 14.sp)
        }
    }
}

@Composable
fun SeccionApuestasActivas() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(30, 30, 30)),
        border = BorderStroke(1.dp, Color.DarkGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Ejemplo de Apuesta 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("América vs Chivas", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Gana América", color = Color.Gray, fontSize = 12.sp)
                }
                Text("En juego", color = Color(170, 50, 225), fontWeight = FontWeight.Bold)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.DarkGray)

            // Ejemplo de Apuesta 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Checo Pérez - Podio", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Top 3", color = Color.Gray, fontSize = 12.sp)
                }
                Text("Pendiente", color = Color.Yellow, fontWeight = FontWeight.Bold)
            }
        }
    }
}