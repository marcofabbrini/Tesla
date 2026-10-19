package com.marcofabbrini.teslachargecost.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcofabbrini.teslachargecost.data.ChargeMode
import com.marcofabbrini.teslachargecost.data.HistoryStats
import com.marcofabbrini.teslachargecost.data.TripRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    records: List<TripRecord>,
    onDelete: (Long) -> Unit,
    onBack: () -> Unit,
) {
    val sorted = remember(records) { records.sortedByDescending { it.id } }
    val summary = remember(records) { HistoryStats.summarize(records) }
    var pendingDelete by remember { mutableStateOf<TripRecord?>(null) }

    NeonBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "STORICO CONSUMI",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                    )
                    NeonIconButton(NeonIcons.Back, "Indietro", onBack)
                }
            }

            if (sorted.isEmpty()) {
                item {
                    GlassCard {
                        NeonLabel("Ancora nessun dato")
                        Text(
                            "Calcola un viaggio nella schermata principale e premi \"Salva consumo\": lo ritroverai qui.",
                            modifier = Modifier.padding(top = 8.dp),
                            color = Neon.TextDim,
                            fontSize = 15.sp,
                        )
                    }
                }
            } else {
                item {
                    GlassCard(glow = Neon.Accent2) {
                        NeonLabel("Riepilogo")
                        Line("Viaggi salvati", "${summary.count}")
                        Line("Energia totale", "${fmt2(summary.totalKwh)} kWh")
                        Line("Costo totale", fmtEuro(summary.totalCost))
                        if (summary.totalKm > 0.0) {
                            Line("Km con dati", "${fmt1(summary.totalKm)} km")
                        }
                        summary.avgWhPerKm?.let { Line("Consumo medio", "${fmt0(it)} Wh/km") }
                        summary.avgCostPerKm?.let { Line("Costo medio al km", "${fmt3(it)} €/km") }
                    }
                }

                items(sorted, key = { it.id }) { record ->
                    TripCard(record = record, onDelete = { pendingDelete = record })
                }
            }
        }
    }

    pendingDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = Neon.Panel,
            titleContentColor = Color.White,
            textContentColor = Neon.TextDim,
            title = { Text("Eliminare questo consumo?") },
            text = { Text("${formatDate(record.id)} · ${fmtEuro(record.cost)}") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(record.id)
                    pendingDelete = null
                }) {
                    Text("Elimina", color = Neon.Warn)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Annulla", color = Neon.Accent)
                }
            },
        )
    }
}

@Composable
private fun TripCard(record: TripRecord, onDelete: () -> Unit) {
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NeonLabel(formatDate(record.id))
            Text(
                if (record.mode == ChargeMode.HOME) "CASA" else "COLONNINA",
                color = Neon.Accent2,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
        }
        Text(
            fmtEuro(record.cost),
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            color = Neon.Accent,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            style = TextStyle(shadow = Shadow(Neon.Accent.copy(alpha = 0.5f), Offset.Zero, 20f)),
        )
        Line("Batteria consumata", "${percentText(record.percent)}% · ${fmt2(record.kwh)} kWh")
        Line(
            if (record.mode == ChargeMode.HOME) "Tariffa casa" else "Prezzo alla colonnina",
            if (record.mode == ChargeMode.HOME && record.costPerKwh == 0.0) {
                "Accumulatore (gratis)"
            } else {
                "${fmt2(record.costPerKwh)} €/kWh"
            },
        )
        record.km?.takeIf { it > 0.0 }?.let { km ->
            Line("Percorso", "${fmt1(km)} km")
            Line("Consumo medio", "${fmt0(record.kwh * 1000.0 / km)} Wh/km")
            Line("Costo al km", "${fmt3(record.cost / km)} €/km")
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.End) {
            NeonIconButton(NeonIcons.Delete, "Elimina", onDelete)
        }
    }
}

@Composable
private fun Line(label: String, value: String) {
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

private fun formatDate(millis: Long): String =
    SimpleDateFormat("EEE dd/MM/yyyy HH:mm", Locale.ITALY).format(Date(millis))

private fun percentText(value: Double): String =
    if (value % 1.0 == 0.0) fmt0(value) else fmt1(value)

private fun fmt0(value: Double): String = String.format(Locale.ITALY, "%.0f", value)
private fun fmt1(value: Double): String = String.format(Locale.ITALY, "%.1f", value)
private fun fmt2(value: Double): String = String.format(Locale.ITALY, "%.2f", value)
private fun fmt3(value: Double): String = String.format(Locale.ITALY, "%.3f", value)
private fun fmtEuro(value: Double): String = String.format(Locale.ITALY, "%.2f €", value)
