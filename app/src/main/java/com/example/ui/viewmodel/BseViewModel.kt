package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.SavedProjectionScenario
import com.example.data.local.WatchlistEntity
import com.example.data.model.BseMarketSummary
import com.example.data.model.BseStock
import com.example.data.repository.BseStockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    MARKET_OVERVIEW,
    STOCK_DETAIL,
    PROJECTION_LAB,
    IPO_RESEARCH,
    WATCHLIST_ALERTS
}

class BseViewModel(
    private val repository: BseStockRepository
) : ViewModel() {

    private val _currentScreen = MutableStateFlow(AppScreen.MARKET_OVERVIEW)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedStockCode = MutableStateFlow<String?>("500325") // Default to Reliance
    val selectedStockCode: StateFlow<String?> = _selectedStockCode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedSectorFilter = MutableStateFlow<String?>("All")
    val selectedSectorFilter: StateFlow<String?> = _selectedSectorFilter.asStateFlow()

    val marketSummary: StateFlow<BseMarketSummary> = repository.marketSummary
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.marketSummary.let {
            com.example.data.model.MockBseData.sampleMarketSummary
        })

    val allStocks: StateFlow<List<BseStock>> = repository.stocks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredStocks: StateFlow<List<BseStock>> = combine(
        allStocks,
        _searchQuery,
        _selectedSectorFilter
    ) { list, query, sector ->
        list.filter { stock ->
            val matchesQuery = query.isBlank() ||
                stock.companyName.contains(query, ignoreCase = true) ||
                stock.scripCode.contains(query, ignoreCase = true) ||
                stock.scripId.contains(query, ignoreCase = true)

            val matchesSector = sector == null || sector == "All" || stock.sector.contains(sector, ignoreCase = true)

            matchesQuery && matchesSector
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedStock: StateFlow<BseStock?> = _selectedStockCode.flatMapLatest { code ->
        if (code == null) flowOf(null) else repository.getStockByCode(code)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isCurrentStockWatched: StateFlow<Boolean> = _selectedStockCode.flatMapLatest { code ->
        if (code == null) flowOf(false) else repository.isStockWatched(code)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val watchlistItems: StateFlow<List<WatchlistEntity>> = repository.watchlist
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedProjections: StateFlow<List<SavedProjectionScenario>> = repository.savedProjections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Projection simulation input states for currently viewed stock
    val simRevenueGrowth = MutableStateFlow(14.0)
    val simOperatingMargin = MutableStateFlow(18.0)
    val simVolatility = MutableStateFlow(20.0)
    val simDiscountRate = MutableStateFlow(11.0)

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun selectStock(scripCode: String) {
        _selectedStockCode.value = scripCode
        val stock = allStocks.value.find { it.scripCode == scripCode }
        if (stock != null) {
            simRevenueGrowth.value = stock.projections.baseRevenueGrowthRate
            simOperatingMargin.value = stock.projections.terminalOperatingMargin
            simVolatility.value = stock.projections.historicalVolatility1y
            simDiscountRate.value = stock.projections.discountRate
        }
        _currentScreen.value = AppScreen.STOCK_DETAIL
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSectorFilter(sector: String?) {
        _selectedSectorFilter.value = sector
    }

    fun updateSimulationParameters(growth: Double, margin: Double, vol: Double, discount: Double) {
        simRevenueGrowth.value = growth
        simOperatingMargin.value = margin
        simVolatility.value = vol
        simDiscountRate.value = discount

        val code = _selectedStockCode.value ?: return
        repository.updateProjectionParameters(
            scripCode = code,
            revenueGrowth = growth,
            operatingMargin = margin,
            volatilityPercent = vol,
            discountRate = discount
        )
    }

    fun resetSimulation() {
        val code = _selectedStockCode.value ?: return
        repository.resetProjectionParameters(code)
        val stock = allStocks.value.find { it.scripCode == code }
        if (stock != null) {
            simRevenueGrowth.value = stock.projections.baseRevenueGrowthRate
            simOperatingMargin.value = stock.projections.terminalOperatingMargin
            simVolatility.value = stock.projections.historicalVolatility1y
            simDiscountRate.value = stock.projections.discountRate
        }
    }

    fun toggleWatchlistCurrentStock(targetAlertPrice: Double? = null, notes: String = "") {
        val stock = selectedStock.value ?: return
        viewModelScope.launch {
            repository.toggleWatchlist(stock, targetAlertPrice, notes)
        }
    }

    fun removeFromWatchlist(scripCode: String) {
        viewModelScope.launch {
            repository.removeFromWatchlist(scripCode)
        }
    }

    fun updateWatchlistAlert(scripCode: String, targetAlert: Double?, condition: String, notes: String) {
        viewModelScope.launch {
            repository.updateWatchlistAlert(scripCode, targetAlert, condition, notes)
        }
    }

    fun saveCurrentProjectionScenario(scenarioName: String) {
        val stock = selectedStock.value ?: return
        viewModelScope.launch {
            val scenario = SavedProjectionScenario(
                scripCode = stock.scripCode,
                scripId = stock.scripId,
                companyName = stock.companyName,
                scenarioName = scenarioName.ifBlank { "${stock.scripId} Scenario" },
                revenueGrowthRate = simRevenueGrowth.value,
                operatingMargin = simOperatingMargin.value,
                volatilityAssumption = simVolatility.value,
                discountRate = simDiscountRate.value,
                projected5yTargetPrice = stock.projections.target5yBase,
                projectedCagr = stock.projections.projectedCagrBase,
                basePriceAtSimulation = stock.currentPrice
            )
            repository.saveProjectionScenario(scenario)
        }
    }

    fun deleteProjectionScenario(scenario: SavedProjectionScenario) {
        viewModelScope.launch {
            repository.deleteProjectionScenario(scenario)
        }
    }
}

class BseViewModelFactory(
    private val repository: BseStockRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
