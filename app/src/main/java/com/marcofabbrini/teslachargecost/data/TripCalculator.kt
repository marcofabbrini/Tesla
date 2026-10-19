package com.marcofabbrini.teslachargecost.data

/**
 * Logica di calcolo dei costi di viaggio, separata dalla UI così è facile da testare.
 */
object TripCalculator {

    /** Risultato per la ricarica a casa: kWh consumati e costo in euro. */
    data class HomeResult(val kwh: Double, val cost: Double)

    /** Risultato per la colonnina pubblica: kWh consumati, €/kWh effettivo e €/punto percentuale. */
    data class PublicResult(val kwh: Double, val costPerKwh: Double, val costPerPercent: Double)

    /** Consumo medio e costo riferiti alla distanza percorsa. */
    data class TripStats(
        val whPerKm: Double,
        val kwhPer100Km: Double,
        val costPerKm: Double,
        val costPer100Km: Double,
    )

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

    /**
     * Costruisce il record da salvare nello storico.
     * Restituisce null se mancano dati: alla colonnina serve l'importo pagato.
     */
    fun record(
        mode: ChargeMode,
        percent: Double,
        capacityKwh: Double,
        tariffPerKwh: Double,
        amountPaid: Double?,
        km: Double?,
        timestamp: Long,
    ): TripRecord? {
        val kwh = kwhFromPercent(percent, capacityKwh)
        if (percent <= 0.0 || kwh <= 0.0) return null
        return when (mode) {
            ChargeMode.HOME -> TripRecord(
                id = timestamp, mode = mode, percent = percent, capacityKwh = capacityKwh,
                kwh = kwh, cost = kwh * tariffPerKwh, costPerKwh = tariffPerKwh, km = km,
            )

            ChargeMode.PUBLIC -> {
                val paid = amountPaid ?: return null
                TripRecord(
                    id = timestamp, mode = mode, percent = percent, capacityKwh = capacityKwh,
                    kwh = kwh, cost = paid, costPerKwh = paid / kwh, km = km,
                )
            }
        }
    }

    /**
     * Consumo medio e costo al km a partire da energia consumata, costo del viaggio e km percorsi.
     * Restituisce null se i km non sono positivi.
     */
    fun stats(kwh: Double, cost: Double, km: Double): TripStats? {
        if (km <= 0.0) return null
        return TripStats(
            whPerKm = kwh * 1000.0 / km,
            kwhPer100Km = kwh * 100.0 / km,
            costPerKm = cost / km,
            costPer100Km = cost * 100.0 / km,
        )
    }
}
