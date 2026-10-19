package com.marcofabbrini.teslachargecost.data

/**
 * Logica di calcolo dei costi di viaggio, separata dalla UI così è facile da testare.
 */
object TripCalculator {

    /** Risultato per la ricarica a casa: kWh consumati e costo in euro. */
    data class HomeResult(val kwh: Double, val cost: Double)

    /** Risultato per la colonnina pubblica: kWh consumati, €/kWh effettivo e €/punto percentuale. */
    data class PublicResult(val kwh: Double, val costPerKwh: Double, val costPerPercent: Double)

    /** kWh corrispondenti ai punti percentuale di batteria consumati. */
    fun kwhFromPercent(percent: Double, capacityKwh: Double): Double =
        percent / 100.0 * capacityKwh

    /** Ricarica a casa: percentuale consumata x capacità x tariffa domestica. */
    fun home(percent: Double, capacityKwh: Double, tariffPerKwh: Double): HomeResult {
        val kwh = kwhFromPercent(percent, capacityKwh)
        return HomeResult(kwh = kwh, cost = kwh * tariffPerKwh)
    }

    /**
     * Colonnina pubblica: dall'importo pagato e dai punti percentuale usati ricava
     * il costo effettivo al kWh. Restituisce null se i dati non permettono il calcolo.
     */
    fun publicCharger(percent: Double, capacityKwh: Double, amountPaid: Double): PublicResult? {
        val kwh = kwhFromPercent(percent, capacityKwh)
        if (percent <= 0.0 || kwh <= 0.0) return null
        return PublicResult(
            kwh = kwh,
            costPerKwh = amountPaid / kwh,
            costPerPercent = amountPaid / percent,
        )
    }
}
