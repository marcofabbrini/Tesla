package com.marcofabbrini.teslachargecost.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
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
    // Ricarica a costo zero con l'energia dell'accumulatore: selezionata di default
    var freeCharge by rememberSaveable { mutableStateOf(true) }
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
    val homeFree = mode == Mode.HOME && freeCharge
    val homeTariff = if (homeFree) 0.0 else settings.homeTariff
    // Percentuale e km sono obbligatori; in colonnina serve anche l'importo pagato
    val complete = percent != null && percent > 0.0 && km != null && (mode == Mode.HOME || paid != null)
    val canSave = complete
    val tripCost = when {
        !complete || percent == null -> 0.0
        mode == Mode.HOME -> TripCalculator.home(percent, settings.batteryCapacityKwh, homeTariff).cost
        else -> paid ?: 0.0
    }

    NeonBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Titolo fisso in alto: il resto scorre sotto e sfuma
            Box(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp)) {
                NeonTitle("TESLA", fitTo = "TESLA STATS", scale = 0.75f)
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    NeonFlatIcon(NeonIcons.History, "Storico", onOpenHistory)
                    NeonFlatIcon(NeonIcons.Settings, "Impostazioni", onOpenSettings)
                }
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // Immagine di sfondo, fissa: le schede scorrono sopra
                HomeBackdrop(settings.headerImageUri, Modifier.align(Alignment.TopCenter))
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black),
                                    startY = 0f,
                                    endY = 24.dp.toPx(),
                                ),
                                blendMode = BlendMode.DstIn,
                            )
                        }
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
            // Spazio in cui si vede l'auto sullo sfondo; il contatore del costo sta in basso a destra e scorre con la pagina
            Box(modifier = Modifier.fillMaxWidth().height(182.dp), contentAlignment = Alignment.BottomEnd) {
                OdometerPrice(value = tripCost)
            }

            // Due dati obbligatori affiancati: batteria consumata e km percorsi
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    NeonLabel("Batteria usata", Modifier.padding(start = 4.dp))
                    LcdField(
                        value = percentText,
                        onValueChange = { percentText = it },
                        isError = percentInvalid,
                        ghost = "888",
                        suffix = "%",
                        modifier = Modifier.fillMaxWidth(),
                        sizeChars = 5,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    NeonLabel("Km percorsi", Modifier.padding(start = 4.dp))
                    LcdField(
                        value = kmText,
                        onValueChange = { kmText = it },
                        isError = kmInvalid,
                        ghost = "888.8",
                        suffix = "km",
                        modifier = Modifier.fillMaxWidth(),
                        decimal = true,
                        maxLength = 5,
                        sizeChars = 5,
                    )
                }
            }
            if (percentInvalid) {
                Text(
                    "La batteria va da 0 a 100",
                    modifier = Modifier.padding(start = 4.dp),
                    color = Neon.Warn,
                    fontSize = 13.sp,
                )
            }

            NeonToggle(
                options = listOf("Ricarica a casa", "Colonnina pubblica"),
                selectedIndex = mode.ordinal,
                onSelect = { mode = Mode.values()[it] },
            )

            if (mode == Mode.HOME) {
                FreeChargeCheck(checked = freeCharge, onCheckedChange = { freeCharge = it })
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

            ResultCard(mode, percent, paid, km, settings, homeFree)

            NeonIconButton(
                icon = if (saved) NeonIcons.Check else NeonIcons.Save,
                contentDescription = if (saved) "Salvato nello storico" else "Salva consumo",
                onClick = {
                    percent?.let { p ->
                        TripCalculator.record(
                            mode = if (mode == Mode.HOME) ChargeMode.HOME else ChargeMode.PUBLIC,
                            percent = p,
                            capacityKwh = settings.batteryCapacityKwh,
                            tariffPerKwh = homeTariff,
                            amountPaid = paid,
                            km = km,
                            timestamp = System.currentTimeMillis(),
                        )?.let { record ->
                            onSaveTrip(record)
                            saved = true
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                enabled = canSave && !saved,
                filled = true,
            )
            }
            }
        }
    }
}

/** Spunta: ricarica fatta interamente con l'energia dell'accumulatore, quindi a costo zero. */
@Composable
private fun FreeChargeCheck(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Neon.Accent,
                uncheckedColor = Neon.Outline,
                checkmarkColor = Color(0xFF14151A),
            ),
        )
        Text(
            "Ricarica a costo zero dall'accumulatore",
            color = if (checked) Color.White else Neon.TextDim,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ResultCard(
    mode: Mode,
    percent: Double?,
    paid: Double?,
    km: Double?,
    settings: Settings,
    free: Boolean,
) {
    // Percentuale e km sono obbligatori: senza non c'è nulla da mostrare
    if (percent == null || km == null) return
    GlassCard(glow = Neon.Accent2) {
        when {
            mode == Mode.HOME -> {
                val tariff = if (free) 0.0 else settings.homeTariff
                val r = TripCalculator.home(percent, settings.batteryCapacityKwh, tariff)
                MetricRow("Energia consumata", "${num(r.kwh)} kWh")
                MetricRow("Tariffa casa", if (free) "Accumulatore (gratis)" else "${num(tariff)} €/kWh")
                StatsSection(TripCalculator.stats(r.kwh, r.cost, km))
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
                    StatsSection(TripCalculator.stats(r.kwh, paid, km))
                }
            }
        }
    }
}

/** Consumo medio e costo al km. */
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
        color = Neon.Accent,
        fontSize = 44.sp,
        fontWeight = FontWeight.Bold,
        style = TextStyle(shadow = Shadow(Neon.Accent.copy(alpha = 0.6f), Offset.Zero, 24f)),
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
