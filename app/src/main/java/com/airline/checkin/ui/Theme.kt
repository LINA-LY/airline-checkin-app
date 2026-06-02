package com.airline.checkin.ui

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// ─── DataStore ────────────────────────────────────────────────────────────────

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")
private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")

@Singleton
class ThemePreferences @Inject constructor(@ApplicationContext private val context: Context) {
    val isDarkMode = context.themeDataStore.data.map { prefs ->
        prefs[DARK_MODE_KEY] ?: false
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.themeDataStore.edit { it[DARK_MODE_KEY] = enabled }
    }
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val prefs: ThemePreferences
) : ViewModel() {

    val isDarkMode = prefs.isDarkMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    fun toggle() {
        viewModelScope.launch {
            prefs.setDarkMode(!isDarkMode.value)
        }
    }

    fun set(dark: Boolean) {
        viewModelScope.launch { prefs.setDarkMode(dark) }
    }
}

// ─── Composition Local ────────────────────────────────────────────────────────

val LocalDarkTheme = compositionLocalOf { false }

// ─── Design Tokens ────────────────────────────────────────────────────────────
// Light palette
private object LightColors {
    val Primary        = Color(0xFF8400FF)
    val PrimaryLight   = Color(0xFFEDD9FF)
    val PrimaryMedium  = Color(0xFFBB80FF)
    val PrimaryDark    = Color(0xFF5500A8)
    val PrimaryFaint   = Color(0xFFF7EEFF)

    val Black          = Color(0xFF000000)
    val Gray900        = Color(0xFF111827)
    val Gray700        = Color(0xFF374151)
    val Gray500        = Color(0xFF6B7280)
    val Gray300        = Color(0xFFD1D5DB)
    val Gray100        = Color(0xFFF3F4F6)
    val Gray50         = Color(0xFFF9FAFB)
    val White          = Color(0xFFFFFFFF)

    val Error          = Color(0xFFDC2626)
    val ErrorLight     = Color(0xFFFEE2E2)
    val Success        = Color(0xFF16A34A)
    val SuccessLight   = Color(0xFFDCFCE7)
    val Warning        = Color(0xFFD97706)
    val WarningLight   = Color(0xFFFEF3C7)

    // Surfaces
    val Background     = Color(0xFFF9FAFB)
    val Surface        = Color(0xFFFFFFFF)
    val CardBorder     = Color(0xFFF3F4F6)
}

// Dark palette
private object DarkColors {
    val Primary        = Color(0xFFA855F7)   // lighter violet readable on dark
    val PrimaryLight   = Color(0xFF3B1A5E)
    val PrimaryMedium  = Color(0xFF7C3ACA)
    val PrimaryDark    = Color(0xFF6B21A8)
    val PrimaryFaint   = Color(0xFF1E1232)

    val Black          = Color(0xFFFFFFFF)   // inverted for text
    val Gray900        = Color(0xFFF9FAFB)
    val Gray700        = Color(0xFFD1D5DB)
    val Gray500        = Color(0xFF9CA3AF)
    val Gray300        = Color(0xFF4B5563)
    val Gray100        = Color(0xFF1F2937)
    val Gray50         = Color(0xFF111827)
    val White          = Color(0xFF1F2937)   // "card" surface in dark

    val Error          = Color(0xFFF87171)
    val ErrorLight     = Color(0xFF3B1515)
    val Success        = Color(0xFF4ADE80)
    val SuccessLight   = Color(0xFF14381F)
    val Warning        = Color(0xFFFBBF24)
    val WarningLight   = Color(0xFF3A2A09)

    // Surfaces
    val Background     = Color(0xFF0F172A)
    val Surface        = Color(0xFF1E293B)
    val CardBorder     = Color(0xFF334155)
}

// ─── AppColors — dynamic, reads from CompositionLocal ─────────────────────────

object AppColors {
    // These are re-declared as vars that swap based on the current theme.
    val Primary        get() = if (_dark) DarkColors.Primary        else LightColors.Primary
    val PrimaryLight   get() = if (_dark) DarkColors.PrimaryLight   else LightColors.PrimaryLight
    val PrimaryMedium  get() = if (_dark) DarkColors.PrimaryMedium  else LightColors.PrimaryMedium
    val PrimaryDark    get() = if (_dark) DarkColors.PrimaryDark    else LightColors.PrimaryDark
    val PrimaryFaint   get() = if (_dark) DarkColors.PrimaryFaint   else LightColors.PrimaryFaint

    val Black          get() = if (_dark) DarkColors.Black          else LightColors.Black
    val Gray900        get() = if (_dark) DarkColors.Gray900        else LightColors.Gray900
    val Gray700        get() = if (_dark) DarkColors.Gray700        else LightColors.Gray700
    val Gray500        get() = if (_dark) DarkColors.Gray500        else LightColors.Gray500
    val Gray300        get() = if (_dark) DarkColors.Gray300        else LightColors.Gray300
    val Gray100        get() = if (_dark) DarkColors.Gray100        else LightColors.Gray100
    val Gray50         get() = if (_dark) DarkColors.Gray50         else LightColors.Gray50
    val White          get() = if (_dark) DarkColors.White          else LightColors.White

    val Error          get() = if (_dark) DarkColors.Error          else LightColors.Error
    val ErrorLight     get() = if (_dark) DarkColors.ErrorLight     else LightColors.ErrorLight
    val Success        get() = if (_dark) DarkColors.Success        else LightColors.Success
    val SuccessLight   get() = if (_dark) DarkColors.SuccessLight   else LightColors.SuccessLight
    val Warning        get() = if (_dark) DarkColors.Warning        else LightColors.Warning
    val WarningLight   get() = if (_dark) DarkColors.WarningLight   else LightColors.WarningLight

    val Background     get() = if (_dark) DarkColors.Background     else LightColors.Background
    val Surface        get() = if (_dark) DarkColors.Surface        else LightColors.Surface
    val CardBorder     get() = if (_dark) DarkColors.CardBorder     else LightColors.CardBorder

    // Internal — set by AppTheme wrapper
    internal var _dark: Boolean by mutableStateOf(false)
}

// ─── Dimensions ───────────────────────────────────────────────────────────────

object AppDimens {
    val radiusSmall    = 8.dp
    val radiusMedium   = 12.dp
    val radiusLarge    = 16.dp
    val radiusXL       = 20.dp
    val radiusFull     = 999.dp

    val paddingXS      = 4.dp
    val paddingS       = 8.dp
    val paddingM       = 12.dp
    val paddingL       = 16.dp
    val paddingXL      = 20.dp
    val paddingXXL     = 24.dp

    val buttonHeight   = 52.dp
    val inputHeight    = 56.dp
}

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Push the flag into the global AppColors object
    AppColors._dark = darkTheme

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary          = DarkColors.Primary,
            background       = DarkColors.Background,
            surface          = DarkColors.Surface,
            onBackground     = DarkColors.Gray900,
            onSurface        = DarkColors.Gray900,
            error            = DarkColors.Error,
        )
    } else {
        lightColorScheme(
            primary          = LightColors.Primary,
            background       = LightColors.Background,
            surface          = LightColors.Surface,
            onBackground     = LightColors.Gray900,
            onSurface        = LightColors.Gray900,
            error            = LightColors.Error,
        )
    }

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}