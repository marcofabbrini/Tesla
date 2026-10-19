package com.marcofabbrini.teslachargecost.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.marcofabbrini.teslachargecost.data.ChargeMode
import com.marcofabbrini.teslachargecost.data.Settings
import com.marcofabbrini.teslachargecost.data.TripCalculator
import com.marcofabbrini.teslachargecost.data.TripRecord
import java.util.Locale
import kotlinx.coroutines.delay

private enum class Mode { HOME, PUBLIC }

@Composable
fun HomeScreen(
    settings: Settings,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onSaveTrip: (TripRecord) -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf(Mode.HOME) }
    var percentText by rememberSaveable { mutableStateOf("") }
    var paidText by rememberSaveable { mutableStateOf("") }
    var kmText by rememberSaveable { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }

    // Dopo il salvataggio il pulsante resta disattivato per un attimo: evita doppi salvataggi involontari
    LaunchedEffect(saved) {
        if (saved) {
            delay(2500)
            saved = false
        }
    }

    val percent = percentText.toDoubleOrNullIt()?.takeIf { it in 0.0..100.0 }
    val paid = paidText.toDoubleOrNullIt()?.takeIf { it >= 0.0 }
    val km = kmText.toDoubleOrNullIt()?.takeIf { it > 0.0 }
    val kmInvalid = kmText.isNotBlank() && km == null
    val percentInvalid = percentText.isNotBlank() && percent == null
    val canSave = percent != null && percent > 0.0 && (mode == Mode.HOME || paid != null)

    NeonBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "TESLA CHARGE COST",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NeonTextButton("Storico", onOpenHistory, Modifier.weight(1f))
                NeonTextButton("Impostazioni", onOpenSettings, Modifier.weight(1f))
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

            OutlinedTextField(
                value = kmText,
                onValueChange = { kmText = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = neonFieldColors(),
                label = { Text("Km percorsi (facoltativo)") },
                suffix = { Text("km") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = kmInvalid,
            )

            ResultCard(mode, percent, paid, km, settings)

            Button(
                onClick = {
                    percent?.let { p ->
                        TripCalculator.record(
                            mode = if (mode == Mode.HOME) ChargeMode.HOME else ChargeMode.PUBLIC,
                            percent = p,
                            capacityKwh = settings.batteryCapacityKwh,
                            tariffPerKwh = settings.homeTariff,
                            amountPaid = paid,
                            km = km,
                            timestamp = System.currentTimeMillis(),
                        )?.let { record ->
                            onSaveTrip(record)
                            saved = true
                        }
                    }
                },
                enabled = canSave && !saved,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Neon.Cyan,
                    contentColor = Color(0xFF00252B),
                    disabledContainerColor = Color(0xFF16263F),
                    disabledContentColor = if (saved) Neon.Cyan else Neon.TextDim,
                ),
            ) {
                Text(
                    if (saved) "SALVATO NELLO STORICO" else "SALVA CONSUMO",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
            }
        }
    }
}

@Composable
private fun ResultCard(mode: Mode, percent: Double?, paid: Double?, km: Double?, settings: Settings) {
    GlassCard(glow = Neon.Violet) {
        when {
            percent == null -> Hint("Inserisci i punti percentuale per vedere il risultato.")

            mode == Mode.HOME -> {
                val r = TripCalculator.home(percent, settings.batteryCapacityKwh, settings.homeTariff)
                NeonLabel("Costo del viaggio")
                BigValue(euro(r.cost))
                MetricRow("Energia consumata", "${num(r.kwh)} kWh")
                MetricRow("Tariffa casa", "${num(settings.homeTariff)} €/kWh")
                StatsSection(km?.let { TripCalculator.stats(r.kwh, r.cost, it) })
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
                    StatsSection(km?.let { TripCalculator.stats(r.kwh, paid, it) })
                }
            }
        }
    }
}

/** Consumo medio e costo al km, mostrati solo se sono stati inseriti i km. */
@Composable
private fun StatsSection(stats: TripCalculator.TripStats?) {
    if (stats == null) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .height(1.dp)
            .background(Neon.Outline),
    )
    NeonLabel("Sul percorso")
    Column(modifier = Modifier.padding(top = 6.dp)) {
        MetricRow("Consumo medio", "${num0(stats.whPerKm)} Wh/km")
        MetricRow("Consumo ogni 100 km", "${num(stats.kwhPer100Km)} kWh")
        MetricRow("Costo al km", "${num3(stats.costPerKm)} €/km")
        MetricRow("Costo ogni 100 km", euro(stats.costPer100Km))
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

private fun num0(value: Double): String = String.format(Locale.ITALY, "%.0f", value)
private fun num3(value: Double): String = String.format(Locale.ITALY, "%.3f", value)
private fun num(value: Double): String = String.format(Locale.ITALY, "%.2f", value)
private fun euro(value: Double): String = String.format(Locale.ITALY, "%.2f €", value)
