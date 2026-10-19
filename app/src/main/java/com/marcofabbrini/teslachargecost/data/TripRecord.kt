package com.marcofabbrini.teslachargecost.data

/** Modalità di ricarica usata per calcolare il costo del viaggio. */
enum class ChargeMode { HOME, PUBLIC }

/**
 * Un consumo salvato nello storico.
 *
 * Contiene anche la capacità batteria e il prezzo dell'energia usati al momento del calcolo,
 * così i dati restano corretti anche se in futuro cambi le impostazioni.
 */
data class TripRecord(
    /** Momento del salvataggio in millisecondi: serve anche da identificativo. */
    val id: Long,
    val mode: ChargeMode,
    val percent: Double,
    val capacityKwh: Double,
    val kwh: Double,
    /** Costo del viaggio in euro (a casa: calcolato; alla colonnina: importo pagato). */
    val cost: Double,
    val costPerKwh: Double,
    val km: Double?,
)
