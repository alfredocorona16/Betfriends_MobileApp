package com.example.betfriends_mobilapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.betfriends_mobilapp.ui.theme.BetFriends_mobilappTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BetFriends_mobilappTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {

                        BetFriendsNavigation()

                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BetFriends_mobilappTheme {
        Greeting("Android")
    }
}

@Composable
fun BetFriendsNavigation(){
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "splash_screen"){

        composable("splash_screen"){
            SplashScreenAnimado(
                alTerminar = {
                    navController.navigate("login_screen"){
                        popUpTo("splash_screen") { inclusive = true }
                    }
                }
            )
        }

        composable("login_screen"){
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home_screen"){
                        popUpTo("login_screen"){ inclusive = true }
                    }
                }
            )
        }

        composable("home_screen") {
            HomeScreen()
        }
    }
}