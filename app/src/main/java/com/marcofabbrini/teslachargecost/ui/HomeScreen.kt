package com.marcofabbrini.teslachargecost.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcofabbrini.teslachargecost.data.Settings
import com.marcofabbrini.teslachargecost.data.TripCalculator
import java.util.Locale

private enum class Mode { HOME, PUBLIC }

@Composable
fun HomeScreen(settings: Settings, onOpenSettings: () -> Unit) {
    var mode by rememberSaveable { mutableStateOf(Mode.HOME) }
    var percentText by rememberSaveable { mutableStateOf("") }
    var paidText by rememberSaveable { mutableStateOf("") }

    val percent = percentText.toDoubleOrNullIt()?.takeIf { it in 0.0..100.0 }
    val paid = paidText.toDoubleOrNullIt()?.takeIf { it >= 0.0 }
    val percentInvalid = percentText.isNotBlank() && percent == null

    NeonBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "TESLA CHARGE COST",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                )
                NeonTextButton("Impostazioni", onOpenSettings)
            }

            HeaderImage(imageUri = settings.headerImageUri)

            NeonToggle(
                options = listOf("Ricarica a casa", "Colonnina pubblica"),
                selectedIndex = mode.ordinal,
                onSelect = { mode = Mode.values()[it] },
            )

            NeonLabel("Punti percentuale consumati", Modifier.padding(start = 4.dp))
            LcdPercentField(
                value = percentText,
                onValueChange = { percentText = it },
                isError = percentInvalid,
            )
            if (percentInvalid) {
                Text(
                    "Inserisci un valore tra 0 e 100",
                    modifier = Modifier.padding(start = 4.dp),
                    color = Neon.Red,
                    fontSize = 13.sp,
                )
            }

            if (mode == Mode.PUBLIC) {
                OutlinedTextField(
                    value = paidText,
                    onValueChange = { paidText = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = neonFieldColors(),
                    label = { Text("Importo pagato alla colonnina") },
                    suffix = { Text("€") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }

            ResultCard(mode, percent, paid, settings)
        }
    }
}

@Composable
private fun ResultCard(mode: Mode, percent: Double?, paid: Double?, settings: Settings) {
    GlassCard(glow = Neon.Violet) {
        when {
            percent == null -> Hint("Inserisci i punti percentuale per vedere il risultato.")

            mode == Mode.HOME -> {
                val r = TripCalculator.home(percent, settings.batteryCapacityKwh, settings.homeTariff)
                NeonLabel("Costo del viaggio")
                BigValue(euro(r.cost))
                MetricRow("Energia consumata", "${num(r.kwh)} kWh")
                MetricRow("Tariffa casa", "${num(settings.homeTariff)} €/kWh")
            }

            paid == null -> Hint("Inserisci l'importo pagato alla colonnina.")

            else -> {
                val r = TripCalculator.publicCharger(percent, settings.batteryCapacityKwh, paid)
                if (r == null) {
                    Hint("Con 0% non si può calcolare il costo al kWh.")
                } else {
                    NeonLabel("Costo effettivo al kWh")
                    BigValue("${num(r.costPerKwh)} €/kWh")
                    MetricRow("Energia consumata", "${num(r.kwh)} kWh")
                    MetricRow("Costo per punto percentuale", "${num(r.costPerPercent)} €")
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, color = Neon.TextDim, fontSize = 15.sp)
}

@Composable
private fun BigValue(text: String) {
    Text(
        text,
        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
        color = Neon.Cyan,
        fontSize = 44.sp,
        fontWeight = FontWeight.Bold,
        style = TextStyle(shadow = Shadow(Neon.Cyan.copy(alpha = 0.6f), Offset.Zero, 24f)),
    )
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Neon.TextDim, fontSize = 14.sp)
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun num(value: Double): String = String.format(Locale.ITALY, "%.2f", value)
private fun euro(value: Double): String = String.format(Locale.ITALY, "%.2f €", value)
