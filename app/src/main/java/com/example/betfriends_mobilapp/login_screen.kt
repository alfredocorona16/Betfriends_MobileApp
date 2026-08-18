package com.example.betfriends_mobilapp

import android.graphics.ColorSpace
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.graphics.Vertices
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.scale
import kotlinx.coroutines.delay
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.platform.LocalContext

@Composable
fun SplashScreenAnimado(alTerminar: () -> Unit) {
    val escala = remember { Animatable(0f) }


    LaunchedEffect(key1 = true) {

        escala.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )

        delay(1500L)
        alTerminar()
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(red = 18, green = 18, blue = 18)),
        contentAlignment = Alignment.Center
    ) {

        Row(modifier = Modifier.scale(escala.value)) {
            Text(
                text = "Bet",
                color = Color.White,
                style = MaterialTheme.typography.displayMedium
            )
            Text(
                text = "Friends",
                color = Color(red = 170, green = 50, blue = 255),
                style = MaterialTheme.typography.displayMedium
            )
        }
    }
}

val auth = FirebaseAuth.getInstance()
val db = FirebaseFirestore.getInstance()

fun registrarUsuario(correo: String, password: String, nombre: String, onExito: () -> Unit, onError: (String) -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    auth.createUserWithEmailAndPassword(correo, password)
        .addOnSuccessListener { resultadoAuth ->
            val userId = resultadoAuth.user?.uid

            if (userId != null) {
                val datosUsuario = hashMapOf(
                    "nombre" to nombre,
                    "correo" to correo,
                    "saldo" to 1000,
                    "fechaCreacion" to com.google.firebase.Timestamp.now()
                )

                db.collection("usuarios").document(userId)
                    .set(datosUsuario)
                    .addOnSuccessListener {
                        onExito() // Llamamos al éxito
                    }
                    .addOnFailureListener { error ->
                        onError("Error al guardar perfil: ${error.message}")
                    }
            }
        }
        .addOnFailureListener { error ->
            onError("Error en Auth al registrar: ${error.message}")
        }
}

fun iniciarSesion(correo: String, password: String, onExito: (String) -> Unit, onError: (String) -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    auth.signInWithEmailAndPassword(correo, password)
        .addOnSuccessListener { resultadoAuth ->
            val userId = resultadoAuth.user?.uid

            if (userId != null) {
                // 2. Conectar con Firestore buscando el documento con ese UID
                db.collection("usuarios").document(userId)
                    .get()
                    .addOnSuccessListener { documento ->
                        if (documento.exists()) {
                            val nombre = documento.getString("nombre") ?: "Usuario"
                            onExito(nombre)
                        } else {
                            onError("El usuario existe en Auth pero no tiene perfil en Firestore")
                        }
                    }
                    .addOnFailureListener { error ->
                        onError("Error al consultar la base de datos: ${error.message}")
                    }
            }
        }
        .addOnFailureListener { error ->
            onError("Credenciales incorrectas: ${error.message}")
        }
}
@Composable

fun LoginScreen(onLoginSuccess: () -> Unit) {
    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var mensajeError by remember { mutableStateOf("") }
    var nombreUsuario by remember { mutableStateOf("") }
    var tipoFormulario by remember { mutableStateOf("NINGUNO") }
    var nombreRegistro by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    8.dp,
                    ambientColor = Color(red = 170, green = 50, blue = 255),
                    spotColor = Color(red = 170, green = 50, blue = 255)
                )
                .background(Color(red = 30, green = 30, blue = 30))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Bet", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text(
                "Friends",
                color = Color(red = 170, green = 50, blue = 225),
                style = MaterialTheme.typography.titleLarge
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .background(Color(red = 18, green = 18, blue = 18))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { tipoFormulario = "NINGUNO" })
                },
            contentAlignment = Alignment.Center

        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 32.dp)
            ) {

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(
                            red = 30,
                            green = 30,
                            blue = 30
                        )
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    border = BorderStroke(
                        width = 2.dp, Color(
                            red = 170,
                            green = 50,
                            blue = 225
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(animationSpec = tween(durationMillis = 500))
                            .padding(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = if (nombreUsuario.isEmpty()) "Bienvenido" else "Bienvenido, $nombreUsuario",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,

                            )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (tipoFormulario == "NINGUNO") {
                            Button(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                onClick = { tipoFormulario = "LOGIN" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        red = 170,
                                        green = 50,
                                        blue = 225
                                    )
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Entrar", color = Color.White)
                            }

                        }


                    }
                    AnimatedVisibility(
                        visible = tipoFormulario == "LOGIN",
                        enter = expandVertically() + fadeIn()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {


                            OutlinedTextField(
                                value = correo,
                                onValueChange = { correo = it },
                                label = { Text("Correo Electronico") },
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(
                                        red = 170,
                                        green = 50,
                                        blue = 225
                                    ),
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedLabelColor = Color(
                                        red = 170,
                                        green = 50,
                                        blue = 225
                                    ),
                                    unfocusedLabelColor = Color.LightGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color.White
                                )

                            )

                            Spacer(modifier = Modifier.height(8.dp))


                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Contraseña") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(red = 170, green = 50, blue = 225),
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedLabelColor = Color(red = 170, green = 50, blue = 225),
                                    unfocusedLabelColor = Color.LightGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))


                            Text(
                                text = "¿Olvidaste tu contraseña?",
                                color = Color(red = 170, green = 50, blue = 225),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp)
                                    .clickable {
                                        mensajeError = "Próximamente: Recuperar contraseña"
                                    },
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (mensajeError.isNotEmpty()) {
                                Text(
                                    text = mensajeError,
                                    color = Color(red = 170, green = 50, blue = 225)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (correo.isNotEmpty() && password.isNotEmpty()) {
                                        println("Intentando ingresar con $correo")
                                        mensajeError = ""

                                        val auth = FirebaseAuth.getInstance()
                                        val db = FirebaseFirestore.getInstance()

                                        auth.signInWithEmailAndPassword(correo, password)
                                            .addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    val userId = auth.currentUser?.uid

                                                    if (userId != null)
                                                        db.collection("usuarios").document(userId)
                                                            .get()
                                                            .addOnSuccessListener { documento ->
                                                                val nombreReal =
                                                                    documento.getString("nombre")
                                                                        ?: "Amigo"

                                                                nombreUsuario = nombreReal

                                                                correo = ""
                                                                password = ""
                                                                tipoFormulario = "NINGUNO"

                                                                onLoginSuccess()
                                                            }
                                                            .addOnFailureListener {
                                                                mensajeError =
                                                                    "Error al descargar el perfil"
                                                            }
                                                } else {
                                                    mensajeError =
                                                        "Credenciales incorrectas o error de red"
                                                }
                                            }

                                    } else {
                                        mensajeError = "Por Favor, llena todos los Campos"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 16.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(red = 170, green = 50, blue = 225),
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Inicias sesión")
                            }

                        }
                    }

                    Text(
                        text = "Registrate",
                        color = Color(red = 170, green = 50, blue = 225),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                            .clickable { tipoFormulario = "REGISTRO" },
                        textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                    )

                    AnimatedVisibility(
                        visible = tipoFormulario == "REGISTRO",
                        enter = expandVertically() + fadeIn()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            OutlinedTextField(
                                value = nombreRegistro,
                                onValueChange = { nombreRegistro = it },
                                label = { Text("Tu Nombre Completo") },
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(red = 170, green = 50, blue = 225),
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedLabelColor = Color(red = 170, green = 50, blue = 225),
                                    unfocusedLabelColor = Color.LightGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color.White
                                )
                            )

                            OutlinedTextField(
                                value = correo,
                                onValueChange = { correo = it },
                                label = { Text("Correo Electrónico") },
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(red = 170, green = 50, blue = 225),
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedLabelColor = Color(red = 170, green = 50, blue = 225),
                                    unfocusedLabelColor = Color.LightGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color.White
                                )
                            )

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Crea una Contraseña") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(red = 170, green = 50, blue = 225),
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedLabelColor = Color(red = 170, green = 50, blue = 225),
                                    unfocusedLabelColor = Color.LightGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (mensajeError.isNotEmpty()) {
                                Text(
                                    text = mensajeError,
                                    color = Color(red = 170, green = 50, blue = 225),
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }

                            Button(
                                onClick = {
                                    if (correo.isNotEmpty() && password.isNotEmpty() && nombreRegistro.isNotEmpty()) {
                                        mensajeError = "Registrando..."
                                        registrarUsuario(
                                            correo = correo,
                                            password = password,
                                            nombre = nombreRegistro,
                                            onExito = {
                                                mensajeError =
                                                    "¡Registro exitoso! Por favor, inicia sesión."
                                                tipoFormulario =
                                                    "LOGIN" // Cambia automáticamente a la pantalla de login
                                            },
                                            onError = { errorRecibido ->
                                                mensajeError = errorRecibido
                                            }
                                        )
                                    } else {
                                        mensajeError = "Por favor llena todos los campos"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 16.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        red = 170,
                                        green = 50,
                                        blue = 225
                                    ), contentColor = Color.White
                                )
                            ) {
                                Text("Crear Cuenta")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.Start
                    )
                     {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = { tipoFormulario = "NINGUNO" },
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color(red = 170, green = 50, blue = 225))
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Casino,
                                    contentDescription = "Apuesta",
                                    tint = Color(30, 30, 30),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Apuesta", color = Color.White, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(
                            red = 30,
                            green = 30,
                            blue = 30
                        )
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    border = BorderStroke(
                        width = 2.dp, Color(
                            red = 170,
                            green = 50,
                            blue = 225
                        )
                    )
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Apuesta en curso",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(24.dp)
                        )
                        Text(
                            text = "Tipo",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(24.dp)

                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = .12.dp),

                        ) {
                        val apuestas = listOf(
                            Pair("Quien llega primero a la uni", "Tiempo"),
                            Pair("América vs Chivas", "Fútbol - Liga MX"),
                            Pair("Checo Pérez - Podio", "Fórmula 1"),
                            Pair("Lakers vs Warriors", "Básquetbol - NBA")
                        )

                        apuestas.forEach { apuesta ->


                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically, // Centra los textos a la misma altura
                                horizontalArrangement = Arrangement.SpaceBetween // Manda uno a la izquierda y otro a la derecha
                            ) {
                                Text(
                                    text = apuesta.first, // El nombre (América vs Chivas)
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .clickable { tipoFormulario = "LOGIN" }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = apuesta.second,
                                    color = Color(red = 170, green = 50, blue = 225),
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .clickable { tipoFormulario = "LOGIN" }
                                )
                            }
                            HorizontalDivider(color = Color.DarkGray, thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }
}