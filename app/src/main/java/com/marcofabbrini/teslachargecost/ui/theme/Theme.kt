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

// Tema futuristico: sempre scuro, accento ciano al neon. Tipografia: Exo 2.
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
