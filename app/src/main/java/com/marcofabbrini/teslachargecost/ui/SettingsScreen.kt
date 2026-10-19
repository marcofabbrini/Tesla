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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcofabbrini.teslachargecost.data.DEFAULT_BATTERY_KWH
import com.marcofabbrini.teslachargecost.data.DEFAULT_HOME_TARIFF
import com.marcofabbrini.teslachargecost.data.Settings

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
                    "IMPOSTAZIONI",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                )
                NeonIconButton(NeonIcons.Back, "Indietro", onBack)
            }

            HeaderImage(imageUri = settings.headerImageUri)

            GlassCard {
                NeonLabel("Immagine di sfondo")
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NeonIconButton(
                        icon = NeonIcons.Photo,
                        contentDescription = "Scegli immagine",
                        onClick = { imagePicker.launch(arrayOf("image/*")) },
                        filled = true,
                    )
                    NeonIconButton(
                        icon = NeonIcons.Restore,
                        contentDescription = "Ripristina l'immagine predefinita",
                        onClick = { onHeaderImageChanged(null) },
                        enabled = settings.headerImageUri != null,
                    )
                }
            }

            GlassCard(glow = Neon.Accent2) {
                NeonLabel("Parametri")

                OutlinedTextField(
                    value = tariffText,
                    onValueChange = { tariffText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = neonFieldColors(),
                    label = { Text("Tariffa ricarica a casa") },
                    suffix = { Text("€/kWh") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = tariff == null,
                )

                OutlinedTextField(
                    value = capacityText,
                    onValueChange = { capacityText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = neonFieldColors(),
                    label = { Text("Capacità utile della batteria") },
                    suffix = { Text("kWh") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = capacity == null,
                )

                Row(
                    modifier = Modifier.padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NeonIconButton(
                        icon = NeonIcons.Save,
                        contentDescription = "Salva i parametri",
                        onClick = { onSaveValues(tariff!!, capacity!!) },
                        enabled = tariff != null && capacity != null,
                        filled = true,
                    )
                    NeonIconButton(
                        icon = NeonIcons.Restore,
                        contentDescription = "Valori predefiniti",
                        onClick = onResetValues,
                    )
                }

                Text(
                    "Predefiniti: ${formatInput(DEFAULT_HOME_TARIFF)} €/kWh e ${formatInput(DEFAULT_BATTERY_KWH)} kWh.",
                    modifier = Modifier.padding(top = 10.dp),
                    color = Neon.TextDim,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

private fun formatInput(value: Double): String {
    val text = value.toString()
    return (if (text.endsWith(".0")) text.dropLast(2) else text).replace('.', ',')
}
