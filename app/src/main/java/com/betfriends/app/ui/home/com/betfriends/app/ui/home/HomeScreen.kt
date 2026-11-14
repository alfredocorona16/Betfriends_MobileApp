package com.betfriends.app.ui.home

@Composable
fun HomeScreen(
    user: UserProfile,
    bets: List<Bet>,
    onCreateBet: () -> Unit,
    pendingInvitations: Int,
    onViewBets: () -> Unit,
    onEditProfile: () -> Unit,
    onViewPoints: () -> Unit,
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
            pendingInvitations = pendingInvitations,
            onViewInvitations = onViewBets,
            onEditProfile = onEditProfile,
            onLogout = onLogout
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        PointsCard(
            points = user.puntos,
            onClick = onViewPoints
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
    pendingInvitations: Int,
    onViewInvitations: () -> Unit,
    onEditProfile: () -> Unit,
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
                onClick = onViewInvitations,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.surface
                    )
            ) {
                BadgedBox(
                    badge = {
                        if (pendingInvitations > 0) {
                            Badge {
                                Text(pendingInvitations.toString())
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector =
                            Icons.Default.Notifications,
                        contentDescription =
                            if (pendingInvitations > 0) {
                                "Tienes $pendingInvitations " +
                                        "invitaciones pendientes"
                            } else {
                                "Notificaciones"
                            },
                        tint =
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            IconButton(
                onClick = onEditProfile,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.surface
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription =
                        "Editar perfil",
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
private fun PointsCard(
    points: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
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
                text = "Mis puntos",
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
                text = "$points pts",
                color =
                    MaterialTheme.colorScheme.onPrimary,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Toca para ver tu historial",
                color = MaterialTheme
                    .colorScheme
                    .onPrimary
                    .copy(alpha = 0.8f),
                fontSize = 13.sp
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
                puntos = 120
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
            pendingInvitations = 2,
            onEditProfile = {},
            onViewPoints = {},
            onLogout = {}
        )
    }
}