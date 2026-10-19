package com.marcofabbrini.teslachargecost

import com.marcofabbrini.teslachargecost.data.ChargeMode
import com.marcofabbrini.teslachargecost.data.HistoryStats
import com.marcofabbrini.teslachargecost.data.TripCalculator
import com.marcofabbrini.teslachargecost.data.TripRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TripCalculatorTest {

    @Test
    fun kwhFromPercent_isProportionalToCapacity() {
        assertEquals(6.05, TripCalculator.kwhFromPercent(10.0, 60.5), 1e-9)
    }

    @Test
    fun home_multipliesKwhByTariff() {
        val result = TripCalculator.home(percent = 20.0, capacityKwh = 60.5, tariffPerKwh = 0.25)
        assertEquals(12.1, result.kwh, 1e-9)
        assertEquals(3.025, result.cost, 1e-9)
    }

    @Test
    fun publicCharger_computesEffectiveCostPerKwh() {
        val result = TripCalculator.publicCharger(percent = 20.0, capacityKwh = 60.5, amountPaid = 6.05)
        assertNotNull(result)
        assertEquals(12.1, result!!.kwh, 1e-9)
        assertEquals(0.5, result.costPerKwh, 1e-9)
        assertEquals(0.3025, result.costPerPercent, 1e-9)
    }

    @Test
    fun publicCharger_zeroPercentIsNotComputable() {
        assertNull(TripCalculator.publicCharger(percent = 0.0, capacityKwh = 60.5, amountPaid = 5.0))
    }

    @Test
    fun stats_computesConsumptionAndCostPerDistance() {
        val stats = TripCalculator.stats(kwh = 12.1, cost = 3.025, km = 80.0)
        assertNotNull(stats)
        assertEquals(151.25, stats!!.whPerKm, 1e-9)
        assertEquals(15.125, stats.kwhPer100Km, 1e-9)
        assertEquals(0.0378125, stats.costPerKm, 1e-9)
        assertEquals(3.78125, stats.costPer100Km, 1e-9)
    }

    @Test
    fun record_home_usesTariffAndKeepsKm() {
        val r = TripCalculator.record(
            ChargeMode.HOME, percent = 20.0, capacityKwh = 60.5, tariffPerKwh = 0.25,
            amountPaid = null, km = 80.0, timestamp = 1L,
        )
        assertNotNull(r)
        assertEquals(12.1, r!!.kwh, 1e-9)
        assertEquals(3.025, r.cost, 1e-9)
        assertEquals(0.25, r.costPerKwh, 1e-9)
        assertEquals(80.0, r.km!!, 1e-9)
        assertEquals(1L, r.id)
    }

    @Test
    fun record_public_usesAmountPaid() {
        val r = TripCalculator.record(
            ChargeMode.PUBLIC, percent = 20.0, capacityKwh = 60.5, tariffPerKwh = 0.25,
            amountPaid = 6.05, km = null, timestamp = 2L,
        )
        assertNotNull(r)
        assertEquals(6.05, r!!.cost, 1e-9)
        assertEquals(0.5, r.costPerKwh, 1e-9)
        assertNull(r.km)
    }

    @Test
    fun record_missingDataIsNotSaved() {
        assertNull(
            TripCalculator.record(
                ChargeMode.PUBLIC, percent = 20.0, capacityKwh = 60.5, tariffPerKwh = 0.25,
                amountPaid = null, km = null, timestamp = 3L,
            ),
        )
        assertNull(
            TripCalculator.record(
                ChargeMode.HOME, percent = 0.0, capacityKwh = 60.5, tariffPerKwh = 0.25,
                amountPaid = null, km = null, timestamp = 4L,
            ),
        )
    }

    @Test
    fun summary_averagesUseOnlyTripsWithKm() {
        val a = TripRecord(1, ChargeMode.HOME, 20.0, 60.5, 12.1, 3.0, 0.25, 80.0)
        val b = TripRecord(2, ChargeMode.HOME, 10.0, 60.5, 6.05, 1.5, 0.25, null)
        val s = HistoryStats.summarize(listOf(a, b))
        assertEquals(2, s.count)
        assertEquals(18.15, s.totalKwh, 1e-9)
        assertEquals(4.5, s.totalCost, 1e-9)
        assertEquals(80.0, s.totalKm, 1e-9)
        assertEquals(151.25, s.avgWhPerKm!!, 1e-9)
        assertEquals(0.0375, s.avgCostPerKm!!, 1e-9)
    }

    @Test
    fun summary_emptyHasNoAverages() {
        val s = HistoryStats.summarize(emptyList())
        assertEquals(0, s.count)
        assertNull(s.avgWhPerKm)
        assertNull(s.avgCostPerKm)
    }

    @Test
    fun stats_nonPositiveKmIsNotComputable() {
        assertNull(TripCalculator.stats(kwh = 12.1, cost = 3.025, km = 0.0))
        assertNull(TripCalculator.stats(kwh = 12.1, cost = 3.025, km = -5.0))
    }
}
