package com.marcofabbrini.teslachargecost

import com.marcofabbrini.teslachargecost.data.TripCalculator
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
    fun stats_nonPositiveKmIsNotComputable() {
        assertNull(TripCalculator.stats(kwh = 12.1, cost = 3.025, km = 0.0))
        assertNull(TripCalculator.stats(kwh = 12.1, cost = 3.025, km = -5.0))
    }
}
