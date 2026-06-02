package com.airline.checkin

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.airline.checkin.data.local.OnboardingPreferences
import com.airline.checkin.data.remote.firebase.DatabaseSeeder
import com.airline.checkin.data.repository.AuthRepository
import com.airline.checkin.ui.AppNavGraph
import com.airline.checkin.ui.AppTheme
import com.airline.checkin.ui.Routes
import com.airline.checkin.ui.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

@HiltAndroidApp
class AirlineApp : Application()

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var databaseSeeder: DatabaseSeeder
    @Inject lateinit var authRepository: AuthRepository
    private val themeViewModel: ThemeViewModel by viewModels()
   
    private var keepSplashScreen = true

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            try { databaseSeeder.seedIfNeeded() } catch (_: Exception) {}

            val onboardingPreferences = OnboardingPreferences(applicationContext)
            val hasOnboarded = onboardingPreferences.hasOnboarded.first()
            val isLoggedIn = authRepository.isLoggedIn()

            val startDest = when {
                !hasOnboarded -> Routes.ONBOARDING
                isLoggedIn    -> Routes.HOME
                else          -> Routes.WELCOME
            }

            keepSplashScreen = false
            setContent {
                val isDarkMode by themeViewModel.isDarkMode.collectAsState()
               
                AppTheme(darkTheme = isDarkMode) {
                    AppNavGraph(
                        startDestination = startDest,
                        onOnboardingComplete = {
                            lifecycleScope.launch { onboardingPreferences.setHasOnboarded(true) }
                        }
                    )
                }
            }
        }
    }
}