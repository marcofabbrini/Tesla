package com.marcofabbrini.teslachargecost.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcofabbrini.teslachargecost.widget.TeslaApi
import com.marcofabbrini.teslachargecost.widget.TeslaWidgetsAll
import com.marcofabbrini.teslachargecost.widget.WidgetPrefs

/** Collegamento dei widget al sito: indirizzo e chiave creata in Auto > Tesla > Tecnica > Collegamento. */
@Composable
fun WidgetSettingsCard() {
    val ctx = LocalContext.current
    var sito by remember { mutableStateOf(WidgetPrefs.sito(ctx)) }
    var chiave by remember { mutableStateOf(WidgetPrefs.chiave(ctx)) }
    var esito by remember { mutableStateOf(WidgetPrefs.ultimoEsito(ctx)) }
    var prova by remember { mutableStateOf(false) }
    val chiaveOk = Regex("^[a-f0-9]{48}$").matches(chiave.trim().lowercase())

    GlassCard(glow = Neon.Accent) {
        NeonLabel("Widget")
        Text(
            "Sul sito apri Auto › Tesla › Tecnica › Collegamento e premi «Chiave per l'app Android», poi incolla qui la chiave.",
            modifier = Modifier.padding(top = 8.dp), color = Neon.TextDim, fontSize = 13.sp,
        )
        OutlinedTextField(
            value = sito, onValueChange = { sito = it },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            shape = RoundedCornerShape(16.dp), colors = neonFieldColors(),
            label = { Text("Indirizzo del sito") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )
        OutlinedTextField(
            value = chiave, onValueChange = { chiave = it.trim() },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            shape = RoundedCornerShape(16.dp), colors = neonFieldColors(),
            label = { Text("Chiave") }, singleLine = true,
            isError = chiave.isNotEmpty() && !chiaveOk,
        )
        NeonButton(
            label = if (prova) "Verifica in corso…" else "Salva e aggiorna i widget",
            onClick = {
                WidgetPrefs.salva(ctx, sito.trim().trimEnd('/'), chiave.trim().lowercase())
                prova = true
                val app = ctx.applicationContext
                Thread {
                    TeslaApi.aggiorna(app)
                    TeslaWidgetsAll.aggiornaTutti(app)
                    esito = WidgetPrefs.ultimoEsito(app)
                    prova = false
                }.start()
            },
            modifier = Modifier.padding(top = 14.dp),
            enabled = chiaveOk && !prova && sito.startsWith("http"),
            filled = true,
        )
        if (esito.isNotBlank()) {
            Text(esito, modifier = Modifier.padding(top = 10.dp), color = Neon.TextDim, fontSize = 12.sp)
        }
        Text(
            "Poi tieni premuto sulla schermata Home › Widget › Tesla Charge Cost. Tocca un widget per aggiornarlo.",
            modifier = Modifier.padding(top = 10.dp), color = Neon.TextDim, fontSize = 12.sp,
        )
    }
}
