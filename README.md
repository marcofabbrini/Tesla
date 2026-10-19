# Tesla Charge Cost

App Android nativa (Kotlin + Jetpack Compose) che calcola quanto è costato un viaggio in
Tesla Model 3 Standard a partire dai punti percentuale di batteria consumati.

## Come funziona

Si inserisce la percentuale consumata in un campo grande, leggibile a colpo d'occhio, e si sceglie la modalità:

- **Ricarica a casa**: `percentuale / 100 × capacità batteria (kWh) × tariffa (€/kWh)`.
  Restituisce energia consumata e costo del viaggio.
- **Colonnina pubblica**: si inserisce l'importo pagato e i punti percentuale usati; l'app calcola
  il costo effettivo al kWh e il costo per punto percentuale.

## Impostazioni

Modificabili dall'app e salvate in locale sul dispositivo (DataStore, nessun backend):

| Parametro | Predefinito |
|---|---|
| Tariffa ricarica a casa | 0,25 €/kWh |
| Capacità utile batteria | 60,5 kWh (Model 3 Standard) |
| Immagine di intestazione | immagine inclusa nell'app, sostituibile con una foto a scelta |

La capacità è una stima: Tesla non pubblica ufficialmente il dato, quindi è modificabile.

## Aspetto

Interfaccia futuristica a tema scuro. Il testo usa il font di sistema; i punti percentuale sono mostrati
in una casella 3D con alone luminoso, con font LCD a 7 segmenti e segmenti spenti sullo sfondo.

## Crediti

- Font LCD **DSEG7 Classic** di keshikan, licenza SIL Open Font License 1.1 (vedi `licenses/DSEG-LICENSE.txt`).
- L'immagine di intestazione è stata fornita dall'autore dell'app. Se non è tua, sostituiscila prima di
  rendere pubblico il repository.

## Build

Servono Android Studio (o JDK 17 + Android SDK) e Gradle 8.7 o superiore.

```bash
gradle assembleDebug          # genera l'APK debug
gradle testDebugUnitTest      # esegue i test
```

Aprendo la cartella con Android Studio, il wrapper Gradle viene generato automaticamente.
Il workflow `.github/workflows/android.yml` compila e testa a ogni push e carica l'APK come artefatto.

## Struttura

```
app/src/main/java/com/marcofabbrini/teslachargecost/
├── MainActivity.kt
├── data/   TripCalculator.kt, SettingsRepository.kt
└── ui/     HomeScreen.kt, SettingsScreen.kt, Header.kt, Neon.kt, theme/
```

## Idee future

- Integrazione con il progetto 3DKnife
- Storico dei viaggi
