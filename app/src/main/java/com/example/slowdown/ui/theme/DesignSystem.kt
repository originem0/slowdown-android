package com.sharonZ.slowdown.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * SlowDown Design System
 *
 * A unified design token system for consistent UI across the app.
 * Concept: Digital Mindfulness - Serenity, Clarity, Organic Softness
 *
 * Usage:
 *   - SlowDownDesign.Spacing.md
 *   - SlowDownDesign.Radius.card
 *   - SlowDownDesign.Elevation.card
 */
object SlowDownDesign {

    // ========================================
    // SPACING SCALE
    // Based on 4dp grid system
    // ========================================
    object Spacing {
        /** 4.dp - Micro spacing, icon gaps */
        val xxs: Dp = 4.dp

        /** 8.dp - Small gaps, between related elements */
        val xs: Dp = 8.dp

        /** 12.dp - Default internal padding */
        val sm: Dp = 12.dp

        /** 16.dp - Standard padding, card content */
        val md: Dp = 16.dp

        /** 20.dp - Medium-large, section spacing */
        val lg: Dp = 20.dp

        /** 24.dp - Large spacing, between cards */
        val xl: Dp = 24.dp

        /** 32.dp - Extra large, screen edges, section gaps */
        val xxl: Dp = 32.dp

        /** 48.dp - Huge, hero sections */
        val xxxl: Dp = 48.dp

        // Semantic aliases
        /** Screen horizontal padding */
        val screenHorizontal: Dp = md

        /** Screen vertical padding */
        val screenVertical: Dp = lg

        /** Card internal padding */
        val cardPadding: Dp = md

        /** Gap between list items */
        val listItemGap: Dp = xs

        /** Gap between sections */
        val sectionGap: Dp = xl

        /** Bottom navigation clearance */
        val bottomNavClearance: Dp = 80.dp
    }

    // ========================================
    // CORNER RADIUS
    // Organic, soft feel for mindfulness app
    // ========================================
    object Radius {
        /** 4.dp - Subtle rounding, small chips */
        val xs: Dp = 4.dp

        /** 8.dp - Small components, buttons */
        val sm: Dp = 8.dp

        /** 12.dp - Medium components, search bars */
        val md: Dp = 12.dp

        /** 16.dp - Cards, dialogs */
        val lg: Dp = 16.dp

        /** 20.dp - Large cards, prominent elements */
        val xl: Dp = 20.dp

        /** 24.dp - Hero cards, bottom sheets */
        val xxl: Dp = 24.dp

        /** 9999.dp - Fully rounded, pills, avatars */
        val full: Dp = 9999.dp

        // Semantic aliases
        val button: Dp = md
        val card: Dp = xxl
        val chip: Dp = sm
        val searchBar: Dp = md
        val bottomSheet: Dp = xxl
        val dialog: Dp = xxl
        val avatar: Dp = full
        val progressBar: Dp = xs
    }

    // ========================================
    // SHAPES (Pre-built RoundedCornerShape)
    // ========================================
    object Shapes {
        val buttonShape = RoundedCornerShape(Radius.button)
        val cardShape = RoundedCornerShape(Radius.card)
        val chipShape = RoundedCornerShape(Radius.chip)
        val searchBarShape = RoundedCornerShape(Radius.searchBar)
        val bottomSheetShape = RoundedCornerShape(
            topStart = Radius.bottomSheet,
            topEnd = Radius.bottomSheet
        )
        val dialogShape = RoundedCornerShape(Radius.dialog)
        val avatarShape = RoundedCornerShape(Radius.avatar)
        val progressBarShape = RoundedCornerShape(Radius.progressBar)
    }

    // ========================================
    // ELEVATION / SHADOW
    // Subtle, non-aggressive depth
    // ========================================
    object Elevation {
        /** 0.dp - Flat, no shadow */
        val none: Dp = 0.dp

        /** 1.dp - Subtle lift, list items */
        val xs: Dp = 1.dp

        /** 2.dp - Low elevation, cards at rest */
        val sm: Dp = 2.dp

        /** 4.dp - Medium, focused/hovered cards */
        val md: Dp = 4.dp

        /** 8.dp - High, dialogs, dropdowns */
        val lg: Dp = 8.dp

        /** 12.dp - Very high, bottom sheets */
        val xl: Dp = 12.dp

        /** 16.dp - Maximum, modals */
        val xxl: Dp = 16.dp

        // Semantic aliases
        val card: Dp = sm
        val cardHovered: Dp = md
        val fab: Dp = md
        val dialog: Dp = lg
        val bottomSheet: Dp = xl
        val dropdown: Dp = lg
    }

    // ========================================
    // ANIMATION DURATIONS
    // Calm, meditative pacing
    // ========================================
    object Duration {
        /** 100ms - Instant feedback, ripples */
        const val instant: Int = 100

        /** 150ms - Quick micro-interactions */
        const val fast: Int = 150

        /** 200ms - Standard transitions */
        const val normal: Int = 200

        /** 300ms - Emphasized transitions */
        const val medium: Int = 300

        /** 400ms - Slow, deliberate animations */
        const val slow: Int = 400

        /** 500ms - Very slow, breathing intro */
        const val verySlow: Int = 500

        /** 800ms - Page transitions */
        const val pageTransition: Int = 800

        /** 4000ms - Breathing cycle (inhale or exhale) */
        const val breathingCycle: Int = 4000

        /** 8000ms - Full breathing animation cycle */
        const val breathingFull: Int = 8000

        // Semantic aliases
        const val buttonPress: Int = fast
        const val cardHover: Int = normal
        const val screenFade: Int = medium
        const val dialogOpen: Int = slow
        const val shimmer: Int = 1500
    }

    // ========================================
    // ANIMATION EASINGS
    // Organic, natural movement
    // ========================================
    object Easing {
        /** Standard Material easing for enter/exit */
        val standard: androidx.compose.animation.core.Easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

        /** Decelerate - elements coming to rest */
        val decelerate: androidx.compose.animation.core.Easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

        /** Accelerate - elements leaving */
        val accelerate: androidx.compose.animation.core.Easing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)

        /** Emphasized - more dramatic, attention-drawing */
        val emphasized: androidx.compose.animation.core.Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

        /** Breathing - organic, meditative rhythm */
        val breathing: androidx.compose.animation.core.Easing = CubicBezierEasing(0.42f, 0.0f, 0.58f, 1.0f)

        /** Bounce - playful, celebratory */
        val overshoot: androidx.compose.animation.core.Easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1.0f)
    }

    // ========================================
    // SIZING
    // Touch targets, icons, avatars
    // ========================================
    object Size {
        // Icons
        /** 16.dp - Tiny icons, inline indicators */
        val iconXs: Dp = 16.dp

        /** 20.dp - Small icons, trailing icons */
        val iconSm: Dp = 20.dp

        /** 24.dp - Standard icons */
        val iconMd: Dp = 24.dp

        /** 32.dp - Large icons, emphasis */
        val iconLg: Dp = 32.dp

        /** 48.dp - Hero icons, empty states */
        val iconXl: Dp = 48.dp

        // Touch targets (minimum 44dp for accessibility)
        /** 44.dp - Minimum touch target */
        val touchTargetMin: Dp = 44.dp

        /** 48.dp - Standard touch target */
        val touchTarget: Dp = 48.dp

        /** 56.dp - Large touch target, FAB */
        val touchTargetLg: Dp = 56.dp

        // Avatars
        /** 32.dp - Small avatar, list items */
        val avatarSm: Dp = 32.dp

        /** 40.dp - Medium avatar, standard */
        val avatarMd: Dp = 40.dp

        /** 56.dp - Large avatar, profile */
        val avatarLg: Dp = 56.dp

        /** 80.dp - Extra large, hero avatar */
        val avatarXl: Dp = 80.dp

        // Components
        /** 48.dp - Standard button height */
        val buttonHeight: Dp = 48.dp

        /** 56.dp - Large button height */
        val buttonHeightLg: Dp = 56.dp

        /** 48.dp - Search bar height */
        val searchBarHeight: Dp = 48.dp

        /** 64.dp - List item height (single line) */
        val listItemSingleLine: Dp = 64.dp

        /** 72.dp - List item height (two lines) */
        val listItemTwoLine: Dp = 72.dp

        /** 88.dp - List item height (three lines) */
        val listItemThreeLine: Dp = 88.dp

        // Special
        /** 320.dp - Breathing circle size */
        val breathingCircle: Dp = 320.dp

        /** 4.dp - Progress bar height */
        val progressBarHeight: Dp = 4.dp

        /** 6.dp - Progress bar height (thick) */
        val progressBarHeightThick: Dp = 6.dp
    }

    // ========================================
    // Z-INDEX (for overlapping elements)
    // ========================================
    object ZIndex {
        const val base: Float = 0f
        const val raised: Float = 1f
        const val dropdown: Float = 10f
        const val sticky: Float = 20f
        const val overlay: Float = 30f
        const val modal: Float = 40f
        const val toast: Float = 50f
    }

    // ========================================
    // OPACITY
    // ========================================
    object Opacity {
        const val disabled: Float = 0.38f
        const val medium: Float = 0.6f
        const val high: Float = 0.87f
        const val full: Float = 1.0f

        // Specific use cases
        const val scrim: Float = 0.32f
        const val divider: Float = 0.12f
        const val hover: Float = 0.08f
        const val pressed: Float = 0.12f
        const val focus: Float = 0.12f
        const val dragged: Float = 0.16f
    }

    // ========================================
    // BORDER
    // ========================================
    object Border {
        /** 0.5.dp - Hairline, subtle dividers */
        val hairline: Dp = 0.5.dp

        /** 1.dp - Standard border */
        val thin: Dp = 1.dp

        /** 2.dp - Emphasized border */
        val medium: Dp = 2.dp

        /** 3.dp - Heavy border, focus rings */
        val thick: Dp = 3.dp
    }

    // ========================================
    // CONTENT WIDTH CONSTRAINTS
    // ========================================
    object MaxWidth {
        /** 360.dp - Compact content, cards in lists */
        val compact: Dp = 360.dp

        /** 480.dp - Standard content width */
        val standard: Dp = 480.dp

        /** 600.dp - Wide content, tablets */
        val wide: Dp = 600.dp

        /** 840.dp - Extra wide, large tablets */
        val extraWide: Dp = 840.dp
    }
}

// ========================================
// Extension functions for convenience
// ========================================

/** Quick access to design tokens */
typealias DS = SlowDownDesign
