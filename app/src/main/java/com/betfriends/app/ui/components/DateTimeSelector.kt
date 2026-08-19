package com.betfriends.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val dateFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy")

private val timeFormatter =
    DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeSelector(
    title: String,
    value: LocalDateTime?,
    onValueChange: (LocalDateTime) -> Unit
) {
    var showDatePicker by rememberSaveable {
        mutableStateOf(false)
    }

    var showTimePicker by rememberSaveable {
        mutableStateOf(false)
    }

    val defaultValue = value
        ?: LocalDateTime.now()
            .withSecond(0)
            .withNano(0)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedButton(
            onClick = {
                showDatePicker = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (value == null) {
                    "Seleccionar fecha"
                } else {
                    "Fecha: ${value.format(dateFormatter)}"
                }
            )
        }

        OutlinedButton(
            onClick = {
                showTimePicker = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (value == null) {
                    "Seleccionar hora"
                } else {
                    "Hora: ${value.format(timeFormatter)}"
                }
            )
        }
    }

    if (showDatePicker) {
        BetDatePickerDialog(
            initialDate = defaultValue.toLocalDate(),
            onDismiss = {
                showDatePicker = false
            },
            onConfirm = { selectedDate ->
                onValueChange(
                    LocalDateTime.of(
                        selectedDate,
                        defaultValue.toLocalTime()
                    )
                )

                showDatePicker = false
            }
        )
    }

    if (showTimePicker) {
        BetTimePickerDialog(
            initialTime = defaultValue.toLocalTime(),
            onDismiss = {
                showTimePicker = false
            },
            onConfirm = { selectedTime ->
                onValueChange(
                    LocalDateTime.of(
                        defaultValue.toLocalDate(),
                        selectedTime
                    )
                )

                showTimePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BetDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    val initialDateMillis = remember(initialDate) {
        initialDate
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedMillis =
                        datePickerState.selectedDateMillis

                    if (selectedMillis != null) {
                        val selectedDate = Instant
                            .ofEpochMilli(selectedMillis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()

                        onConfirm(selectedDate)
                    }
                },
                enabled =
                    datePickerState.selectedDateMillis != null
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancelar")
            }
        }
    ) {
        DatePicker(
            state = datePickerState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BetTimePickerDialog(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Seleccionar hora")
        },
        text = {
            TimePicker(
                state = timePickerState
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        LocalTime.of(
                            timePickerState.hour,
                            timePickerState.minute
                        )
                    )
                }
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancelar")
            }
        }
    )
}