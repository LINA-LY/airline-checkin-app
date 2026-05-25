package com.airline.checkin

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.airline.checkin.data.local.OnboardingPreferences
import com.airline.checkin.data.remote.firebase.DatabaseSeeder
import com.airline.checkin.ui.AppNavGraph
import com.airline.checkin.ui.Routes
import com.airline.checkin.ui.onboarding.SplashScreen
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            databaseSeeder.seedIfNeeded()
        }
        setContent {
            val context = LocalContext.current
            val onboardingPreferences = remember { OnboardingPreferences(context) }
            val coroutineScope = rememberCoroutineScope()
            val hasOnboarded by produceState<Boolean?>(initialValue = null) {
                value = onboardingPreferences.hasOnboarded.first()
            }

            if (hasOnboarded == null) {
                SplashScreen()
            } else {
                AppNavGraph(
                    startDestination = if (hasOnboarded == true) Routes.WELCOME else Routes.ONBOARDING,
                    onOnboardingComplete = {
                        coroutineScope.launch { onboardingPreferences.setHasOnboarded(true) }
                    }
                )
            }
        }
    }
}
