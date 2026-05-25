package com.airline.checkin.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val ONBOARDING_DATASTORE = "onboarding_prefs"

val Context.onboardingDataStore by preferencesDataStore(name = ONBOARDING_DATASTORE)

class OnboardingPreferences(private val context: Context) {
    private val hasOnboardedKey = booleanPreferencesKey("has_onboarded")

    val hasOnboarded: Flow<Boolean> = context.onboardingDataStore.data
        .map { prefs -> prefs[hasOnboardedKey] ?: false }

    suspend fun setHasOnboarded(value: Boolean) {
        context.onboardingDataStore.edit { prefs ->
            prefs[hasOnboardedKey] = value
        }
    }
}
