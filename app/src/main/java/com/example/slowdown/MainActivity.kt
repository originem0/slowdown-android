package com.example.slowdown

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.slowdown.ui.navigation.SlowDownNavGraph
import com.example.slowdown.ui.theme.SlowDownTheme
import com.example.slowdown.util.LocaleHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context?) {
        if (newBase == null) {
            super.attachBaseContext(newBase)
            return
        }
        
        // Get the saved language preference synchronously
        val app = newBase.applicationContext as? SlowDownApp
        val language = if (app != null) {
            runBlocking { app.userPreferences.appLanguage.first() }
        } else {
            "en"
        }
        
        val localizedContext = LocaleHelper.setLocale(newBase, language)
        super.attachBaseContext(localizedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SlowDownApp
        val repository = app.repository

        setContent {
            SlowDownTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    SlowDownNavGraph(
                        navController = navController,
                        repository = repository,
                        context = this
                    )
                }
            }
        }
    }
}

