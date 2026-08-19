package com.betfriends.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.betfriends.app.data.auth.FirebaseAuthRepository
import com.betfriends.app.domain.model.Bet
import com.betfriends.app.ui.auth.AuthViewModel
import com.betfriends.app.ui.auth.AuthViewModelFactory
import com.betfriends.app.ui.auth.LoginScreen
import com.betfriends.app.ui.auth.RegisterScreen
import com.betfriends.app.ui.auth.SplashScreen
import com.betfriends.app.ui.bets.MyBetsScreen
import com.betfriends.app.ui.createbet.CreateBetScreen
import com.betfriends.app.ui.home.HomeScreen

@Composable
fun BetFriendsApp() {
    val backStack = rememberNavBackStack(
        Splash
    )

    val bets = remember {
        mutableStateListOf<Bet>()
    }

    var splashFinished by rememberSaveable {
        mutableStateOf(false)
    }

    val authRepository = remember {
        FirebaseAuthRepository()
    }

    val authViewModelFactory = remember(
        authRepository
    ) {
        AuthViewModelFactory(
            authRepository = authRepository
        )
    }

    val authViewModel: AuthViewModel = viewModel(
        factory = authViewModelFactory
    )

    val authState = authViewModel.uiState
    val currentRoute = backStack.lastOrNull()

    /*
     * Control central de sesión:
     *
     * 1. Splash espera a que Firebase revise la sesión.
     * 2. Si hay usuario, abre Home.
     * 3. Si no hay usuario, abre Login.
     * 4. Después de login o registro, elimina las
     *    pantallas de autenticación del historial.
     * 5. Después de cerrar sesión, regresa a Login.
     */
    LaunchedEffect(
        splashFinished,
        authState.isLoading,
        authState.user?.uid,
        currentRoute
    ) {
        if (
            !splashFinished ||
            authState.isLoading
        ) {
            return@LaunchedEffect
        }

        if (authState.user != null) {
            val isAuthenticationRoute =
                currentRoute == Splash ||
                        currentRoute == Login ||
                        currentRoute == Register

            if (isAuthenticationRoute) {
                backStack.clear()
                backStack.add(Home)
            }
        } else {
            val canRemainWithoutSession =
                currentRoute == Login ||
                        currentRoute == Register

            if (!canRemainWithoutSession) {
                backStack.clear()
                backStack.add(Login)
            }
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        },
        entryProvider = entryProvider {

            entry<Splash> {
                SplashScreen(
                    onFinished = {
                        splashFinished = true
                    }
                )
            }

            entry<Login> {
                LoginScreen(
                    uiState = authState,
                    onLogin = { correo, password ->
                        authViewModel.login(
                            correo = correo,
                            password = password
                        )
                    },
                    onRegisterClick = {
                        authViewModel.clearError()
                        backStack.add(Register)
                    }
                )
            }

            entry<Register> {
                RegisterScreen(
                    uiState = authState,
                    onRegister = {
                            nombre,
                            correo,
                            password ->

                        authViewModel.register(
                            nombre = nombre,
                            correo = correo,
                            password = password
                        )
                    },
                    onBackToLogin = {
                        authViewModel.clearError()
                        backStack.removeLastOrNull()
                    }
                )
            }

            entry<Home> {
                val authenticatedUser = authState.user

                if (authenticatedUser != null) {
                    HomeScreen(
                        user = authenticatedUser,
                        bets = bets,
                        onCreateBet = {
                            backStack.add(CreateBet)
                        },
                        onViewBets = {
                            backStack.add(MyBets)
                        },
                        onLogout = {
                            authViewModel.logout()
                        }
                    )
                }
            }

            entry<CreateBet> {
                CreateBetScreen(
                    onBetCreated = { newBet ->
                        bets.add(newBet)
                        backStack.removeLastOrNull()
                    },
                    onBack = {
                        backStack.removeLastOrNull()
                    }
                )
            }

            entry<MyBets> {
                MyBetsScreen(
                    bets = bets,
                    onBetUpdated = { updatedBet ->
                        val index = bets.indexOfFirst { bet ->
                            bet.id == updatedBet.id
                        }

                        if (index >= 0) {
                            bets[index] = updatedBet
                        }
                    },
                    onBack = {
                        backStack.removeLastOrNull()
                    }
                )
            }
        }
    )
}