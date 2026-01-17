package com.example.slowdown.ui.theme

import androidx.compose.ui.graphics.Color

// ========================================
// SlowDown Refined Palette - Digital Mindfulness
// Concept: Serenity, clarity, softness
// ========================================

// Primary - Calming Teal/Cyan
val Primary500 = Color(0xFF2A9D8F)
val Primary600 = Color(0xFF264653) // Darker, for contrast
val Primary100 = Color(0xFFE0F2F1) // Light background

// Secondary - Warm Sand/Earth
val Secondary500 = Color(0xFFE9C46A)
val Secondary600 = Color(0xFFF4A261) // Warmer, orange-ish
val Secondary100 = Color(0xFFFFF8E1)

// Tertiary - Soft Accent (Rose/Coral)
val Tertiary500 = Color(0xFFE76F51)
val Tertiary100 = Color(0xFFFFEBE6)

// Neutrals - Soft & Warm Grays
val Neutral50 = Color(0xFFF9FAFB)
val Neutral100 = Color(0xFFF3F4F6)
val Neutral200 = Color(0xFFE5E7EB)
val Neutral800 = Color(0xFF1F2937)
val Neutral900 = Color(0xFF111827)

// Semantic
val Success = Color(0xFF66BB6A)
val Warning = Color(0xFFFFA726)
val Error = Color(0xFFEF5350)
val Info = Color(0xFF42A5F5)

// Text
val TextPrimary = Neutral900
val TextSecondary = Color(0xFF4B5563) // Gray 600
val TextMuted = Color(0xFF9CA3AF)    // Gray 400
val TextOnPrimary = Color.White

// Dark Mode
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkTextPrimary = Color(0xFFEEEEEE)
val DarkTextSecondary = Color(0xFFB0B0B0)

// Chart Colors
val ChartColors = listOf(
    Primary500,
    Secondary500,
    Tertiary500,
    Color(0xFF264653),
    Color(0xFF8AB17D),
    Color(0xFFB5838D)
)

// Legacy mappings (to prevent build errors during migration)
val Teal500 = Primary500
val Teal700 = Primary600
val Sage400 = Primary100
val Sage500 = Color(0xFF8AB17D) 
val Amber400 = Secondary600
val Amber500 = Secondary500
val Amber600 = Color(0xFFD97706)
val Ivory100 = Neutral50
val Ivory200 = Neutral100
val Ivory300 = Neutral200
val ErrorLight = Color(0xFFFFEBEE)
