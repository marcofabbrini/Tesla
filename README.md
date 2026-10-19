# Tesla Charge Cost

App Android nativa (Kotlin + Jetpack Compose) che calcola quanto è costato un viaggio in
Tesla Model 3 Standard a partire dai punti percentuale di batteria consumati.

## Come funziona

Si inserisce la percentuale consumata in un campo grande, leggibile a colpo d'occhio, e si sceglie la modalità:

- **Ricarica a casa**: `percentuale / 100 × capacità batteria (kWh) × tariffa (€/kWh)`.
  Restituisce energia consumata e costo del viaggio.
- **Colonnina pubblica**: si inserisce l'importo pagato e i punti percentuale usati; l'app calcola
  il costo effettivo al kWh e il costo per punto percentuale.

In entrambe le modalità puoi indicare anche i **km percorsi** (facoltativo): l'app calcola il consumo
medio (Wh/km e kWh/100 km) e il costo al km e ogni 100 km.

## Storico

Il pulsante **Salva consumo** registra il calcolo corrente (data e ora, modalità, percentuale, kWh, costo,
km). La schermata **Storico** mostra un riepilogo (totali e medie) e l'elenco dei viaggi, dal più recente,
con la possibilità di eliminarli. I dati restano solo sul dispositivo. Ogni record conserva la capacità batteria
e il prezzo dell'energia usati, quindi non cambia se modifichi poi le impostazioni.

## Impostazioni

Modificabili dall'app e salvate in locale sul dispositivo (DataStore, nessun backend):

| Parametro | Predefinito |
|---|---|
| Tariffa ricarica a casa | 0,25 €/kWh |
| Capacità utile batteria | 60,5 kWh (Model 3 Standard) |
| Immagine di intestazione | immagine inclusa nell'app, sostituibile con una foto a scelta |

La capacità è una stima: Tesla non pubblica ufficialmente il dato, quindi è modificabile.

## Aspetto

Interfaccia futuristica a tema scuro. Il titolo è nel font TESLA, rosso con un leggero bagliore e adattato alla
larghezza dello schermo; l'auto in intestazione ha un alone rosso. Tutto il testo usa Exo 2. I pulsanti hanno un
leggero effetto 3D (faccia sfumata, spessore, abbassamento al tocco). I punti percentuale sono mostrati in una
casella 3D con alone luminoso, con font LCD a 7 segmenti e segmenti spenti sullo sfondo.

## Crediti

- Font **Exo 2** (Natanael Gama), licenza SIL Open Font License 1.1 (vedi `licenses/Exo2-OFL.txt`).
- Font del titolo **TESLA**, licenza Creative Commons BY-NC-SA: solo uso non commerciale.
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
Il workflow `.github/workflows/android.yml` compila e testa a ogni push, pubblica l'APK nella Release `latest`
e lo carica come artefatto.

## Aggiornare l'app sovrapponendo l'APK

Android accetta un aggiornamento solo se l'APK nuovo ha lo stesso nome pacchetto, la **stessa chiave di firma**
e un `versionCode` più alto. Il `versionCode` è il numero della build su GitHub Actions e cresce da solo.
La chiave di firma va impostata una volta nei secret del repository (Settings, Secrets and variables, Actions):

| Secret | Contenuto |
|---|---|
| `KEYSTORE_B64` | il file `.jks` codificato in base64 |
| `KEYSTORE_PASSWORD` | password del keystore (e della chiave) |

L'alias della chiave è `teslachargecost` e sta nel workflow. Conserva una copia della chiave: senza,
i nuovi APK non potranno aggiornare quelli già installati.

## Struttura

```
app/src/main/java/com/marcofabbrini/teslachargecost/
├── MainActivity.kt
├── data/   TripCalculator.kt, SettingsRepository.kt, TripRepository.kt, TripRecord.kt, HistoryStats.kt
└── ui/     HomeScreen.kt, HistoryScreen.kt, SettingsScreen.kt, Header.kt, Neon.kt, theme/
```

## Idee future

- Integrazione con il progetto 3DKnife
- Grafici dell'andamento dei consumi
- Esportazione dello storico (CSV)
