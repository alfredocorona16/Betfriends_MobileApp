package com.betfriends.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold


fun ProfileScreen  (onBackClick: () -> Unit){
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text ('Perfil')},
                navigationIcon = {
                    IconButton (onClick = onBakcClick){
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                    
                }
            )
        }
    ) { }

}