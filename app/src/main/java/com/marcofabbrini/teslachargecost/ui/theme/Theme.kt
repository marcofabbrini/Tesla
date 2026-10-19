package com.marcofabbrini.teslachargecost.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.marcofabbrini.teslachargecost.ui.ExoFamily
import com.marcofabbrini.teslachargecost.ui.Neon

// Tema futuristico: sempre scuro, accento rosso al neon. Tipografia: Exo 2.
private val NeonColors = darkColorScheme(
    primary = Neon.Accent,
    onPrimary = Color(0xFF14151A),
    primaryContainer = Color(0xFF33353B),
    onPrimaryContainer = Color(0xFFE6E8EC),
    secondary = Neon.Accent2,
    background = Neon.BgBottom,
    onBackground = Color(0xFFF0F1F3),
    surface = Neon.Panel,
    onSurface = Color(0xFFF0F1F3),
    surfaceVariant = Color(0xFF1C1D22),
    onSurfaceVariant = Neon.TextDim,
    outline = Neon.Outline,
    error = Neon.Warn,
)

private val ExoTypography = Typography().let { t ->
    t.copy(
        displayLarge = t.displayLarge.copy(fontFamily = ExoFamily),
        displayMedium = t.displayMedium.copy(fontFamily = ExoFamily),
        displaySmall = t.displaySmall.copy(fontFamily = ExoFamily),
        headlineLarge = t.headlineLarge.copy(fontFamily = ExoFamily),
        headlineMedium = t.headlineMedium.copy(fontFamily = ExoFamily),
        headlineSmall = t.headlineSmall.copy(fontFamily = ExoFamily),
        titleLarge = t.titleLarge.copy(fontFamily = ExoFamily),
        titleMedium = t.titleMedium.copy(fontFamily = ExoFamily),
        titleSmall = t.titleSmall.copy(fontFamily = ExoFamily),
        bodyLarge = t.bodyLarge.copy(fontFamily = ExoFamily),
        bodyMedium = t.bodyMedium.copy(fontFamily = ExoFamily),
        bodySmall = t.bodySmall.copy(fontFamily = ExoFamily),
        labelLarge = t.labelLarge.copy(fontFamily = ExoFamily),
        labelMedium = t.labelMedium.copy(fontFamily = ExoFamily),
        labelSmall = t.labelSmall.copy(fontFamily = ExoFamily),
    )
}

@Composable
fun TeslaChargeCostTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NeonColors,
        typography = ExoTypography,
    ) {
        // Senza una Surface il testo non erediterebbe il font: lo si imposta qui per tutta l'app
        ProvideTextStyle(TextStyle(fontFamily = ExoFamily), content = content)
    }
}
