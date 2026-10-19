package com.marcofabbrini.teslachargecost.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcofabbrini.teslachargecost.data.Settings
import com.marcofabbrini.teslachargecost.data.TripCalculator
import java.util.Locale

private enum class Mode { HOME, PUBLIC }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(settings: Settings, onOpenSettings: () -> Unit) {
    var mode by rememberSaveable { mutableStateOf(Mode.HOME) }
    var percentText by rememberSaveable { mutableStateOf("") }
    var paidText by rememberSaveable { mutableStateOf("") }

    val percent = percentText.toDoubleOrNullIt()?.takeIf { it in 0.0..100.0 }
    val paid = paidText.toDoubleOrNullIt()?.takeIf { it >= 0.0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tesla Charge Cost") },
                actions = { TextButton(onClick = onOpenSettings) { Text("Impostazioni") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HeaderImage(imageUri = settings.headerImageUri)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = mode == Mode.HOME,
                    onClick = { mode = Mode.HOME },
                    label = { Text("Ricarica a casa") },
                )
                FilterChip(
                    selected = mode == Mode.PUBLIC,
                    onClick = { mode = Mode.PUBLIC },
                    label = { Text("Colonnina pubblica") },
                )
            }

            // Campo grande per i punti percentuale, leggibile a colpo d'occhio
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Punti percentuale consumati",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    OutlinedTextField(
                        value = percentText,
                        onValueChange = { percentText = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        ),
                        suffix = { Text("%", fontSize = 40.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = percentText.isNotBlank() && percent == null,
                        supportingText = {
                            if (percentText.isNotBlank() && percent == null) {
                                Text("Inserisci un valore tra 0 e 100")
                            }
                        },
                    )
                }
            }

            if (mode == Mode.PUBLIC) {
                OutlinedTextField(
                    value = paidText,
                    onValueChange = { paidText = it },
                    modifier = Modifier.fillMaxWidth(),
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            when {
                percent == null -> Text("Inserisci i punti percentuale per vedere il risultato.")

                mode == Mode.HOME -> {
                    val r = TripCalculator.home(percent, settings.batteryCapacityKwh, settings.homeTariff)
                    Text("Costo del viaggio", style = MaterialTheme.typography.titleMedium)
                    Text(euro(r.cost), fontSize = 44.sp, fontWeight = FontWeight.Bold)
                    Text("Energia consumata: ${num(r.kwh)} kWh")
                    Text("Tariffa casa: ${num(settings.homeTariff)} €/kWh")
                }

                paid == null -> Text("Inserisci l'importo pagato alla colonnina.")

                else -> {
                    val r = TripCalculator.publicCharger(percent, settings.batteryCapacityKwh, paid)
                    if (r == null) {
                        Text("Con 0% non si può calcolare il costo al kWh.")
                    } else {
                        Text("Costo effettivo al kWh", style = MaterialTheme.typography.titleMedium)
                        Text("${num(r.costPerKwh)} €/kWh", fontSize = 44.sp, fontWeight = FontWeight.Bold)
                        Text("Energia consumata: ${num(r.kwh)} kWh")
                        Text("Costo per punto percentuale: ${num(r.costPerPercent)} €")
                    }
                }
            }
        }
    }
}

private fun num(value: Double): String = String.format(Locale.ITALY, "%.2f", value)
private fun euro(value: Double): String = String.format(Locale.ITALY, "%.2f €", value)
