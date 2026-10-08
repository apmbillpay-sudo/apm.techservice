package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BseStock
import com.example.data.model.CorporateGovernance
import com.example.data.model.DividendRecord
import com.example.data.model.InstitutionalDeal
import com.example.data.model.ListingAndIpoMetrics
import com.example.data.model.ManagementProfile
import com.example.data.model.PriceProjectionModel
import com.example.data.model.RegulatoryFiling
import com.example.data.model.RisksAndOpportunities
import com.example.data.model.ShareholdingSummary
import com.example.data.model.TechnicalParameters
import com.example.ui.components.InteractiveStockChart
import com.example.ui.components.MonteCarloProjectionFanChart
import com.example.ui.components.MovingAveragesGrid
import com.example.ui.components.PivotLevelsTable
import com.example.ui.components.RangeSliderBar
import com.example.ui.components.RsiIndicatorCard
import com.example.ui.components.ShareholdingDonutChart
import com.example.ui.components.TechnicalScoreMeter
import com.example.ui.components.ValuationMultiplesCard
import com.example.ui.components.VolatilityMetricsCard
import com.example.ui.theme.BearRed
import com.example.ui.theme.BullGreen
import com.example.ui.theme.DalalBlueLight
import com.example.ui.theme.DalalGold
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.BseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(
    viewModel: BseViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateTo(AppScreen.MARKET_OVERVIEW)
    }

    val stock by viewModel.selectedStock.collectAsState()
    val isWatched by viewModel.isCurrentStockWatched.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var isCandleMode by remember { mutableStateOf(false) }
    var showSaveScenarioDialog by remember { mutableStateOf(false) }

    if (stock == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Stock not found", color = MaterialTheme.colorScheme.onSurface)
        }
        return
    }

    val curStock = stock!!

    val tabs = listOf(
        "Quote & Charts",
        "Technicals",
        "5Y Projections & Volatility",
        "IPO & Listing",
        "Shareholding & Institutional",
        "Governance & Filings",
        "Risks & Opportunities"
    )

    Column(modifier = modifier.fillMaxSize()) {
        // Top App Bar
        TopAppBar(
            title = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = curStock.scripId,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            color = DalalBlueLight.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "BSE: ${curStock.scripCode}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = DalalBlueLight,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = curStock.companyName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = { viewModel.navigateTo(AppScreen.MARKET_OVERVIEW) },
                    modifier = Modifier.testTag("stock_detail_back_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = { viewModel.toggleWatchlistCurrentStock() },
                    modifier = Modifier.testTag("watchlist_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isWatched) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Watchlist",
                        tint = if (isWatched) DalalGold else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Live Price Banner Strip
        PriceHeaderStrip(stock = curStock)

        // Tab Navigation
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 16.dp,
            divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)) }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        // Active Tab Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("stock_detail_tab_content"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (selectedTabIndex) {
                0 -> {
                    // TAB 1: Quote & Charts
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PRICE ACTION & VOLUME",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { isCandleMode = !isCandleMode },
                                    color = if (isCandleMode) DalalBlueLight.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isCandleMode) Icons.Default.CandlestickChart else Icons.Default.ShowChart,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (isCandleMode) DalalBlueLight else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (isCandleMode) "Candles" else "Area",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isCandleMode) DalalBlueLight else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        InteractiveStockChart(
                            quotes = curStock.historicalQuotes,
                            isCandleMode = isCandleMode
                        )
                    }

                    item {
                        RangeSliderBar(
                            title = "Day Range (High / Low)",
                            low = curStock.dayLow,
                            high = curStock.dayHigh,
                            current = curStock.currentPrice
                        )
                    }

                    item {
                        RangeSliderBar(
                            title = "52-Week Range",
                            low = curStock.fiftyTwoWeekLow,
                            high = curStock.fiftyTwoWeekHigh,
                            current = curStock.currentPrice
                        )
                    }

                    item {
                        ValuationMultiplesCard(stock = curStock)
                    }
                }

                1 -> {
                    // TAB 2: Technical Parameters
                    item {
                        TechnicalScoreMeter(
                            signal = curStock.technicals.overallSignal,
                            score = curStock.technicals.technicalScore
                        )
                    }

                    item {
                        RsiIndicatorCard(
                            rsi = curStock.technicals.rsi14,
                            status = curStock.technicals.rsiStatus
                        )
                    }

                    item {
                        MovingAveragesGrid(
                            currentPrice = curStock.currentPrice,
                            sma20 = curStock.technicals.sma20,
                            sma50 = curStock.technicals.sma50,
                            sma200 = curStock.technicals.sma200,
                            dmaSignal = curStock.technicals.dmaCrossSignal
                        )
                    }

                    item {
                        MacdSummaryCard(technicals = curStock.technicals)
                    }

                    item {
                        PivotLevelsTable(
                            pivotLevels = curStock.technicals.pivotClassic,
                            currentPrice = curStock.currentPrice
                        )
                    }

                    item {
                        VolatilityMetricsCard(technicals = curStock.technicals)
                    }
                }

                2 -> {
                    // TAB 3: 5Y Price Projection & Volatility Analysis (Interactive Simulator!)
                    item {
                        Text(
                            text = "5-YEAR PREDICTIVE MODEL & MONTE CARLO SIMULATION",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    item {
                        MonteCarloProjectionFanChart(
                            projections = curStock.projections,
                            currentPrice = curStock.currentPrice
                        )
                    }

                    item {
                        ProjectionTargetsCard(
                            currentPrice = curStock.currentPrice,
                            projections = curStock.projections
                        )
                    }

                    item {
                        HistoricalVolatilityBreakdownCard(projections = curStock.projections)
                    }

                    item {
                        // Interactive Scenario Tweaker
                        SimulationControlSliders(
                            viewModel = viewModel,
                            onSaveScenario = { showSaveScenarioDialog = true }
                        )
                    }
                }

                3 -> {
                    // TAB 4: Listing Status & Historical IPO Performance Metrics
                    item {
                        ListingAndIpoPerformanceCard(
                            metrics = curStock.ipoMetrics,
                            currentPrice = curStock.currentPrice
                        )
                    }
                }

                4 -> {
                    // TAB 5: Shareholding Pattern & Institutional Trends
                    item {
                        ShareholdingOverviewCard(shareholding = curStock.shareholding)
                    }

                    item {
                        MajorShareholdersListCard(holders = curStock.shareholding.majorHolders)
                    }

                    item {
                        InstitutionalDealsCard(deals = curStock.institutionalDeals)
                    }
                }

                5 -> {
                    // TAB 6: Corporate Governance, Management Team & Filings
                    item {
                        CorporateGovernanceHeaderCard(
                            governance = curStock.corporateGovernance,
                            stock = curStock
                        )
                    }

                    item {
                        ManagementTeamList(profiles = curStock.corporateGovernance.managementTeam)
                    }

                    item {
                        RegulatoryFilingsCard(filings = curStock.regulatoryFilings)
                    }

                    item {
                        DividendHistoryCard(dividends = curStock.dividendHistory)
                    }
                }

                6 -> {
                    // TAB 7: Potential Risks & Growth Opportunities
                    item {
                        RisksAndOpportunitiesCard(
                            risksAndOpp = curStock.risksAndOpportunities,
                            stock = curStock
                        )
                    }
                }
            }
        }
    }

    if (showSaveScenarioDialog) {
        var scenarioName by remember { mutableStateOf("${curStock.scripId} 5Y Base Model") }
        AlertDialog(
            onDismissRequest = { showSaveScenarioDialog = false },
            title = { Text("Save 5-Year Simulation Scenario") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Save current growth (${viewModel.simRevenueGrowth.value}%), margin (${viewModel.simOperatingMargin.value}%), and volatility assumptions to your saved scenarios lab.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = scenarioName,
                        onValueChange = { scenarioName = it },
                        label = { Text("Scenario Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCurrentProjectionScenario(scenarioName)
                        showSaveScenarioDialog = false
                    }
                ) {
                    Text("Save Scenario")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveScenarioDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PriceHeaderStrip(stock: BseStock) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "₹${"%.2f".format(stock.currentPrice)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BSE LTP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            val isPos = stock.isPositive
            val color = if (isPos) BullGreen else BearRed
            Surface(
                color = color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${if (isPos) "+" else ""}${"%.2f".format(stock.dayChange)} (${"%.2f".format(stock.dayChangePercent)}%)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun MacdSummaryCard(technicals: TechnicalParameters) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MACD (12, 26, 9)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    color = BullGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = technicals.macdStatus,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = BullGreen,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailMiniItem("MACD Line", "${technicals.macdLine}", modifier = Modifier.weight(1f))
                DetailMiniItem("Signal Line", "${technicals.macdSignal}", modifier = Modifier.weight(1f))
                DetailMiniItem("Histogram", "${technicals.macdHistogram}", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProjectionTargetsCard(
    currentPrice: Double,
    projections: PriceProjectionModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "QUANTITATIVE CAGR TARGET CONES",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    color = DalalBlueLight.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "Base CAGR: +${projections.projectedCagrBase}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = DalalBlueLight,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Target table
            TargetHorizonRow("1-Year Horizon", projections.target1yConservative, projections.target1yBase, projections.target1yBull, currentPrice)
            TargetHorizonRow("3-Year Horizon", projections.target3yConservative, projections.target3yBase, projections.target3yBull, currentPrice)
            TargetHorizonRow("5-Year Horizon", projections.target5yConservative, projections.target5yBase, projections.target5yBull, currentPrice)
        }
    }
}

@Composable
private fun TargetHorizonRow(
    horizon: String,
    bear: Double,
    base: Double,
    bull: Double,
    currentPrice: Double
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(horizon, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Bear Case (-1.65σ)", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = BearRed)
                    Text("₹${"%.0f".format(bear)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = BearRed)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Base Expected", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = DalalBlueLight)
                    Text("₹${"%.0f".format(base)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black, color = DalalBlueLight)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Bull Case (+1.65σ)", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = BullGreen)
                    Text("₹${"%.0f".format(bull)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = BullGreen)
                }
            }
        }
    }
}

@Composable
private fun HistoricalVolatilityBreakdownCard(projections: PriceProjectionModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "VOLATILITY & DOWNSIDE TAIL RISK",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DetailMiniItem("30D Vol", "${projections.historicalVolatility30d}%", modifier = Modifier.weight(1f))
                DetailMiniItem("1Y Vol (σ)", "${projections.historicalVolatility1y}%", modifier = Modifier.weight(1f))
                DetailMiniItem("Max Drawdown", "-${projections.maxDrawdown5y}%", modifier = Modifier.weight(1f))
                DetailMiniItem("1D VaR 95%", "${projections.valueAtRisk95}%", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SimulationControlSliders(
    viewModel: BseViewModel,
    onSaveScenario: () -> Unit
) {
    val growth by viewModel.simRevenueGrowth.collectAsState()
    val margin by viewModel.simOperatingMargin.collectAsState()
    val vol by viewModel.simVolatility.collectAsState()
    val discount by viewModel.simDiscountRate.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ADVANCED SCENARIO LAB (DYNAMIC)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = { viewModel.resetSimulation() }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Slider 1: Revenue Growth
            SliderItem(
                label = "Expected Revenue Growth (CAGR)",
                valueText = "${"%.1f".format(growth)}%",
                value = growth.toFloat(),
                valueRange = 2f..40f,
                onValueChange = { viewModel.updateSimulationParameters(it.toDouble(), margin, vol, discount) }
            )

            // Slider 2: Operating Margin
            SliderItem(
                label = "Terminal Operating Margin",
                valueText = "${"%.1f".format(margin)}%",
                value = margin.toFloat(),
                valueRange = 5f..50f,
                onValueChange = { viewModel.updateSimulationParameters(growth, it.toDouble(), vol, discount) }
            )

            // Slider 3: Volatility Assumption
            SliderItem(
                label = "Annual Volatility (σ)",
                valueText = "${"%.1f".format(vol)}%",
                value = vol.toFloat(),
                valueRange = 10f..60f,
                onValueChange = { viewModel.updateSimulationParameters(growth, margin, it.toDouble(), discount) }
            )

            // Slider 4: Discount Rate
            SliderItem(
                label = "Cost of Capital / WACC Discount Rate",
                valueText = "${"%.1f".format(discount)}%",
                value = discount.toFloat(),
                valueRange = 8f..18f,
                onValueChange = { viewModel.updateSimulationParameters(growth, margin, vol, it.toDouble()) }
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onSaveScenario,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Scenario to Projection Lab")
            }
        }
    }
}

@Composable
private fun SliderItem(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            Text(valueText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = DalalBlueLight)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = DalalBlueLight,
                activeTrackColor = DalalBlueLight
            )
        )
    }
}

@Composable
private fun ListingAndIpoPerformanceCard(
    metrics: ListingAndIpoMetrics,
    currentPrice: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "BSE LISTING STATUS & HISTORICAL IPO AUDIT",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailMiniItem("BSE Scrip Group", metrics.bseScripGroup, modifier = Modifier.weight(1f))
                DetailMiniItem("Trading Status", metrics.listingStatus, modifier = Modifier.weight(1f))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Text("HISTORICAL IPO PERFORMANCE METRICS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = DalalGold)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailMiniItem("IPO Date", metrics.ipoIssueDate, modifier = Modifier.weight(1f))
                DetailMiniItem("Issue Price", "₹${metrics.ipoIssuePrice}", modifier = Modifier.weight(1f))
                DetailMiniItem("Issue Size", "₹${metrics.ipoIssueSizeCr} Cr", modifier = Modifier.weight(1f))
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("SUBSCRIPTION RATIOS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("QIB: ${metrics.subscriptionQib}x", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Text("HNI/NII: ${metrics.subscriptionHni}x", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Text("Retail: ${metrics.subscriptionRetail}x", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Text("Total: ${metrics.subscriptionTotal}x", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = DalalGold)
                    }
                }
            }

            // Listing Pop & Multi-bagger gain
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Listing Day Pop", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("+${metrics.listingDayGainPercent}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BullGreen)
                        Text("Close: ₹${metrics.listingDayClose}", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Since IPO Return", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${metrics.multibaggerFactor}x BAGGER", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = DalalGold)
                        Text("+${"%.0f".format(metrics.allTimeGainPercent)}%", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = BullGreen)
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = BullGreen.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Alpha vs SENSEX since listing: +${"%.0f".format(metrics.outperformanceVsSensex)}% outperformance",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = BullGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareholdingOverviewCard(shareholding: ShareholdingSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SHAREHOLDING PATTERN",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = shareholding.quarter,
                    style = MaterialTheme.typography.labelSmall,
                    color = DalalBlueLight
                )
            }

            ShareholdingDonutChart(
                promoter = shareholding.promoterPercent,
                fii = shareholding.fiiPercent,
                dii = shareholding.diiPercent,
                publicRetail = shareholding.publicRetailPercent
            )

            // Critical BSE Risk Indicator: Promoter Pledge
            val isPledgeZero = shareholding.promoterPledgedPercent == 0.0
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isPledgeZero) BullGreen.copy(alpha = 0.12f) else BearRed.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Promoter Shares Pledged:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isPledgeZero) "0.0% (Zero Pledge - Safe)" else "${shareholding.promoterPledgedPercent}% (Pledged)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isPledgeZero) BullGreen else BearRed
                    )
                }
            }
        }
    }
}

@Composable
private fun MajorShareholdersListCard(holders: List<com.example.data.model.MajorShareholder>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "MAJOR INSTITUTIONAL & PROMOTER HOLDERS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            holders.forEach { holder ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = holder.holderName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${holder.category} • ${"%.2f".format(holder.sharesHeld / 10000000.0)} Cr Shares",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = DalalBlueLight.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${holder.percentage}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = DalalBlueLight,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstitutionalDealsCard(deals: List<InstitutionalDeal>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "RECENT BSE BULK & BLOCK DEALS (INSTITUTIONAL)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (deals.isEmpty()) {
                Text("No recent bulk or block deals reported.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                deals.forEach { deal ->
                    val isBuy = deal.action == "BUY"
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = deal.clientName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${deal.date} • ${deal.dealType} • ₹${deal.price}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Surface(
                                    color = (if (isBuy) BullGreen else BearRed).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "${deal.action} ₹${deal.valueCr} Cr",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isBuy) BullGreen else BearRed,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "${"%.1f".format(deal.quantity / 100000.0)}L Qty",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CorporateGovernanceHeaderCard(
    governance: CorporateGovernance,
    stock: BseStock
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "CORPORATE STRUCTURE & REGISTERED ENTITY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            DetailMiniItem("Corporate Group", governance.corporateGroup)
            DetailMiniItem("CIN", governance.cin)
            DetailMiniItem("Registered Office", governance.registeredOffice)
            DetailMiniItem("Statutory Auditor", governance.auditor)
            DetailMiniItem("Investor Portal", governance.website)
        }
    }
}

@Composable
private fun ManagementTeamList(profiles: List<ManagementProfile>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "KEY MANAGEMENT PROFILES & BOARD OF DIRECTORS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            profiles.forEach { profile ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = profile.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                color = DalalBlueLight.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "${profile.tenureYears} yrs tenure",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = DalalBlueLight,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = profile.designation,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = DalalGold
                        )
                        Text(
                            text = "Qualification: ${profile.qualification}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = profile.bio,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RegulatoryFilingsCard(filings: List<RegulatoryFiling>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "BSE REGULATION 30 & 33 FILING SUMMARIES",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            filings.forEach { filing ->
                val tagColor = when (filing.sentiment) {
                    "POSITIVE" -> BullGreen
                    "CAUTION" -> BearRed
                    else -> DalalBlueLight
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = filing.regulationType,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = tagColor
                            )
                            Text(
                                text = filing.filingDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = filing.title,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = filing.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DividendHistoryCard(dividends: List<DividendRecord>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "CORPORATE DIVIDEND HISTORY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (dividends.isEmpty()) {
                Text("No recent dividend distributions (Company reinvesting capital into expansion).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                dividends.forEach { div ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Ex-Date: ${div.exDate}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text("${div.dividendType} Dividend • Payout: ${div.payoutRatio}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("₹${"%.2f".format(div.amountPerShare)} / Sh", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = BullGreen)
                                Text("Yield: ${div.yieldPercent}%", style = MaterialTheme.typography.labelSmall, color = DalalGold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RisksAndOpportunitiesCard(
    risksAndOpp: RisksAndOpportunities,
    stock: BseStock
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "STRATEGIC ASSESSMENT: RISKS & GROWTH CATALYSTS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Growth Opportunities
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("KEY GROWTH CATALYSTS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = BullGreen)
                }
                risksAndOpp.growthOpportunities.forEach { opp ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("•", color = BullGreen, fontWeight = FontWeight.Bold)
                        Text(opp, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Strategic Risks
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("STRATEGIC & REGULATORY RISKS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = BearRed)
                }
                risksAndOpp.strategicRisks.forEach { risk ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("•", color = BearRed, fontWeight = FontWeight.Bold)
                        Text(risk, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Analyst Consensus & Target
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Dalal Street Consensus:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(risksAndOpp.analystConsensus, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = BullGreen)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Mean 12M Target Price:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${"%.2f".format(risksAndOpp.targetConsensusMean)} (+${"%.1f".format(((risksAndOpp.targetConsensusMean - stock.currentPrice) / stock.currentPrice) * 100)}%)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = DalalBlueLight)
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DalalBlueLight.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("QUANTITATIVE VERDICT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = DalalBlueLight)
                    Text(risksAndOpp.quantitativeVerdict, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun DetailMiniItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
