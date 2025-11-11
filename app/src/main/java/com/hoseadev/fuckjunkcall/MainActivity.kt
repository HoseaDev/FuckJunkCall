package com.hoseadev.fuckjunkcall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hoseadev.fuckjunkcall.ui.screens.HomeScreen
import com.hoseadev.fuckjunkcall.ui.theme.FuckJunkCallTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FuckJunkCallTheme {
                HomeScreen()
            }
        }
    }
}
