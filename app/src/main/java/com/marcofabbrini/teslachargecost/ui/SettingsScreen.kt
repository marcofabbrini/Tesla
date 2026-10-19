package com.marcofabbrini.teslachargecost.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.marcofabbrini.teslachargecost.data.DEFAULT_BATTERY_KWH
import com.marcofabbrini.teslachargecost.data.DEFAULT_HOME_TARIFF
import com.marcofabbrini.teslachargecost.data.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    onSaveValues: (tariff: Double, capacityKwh: Double) -> Unit,
    onResetValues: () -> Unit,
    onHeaderImageChanged: (String?) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    var tariffText by remember(settings.homeTariff) { mutableStateOf(formatInput(settings.homeTariff)) }
    var capacityText by remember(settings.batteryCapacityKwh) {
        mutableStateOf(formatInput(settings.batteryCapacityKwh))
    }

    val tariff = tariffText.toDoubleOrNullIt()?.takeIf { it >= 0.0 }
    val capacity = capacityText.toDoubleOrNullIt()?.takeIf { it > 0.0 }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            // Mantiene l'accesso all'immagine anche dopo il riavvio dell'app
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            onHeaderImageChanged(uri.toString())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Impostazioni") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Indietro") } },
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
                Button(onClick = { imagePicker.launch(arrayOf("image/*")) }) {
                    Text("Scegli immagine")
                }
                OutlinedButton(
                    onClick = { onHeaderImageChanged(null) },
                    enabled = settings.headerImageUri != null,
                ) {
                    Text("Rimuovi")
                }
            }

            OutlinedTextField(
                value = tariffText,
                onValueChange = { tariffText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Tariffa ricarica a casa") },
                suffix = { Text("€/kWh") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = tariff == null,
            )

            OutlinedTextField(
                value = capacityText,
                onValueChange = { capacityText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Capacità utile della batteria") },
                suffix = { Text("kWh") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = capacity == null,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onSaveValues(tariff!!, capacity!!) },
                    enabled = tariff != null && capacity != null,
                ) {
                    Text("Salva")
                }
                OutlinedButton(onClick = onResetValues) {
                    Text("Ripristina predefiniti")
                }
            }

            Text(
                "Predefiniti: ${formatInput(DEFAULT_HOME_TARIFF)} €/kWh e ${formatInput(DEFAULT_BATTERY_KWH)} kWh.",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun formatInput(value: Double): String {
    val text = value.toString()
    return (if (text.endsWith(".0")) text.dropLast(2) else text).replace('.', ',')
}
