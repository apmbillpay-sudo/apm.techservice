package com.example.data.model

data class BseStock(
    val scripCode: String,          // e.g. "500325"
    val scripId: String,            // e.g. "RELIANCE"
    val companyName: String,        // e.g. "Reliance Industries Ltd"
    val sector: String,             // e.g. "Conglomerate / Energy"
    val group: String,              // "A", "B", "T"
    val marketCapType: MarketCapType,
    val currentPrice: Double,       // Last Traded Price (₹)
    val previousClose: Double,
    val dayOpen: Double,
    val dayHigh: Double,
    val dayLow: Double,
    val volume: Long,
    val turnoverCr: Double,         // ₹ Crores
    val fiftyTwoWeekHigh: Double,
    val fiftyTwoWeekLow: Double,
    val upperCircuit: Double,
    val lowerCircuit: Double,
    val peRatio: Double,
    val pbRatio: Double,
    val sectorPe: Double,
    val dividendYield: Double,      // %
    val faceValue: Double,
    val technicals: TechnicalParameters,
    val projections: PriceProjectionModel,
    val ipoMetrics: ListingAndIpoMetrics,
    val corporateGovernance: CorporateGovernance,
    val shareholding: ShareholdingSummary,
    val dividendHistory: List<DividendRecord>,
    val regulatoryFilings: List<RegulatoryFiling>,
    val institutionalDeals: List<InstitutionalDeal>,
    val risksAndOpportunities: RisksAndOpportunities,
    val historicalQuotes: List<HistoricalQuote>
) {
    val dayChange: Double get() = currentPrice - previousClose
    val dayChangePercent: Double get() = if (previousClose > 0) ((currentPrice - previousClose) / previousClose) * 100 else 0.0
    val isPositive: Boolean get() = dayChange >= 0
    val marketCapCr: Double get() = when (marketCapType) {
        MarketCapType.LARGE_CAP -> (currentPrice * 676.0) / 100.0 // Approximate representation
        MarketCapType.MID_CAP -> (currentPrice * 180.0) / 100.0
        MarketCapType.SMALL_CAP -> (currentPrice * 45.0) / 100.0
    }
}

enum class MarketCapType {
    LARGE_CAP, MID_CAP, SMALL_CAP
}

enum class TechnicalSignal {
    STRONG_BUY, BUY, NEUTRAL, SELL, STRONG_SELL
}

data class TechnicalParameters(
    val rsi14: Double,
    val rsiStatus: String,          // "Neutral (56.4)", "Overbought (74.2)", "Oversold (28.1)"
    val macdLine: Double,
    val macdSignal: Double,
    val macdHistogram: Double,
    val macdStatus: String,         // "Bullish Crossover", "Bearish Momentum", etc.
    val bollingerUpper: Double,
    val bollingerMiddle: Double,
    val bollingerLower: Double,
    val bollingerBandwidth: Double,
    val sma20: Double,
    val sma50: Double,
    val sma200: Double,
    val dmaCrossSignal: String,     // "Golden Cross (Bullish)", "Above 200 DMA", etc.
    val pivotClassic: PivotLevels,
    val pivotFibonacci: PivotLevels,
    val stochasticK: Double,
    val stochasticD: Double,
    val atr14: Double,              // Average True Range (₹)
    val betaSensex: Double,         // Beta relative to BSE SENSEX
    val sharpeRatio: Double,
    val alphaRatio: Double,
    val overallSignal: TechnicalSignal,
    val technicalScore: Int         // 0 to 100
)

data class PivotLevels(
    val r3: Double,
    val r2: Double,
    val r1: Double,
    val pivot: Double,
    val s1: Double,
    val s2: Double,
    val s3: Double
)

data class PriceProjectionModel(
    val historicalVolatility30d: Double,  // Annualized %
    val historicalVolatility90d: Double,
    val historicalVolatility1y: Double,
    val historicalVolatility5y: Double,
    val maxDrawdown5y: Double,            // %
    val valueAtRisk95: Double,            // % (1-day or monthly VaR)
    val expectedShortfall: Double,        // %
    val cagrHistorical5y: Double,         // %
    // Projected target prices at end of 1Y, 2Y, 3Y, 4Y, 5Y
    val target1yConservative: Double,
    val target1yBase: Double,
    val target1yBull: Double,
    val target3yConservative: Double,
    val target3yBase: Double,
    val target3yBull: Double,
    val target5yConservative: Double,
    val target5yBase: Double,
    val target5yBull: Double,
    // Expected CAGR rates
    val projectedCagrBase: Double,
    val projectedCagrBull: Double,
    val projectedCagrBear: Double,
    // Monte Carlo simulation trajectories: list of simulated price paths over 60 months
    val monteCarloP5: List<Double>,       // 5th percentile (Bearish floor)
    val monteCarloP25: List<Double>,      // 25th percentile
    val monteCarloP50: List<Double>,      // 50th percentile (Median expected path)
    val monteCarloP75: List<Double>,      // 75th percentile
    val monteCarloP95: List<Double>,      // 95th percentile (Bullish ceiling)
    val baseRevenueGrowthRate: Double,    // % input parameter
    val terminalOperatingMargin: Double,  // % input parameter
    val discountRate: Double              // % input parameter
)

data class ListingAndIpoMetrics(
    val listingDate: String,        // e.g. "1977-11-29"
    val listingStatus: String,      // "Active - Normal Settlement (T+1)"
    val bseScripGroup: String,      // "A - High Liquidity"
    val isinCode: String,           // e.g. "INE002A01018"
    val ipoIssueDate: String,       // e.g. "Nov 1977"
    val ipoIssuePrice: Double,      // ₹
    val ipoIssueSizeCr: Double,     // ₹ Cr
    val subscriptionQib: Double,    // Times
    val subscriptionHni: Double,
    val subscriptionRetail: Double,
    val subscriptionTotal: Double,
    val listingDayOpen: Double,     // ₹
    val listingDayClose: Double,    // ₹
    val listingDayGainPercent: Double,
    val allTimeGainPercent: Double, // % since IPO
    val multibaggerFactor: Double,  // e.g. 240x
    val outperformanceVsSensex: Double // % outperformance
)

data class CorporateGovernance(
    val corporateGroup: String,     // e.g. "Reliance Group", "Tata Group"
    val cin: String,                // Corporate Identity Number
    val registeredOffice: String,   // Address
    val website: String,
    val auditor: String,
    val managementTeam: List<ManagementProfile>
)

data class ManagementProfile(
    val name: String,
    val designation: String,        // e.g. "Chairman & Managing Director"
    val tenureYears: Int,
    val qualification: String,
    val bio: String
)

data class ShareholdingSummary(
    val quarter: String,            // e.g. "Q1 FY26 (June 2026)"
    val promoterPercent: Double,
    val promoterPledgedPercent: Double, // Critical BSE risk factor
    val fiiPercent: Double,         // Foreign Institutional Investors
    val diiPercent: Double,         // Domestic Institutional Investors (Mutual Funds/Insurance)
    val publicRetailPercent: Double,
    val governmentPercent: Double,
    val nonInstitutionPercent: Double,
    val promoterChangeQoQ: Double,
    val fiiChangeQoQ: Double,
    val diiChangeQoQ: Double,
    val majorHolders: List<MajorShareholder>
)

data class MajorShareholder(
    val holderName: String,
    val category: String,           // "Promoter", "FII / FPI", "Mutual Fund", "Insurance"
    val percentage: Double,
    val sharesHeld: Long
)

data class InstitutionalDeal(
    val date: String,
    val dealType: String,           // "Bulk Deal" or "Block Deal"
    val clientName: String,
    val action: String,             // "BUY" or "SELL"
    val quantity: Long,
    val price: Double,
    val valueCr: Double
)

data class DividendRecord(
    val exDate: String,
    val dividendType: String,       // "Interim", "Final", "Special"
    val amountPerShare: Double,     // ₹
    val yieldPercent: Double,
    val payoutRatio: Double         // %
)

data class RegulatoryFiling(
    val filingDate: String,
    val regulationType: String,     // "Reg 30 (Material Event)", "Reg 33 (Financial Results)", "SEBI PIT"
    val title: String,
    val summary: String,
    val sentiment: String           // "POSITIVE", "NEUTRAL", "CAUTION"
)

data class RisksAndOpportunities(
    val growthOpportunities: List<String>,
    val strategicRisks: List<String>,
    val analystConsensus: String,   // "82% BUY (34 Analysts)"
    val targetConsensusMean: Double,
    val quantitativeVerdict: String
)

data class HistoricalQuote(
    val date: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
)

data class BseMarketSummary(
    val sensexCurrent: Double,
    val sensexChange: Double,
    val sensexChangePercent: Double,
    val bse100Current: Double,
    val bse100ChangePercent: Double,
    val bseMidcapCurrent: Double,
    val bseMidcapChangePercent: Double,
    val bseSmallcapCurrent: Double,
    val bseSmallcapChangePercent: Double,
    val advancesCount: Int,
    val declinesCount: Int,
    val unchangedCount: Int,
    val totalTurnoverCr: Double
)
