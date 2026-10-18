package com.motofix.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.motofix.app.navigation.MotoFixNavHost
import com.motofix.app.ui.theme.Background
import com.motofix.app.ui.theme.MotoFixTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Iconos claros en las barras del sistema porque la app siempre es oscura
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            MotoFixTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Background)
                ) {
                    MotoFixNavHost()
                }
            }
        }
    }
}
