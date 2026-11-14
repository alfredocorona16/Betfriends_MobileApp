package com.betfriends.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute : NavKey

@Serializable
data object Splash : AppRoute

@Serializable
data object Login : AppRoute

@Serializable
data object Register : AppRoute

@Serializable
data object Home : AppRoute

@Serializable
data object CreateBet : AppRoute

@Serializable
data object MyBets : AppRoute