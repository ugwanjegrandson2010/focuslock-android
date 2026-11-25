package com.focuslock.app.ui.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Single source of truth for visual decisions. No screen defines its own palette. */
object DesignTokens {
    object Palette {
        val Void = Color(0xFF080912)
        val Surface = Color(0xFF0F1224)
        val SurfaceRaised = Color(0xFF171B33)
        val Hairline = Color(0xFF2A3159)
        val TextPrimary = Color(0xFFF2F4FF)
        val TextSecondary = Color(0xFFA9B2D9)
        val Accent = Color(0xFF8C6CFF)
        val AccentDeep = Color(0xFF441363)
        val AccentSoft = Color(0xFFC4B5FF)
        val Success = Color(0xFF7FE3C2)
        val Danger = Color(0xFFFF8B8B)
    }

    object Type {
        val Display: TextUnit = 68.sp
        val Hero: TextUnit = 44.sp
        val Headline: TextUnit = 30.sp
        val Title: TextUnit = 20.sp
        val Body: TextUnit = 16.sp
        val Label: TextUnit = 13.sp
        val Caption: TextUnit = 12.sp

        val displayStyle = TextStyle(
            fontSize = Display,
            fontWeight = FontWeight.SemiBold,
            fontFeatureSettings = "tnum"
        )
        val numericStyle = TextStyle(
            fontSize = Hero,
            fontWeight = FontWeight.SemiBold,
            fontFeatureSettings = "tnum"
        )
    }

    object Space {
        val xs = 4.dp
        val sm = 8.dp
        val md = 16.dp
        val lg = 24.dp
        val xl = 32.dp
        val xxl = 48.dp
    }

    object Shape {
        val panel = 20.dp
        val pill = 999.dp
        val field = 12.dp
    }

    object Motion {
        const val fastMs = 150
        const val normalMs = 300
        const val ambientMs = 6000
    }
}
