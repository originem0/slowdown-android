package com.example.slowdown

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.slowdown.ui.navigation.SlowDownNavGraph
import com.example.slowdown.ui.onboarding.OnboardingScreen
import com.example.slowdown.ui.theme.SlowDownTheme
import com.example.slowdown.util.LocaleHelper
import com.example.slowdown.viewmodel.OnboardingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context?) {
        if (newBase == null) {
            super.attachBaseContext(newBase)
            return
        }

        // Get the saved language preference synchronously
        // Note: runBlocking is necessary here because attachBaseContext is called
        // synchronously by the Android system and locale must be set immediately.
        // Using withTimeoutOrNull to prevent ANR if DataStore hangs.
        val app = newBase.applicationContext as? SlowDownApp
        val language = if (app != null) {
            runBlocking {
                withTimeoutOrNull(1000L) {  // 1 second timeout
                    app.userPreferences.appLanguage.first()
                } ?: "en"  // Default to English if timeout
            }
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
                    // Use state to handle async loading
                    var isLoading by remember { mutableStateOf(true) }
                    var showOnboarding by remember { mutableStateOf(false) }

                    // Load onboarding state asynchronously to avoid blocking main thread
                    LaunchedEffect(Unit) {
                        val onboardingCompleted = withContext(Dispatchers.IO) {
                            try {
                                withTimeoutOrNull(2000L) {  // 2 second timeout
                                    app.userPreferences.onboardingCompleted.first()
                                } ?: false  // Default to showing onboarding if timeout
                            } catch (e: Exception) {
                                false  // Show onboarding on error
                            }
                        }
                        showOnboarding = !onboardingCompleted
                        isLoading = false
                    }

                    when {
                        isLoading -> {
                            // Show loading indicator while checking preferences
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        showOnboarding -> {
                            val onboardingViewModel: OnboardingViewModel = viewModel(
                                factory = OnboardingViewModel.Factory(repository, this@MainActivity)
                            )
                            OnboardingScreen(
                                viewModel = onboardingViewModel,
                                onComplete = { showOnboarding = false }
                            )
                        }
                        else -> {
                            val navController = rememberNavController()
                            SlowDownNavGraph(
                                navController = navController,
                                repository = repository,
                                context = this@MainActivity
                            )
                        }
                    }
                }
            }
        }
    }
}
