package com.airline.checkin

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.airline.checkin.data.local.OnboardingPreferences
import com.airline.checkin.data.remote.firebase.DatabaseSeeder
import com.airline.checkin.ui.AppNavGraph
import com.airline.checkin.ui.Routes
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
    private var keepSplashScreen = true

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            databaseSeeder.seedIfNeeded()
        }
        val onboardingPreferences = OnboardingPreferences(applicationContext)
        lifecycleScope.launch {
            val hasOnboarded = onboardingPreferences.hasOnboarded.first()
            keepSplashScreen = false
            setContent {
                AppNavGraph(
                    startDestination = if (hasOnboarded) Routes.WELCOME else Routes.ONBOARDING,
                    onOnboardingComplete = {
                        lifecycleScope.launch { onboardingPreferences.setHasOnboarded(true) }
                    }
                )
            }
        }
    }
}
