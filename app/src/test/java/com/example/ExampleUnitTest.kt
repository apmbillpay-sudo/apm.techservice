package com.example

import com.example.data.model.MockBseData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testMonteCarloQuantilesOrdering() {
        val paths = MockBseData.generateMonteCarloPaths(
            currentPrice = 2500.0,
            annualDrift = 0.14,
            annualVol = 0.20,
            months = 60
        )
        val p5 = paths["p5"]!!
        val p25 = paths["p25"]!!
        val p50 = paths["p50"]!!
        val p75 = paths["p75"]!!
        val p95 = paths["p95"]!!

        assertEquals(61, p5.size)

        // At end of month 60, quantiles must follow P5 <= P25 <= P50 <= P75 <= P95
        val lastIdx = 60
        assertTrue("P5 must be <= P25", p5[lastIdx] <= p25[lastIdx])
        assertTrue("P25 must be <= P50", p25[lastIdx] <= p50[lastIdx])
        assertTrue("P50 must be <= P75", p50[lastIdx] <= p75[lastIdx])
        assertTrue("P75 must be <= P95", p75[lastIdx] <= p95[lastIdx])
    }

    @Test
    fun testProjectionRecalculationEngine() {
        val proj = MockBseData.recalculateProjections(
            basePrice = 1000.0,
            revenueGrowthRate = 15.0,
            operatingMargin = 20.0,
            volatilityPercent = 22.0,
            discountRate = 11.0
        )

        assertTrue(proj.target1yBase > 1000.0)
        assertTrue(proj.target3yBase > proj.target1yBase)
        assertTrue(proj.target5yBase > proj.target3yBase)
        assertTrue(proj.target5yBull > proj.target5yBase)
        assertTrue(proj.target5yConservative < proj.target5yBase)
    }

    @Test
    fun testInitialStocksDataset() {
        val stocks = MockBseData.getInitialStocks()
        assertTrue("Should have at least 10 BSE flagship stocks", stocks.size >= 10)

        val reliance = stocks.find { it.scripCode == "500325" }
        assertNotNull(reliance)
        assertEquals("RELIANCE", reliance?.scripId)
        assertTrue((reliance?.technicals?.technicalScore ?: 0) in 0..100)
    }
}
