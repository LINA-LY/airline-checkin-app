package com.airline.checkin.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ─── Design System ───────────────────────────────────────────────
// Primary:   #8400FF  (deep violet)
// Accents:   tints/shades of primary
// Neutrals:  white (#FFFFFF) and black (#000000)
// Icons:     Lucide (lucide-icons/lucide-android) — monochrome, minimal
// No emojis anywhere in UI

object AppColors {
    val Primary        = Color(0xFF8400FF)
    val PrimaryLight   = Color(0xFFEDD9FF)   // 10% primary
    val PrimaryMedium  = Color(0xFFBB80FF)   // 50% primary
    val PrimaryDark    = Color(0xFF5500A8)   // 65% primary
    val PrimaryFaint   = Color(0xFFF7EEFF)   // 5% primary — backgrounds

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
}

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