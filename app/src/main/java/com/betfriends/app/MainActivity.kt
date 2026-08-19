package com.betfriends.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import com.betfriends.app.navigation.BetFriendsApp
import com.betfriends.app.ui.theme.BetBlack
import com.betfriends.app.ui.theme.BetFriendsTheme
import com.betfriends.app.ui.theme.BetStatusBar

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                scrim = BetStatusBar.toArgb()
            ),
            navigationBarStyle = SystemBarStyle.dark(
                scrim = BetBlack.toArgb()
            )
        )

        setContent {
            BetFriendsTheme(
                darkTheme = true
            ) {
                BetFriendsApp()
            }
        }
    }
}