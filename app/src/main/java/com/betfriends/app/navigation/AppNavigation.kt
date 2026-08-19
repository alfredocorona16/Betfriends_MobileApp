package com.betfriends.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.betfriends.app.domain.model.Bet
import com.betfriends.app.ui.bets.MyBetsScreen
import com.betfriends.app.ui.createbet.CreateBetScreen
import com.betfriends.app.ui.home.HomeScreen

@Composable
fun BetFriendsApp() {
    val backStack = rememberNavBackStack(Home)

    val bets = remember {
        mutableStateListOf<Bet>()
    }

    NavDisplay(
        backStack = backStack,
        onBack = {
            backStack.removeLastOrNull()
        },
        entryProvider = entryProvider {

            entry<Home> {
                HomeScreen(
                    onCreateBet = {
                        backStack.add(CreateBet)
                    },
                    onViewBets = {
                        backStack.add(MyBets)
                    }
                )
            }

            entry<CreateBet> {
                CreateBetScreen(
                    onBack = {
                        backStack.removeLastOrNull()
                    },
                    onBetCreated = { newBet ->
                        bets.add(newBet)

                        backStack.removeLastOrNull()
                        backStack.add(MyBets)
                    }
                )
            }

            entry<MyBets> {
                MyBetsScreen(
                    bets = bets,
                    onBetUpdated = { updatedBet ->
                        val index = bets.indexOfFirst {
                            it.id == updatedBet.id
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