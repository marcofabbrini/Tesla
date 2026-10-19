package com.marcofabbrini.teslachargecost.data

/** Riepilogo dello storico: totali e medie. Le medie al km usano solo i viaggi con i km inseriti. */
data class HistorySummary(
    val count: Int,
    val totalKwh: Double,
    val totalCost: Double,
    val totalKm: Double,
    val avgWhPerKm: Double?,
    val avgCostPerKm: Double?,
)

object HistoryStats {

    fun summarize(records: List<TripRecord>): HistorySummary {
        val withKm = records.filter { (it.km ?: 0.0) > 0.0 }
        val km = withKm.sumOf { it.km ?: 0.0 }
        val kwhWithKm = withKm.sumOf { it.kwh }
        val costWithKm = withKm.sumOf { it.cost }
        return HistorySummary(
            count = records.size,
            totalKwh = records.sumOf { it.kwh },
            totalCost = records.sumOf { it.cost },
            totalKm = km,
            avgWhPerKm = if (km > 0.0) kwhWithKm * 1000.0 / km else null,
            avgCostPerKm = if (km > 0.0) costWithKm / km else null,
        )
    }
}
