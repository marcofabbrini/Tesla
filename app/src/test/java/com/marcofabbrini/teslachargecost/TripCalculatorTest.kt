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
}
