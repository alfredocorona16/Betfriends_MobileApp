package com.betfriends.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.betfriends.app.domain.model.Bet
import com.betfriends.app.domain.model.BetStatus
import com.betfriends.app.domain.model.BetType
import com.betfriends.app.domain.model.UserProfile
import com.betfriends.app.ui.theme.BetFriendsTheme
import java.text.NumberFormat
import java.time.LocalDateTime
import java.util.Locale

@Composable
fun HomeScreen(
    user: UserProfile,
    bets: List<Bet>,
    onCreateBet: () -> Unit,
    onViewBets: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.background
            )
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        UserHeader(
            name = user.nombre,
            onLogout = onLogout
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        BalanceCard(
            balance = user.saldo
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        QuickActions(
            onCreateBet = onCreateBet,
            onViewBets = onViewBets
        )

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        Text(
            text = "Mis Apuestas Activas",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(
                bottom = 16.dp
            )
        )

        ActiveBetsSection(
            bets = bets,
            onViewBets = onViewBets
        )
    }
}

@Composable
private fun UserHeader(
    name: String,
    onLogout: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Hola,",
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp
            )

            Text(
                text = name.ifBlank {
                    "Usuario"
                },
                color =
                    MaterialTheme.colorScheme.onBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = {
                    // Las notificaciones se implementarán después.
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.surface
                    )
            ) {
                Icon(
                    imageVector =
                        Icons.Default.Notifications,
                    contentDescription =
                        "Notificaciones",
                    tint =
                        MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                onClick = onLogout,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.primary
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription =
                        "Cerrar sesión",
                    tint =
                        MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun BalanceCard(
    balance: Double
) {
    val formattedBalance = remember(balance) {
        NumberFormat
            .getCurrencyInstance(
                Locale("es", "MX")
            )
            .format(balance)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.primary
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Saldo Disponible",
                color = MaterialTheme
                    .colorScheme
                    .onPrimary
                    .copy(alpha = 0.8f),
                fontSize = 16.sp
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = formattedBalance,
                color =
                    MaterialTheme.colorScheme.onPrimary,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun QuickActions(
    onCreateBet: () -> Unit,
    onViewBets: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {
        QuickActionButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Casino,
            label = "Apuesta",
            enabled = true,
            onClick = onCreateBet
        )

        QuickActionButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Add,
            label = "Recargar",
            enabled = false,
            onClick = {}
        )

        QuickActionButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.AttachMoney,
            label = "Retirar",
            enabled = false,
            onClick = {}
        )

        QuickActionButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.List,
            label = "Historial",
            enabled = true,
            onClick = onViewBets
        )
    }
}

@Composable
private fun QuickActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val contentAlpha = if (enabled) {
        1f
    } else {
        0.45f
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.surface
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme
                    .colorScheme
                    .primary
                    .copy(alpha = contentAlpha),
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = label,
            color = MaterialTheme
                .colorScheme
                .onBackground
                .copy(alpha = contentAlpha),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun ActiveBetsSection(
    bets: List<Bet>,
    onViewBets: () -> Unit
) {
    val activeBets = bets.filter { bet ->
        bet.status == BetStatus.WAITING ||
                bet.status == BetStatus.ACTIVE
    }.take(3)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onViewBets
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color =
                MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            if (activeBets.isEmpty()) {
                Text(
                    text = "No tienes apuestas activas.",
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    style =
                        MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(
                        vertical = 16.dp
                    )
                )

                Text(
                    text = "Presiona “Apuesta” para crear la primera.",
                    color =
                        MaterialTheme.colorScheme.primary,
                    style =
                        MaterialTheme.typography.bodySmall
                )
            } else {
                activeBets.forEachIndexed { index, bet ->
                    ActiveBetRow(
                        bet = bet
                    )

                    if (index < activeBets.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(
                                vertical = 12.dp
                            ),
                            color =
                                MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveBetRow(
    bet: Bet
) {
    val statusText = when (bet.status) {
        BetStatus.WAITING -> "Pendiente"
        BetStatus.ACTIVE -> "En juego"
        BetStatus.FINISHED -> "Finalizada"
        BetStatus.CANCELLED -> "Cancelada"
    }

    val statusColor = when (bet.status) {
        BetStatus.WAITING -> Color(0xFFFFEB3B)
        BetStatus.ACTIVE ->
            MaterialTheme.colorScheme.primary

        BetStatus.FINISHED ->
            MaterialTheme.colorScheme.secondary

        BetStatus.CANCELLED ->
            MaterialTheme.colorScheme.error
    }

    val betType = when (bet.type) {
        BetType.TIME -> "Apuesta por tiempo"
        BetType.LOCATION -> "Apuesta por ubicación"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = bet.title,
                color =
                    MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = bet.description.ifBlank {
                    betType
                },
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1
            )
        }

        Text(
            text = statusText,
            color = statusColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(
                start = 12.dp
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    BetFriendsTheme {
        HomeScreen(
            user = UserProfile(
                uid = "1",
                nombre = "Alfredo",
                correo = "alfredo@example.com",
                saldo = 1000.0
            ),
            bets = listOf(
                Bet(
                    id = "1",
                    title = "¿Quién llegará primero?",
                    description =
                        "El primero en llegar gana.",
                    type = BetType.LOCATION,
                    startsAt = LocalDateTime.of(
                        2026,
                        8,
                        19,
                        8,
                        0
                    ),
                    endsAt = LocalDateTime.of(
                        2026,
                        8,
                        19,
                        9,
                        0
                    ),
                    locationName = "UTT",
                    radiusMeters = 100,
                    status = BetStatus.ACTIVE
                )
            ),
            onCreateBet = {},
            onViewBets = {},
            onLogout = {}
        )
    }
}