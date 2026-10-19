package com.marcofabbrini.teslachargecost.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.marcofabbrini.teslachargecost.R

private fun family(regular: Int, medium: Int, semibold: Int, bold: Int) = FontFamily(
    Font(regular, FontWeight.Normal),
    Font(medium, FontWeight.Medium),
    Font(semibold, FontWeight.SemiBold),
    Font(bold, FontWeight.Bold),
)

private val OrbitronFamily = family(R.font.orbitron_regular, R.font.orbitron_medium, R.font.orbitron_semibold, R.font.orbitron_bold)
private val RajdhaniFamily = family(R.font.rajdhani_regular, R.font.rajdhani_medium, R.font.rajdhani_semibold, R.font.rajdhani_bold)

/** Font selezionabili per il testo dell'app (non per il titolo né per il numero LCD). */
enum class AppFont(val key: String, val label: String, val family: FontFamily) {
    EXO2("exo2", "Exo 2", ExoFamily),
    ORBITRON("orbitron", "Orbitron", OrbitronFamily),
    RAJDHANI("rajdhani", "Rajdhani", RajdhaniFamily),
    SYSTEM("system", "Sistema", FontFamily.Default),
    MONO("mono", "Monospazio", FontFamily.Monospace);

    companion object {
        fun fromKey(key: String?): AppFont = values().firstOrNull { it.key == key } ?: EXO2
    }
}
