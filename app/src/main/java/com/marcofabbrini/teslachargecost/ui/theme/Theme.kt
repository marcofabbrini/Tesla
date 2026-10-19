package com.marcofabbrini.teslachargecost.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
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

private fun typographyFor(family: FontFamily): Typography = Typography().let { t ->
    t.copy(
        displayLarge = t.displayLarge.copy(fontFamily = family),
        displayMedium = t.displayMedium.copy(fontFamily = family),
        displaySmall = t.displaySmall.copy(fontFamily = family),
        headlineLarge = t.headlineLarge.copy(fontFamily = family),
        headlineMedium = t.headlineMedium.copy(fontFamily = family),
        headlineSmall = t.headlineSmall.copy(fontFamily = family),
        titleLarge = t.titleLarge.copy(fontFamily = family),
        titleMedium = t.titleMedium.copy(fontFamily = family),
        titleSmall = t.titleSmall.copy(fontFamily = family),
        bodyLarge = t.bodyLarge.copy(fontFamily = family),
        bodyMedium = t.bodyMedium.copy(fontFamily = family),
        bodySmall = t.bodySmall.copy(fontFamily = family),
        labelLarge = t.labelLarge.copy(fontFamily = family),
        labelMedium = t.labelMedium.copy(fontFamily = family),
        labelSmall = t.labelSmall.copy(fontFamily = family),
    )
}

@Composable
fun TeslaChargeCostTheme(appFont: FontFamily = ExoFamily, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NeonColors,
        typography = remember(appFont) { typographyFor(appFont) },
    ) {
        // Senza una Surface il testo non erediterebbe il font: lo si imposta qui per tutta l'app
        ProvideTextStyle(TextStyle(fontFamily = appFont), content = content)
    }
}
