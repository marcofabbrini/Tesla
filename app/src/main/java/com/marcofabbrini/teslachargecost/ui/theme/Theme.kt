package com.marcofabbrini.teslachargecost.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.marcofabbrini.teslachargecost.ui.Neon

// Tema futuristico: sempre scuro, accento ciano al neon. Tipografia: font di sistema.
private val NeonColors = darkColorScheme(
    primary = Neon.Cyan,
    onPrimary = Color(0xFF00252B),
    primaryContainer = Color(0xFF0B2A3A),
    onPrimaryContainer = Color(0xFFBDF4FF),
    secondary = Neon.Violet,
    background = Neon.BgBottom,
    onBackground = Color(0xFFE6F1FF),
    surface = Neon.Panel,
    onSurface = Color(0xFFE6F1FF),
    surfaceVariant = Color(0xFF111B30),
    onSurfaceVariant = Neon.TextDim,
    outline = Neon.Outline,
    error = Neon.Red,
)

@Composable
fun TeslaChargeCostTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NeonColors,
        content = content,
    )
}
