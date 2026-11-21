package com.marcofabbrini.teslachargecost.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.marcofabbrini.teslachargecost.data.AppUpdate
import com.marcofabbrini.teslachargecost.data.Aggiornamento
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** All'apertura dell'app controlla se su GitHub c'è una build più nuova e propone di scaricarla e installarla */
@Composable
fun AggiornamentoDialog() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var agg by remember { mutableStateOf<Aggiornamento?>(null) }
    var progresso by remember { mutableStateOf(-1) }
    var messaggio by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { agg = withContext(Dispatchers.IO) { AppUpdate.controlla(ctx) } }

    val a = agg ?: return
    val inCorso = progresso >= 0
    AlertDialog(
        onDismissRequest = { if (!inCorso) agg = null },
        title = { Text("Aggiornamento disponibile") },
        text = {
            Column {
                Text("Build ${a.build} (installata: ${AppUpdate.versioneInstallata(ctx)}). I tuoi dati restano.")
                if (inCorso) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(progress = { progresso / 100f })
                    Text("Download $progresso%")
                }
                messaggio?.let { Spacer(Modifier.height(8.dp)); Text(it) }
            }
        },
        confirmButton = {
            TextButton(enabled = !inCorso, onClick = {
                messaggio = null
                if (!AppUpdate.puoInstallare(ctx)) {
                    messaggio = "Consenti a questa app di installare aggiornamenti, poi torna qui e premi di nuovo Aggiorna."
                    AppUpdate.apriPermessoInstallazione(ctx)
                    return@TextButton
                }
                progresso = 0
                scope.launch {
                    val file = withContext(Dispatchers.IO) { AppUpdate.scarica(ctx, a) { p -> progresso = p } }
                    progresso = -1
                    if (file == null) messaggio = "Download non riuscito: controlla la connessione e riprova."
                    else { AppUpdate.installa(ctx, file); agg = null }
                }
            }) { Text("Aggiorna") }
        },
        dismissButton = { TextButton(enabled = !inCorso, onClick = { agg = null }) { Text("Più tardi") } },
    )
}
