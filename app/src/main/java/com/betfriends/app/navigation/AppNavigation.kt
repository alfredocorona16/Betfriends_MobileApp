package com.betfriends.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.betfriends.app.data.auth.FirebaseAuthRepository
import com.betfriends.app.data.bet.FirebaseBetRepository
import com.betfriends.app.ui.auth.AuthViewModel
import com.betfriends.app.ui.auth.AuthViewModelFactory
import com.betfriends.app.ui.auth.LoginScreen
import com.betfriends.app.ui.auth.RegisterScreen
import com.betfriends.app.ui.auth.SplashScreen
import com.betfriends.app.ui.bets.BetsViewModel
import com.betfriends.app.ui.bets.BetsViewModelFactory
import com.betfriends.app.ui.bets.MyBetsScreen
import com.betfriends.app.ui.createbet.CreateBetScreen
import com.betfriends.app.ui.home.HomeScreen

@Composable
fun BetFriendsApp() {
    val backStack = rememberNavBackStack(Splash)

    var splashFinished by rememberSaveable {
        mutableStateOf(false)
    }

    val authRepository = remember {
        FirebaseAuthRepository()
    }
    val authViewModelFactory = remember(authRepository) {
        AuthViewModelFactory(authRepository = authRepository)
    }
    val authViewModel: AuthViewModel = viewModel(
        factory = authViewModelFactory
    )

    val betRepository = remember {
        FirebaseBetRepository()
    }
    val betsViewModelFactory = remember(betRepository) {
        BetsViewModelFactory(repository = betRepository)
    }
    val betsViewModel: BetsViewModel = viewModel(
        factory = betsViewModelFactory
    )

    val authState = authViewModel.uiState
    val betsState = betsViewModel.uiState
    val currentRoute = backStack.lastOrNull()
    val authenticatedUser = authState.user

    val displayedUser = authenticatedUser?.let { user ->
        user.copy(
            saldo = betsState.currentBalance ?: user.saldo
        )
    }

    LaunchedEffect(authenticatedUser?.uid) {
        if (authenticatedUser != null) {
            betsViewModel.start(authenticatedUser)
        } else {
            betsViewModel.stop()
        }
    }

    LaunchedEffect(
        splashFinished,
        authState.isLoading,
        authenticatedUser?.uid,
        currentRoute
    ) {
        if (!splashFinished || authState.isLoading) {
            return@LaunchedEffect
        }

        if (authenticatedUser != null) {
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
                    onRegister = { nombre, correo, password ->
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
                if (displayedUser != null) {
                    HomeScreen(
                        user = displayedUser,
                        bets = betsState.bets,
                        onCreateBet = {
                            betsViewModel.clearMessage()
                            betsViewModel.clearUserSearch()
                            backStack.add(CreateBet)
                        },
                        onViewBets = {
                            betsViewModel.clearMessage()
                            backStack.add(MyBets)
                        },
                        onLogout = {
                            authViewModel.logout()
                        }
                    )
                }
            }

            entry<CreateBet> {
                if (displayedUser != null) {
                    CreateBetScreen(
                        currentUser = displayedUser,
                        searchResult = betsState.searchResult,
                        isSearchingUser =
                            betsState.isSearchingUser,
                        isCreatingBet = betsState.isCreatingBet,
                        externalMessage = betsState.message,
                        onSearchUser = betsViewModel::searchUser,
                        onClearUserSearch =
                            betsViewModel::clearUserSearch,
                        onBetCreated = { newBet ->
                            betsViewModel.createBet(
                                bet = newBet,
                                creator = displayedUser,
                                onSuccess = {
                                    backStack.removeLastOrNull()
                                }
                            )
                        },
                        onBack = {
                            betsViewModel.clearUserSearch()
                            betsViewModel.clearMessage()
                            backStack.removeLastOrNull()
                        }
                    )
                }
            }

            entry<MyBets> {
                if (displayedUser != null) {
                    MyBetsScreen(
                        currentUserId = displayedUser.uid,
                        bets = betsState.bets,
                        invitations = betsState.invitations,
                        processingInvitationId =
                            betsState.processingInvitationId,
                        message = betsState.message,
                        onAcceptInvitation =
                            betsViewModel::acceptInvitation,
                        onDeclineInvitation =
                            betsViewModel::declineInvitation,
                        onBetUpdated = betsViewModel::updateBet,
                        onBack = {
                            betsViewModel.clearMessage()
                            backStack.removeLastOrNull()
                        }
                    )
                }
            }
        }
    )
}
