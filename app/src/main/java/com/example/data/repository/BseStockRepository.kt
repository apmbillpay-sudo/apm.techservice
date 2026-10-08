package com.example.data.repository

import com.example.data.local.BseDao
import com.example.data.local.SavedProjectionScenario
import com.example.data.local.WatchlistEntity
import com.example.data.model.BseMarketSummary
import com.example.data.model.BseStock
import com.example.data.model.MockBseData
import com.example.data.model.PriceProjectionModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class BseStockRepository(
    private val bseDao: BseDao,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _stocks = MutableStateFlow<List<BseStock>>(MockBseData.getInitialStocks())
    val stocks: Flow<List<BseStock>> = _stocks.asStateFlow()

    private val _marketSummary = MutableStateFlow(MockBseData.sampleMarketSummary)
    val marketSummary: Flow<BseMarketSummary> = _marketSummary.asStateFlow()

    // Scrip-specific customized simulation overrides
    private val _customProjections = MutableStateFlow<Map<String, PriceProjectionModel>>(emptyMap())

    init {
        startLiveMarketTickSimulation()
    }

    private fun startLiveMarketTickSimulation() {
        externalScope.launch {
            while (isActive) {
                delay(3000) // Simulated Dalal Street tick update every 3 seconds
                updateRandomTick()
            }
        }
    }

    private fun updateRandomTick() {
        val currentList = _stocks.value.toMutableList()
        if (currentList.isEmpty()) return

        // Pick 2-3 stocks to tick
        val numTicks = Random.nextInt(1, 4)
        for (i in 0 until numTicks) {
            val index = Random.nextInt(currentList.size)
            val stock = currentList[index]
            val pctDelta = (Random.nextDouble(-0.35, 0.40)) / 100.0
            val priceChange = stock.currentPrice * pctDelta
            val newPrice = Math.round((stock.currentPrice + priceChange) * 100.0) / 100.0
            val newHigh = maxOf(stock.dayHigh, newPrice)
            val newLow = minOf(stock.dayLow, newPrice)
            val newVolume = stock.volume + Random.nextLong(200, 3500)

            currentList[index] = stock.copy(
                currentPrice = newPrice,
                dayHigh = newHigh,
                dayLow = newLow,
                volume = newVolume
            )
        }
        _stocks.value = currentList

        // Ticker Sensex slightly
        val sensexDelta = Random.nextDouble(-25.0, 35.0)
        _marketSummary.update { prev ->
            val newSensex = Math.round((prev.sensexCurrent + sensexDelta) * 100.0) / 100.0
            val newChange = Math.round((prev.sensexChange + sensexDelta) * 100.0) / 100.0
            prev.copy(
                sensexCurrent = newSensex,
                sensexChange = newChange,
                sensexChangePercent = Math.round((newChange / (newSensex - newChange) * 100.0) * 100.0) / 100.0
            )
        }
    }

    fun getStockByCode(scripCode: String): Flow<BseStock?> {
        return stocks.map { list ->
            val base = list.find { it.scripCode == scripCode } ?: return@map null
            val customProj = _customProjections.value[scripCode]
            if (customProj != null) {
                base.copy(projections = customProj)
            } else {
                base
            }
        }
    }

    fun updateProjectionParameters(
        scripCode: String,
        revenueGrowth: Double,
        operatingMargin: Double,
        volatilityPercent: Double,
        discountRate: Double
    ) {
        val stock = _stocks.value.find { it.scripCode == scripCode } ?: return
        val newProj = MockBseData.recalculateProjections(
            basePrice = stock.currentPrice,
            revenueGrowthRate = revenueGrowth,
            operatingMargin = operatingMargin,
            volatilityPercent = volatilityPercent,
            discountRate = discountRate
        )
        _customProjections.update { map ->
            map + (scripCode to newProj)
        }
    }

    fun resetProjectionParameters(scripCode: String) {
        _customProjections.update { map ->
            map - scripCode
        }
    }

    // Watchlist Room integration
    val watchlist: Flow<List<WatchlistEntity>> = bseDao.getAllWatchlist()

    fun isStockWatched(scripCode: String): Flow<Boolean> = bseDao.isStockWatched(scripCode)

    suspend fun toggleWatchlist(stock: BseStock, targetAlert: Double? = null, notes: String = "") {
        val existing = bseDao.getWatchlistItem(stock.scripCode)
        if (existing != null) {
            bseDao.removeFromWatchlistByCode(stock.scripCode)
        } else {
            bseDao.addToWatchlist(
                WatchlistEntity(
                    scripCode = stock.scripCode,
                    scripId = stock.scripId,
                    companyName = stock.companyName,
                    targetAlertPrice = targetAlert,
                    researchNotes = notes
                )
            )
        }
    }

    suspend fun updateWatchlistAlert(scripCode: String, targetAlert: Double?, alertCondition: String, notes: String) {
        val existing = bseDao.getWatchlistItem(scripCode) ?: return
        bseDao.updateWatchlist(
            existing.copy(
                targetAlertPrice = targetAlert,
                alertCondition = alertCondition,
                researchNotes = notes
            )
        )
    }

    suspend fun removeFromWatchlist(scripCode: String) {
        bseDao.removeFromWatchlistByCode(scripCode)
    }

    // Saved Projections
    val savedProjections: Flow<List<SavedProjectionScenario>> = bseDao.getAllSavedProjections()

    suspend fun saveProjectionScenario(scenario: SavedProjectionScenario): Long {
        return bseDao.insertProjectionScenario(scenario)
    }

    suspend fun deleteProjectionScenario(scenario: SavedProjectionScenario) {
        bseDao.deleteProjectionScenario(scenario)
    }
}
