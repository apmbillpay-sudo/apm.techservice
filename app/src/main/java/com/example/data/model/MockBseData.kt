package com.example.data.model

import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

object MockBseData {

    fun generateMonteCarloPaths(
        currentPrice: Double,
        annualDrift: Double,
        annualVol: Double,
        months: Int = 60
    ): Map<String, List<Double>> {
        val dt = 1.0 / 12.0
        val p5 = mutableListOf(currentPrice)
        val p25 = mutableListOf(currentPrice)
        val p50 = mutableListOf(currentPrice)
        val p75 = mutableListOf(currentPrice)
        val p95 = mutableListOf(currentPrice)

        var cur5 = currentPrice
        var cur25 = currentPrice
        var cur50 = currentPrice
        var cur75 = currentPrice
        var cur95 = currentPrice

        val mu = annualDrift - 0.5 * annualVol * annualVol

        for (m in 1..months) {
            val t = m * dt
            // Quantile analytical solutions for Geometric Brownian Motion
            // z-scores: p5 = -1.645, p25 = -0.674, p50 = 0.0, p75 = +0.674, p95 = +1.645
            cur5 = currentPrice * exp(mu * t - 1.645 * annualVol * sqrt(t))
            cur25 = currentPrice * exp(mu * t - 0.674 * annualVol * sqrt(t))
            cur50 = currentPrice * exp(mu * t)
            cur75 = currentPrice * exp(mu * t + 0.674 * annualVol * sqrt(t))
            cur95 = currentPrice * exp(mu * t + 1.645 * annualVol * sqrt(t))

            p5.add(round2(cur5))
            p25.add(round2(cur25))
            p50.add(round2(cur50))
            p75.add(round2(cur75))
            p95.add(round2(cur95))
        }

        return mapOf(
            "p5" to p5,
            "p25" to p25,
            "p50" to p50,
            "p75" to p75,
            "p95" to p95
        )
    }

    fun recalculateProjections(
        basePrice: Double,
        revenueGrowthRate: Double,
        operatingMargin: Double,
        volatilityPercent: Double,
        discountRate: Double
    ): PriceProjectionModel {
        val annualVol = (volatilityPercent / 100.0).coerceIn(0.08, 0.65)
        val baseGrowth = (revenueGrowthRate / 100.0).coerceIn(0.02, 0.45)
        val marginBonus = (operatingMargin - 18.0) * 0.003
        val effectiveDrift = (baseGrowth + marginBonus).coerceIn(0.04, 0.35)

        val paths = generateMonteCarloPaths(basePrice, effectiveDrift, annualVol, 60)

        val cagrBase = (effectiveDrift * 100.0)
        val cagrBull = ((effectiveDrift + annualVol * 0.5) * 100.0)
        val cagrBear = ((effectiveDrift - annualVol * 0.4).coerceAtLeast(0.02) * 100.0)

        val target1yBase = round2(basePrice * (1 + effectiveDrift))
        val target1yBull = round2(basePrice * (1 + effectiveDrift + annualVol * 0.4))
        val target1yBear = round2(basePrice * (1 + (effectiveDrift - annualVol * 0.3).coerceAtLeast(-0.25)))

        val target3yBase = round2(basePrice * (1 + effectiveDrift).pow(3))
        val target3yBull = round2(basePrice * (1 + effectiveDrift + annualVol * 0.35).pow(3))
        val target3yBear = round2(basePrice * (1 + (effectiveDrift - annualVol * 0.25).coerceAtLeast(0.01)).pow(3))

        val target5yBase = round2(basePrice * (1 + effectiveDrift).pow(5))
        val target5yBull = round2(basePrice * (1 + effectiveDrift + annualVol * 0.3).pow(5))
        val target5yBear = round2(basePrice * (1 + (effectiveDrift - annualVol * 0.2).coerceAtLeast(0.02)).pow(5))

        val var95 = round2(1.645 * (annualVol / sqrt(252.0)) * 100.0) // 1-day 95% VaR
        val expectedShortfall = round2(var95 * 1.25)
        val maxDrawdown = round2((annualVol * 1.75 * 100.0).coerceAtMost(62.0))

        return PriceProjectionModel(
            historicalVolatility30d = round2(volatilityPercent * 0.92),
            historicalVolatility90d = round2(volatilityPercent * 0.98),
            historicalVolatility1y = round2(volatilityPercent),
            historicalVolatility5y = round2(volatilityPercent * 1.05),
            maxDrawdown5y = maxDrawdown,
            valueAtRisk95 = var95,
            expectedShortfall = expectedShortfall,
            cagrHistorical5y = round2(cagrBase * 0.95),
            target1yConservative = target1yBear,
            target1yBase = target1yBase,
            target1yBull = target1yBull,
            target3yConservative = target3yBear,
            target3yBase = target3yBase,
            target3yBull = target3yBull,
            target5yConservative = target5yBear,
            target5yBase = target5yBase,
            target5yBull = target5yBull,
            projectedCagrBase = round2(cagrBase),
            projectedCagrBull = round2(cagrBull),
            projectedCagrBear = round2(cagrBear),
            monteCarloP5 = paths["p5"] ?: emptyList(),
            monteCarloP25 = paths["p25"] ?: emptyList(),
            monteCarloP50 = paths["p50"] ?: emptyList(),
            monteCarloP75 = paths["p75"] ?: emptyList(),
            monteCarloP95 = paths["p95"] ?: emptyList(),
            baseRevenueGrowthRate = revenueGrowthRate,
            terminalOperatingMargin = operatingMargin,
            discountRate = discountRate
        )
    }

    private fun round2(v: Double): Double = (Math.round(v * 100.0) / 100.0)

    val sampleMarketSummary = BseMarketSummary(
        sensexCurrent = 82497.10,
        sensexChange = 432.80,
        sensexChangePercent = 0.53,
        bse100Current = 25840.45,
        bse100ChangePercent = 0.61,
        bseMidcapCurrent = 48920.15,
        bseMidcapChangePercent = 0.78,
        bseSmallcapCurrent = 56210.30,
        bseSmallcapChangePercent = 0.94,
        advancesCount = 2380,
        declinesCount = 1420,
        unchangedCount = 115,
        totalTurnoverCr = 8492.35
    )

    fun getInitialStocks(): List<BseStock> {
        return listOf(
            createReliance(),
            createTcs(),
            createHdfcBank(),
            createInfosys(),
            createLarsenToubro(),
            createItc(),
            createBhartiAirtel(),
            createSbi(),
            createTataMotors(),
            createBseLimited(),
            createSunPharma(),
            createZomato()
        )
    }

    private fun createReliance(): BseStock {
        val curPrice = 2942.50
        val prevClose = 2915.20
        val history = generateHistory(curPrice, 2200.0, 3050.0)
        val proj = recalculateProjections(curPrice, 14.5, 18.2, 19.5, 11.0)
        return BseStock(
            scripCode = "500325",
            scripId = "RELIANCE",
            companyName = "Reliance Industries Ltd",
            sector = "Conglomerate / Energy & Retail",
            group = "A",
            marketCapType = MarketCapType.LARGE_CAP,
            currentPrice = curPrice,
            previousClose = prevClose,
            dayOpen = 2920.00,
            dayHigh = 2958.40,
            dayLow = 2918.00,
            volume = 4125890,
            turnoverCr = 1215.42,
            fiftyTwoWeekHigh = 3217.90,
            fiftyTwoWeekLow = 2221.05,
            upperCircuit = 3206.70,
            lowerCircuit = 2623.70,
            peRatio = 27.8,
            pbRatio = 2.45,
            sectorPe = 22.4,
            dividendYield = 0.35,
            faceValue = 10.0,
            technicals = TechnicalParameters(
                rsi14 = 59.8,
                rsiStatus = "Neutral (59.8)",
                macdLine = 18.4,
                macdSignal = 12.1,
                macdHistogram = 6.3,
                macdStatus = "Bullish Crossover",
                bollingerUpper = 3012.0,
                bollingerMiddle = 2920.0,
                bollingerLower = 2828.0,
                bollingerBandwidth = 6.3,
                sma20 = 2924.5,
                sma50 = 2890.2,
                sma200 = 2685.0,
                dmaCrossSignal = "Trading Above 200 DMA",
                pivotClassic = PivotLevels(r3 = 3020.0, r2 = 2980.0, r1 = 2960.0, pivot = 2932.0, s1 = 2910.0, s2 = 2885.0, s3 = 2850.0),
                pivotFibonacci = PivotLevels(r3 = 3010.0, r2 = 2975.0, r1 = 2952.0, pivot = 2932.0, s1 = 2912.0, s2 = 2889.0, s3 = 2854.0),
                stochasticK = 64.2,
                stochasticD = 58.5,
                atr14 = 42.50,
                betaSensex = 1.08,
                sharpeRatio = 1.42,
                alphaRatio = 3.85,
                overallSignal = TechnicalSignal.BUY,
                technicalScore = 74
            ),
            projections = proj,
            ipoMetrics = ListingAndIpoMetrics(
                listingDate = "1977-11-29",
                listingStatus = "Active - Normal T+1 Rolling",
                bseScripGroup = "A - Premier High Liquidity",
                isinCode = "INE002A01018",
                ipoIssueDate = "Nov 1977",
                ipoIssuePrice = 10.0,
                ipoIssueSizeCr = 2.82,
                subscriptionQib = 4.2,
                subscriptionHni = 3.5,
                subscriptionRetail = 7.1,
                subscriptionTotal = 5.8,
                listingDayOpen = 14.50,
                listingDayClose = 16.00,
                listingDayGainPercent = 60.0,
                allTimeGainPercent = 294200.0,
                multibaggerFactor = 2942.0,
                outperformanceVsSensex = 425.0
            ),
            corporateGovernance = CorporateGovernance(
                corporateGroup = "Reliance Group (Mukesh Ambani)",
                cin = "L17110MH1973PLC019786",
                registeredOffice = "Maker Chambers IV, 222 Nariman Point, Mumbai 400021",
                website = "www.ril.com",
                auditor = "S.R. Batliboi & Co. LLP",
                managementTeam = listOf(
                    ManagementProfile("Mukesh D. Ambani", "Chairman & Managing Director", 46, "Chemical Engineer (UDCT), MBA (Stanford)", "Visionary Indian industrialist leading RIL expansion into Telecom (Jio), Retail, and New Green Energy giga-complexes."),
                    ManagementProfile("Isha M. Ambani", "Executive Director - Retail", 8, "Yale University, Stanford GSB", "Driving rapid omnichannel scaling of Reliance Retail across 18,000+ stores nationally."),
                    ManagementProfile("Akash M. Ambani", "Chairman - Reliance Jio", 10, "Brown University", "Heading digital telecom network operations, 5G standalone architecture, and cloud infrastructure."),
                    ManagementProfile("Srikanth Venkatachari", "Chief Financial Officer", 14, "Chartered Accountant", "Oversees global capital allocation, debt management, and treasury operations.")
                )
            ),
            shareholding = ShareholdingSummary(
                quarter = "Q1 FY26 (June 2026)",
                promoterPercent = 50.31,
                promoterPledgedPercent = 0.0, // Zero pledge!
                fiiPercent = 21.84,
                diiPercent = 16.92,
                publicRetailPercent = 10.74,
                governmentPercent = 0.19,
                nonInstitutionPercent = 0.0,
                promoterChangeQoQ = 0.02,
                fiiChangeQoQ = 0.45,
                diiChangeQoQ = 0.12,
                majorHolders = listOf(
                    MajorShareholder("Devarshi Commercials LLP (Promoter Group)", "Promoter", 8.01, 542000000),
                    MajorShareholder("Life Insurance Corporation of India (LIC)", "Insurance", 6.25, 422800000),
                    MajorShareholder("Europacific Growth Fund", "FII / FPI", 2.15, 145400000),
                    MajorShareholder("SBI Bluechip Fund", "Mutual Fund", 1.84, 124500000),
                    MajorShareholder("Vanguard Emerging Markets Stock Index Fund", "FII / FPI", 1.48, 100100000)
                )
            ),
            dividendHistory = listOf(
                DividendRecord("2026-08-19", "Final", 10.00, 0.34, 11.2),
                DividendRecord("2025-08-21", "Final", 9.00, 0.31, 10.8),
                DividendRecord("2024-08-22", "Final", 8.00, 0.28, 10.1),
                DividendRecord("2023-08-21", "Final", 8.00, 0.32, 10.5)
            ),
            regulatoryFilings = listOf(
                RegulatoryFiling("2026-09-28", "Reg 30 (Material Event)", "Commercial Launch of 10GW Solar Giga-factory in Jamnagar", "Phase-1 operationalized ahead of schedule with 26% cell efficiency rating.", "POSITIVE"),
                RegulatoryFiling("2026-07-24", "Reg 33 (Financial Results)", "Q1 FY26 Consolidated Revenue up 11.8% YoY", "Jio ARPU increased to ₹195; Retail EBITDA margin expanded by 45 bps.", "POSITIVE"),
                RegulatoryFiling("2026-06-12", "SEBI PIT (Insider Trading)", "Disclosure under Reg 7(2) regarding ESOP allotment", "Ordinary course ESOP share transfer to designated executives.", "NEUTRAL")
            ),
            institutionalDeals = listOf(
                InstitutionalDeal("2026-09-15", "Block Deal", "Government Pension Fund Global", "BUY", 1250000, 2930.50, 366.31),
                InstitutionalDeal("2026-08-28", "Bulk Deal", "Morgan Stanley Asia Singapore", "BUY", 840000, 2895.00, 243.18),
                InstitutionalDeal("2026-07-30", "Block Deal", "Societe Generale", "SELL", 410000, 2910.00, 119.31)
            ),
            risksAndOpportunities = RisksAndOpportunities(
                growthOpportunities = listOf(
                    "Potential value-unlocking mega-IPOs for Reliance Retail and Jio Platforms on BSE/NSE.",
                    "Aggressive solar and hydrogen giga-factory commercialization positioning RIL as green energy titan.",
                    "5G monetization, enterprise cloud scaling, and AI data center joint venture with global hyperscalers."
                ),
                strategicRisks = listOf(
                    "Global crude oil refining margins (GRM) volatility and geopolitical oil transport supply chain shocks.",
                    "Elevated ongoing capital expenditure impacting near-term free cash flow yield.",
                    "Intense competitive pricing pressure in quick-commerce retail sector."
                ),
                analystConsensus = "84% BUY (38 Analysts)",
                targetConsensusMean = 3380.00,
                quantitativeVerdict = "Robust long-term compounder with defensible consumer duopoly and green energy optionality."
            ),
            historicalQuotes = history
        )
    }

    private fun createTcs(): BseStock {
        val curPrice = 4310.20
        val prevClose = 4280.50
        val history = generateHistory(curPrice, 3400.0, 4550.0)
        val proj = recalculateProjections(curPrice, 11.2, 26.5, 16.0, 10.5)
        return BseStock(
            scripCode = "532540",
            scripId = "TCS",
            companyName = "Tata Consultancy Services Ltd",
            sector = "Information Technology",
            group = "A",
            marketCapType = MarketCapType.LARGE_CAP,
            currentPrice = curPrice,
            previousClose = prevClose,
            dayOpen = 4295.00,
            dayHigh = 4335.00,
            dayLow = 4278.00,
            volume = 1845200,
            turnoverCr = 795.30,
            fiftyTwoWeekHigh = 4585.00,
            fiftyTwoWeekLow = 3450.00,
            upperCircuit = 4708.50,
            lowerCircuit = 3852.50,
            peRatio = 31.4,
            pbRatio = 14.8,
            sectorPe = 29.1,
            dividendYield = 2.15,
            faceValue = 1.0,
            technicals = TechnicalParameters(
                rsi14 = 54.2,
                rsiStatus = "Neutral (54.2)",
                macdLine = 22.8,
                macdSignal = 20.1,
                macdHistogram = 2.7,
                macdStatus = "Bullish Momentum",
                bollingerUpper = 4420.0,
                bollingerMiddle = 4280.0,
                bollingerLower = 4140.0,
                bollingerBandwidth = 6.5,
                sma20 = 4290.0,
                sma50 = 4220.0,
                sma200 = 3980.0,
                dmaCrossSignal = "Strong Bullish (Golden Cross)",
                pivotClassic = PivotLevels(r3 = 4410.0, r2 = 4370.0, r1 = 4340.0, pivot = 4305.0, s1 = 4275.0, s2 = 4240.0, s3 = 4200.0),
                pivotFibonacci = PivotLevels(r3 = 4400.0, r2 = 4360.0, r1 = 4335.0, pivot = 4305.0, s1 = 4278.0, s2 = 4245.0, s3 = 4205.0),
                stochasticK = 59.0,
                stochasticD = 55.4,
                atr14 = 58.20,
                betaSensex = 0.72,
                sharpeRatio = 1.85,
                alphaRatio = 5.20,
                overallSignal = TechnicalSignal.BUY,
                technicalScore = 78
            ),
            projections = proj,
            ipoMetrics = ListingAndIpoMetrics(
                listingDate = "2004-08-25",
                listingStatus = "Active - Normal T+1 Rolling",
                bseScripGroup = "A - Premier",
                isinCode = "INE467B01029",
                ipoIssueDate = "Aug 2004",
                ipoIssuePrice = 850.0, // Face value was adjusted post bonus 1:1 twice!
                ipoIssueSizeCr = 5420.0,
                subscriptionQib = 9.8,
                subscriptionHni = 7.4,
                subscriptionRetail = 3.6,
                subscriptionTotal = 7.7,
                listingDayOpen = 1010.0,
                listingDayClose = 987.50,
                listingDayGainPercent = 16.2,
                allTimeGainPercent = 3950.0,
                multibaggerFactor = 40.5,
                outperformanceVsSensex = 285.0
            ),
            corporateGovernance = CorporateGovernance(
                corporateGroup = "Tata Group",
                cin = "L22210MH1995PLC084781",
                registeredOffice = "TCS House, Raveline Street, Fort, Mumbai 400001",
                website = "www.tcs.com",
                auditor = "B S R & Co. LLP",
                managementTeam = listOf(
                    ManagementProfile("N. Chandrasekaran", "Chairman", 22, "MCA, Regional Engineering College", "Chairman of Tata Sons, pioneer of global IT offshoring and industrial engineering."),
                    ManagementProfile("K. Krithivasan", "MD & Chief Executive Officer", 35, "Mechanical Engg, IIT Kanpur", "Steering TCS's Generative AI practice, deep client banking relationships and cloud transition."),
                    ManagementProfile("Samir Seksaria", "Chief Financial Officer", 24, "Chartered Accountant", "Disciplined cost governance maintaining sector-leading 26% operating margins.")
                )
            ),
            shareholding = ShareholdingSummary(
                quarter = "Q1 FY26 (June 2026)",
                promoterPercent = 71.77,
                promoterPledgedPercent = 0.0,
                fiiPercent = 12.65,
                diiPercent = 10.42,
                publicRetailPercent = 5.16,
                governmentPercent = 0.0,
                nonInstitutionPercent = 0.0,
                promoterChangeQoQ = 0.0,
                fiiChangeQoQ = -0.15,
                diiChangeQoQ = 0.28,
                majorHolders = listOf(
                    MajorShareholder("Tata Sons Private Limited", "Promoter", 71.74, 2596000000),
                    MajorShareholder("Life Insurance Corporation of India", "Insurance", 4.12, 149100000),
                    MajorShareholder("SBI Nifty 50 ETF", "Mutual Fund", 1.25, 45200000),
                    MajorShareholder("Government of Singapore (GIC)", "FII / FPI", 1.10, 39800000)
                )
            ),
            dividendHistory = listOf(
                DividendRecord("2026-07-16", "Interim", 10.00, 0.23, 78.0),
                DividendRecord("2026-05-18", "Final", 28.00, 0.65, 82.0),
                DividendRecord("2026-01-19", "Special", 18.00, 0.42, 80.0),
                DividendRecord("2025-07-20", "Interim", 9.00, 0.21, 75.0)
            ),
            regulatoryFilings = listOf(
                RegulatoryFiling("2026-09-18", "Reg 30", "Strategic 10-year IT Transformation Deal with European Tier-1 Bank ($1.2B TCV)", "Mega enterprise AI migration deal signed.", "POSITIVE"),
                RegulatoryFiling("2026-07-11", "Reg 33", "Q1 FY26 Net Profit surges 8.7% YoY; Operating Margin resilient at 26.1%", "BFSI sector contract ramp-ups driving growth.", "POSITIVE")
            ),
            institutionalDeals = listOf(
                InstitutionalDeal("2026-08-14", "Block Deal", "Norges Bank Investment Management", "BUY", 420000, 4290.00, 180.18),
                InstitutionalDeal("2026-06-22", "Bulk Deal", "Vanguard International Stock Fund", "BUY", 350000, 4180.00, 146.30)
            ),
            risksAndOpportunities = RisksAndOpportunities(
                growthOpportunities = listOf(
                    "Massive corporate transformation contracts integrating Generative AI into enterprise ERP workflows.",
                    "Resurgence of BFSI spending in North America and Western Europe.",
                    "Highest free-cash generation in Indian IT with 80%+ dividend payout policy."
                ),
                strategicRisks = listOf(
                    "Macro headwinds in discretionary enterprise cloud consulting contracts.",
                    "H1B visa regulatory adjustments and higher local delivery onshore wage costs."
                ),
                analystConsensus = "78% BUY (42 Analysts)",
                targetConsensusMean = 4850.00,
                quantitativeVerdict = "Highest quality compounding defensive stock with rock-solid balance sheet and generous dividend yields."
            ),
            historicalQuotes = history
        )
    }

    private fun createHdfcBank(): BseStock {
        val curPrice = 1684.00
        val prevClose = 1672.50
        val history = generateHistory(curPrice, 1420.0, 1780.0)
        val proj = recalculateProjections(curPrice, 16.0, 22.0, 18.0, 11.2)
        return BseStock(
            scripCode = "500180",
            scripId = "HDFCBANK",
            companyName = "HDFC Bank Ltd",
            sector = "Banking & Financial Services",
            group = "A",
            marketCapType = MarketCapType.LARGE_CAP,
            currentPrice = curPrice,
            previousClose = prevClose,
            dayOpen = 1675.00,
            dayHigh = 1692.00,
            dayLow = 1670.00,
            volume = 5420100,
            turnoverCr = 912.80,
            fiftyTwoWeekHigh = 1794.00,
            fiftyTwoWeekLow = 1363.55,
            upperCircuit = 1839.75,
            lowerCircuit = 1505.25,
            peRatio = 18.2,
            pbRatio = 2.65,
            sectorPe = 16.8,
            dividendYield = 1.15,
            faceValue = 1.0,
            technicals = TechnicalParameters(
                rsi14 = 56.1,
                rsiStatus = "Neutral (56.1)",
                macdLine = 8.5,
                macdSignal = 6.2,
                macdHistogram = 2.3,
                macdStatus = "Bullish",
                bollingerUpper = 1720.0,
                bollingerMiddle = 1665.0,
                bollingerLower = 1610.0,
                bollingerBandwidth = 6.6,
                sma20 = 1668.0,
                sma50 = 1640.0,
                sma200 = 1580.0,
                dmaCrossSignal = "Above 200 DMA",
                pivotClassic = PivotLevels(r3 = 1720.0, r2 = 1705.0, r1 = 1695.0, pivot = 1680.0, s1 = 1668.0, s2 = 1655.0, s3 = 1640.0),
                pivotFibonacci = PivotLevels(r3 = 1715.0, r2 = 1700.0, r1 = 1692.0, pivot = 1680.0, s1 = 1670.0, s2 = 1658.0, s3 = 1642.0),
                stochasticK = 61.2,
                stochasticD = 57.8,
                atr14 = 22.40,
                betaSensex = 1.15,
                sharpeRatio = 1.35,
                alphaRatio = 2.90,
                overallSignal = TechnicalSignal.BUY,
                technicalScore = 72
            ),
            projections = proj,
            ipoMetrics = ListingAndIpoMetrics(
                listingDate = "1995-05-19",
                listingStatus = "Active - Normal T+1 Rolling",
                bseScripGroup = "A - Premier",
                isinCode = "INE040A01034",
                ipoIssueDate = "May 1995",
                ipoIssuePrice = 10.0,
                ipoIssueSizeCr = 50.0,
                subscriptionQib = 12.0,
                subscriptionHni = 8.5,
                subscriptionRetail = 6.2,
                subscriptionTotal = 8.9,
                listingDayOpen = 38.0,
                listingDayClose = 42.50,
                listingDayGainPercent = 325.0,
                allTimeGainPercent = 16840.0,
                multibaggerFactor = 168.4,
                outperformanceVsSensex = 340.0
            ),
            corporateGovernance = CorporateGovernance(
                corporateGroup = "HDFC Financial Conglomerate",
                cin = "L65920MH1994PLC080618",
                registeredOffice = "HDFC Bank House, Senapati Bapat Marg, Lower Parel, Mumbai 400013",
                website = "www.hdfcbank.com",
                auditor = "Price Waterhouse & Co Chartered Accountants",
                managementTeam = listOf(
                    ManagementProfile("Atanu Chakraborty", "Part-Time Chairman", 4, "IAS, Master in Public Policy", "Former Economic Affairs Secretary guiding corporate governance and compliance."),
                    ManagementProfile("Sashidhar Jagdishan", "Managing Director & CEO", 28, "Chartered Accountant, MSc Economics", "Spearheaded successful historic mega-merger with HDFC Ltd and deposit mobilization drive."),
                    ManagementProfile("Srinivasan Vaidyanathan", "Chief Financial Officer", 12, "Chartered Accountant, Cost Accountant", "Directs liability franchise strategy, net interest margin stability, and Basel III liquidity.")
                )
            ),
            shareholding = ShareholdingSummary(
                quarter = "Q1 FY26 (June 2026)",
                promoterPercent = 0.0, // Professionally managed bank post merger!
                promoterPledgedPercent = 0.0,
                fiiPercent = 47.15,
                diiPercent = 34.28,
                publicRetailPercent = 17.82,
                governmentPercent = 0.75,
                nonInstitutionPercent = 0.0,
                promoterChangeQoQ = 0.0,
                fiiChangeQoQ = 1.15,
                diiChangeQoQ = -0.40,
                majorHolders = listOf(
                    MajorShareholder("Life Insurance Corporation of India (LIC)", "Insurance", 5.20, 395000000),
                    MajorShareholder("SBI Mutual Fund (Combined Schemes)", "Mutual Fund", 4.85, 368000000),
                    MajorShareholder("Government Pension Fund Global", "FII / FPI", 3.12, 237000000),
                    MajorShareholder("ICICI Prudential Mutual Fund", "Mutual Fund", 2.80, 212500000)
                )
            ),
            dividendHistory = listOf(
                DividendRecord("2026-05-10", "Final", 19.50, 1.16, 21.5),
                DividendRecord("2025-05-15", "Final", 19.00, 1.25, 22.0),
                DividendRecord("2024-05-16", "Final", 15.50, 1.05, 20.0)
            ),
            regulatoryFilings = listOf(
                RegulatoryFiling("2026-08-30", "Reg 30", "RBI approval for setting up 250 new semi-urban branches", "Accelerating retail CASA deposit acquisition across non-metro regions.", "POSITIVE"),
                RegulatoryFiling("2026-07-20", "Reg 33", "Q1 FY26 Net Profit grows 15.2% YoY; Gross NPA down to 1.21%", "Deposit accretion outpaces credit growth; CD ratio moderates to 98%.", "POSITIVE")
            ),
            institutionalDeals = listOf(
                InstitutionalDeal("2026-09-02", "Block Deal", "Fidelity Investment Funds", "BUY", 1850000, 1680.00, 310.80),
                InstitutionalDeal("2026-08-11", "Bulk Deal", "BlackRock Institutional Trust", "BUY", 920000, 1665.50, 153.22)
            ),
            risksAndOpportunities = RisksAndOpportunities(
                growthOpportunities = listOf(
                    "Unrivaled distribution network with 8,800+ branches unlocking cross-selling of mortgages, auto loans, and insurance.",
                    "FII headroom normalization opening significant passive index weight additions (MSCI / FTSE).",
                    "Digitized underwriting scaling unsecured retail and MSME books safely."
                ),
                strategicRisks = listOf(
                    "Credit-Deposit (CD) ratio normalization requiring competitive pricing on term deposits.",
                    "Industry-wide net interest margin (NIM) pressure during RBI rate easing cycles."
                ),
                analystConsensus = "88% BUY (45 Analysts)",
                targetConsensusMean = 2050.00,
                quantitativeVerdict = "Blue-chip systemic anchor with premier asset quality, trading at multi-year historical valuation discount."
            ),
            historicalQuotes = history
        )
    }

    private fun createInfosys(): BseStock {
        val curPrice = 1892.40
        val prevClose = 1878.10
        val history = generateHistory(curPrice, 1380.0, 1960.0)
        val proj = recalculateProjections(curPrice, 12.0, 21.5, 17.5, 10.8)
        return BseStock(
            scripCode = "500209",
            scripId = "INFY",
            companyName = "Infosys Ltd",
            sector = "Information Technology",
            group = "A",
            marketCapType = MarketCapType.LARGE_CAP,
            currentPrice = curPrice,
            previousClose = prevClose,
            dayOpen = 1882.00,
            dayHigh = 1905.00,
            dayLow = 1875.00,
            volume = 3210400,
            turnoverCr = 608.20,
            fiftyTwoWeekHigh = 1990.00,
            fiftyTwoWeekLow = 1358.35,
            upperCircuit = 2065.90,
            lowerCircuit = 1690.30,
            peRatio = 28.5,
            pbRatio = 8.9,
            sectorPe = 29.1,
            dividendYield = 2.30,
            faceValue = 5.0,
            technicals = TechnicalParameters(
                rsi14 = 62.4,
                rsiStatus = "Mild Bullish (62.4)",
                macdLine = 16.2,
                macdSignal = 12.8,
                macdHistogram = 3.4,
                macdStatus = "Bullish",
                bollingerUpper = 1940.0,
                bollingerMiddle = 1870.0,
                bollingerLower = 1800.0,
                bollingerBandwidth = 7.5,
                sma20 = 1875.0,
                sma50 = 1820.0,
                sma200 = 1650.0,
                dmaCrossSignal = "Above 200 DMA",
                pivotClassic = PivotLevels(r3 = 1935.0, r2 = 1915.0, r1 = 1902.0, pivot = 1888.0, s1 = 1872.0, s2 = 1858.0, s3 = 1840.0),
                pivotFibonacci = PivotLevels(r3 = 1930.0, r2 = 1912.0, r1 = 1900.0, pivot = 1888.0, s1 = 1875.0, s2 = 1860.0, s3 = 1842.0),
                stochasticK = 68.0,
                stochasticD = 63.5,
                atr14 = 28.50,
                betaSensex = 0.88,
                sharpeRatio = 1.62,
                alphaRatio = 4.10,
                overallSignal = TechnicalSignal.BUY,
                technicalScore = 76
            ),
            projections = proj,
            ipoMetrics = ListingAndIpoMetrics(
                listingDate = "1993-06-14",
                listingStatus = "Active - Normal T+1 Rolling",
                bseScripGroup = "A - Premier",
                isinCode = "INE009A01021",
                ipoIssueDate = "Feb 1993",
                ipoIssuePrice = 95.0,
                ipoIssueSizeCr = 13.0,
                subscriptionQib = 1.06,
                subscriptionHni = 1.02,
                subscriptionRetail = 1.15,
                subscriptionTotal = 1.06, // Historically undersubscribed initially until Enam bailed it out!
                listingDayOpen = 145.0,
                listingDayClose = 158.0,
                listingDayGainPercent = 66.3,
                allTimeGainPercent = 420000.0,
                multibaggerFactor = 4200.0,
                outperformanceVsSensex = 680.0
            ),
            corporateGovernance = CorporateGovernance(
                corporateGroup = "Infosys Group (Narayan Murthy / Nandan Nilekani legacy)",
                cin = "L85110KA1981PLC013115",
                registeredOffice = "Electronics City, Hosur Road, Bengaluru 560100",
                website = "www.infosys.com",
                auditor = "Deloitte Haskins & Sells LLP",
                managementTeam = listOf(
                    ManagementProfile("Nandan M. Nilekani", "Chairman", 43, "IIT Bombay", "Architect of Aadhaar and India Stack, leading AI Topaz architecture at Infosys."),
                    ManagementProfile("Salil Parekh", "Chief Executive Officer & MD", 7, "Aeronautical Engg (IIT Bombay), Cornell", "Stabilized enterprise client retention and steered massive mega-deal execution."),
                    ManagementProfile("Jayesh Sanghrajka", "Chief Financial Officer", 18, "Chartered Accountant", "Veteran financial controller overseeing margin optimization and capital return.")
                )
            ),
            shareholding = ShareholdingSummary(
                quarter = "Q1 FY26 (June 2026)",
                promoterPercent = 14.65,
                promoterPledgedPercent = 0.0,
                fiiPercent = 33.15,
                diiPercent = 36.80,
                publicRetailPercent = 15.20,
                governmentPercent = 0.20,
                nonInstitutionPercent = 0.0,
                promoterChangeQoQ = 0.0,
                fiiChangeQoQ = 0.35,
                diiChangeQoQ = 0.10,
                majorHolders = listOf(
                    MajorShareholder("Life Insurance Corporation of India (LIC)", "Insurance", 7.85, 325000000),
                    MajorShareholder("Sudha Gopalakrishnan (Promoter)", "Promoter", 2.14, 88800000),
                    MajorShareholder("SBI S&P BSE SENSEX ETF", "Mutual Fund", 2.05, 85000000),
                    MajorShareholder("Rohan Murty", "Promoter", 1.45, 60100000)
                )
            ),
            dividendHistory = listOf(
                DividendRecord("2026-05-30", "Final", 20.00, 1.05, 85.0),
                DividendRecord("2025-10-25", "Interim", 18.00, 0.95, 83.0),
                DividendRecord("2025-05-31", "Final", 20.00, 1.10, 85.0)
            ),
            regulatoryFilings = listOf(
                RegulatoryFiling("2026-09-08", "Reg 30", "Strategic collaboration with NVIDIA for Generative AI telecom stacks", "Delivering telco LLM microservices to global operators.", "POSITIVE"),
                RegulatoryFiling("2026-07-18", "Reg 33", "Q1 FY26 Large Deal TCV stands at $4.1 Billion", "Record pipeline of AI Topaz driven enterprise implementations.", "POSITIVE")
            ),
            institutionalDeals = listOf(
                InstitutionalDeal("2026-08-20", "Block Deal", "Capital Research Global Investors", "BUY", 750000, 1885.00, 141.38),
                InstitutionalDeal("2026-07-28", "Bulk Deal", "SBI Mutual Fund", "BUY", 600000, 1860.00, 111.60)
            ),
            risksAndOpportunities = RisksAndOpportunities(
                growthOpportunities = listOf(
                    "Infosys Topaz AI suite enabling enterprise code modernization and workflow automation.",
                    "Strong financial services & manufacturing demand rebound in Europe.",
                    "Generous capital allocation with share buybacks and 85% dividend payout."
                ),
                strategicRisks = listOf(
                    "Prolonged decision-making cycles on uncommitted cloud discretionary spend.",
                    "Subcontracting and employee attrition costs in specialized AI engineering talent."
                ),
                analystConsensus = "74% BUY (40 Analysts)",
                targetConsensusMean = 2150.00,
                quantitativeVerdict = "Solid cash generator with consistent shareholder returns and expanding large-deal pipeline."
            ),
            historicalQuotes = history
        )
    }

    private fun createLarsenToubro(): BseStock {
        val curPrice = 3680.00
        val prevClose = 3645.00
        val history = generateHistory(curPrice, 2750.0, 3850.0)
        val proj = recalculateProjections(curPrice, 15.0, 11.5, 21.0, 11.5)
        return BseStock(
            scripCode = "500510",
            scripId = "LT",
            companyName = "Larsen & Toubro Ltd",
            sector = "Infrastructure & Engineering",
            group = "A",
            marketCapType = MarketCapType.LARGE_CAP,
            currentPrice = curPrice,
            previousClose = prevClose,
            dayOpen = 3655.00,
            dayHigh = 3710.00,
            dayLow = 3640.00,
            volume = 1420500,
            turnoverCr = 522.70,
            fiftyTwoWeekHigh = 3948.60,
            fiftyTwoWeekLow = 2820.00,
            upperCircuit = 4009.50,
            lowerCircuit = 3280.50,
            peRatio = 34.2,
            pbRatio = 5.1,
            sectorPe = 31.0,
            dividendYield = 0.95,
            faceValue = 2.0,
            technicals = TechnicalParameters(
                rsi14 = 58.7,
                rsiStatus = "Neutral-Bullish",
                macdLine = 24.5,
                macdSignal = 18.0,
                macdHistogram = 6.5,
                macdStatus = "Bullish",
                bollingerUpper = 3780.0,
                bollingerMiddle = 3640.0,
                bollingerLower = 3500.0,
                bollingerBandwidth = 7.7,
                sma20 = 3650.0,
                sma50 = 3580.0,
                sma200 = 3320.0,
                dmaCrossSignal = "Above 200 DMA",
                pivotClassic = PivotLevels(r3 = 3765.0, r2 = 3735.0, r1 = 3710.0, pivot = 3675.0, s1 = 3645.0, s2 = 3620.0, s3 = 3580.0),
                pivotFibonacci = PivotLevels(r3 = 3755.0, r2 = 3730.0, r1 = 3705.0, pivot = 3675.0, s1 = 3648.0, s2 = 3625.0, s3 = 3585.0),
                stochasticK = 65.4,
                stochasticD = 60.1,
                atr14 = 52.00,
                betaSensex = 1.22,
                sharpeRatio = 1.48,
                alphaRatio = 4.25,
                overallSignal = TechnicalSignal.BUY,
                technicalScore = 75
            ),
            projections = proj,
            ipoMetrics = ListingAndIpoMetrics(
                listingDate = "1950-12-01",
                listingStatus = "Active - Normal T+1 Rolling",
                bseScripGroup = "A - Premier",
                isinCode = "INE018A01030",
                ipoIssueDate = "Dec 1950",
                ipoIssuePrice = 10.0,
                ipoIssueSizeCr = 0.50,
                subscriptionQib = 3.0,
                subscriptionHni = 2.5,
                subscriptionRetail = 4.0,
                subscriptionTotal = 3.2,
                listingDayOpen = 12.0,
                listingDayClose = 14.0,
                listingDayGainPercent = 40.0,
                allTimeGainPercent = 368000.0,
                multibaggerFactor = 3680.0,
                outperformanceVsSensex = 510.0
            ),
            corporateGovernance = CorporateGovernance(
                corporateGroup = "L&T Conglomerate (Professionally Run)",
                cin = "L99999MH1946PLC004768",
                registeredOffice = "L&T House, Ballard Estate, Mumbai 400001",
                website = "www.larsentoubro.com",
                auditor = "Deloitte Haskins & Sells LLP",
                managementTeam = listOf(
                    ManagementProfile("S. N. Subrahmanyan", "Chairman & Managing Director", 40, "Civil Engineer, MBA, London Business School", "Transformed L&T into high-tech engineering, semiconductor manufacturing, and green EPC power."),
                    ManagementProfile("R. Shankar Raman", "President & Whole-time Director & CFO", 30, "Chartered Accountant", "Steward of capital discipline and non-core asset monetization (L&T Metro / IDPL).")
                )
            ),
            shareholding = ShareholdingSummary(
                quarter = "Q1 FY26 (June 2026)",
                promoterPercent = 0.0, // Zero promoter - owned by trust, FIIs, DIIs and retail!
                promoterPledgedPercent = 0.0,
                fiiPercent = 23.90,
                diiPercent = 39.40,
                publicRetailPercent = 22.80,
                governmentPercent = 0.0,
                nonInstitutionPercent = 13.90, // L&T Employees Trust
                promoterChangeQoQ = 0.0,
                fiiChangeQoQ = 0.60,
                diiChangeQoQ = 0.15,
                majorHolders = listOf(
                    MajorShareholder("L&T Employees Welfare Foundation", "Trust", 13.68, 188000000),
                    MajorShareholder("Life Insurance Corporation of India (LIC)", "Insurance", 9.85, 135400000),
                    MajorShareholder("HDFC Mutual Fund", "Mutual Fund", 3.40, 46700000),
                    MajorShareholder("SBI Bluechip Fund", "Mutual Fund", 2.95, 40500000)
                )
            ),
            dividendHistory = listOf(
                DividendRecord("2026-06-20", "Final", 28.00, 0.76, 32.0),
                DividendRecord("2025-06-22", "Final", 24.00, 0.72, 30.5),
                DividendRecord("2024-06-25", "Final", 24.00, 0.82, 31.0)
            ),
            regulatoryFilings = listOf(
                RegulatoryFiling("2026-09-22", "Reg 30", "Order Inflow: Secured Mega Order worth ₹15,000+ Cr from Middle East Hydrocarbon client", "Confirms highest ever order book surpassing ₹5.1 Lakh Crore.", "POSITIVE"),
                RegulatoryFiling("2026-08-05", "Reg 30", "Cabinet approval for Defense Warship building contract", "L&T Heavy Engineering selected as prime contractor.", "POSITIVE")
            ),
            institutionalDeals = listOf(
                InstitutionalDeal("2026-09-12", "Block Deal", "Government of Singapore (GIC)", "BUY", 380000, 3685.00, 140.03),
                InstitutionalDeal("2026-08-19", "Bulk Deal", "ICICI Prudential Asset Management", "BUY", 290000, 3650.00, 105.85)
            ),
            risksAndOpportunities = RisksAndOpportunities(
                growthOpportunities = listOf(
                    "All-time record order book above ₹5 Lakh Crore providing 3.5 years of revenue visibility.",
                    "National India Capex push (high-speed rail, ports, renewable transmission grids, defense).",
                    "Expanding high-margin international offshore EPC contracts across GCC nations."
                ),
                strategicRisks = listOf(
                    "Geopolitical tensions in Middle East impacting international project completion timelines.",
                    "Volatile raw material input costs (steel, cement) on fixed-price legacy infrastructure contracts."
                ),
                analystConsensus = "86% BUY (36 Analysts)",
                targetConsensusMean = 4250.00,
                quantitativeVerdict = "Indisputable proxy for India's decadal infrastructure and defense self-reliance."
            ),
            historicalQuotes = history
        )
    }

    private fun createBseLimited(): BseStock {
        val curPrice = 2840.00
        val prevClose = 2775.00
        val history = generateHistory(curPrice, 1100.0, 3150.0)
        val proj = recalculateProjections(curPrice, 28.0, 48.0, 32.0, 12.0)
        return BseStock(
            scripCode = "542651",
            scripId = "BSE",
            companyName = "BSE Limited",
            sector = "Financial Market Infrastructure",
            group = "A",
            marketCapType = MarketCapType.MID_CAP,
            currentPrice = curPrice,
            previousClose = prevClose,
            dayOpen = 2785.00,
            dayHigh = 2865.00,
            dayLow = 2780.00,
            volume = 2840100,
            turnoverCr = 804.50,
            fiftyTwoWeekHigh = 3120.00,
            fiftyTwoWeekLow = 1180.00,
            upperCircuit = 3330.00,
            lowerCircuit = 2220.00,
            peRatio = 42.1,
            pbRatio = 8.4,
            sectorPe = 38.5,
            dividendYield = 0.85,
            faceValue = 2.0,
            technicals = TechnicalParameters(
                rsi14 = 66.8,
                rsiStatus = "Bullish Momentum",
                macdLine = 48.2,
                macdSignal = 36.5,
                macdHistogram = 11.7,
                macdStatus = "Strong Bullish Expansion",
                bollingerUpper = 2950.0,
                bollingerMiddle = 2720.0,
                bollingerLower = 2490.0,
                bollingerBandwidth = 16.9,
                sma20 = 2740.0,
                sma50 = 2580.0,
                sma200 = 2120.0,
                dmaCrossSignal = "Super Bullish Above 200 DMA",
                pivotClassic = PivotLevels(r3 = 2920.0, r2 = 2885.0, r1 = 2860.0, pivot = 2825.0, s1 = 2795.0, s2 = 2770.0, s3 = 2730.0),
                pivotFibonacci = PivotLevels(r3 = 2915.0, r2 = 2880.0, r1 = 2855.0, pivot = 2825.0, s1 = 2800.0, s2 = 2775.0, s3 = 2735.0),
                stochasticK = 76.5,
                stochasticD = 71.2,
                atr14 = 78.50,
                betaSensex = 1.65,
                sharpeRatio = 2.15,
                alphaRatio = 12.80,
                overallSignal = TechnicalSignal.STRONG_BUY,
                technicalScore = 88
            ),
            projections = proj,
            ipoMetrics = ListingAndIpoMetrics(
                listingDate = "2017-02-03",
                listingStatus = "Active - Normal T+1 Rolling",
                bseScripGroup = "A - Premier",
                isinCode = "INE118H01025",
                ipoIssueDate = "Jan 2017",
                ipoIssuePrice = 806.0,
                ipoIssueSizeCr = 1243.0,
                subscriptionQib = 48.6,
                subscriptionHni = 245.1,
                subscriptionRetail = 6.3,
                subscriptionTotal = 51.2,
                listingDayOpen = 1085.0,
                listingDayClose = 1069.20,
                listingDayGainPercent = 32.7,
                allTimeGainPercent = 605.0,
                multibaggerFactor = 7.05,
                outperformanceVsSensex = 480.0
            ),
            corporateGovernance = CorporateGovernance(
                corporateGroup = "BSE Ltd (Asia's Oldest Stock Exchange - Est. 1875)",
                cin = "L67120MH2005PLC155188",
                registeredOffice = "25th Floor, P. J. Towers, Dalal Street, Fort, Mumbai 400001",
                website = "www.bseindia.com",
                auditor = "S.R. Batliboi & Associates LLP",
                managementTeam = listOf(
                    ManagementProfile("Justice Vikramajit Sen (Retd.)", "Public Interest Director & Chairman", 6, "Former Supreme Court of India Judge", "Upholding exchange market integrity and regulatory compliance."),
                    ManagementProfile("Sundararaman Ramamurthy", "Managing Director & CEO", 3, "Chartered Accountant, ICWA", "Architect behind the explosive turnaround in BSE equity derivatives (Sensex & Bankex weekly options)."),
                    ManagementProfile("Deepak Goel", "Chief Financial Officer", 4, "Chartered Accountant", "Manages treasury yield optimization, transaction fee revenue, and Star MF platform economics.")
                )
            ),
            shareholding = ShareholdingSummary(
                quarter = "Q1 FY26 (June 2026)",
                promoterPercent = 0.0, // Demutualized exchange - Zero promoter!
                promoterPledgedPercent = 0.0,
                fiiPercent = 14.20,
                diiPercent = 21.80,
                publicRetailPercent = 64.00,
                governmentPercent = 0.0,
                nonInstitutionPercent = 0.0,
                promoterChangeQoQ = 0.0,
                fiiChangeQoQ = 1.45,
                diiChangeQoQ = 0.80,
                majorHolders = listOf(
                    MajorShareholder("Life Insurance Corporation of India (LIC)", "Insurance", 4.80, 6500000),
                    MajorShareholder("Zerodha Broking Limited", "Corporate", 3.10, 4200000),
                    MajorShareholder("Vanguard Total International Stock ETF", "FII / FPI", 2.25, 3050000),
                    MajorShareholder("Motilal Oswal Financial Services", "Corporate", 1.95, 2640000)
                )
            ),
            dividendHistory = listOf(
                DividendRecord("2026-06-15", "Final", 24.00, 0.85, 45.0),
                DividendRecord("2025-06-18", "Final", 15.00, 0.75, 42.0),
                DividendRecord("2024-06-20", "Final", 12.00, 0.80, 40.0)
            ),
            regulatoryFilings = listOf(
                RegulatoryFiling("2026-09-14", "Reg 30", "BSE StAR MF platform processes record 50 Million monthly transactions", "Over 88% market share in digital mutual fund distribution in India.", "POSITIVE"),
                RegulatoryFiling("2026-07-26", "Reg 33", "Q1 FY26 Consolidated Net Profit jumps 122% YoY on Derivatives Volume surge", "Average Daily Turnover (ADTV) in Sensex derivatives crosses ₹140 Lakh Crore.", "POSITIVE")
            ),
            institutionalDeals = listOf(
                InstitutionalDeal("2026-09-08", "Bulk Deal", "Citigroup Global Markets Mauritius", "BUY", 540000, 2820.00, 152.28),
                InstitutionalDeal("2026-08-25", "Block Deal", "Goldman Sachs India", "BUY", 320000, 2760.00, 88.32)
            ),
            risksAndOpportunities = RisksAndOpportunities(
                growthOpportunities = listOf(
                    "Market share expansion in equity derivatives (Sensex / Bankex) challenging traditional exchange monopoly.",
                    "Monetization of India International Exchange (INX) at GIFT City and commodity derivatives.",
                    "Hyper-growth in mutual fund retail SIPs processed through BSE StAR MF platform."
                ),
                strategicRisks = listOf(
                    "SEBI regulatory tightening on index derivatives contract sizes and weekly expiries.",
                    "Exchange clearing corporation charges and regulatory fee revisions."
                ),
                analystConsensus = "82% BUY (22 Analysts)",
                targetConsensusMean = 3350.00,
                quantitativeVerdict = "High operating leverage market infrastructure play benefiting directly from financialization of Indian savings."
            ),
            historicalQuotes = history
        )
    }

    private fun createItc(): BseStock {
        val curPrice = 508.50
        val prevClose = 502.80
        val history = generateHistory(curPrice, 410.0, 535.0)
        val proj = recalculateProjections(curPrice, 10.5, 36.5, 14.2, 10.2)
        return BseStock(
            scripCode = "500875",
            scripId = "ITC",
            companyName = "ITC Ltd",
            sector = "FMCG / Hotels & Paperboards",
            group = "A",
            marketCapType = MarketCapType.LARGE_CAP,
            currentPrice = curPrice,
            previousClose = prevClose,
            dayOpen = 504.00,
            dayHigh = 512.00,
            dayLow = 503.20,
            volume = 6840200,
            turnoverCr = 348.50,
            fiftyTwoWeekHigh = 528.50,
            fiftyTwoWeekLow = 399.30,
            upperCircuit = 553.00,
            lowerCircuit = 452.50,
            peRatio = 29.5,
            pbRatio = 8.8,
            sectorPe = 38.0,
            dividendYield = 3.25,
            faceValue = 1.0,
            technicals = TechnicalParameters(
                rsi14 = 55.4,
                rsiStatus = "Neutral",
                macdLine = 3.8,
                macdSignal = 3.1,
                macdHistogram = 0.7,
                macdStatus = "Mild Bullish",
                bollingerUpper = 520.0,
                bollingerMiddle = 505.0,
                bollingerLower = 490.0,
                bollingerBandwidth = 5.9,
                sma20 = 506.0,
                sma50 = 498.0,
                sma200 = 462.0,
                dmaCrossSignal = "Above 200 DMA",
                pivotClassic = PivotLevels(r3 = 518.0, r2 = 514.0, r1 = 510.0, pivot = 507.0, s1 = 504.0, s2 = 501.0, s3 = 496.0),
                pivotFibonacci = PivotLevels(r3 = 517.0, r2 = 513.0, r1 = 510.0, pivot = 507.0, s1 = 505.0, s2 = 502.0, s3 = 497.0),
                stochasticK = 62.0,
                stochasticD = 58.2,
                atr14 = 6.80,
                betaSensex = 0.65,
                sharpeRatio = 1.72,
                alphaRatio = 3.40,
                overallSignal = TechnicalSignal.BUY,
                technicalScore = 70
            ),
            projections = proj,
            ipoMetrics = ListingAndIpoMetrics(
                listingDate = "1954-01-01",
                listingStatus = "Active - Normal T+1 Rolling",
                bseScripGroup = "A - Premier",
                isinCode = "INE154A01025",
                ipoIssueDate = "Jan 1954",
                ipoIssuePrice = 1.0,
                ipoIssueSizeCr = 0.25,
                subscriptionQib = 2.0,
                subscriptionHni = 1.8,
                subscriptionRetail = 3.0,
                subscriptionTotal = 2.3,
                listingDayOpen = 1.5,
                listingDayClose = 1.8,
                listingDayGainPercent = 80.0,
                allTimeGainPercent = 50850.0,
                multibaggerFactor = 508.5,
                outperformanceVsSensex = 210.0
            ),
            corporateGovernance = CorporateGovernance(
                corporateGroup = "ITC Conglomerate (Professionally Run)",
                cin = "L16005WB1910PLC001981",
                registeredOffice = "Virginia House, 37 J. L. Nehru Road, Kolkata 700071",
                website = "www.itcportal.com",
                auditor = "S.R. Batliboi & Co. LLP",
                managementTeam = listOf(
                    ManagementProfile("Sanjiv Puri", "Chairman & Managing Director", 38, "IIT Kanpur, Wharton Executive", "Pioneered ITC Next strategy accelerating FMCG food brands (Aashirvaad, Sunfeast) and demutualizing ITC Hotels."),
                    ManagementProfile("Sujit Vaidya", "Chief Financial Officer", 22, "Chartered Accountant", "Disciplined working capital management and 80%+ dividend distribution.")
                )
            ),
            shareholding = ShareholdingSummary(
                quarter = "Q1 FY26 (June 2026)",
                promoterPercent = 0.0, // Zero promoter group!
                promoterPledgedPercent = 0.0,
                fiiPercent = 38.40,
                diiPercent = 42.10,
                publicRetailPercent = 19.50,
                governmentPercent = 0.0,
                nonInstitutionPercent = 0.0,
                promoterChangeQoQ = 0.0,
                fiiChangeQoQ = -0.30,
                diiChangeQoQ = 0.45,
                majorHolders = listOf(
                    MajorShareholder("British American Tobacco (BAT plc)", "Foreign Strategic", 25.50, 3180000000),
                    MajorShareholder("Life Insurance Corporation of India (LIC)", "Insurance", 15.20, 1895000000),
                    MajorShareholder("Specified Undertaking of UTI (SUUTI)", "Government / Trust", 7.82, 975000000),
                    MajorShareholder("SBI S&P BSE SENSEX ETF", "Mutual Fund", 2.10, 262000000)
                )
            ),
            dividendHistory = listOf(
                DividendRecord("2026-06-04", "Final", 7.50, 1.48, 82.0),
                DividendRecord("2026-02-14", "Interim", 6.25, 1.23, 80.0),
                DividendRecord("2025-06-05", "Final", 7.50, 1.62, 85.0)
            ),
            regulatoryFilings = listOf(
                RegulatoryFiling("2026-08-10", "Reg 30", "Demerger of ITC Hotels Limited receives Final NCLT Sanction", "Shareholders to receive 1 share of ITC Hotels for every 10 shares held in ITC.", "POSITIVE"),
                RegulatoryFiling("2026-07-22", "Reg 33", "Q1 FY26 FMCG-Others revenue grows 8.2%; Segment EBITDA margin expands to 11.4%", "Staples and spices drive volume expansion.", "POSITIVE")
            ),
            institutionalDeals = listOf(
                InstitutionalDeal("2026-08-18", "Block Deal", "Government Pension Fund Global", "BUY", 1200000, 506.00, 60.72),
                InstitutionalDeal("2026-06-29", "Bulk Deal", "Kotak Flexicap Fund", "BUY", 950000, 498.00, 47.31)
            ),
            risksAndOpportunities = RisksAndOpportunities(
                growthOpportunities = listOf(
                    "Demerger of ITC Hotels unlocking asset-light ROCE expansion.",
                    "FMCG non-cigarette segment approaching inflection point with ₹22,000+ Cr consumer spend.",
                    "Stable tax regime on legal cigarettes with minimal GST council excise shocks."
                ),
                strategicRisks = listOf(
                    "Any unexpected tobacco tax/cess hike by GST Council.",
                    "Agri-business export restrictions on wheat and agricultural commodities."
                ),
                analystConsensus = "80% BUY (35 Analysts)",
                targetConsensusMean = 565.00,
                quantitativeVerdict = "Highest dividend yield among Nifty 50 anchors with resilient free-cash flow and FMCG margin expansion."
            ),
            historicalQuotes = history
        )
    }

    private fun createBhartiAirtel(): BseStock {
        val curPrice = 1720.00
        val prevClose = 1695.00
        val history = generateHistory(curPrice, 1150.0, 1780.0)
        val proj = recalculateProjections(curPrice, 16.5, 52.0, 18.5, 11.0)
        return BseStock(
            scripCode = "532454",
            scripId = "BHARTIARTL",
            companyName = "Bharti Airtel Ltd",
            sector = "Telecommunications",
            group = "A",
            marketCapType = MarketCapType.LARGE_CAP,
            currentPrice = curPrice,
            previousClose = prevClose,
            dayOpen = 1705.00,
            dayHigh = 1735.00,
            dayLow = 1698.00,
            volume = 3840000,
            turnoverCr = 660.40,
            fiftyTwoWeekHigh = 1778.00,
            fiftyTwoWeekLow = 1115.00,
            upperCircuit = 1864.50,
            lowerCircuit = 1525.50,
            peRatio = 64.2,
            pbRatio = 8.5,
            sectorPe = 58.0,
            dividendYield = 0.55,
            faceValue = 5.0,
            technicals = TechnicalParameters(
                rsi14 = 63.2,
                rsiStatus = "Bullish",
                macdLine = 19.5,
                macdSignal = 14.8,
                macdHistogram = 4.7,
                macdStatus = "Bullish Expansion",
                bollingerUpper = 1760.0,
                bollingerMiddle = 1700.0,
                bollingerLower = 1640.0,
                bollingerBandwidth = 7.0,
                sma20 = 1705.0,
                sma50 = 1660.0,
                sma200 = 1420.0,
                dmaCrossSignal = "Super Bullish Above 200 DMA",
                pivotClassic = PivotLevels(r3 = 1755.0, r2 = 1738.0, r1 = 1726.0, pivot = 1712.0, s1 = 1698.0, s2 = 1682.0, s3 = 1660.0),
                pivotFibonacci = PivotLevels(r3 = 1750.0, r2 = 1735.0, r1 = 1724.0, pivot = 1712.0, s1 = 1700.0, s2 = 1685.0, s3 = 1665.0),
                stochasticK = 71.0,
                stochasticD = 66.5,
                atr14 = 26.80,
                betaSensex = 0.94,
                sharpeRatio = 1.95,
                alphaRatio = 8.40,
                overallSignal = TechnicalSignal.BUY,
                technicalScore = 80
            ),
            projections = proj,
            ipoMetrics = ListingAndIpoMetrics(
                listingDate = "2002-02-18",
                listingStatus = "Active - Normal T+1 Rolling",
                bseScripGroup = "A - Premier",
                isinCode = "INE397D01024",
                ipoIssueDate = "Feb 2002",
                ipoIssuePrice = 45.0,
                ipoIssueSizeCr = 834.0,
                subscriptionQib = 3.8,
                subscriptionHni = 2.1,
                subscriptionRetail = 1.7,
                subscriptionTotal = 2.5,
                listingDayOpen = 47.0,
                listingDayClose = 48.25,
                listingDayGainPercent = 7.2,
                allTimeGainPercent = 3722.0,
                multibaggerFactor = 38.2,
                outperformanceVsSensex = 310.0
            ),
            corporateGovernance = CorporateGovernance(
                corporateGroup = "Bharti Enterprises (Sunil Mittal)",
                cin = "L74899HR1995PLC095960",
                registeredOffice = "Bharti Crescent, 1 Nelson Mandela Road, Vasant Kunj, New Delhi 110070",
                website = "www.airtel.in",
                auditor = "Deloitte Haskins & Sells LLP",
                managementTeam = listOf(
                    ManagementProfile("Sunil Bharti Mittal", "Chairman", 32, "Harvard Business School Alumni", "Pioneer of Indian private telecom revolution, leading global digital infrastructure expansion."),
                    ManagementProfile("Gopal Vittal", "Vice Chairman & MD", 12, "IIM Calcutta", "Engineered industry-leading ARPU expansion from ₹125 to ₹220+ via premiumization."),
                    ManagementProfile("Soumen Ray", "Chief Financial Officer", 6, "Chartered Accountant", "Oversaw massive debt reduction and prepaid spectrum liability clearance.")
                )
            ),
            shareholding = ShareholdingSummary(
                quarter = "Q1 FY26 (June 2026)",
                promoterPercent = 53.12,
                promoterPledgedPercent = 0.0,
                fiiPercent = 25.40,
                diiPercent = 14.80,
                publicRetailPercent = 6.68,
                governmentPercent = 0.0,
                nonInstitutionPercent = 0.0,
                promoterChangeQoQ = 0.0,
                fiiChangeQoQ = 0.85,
                diiChangeQoQ = -0.20,
                majorHolders = listOf(
                    MajorShareholder("Bharti Telecom Limited (Promoter)", "Promoter", 38.20, 2280000000),
                    MajorShareholder("Singtel Telecom (Pastel Ltd)", "Promoter Group", 10.45, 624000000),
                    MajorShareholder("Life Insurance Corporation of India (LIC)", "Insurance", 4.10, 245000000),
                    MajorShareholder("SBI Mutual Fund", "Mutual Fund", 2.85, 170000000)
                )
            ),
            dividendHistory = listOf(
                DividendRecord("2026-07-28", "Final", 8.00, 0.47, 24.0),
                DividendRecord("2025-07-30", "Final", 4.00, 0.28, 18.0),
                DividendRecord("2024-08-02", "Final", 4.00, 0.35, 16.0)
            ),
            regulatoryFilings = listOf(
                RegulatoryFiling("2026-09-04", "Reg 30", "Prepayment of ₹8,465 Cr of high-cost DoT deferred spectrum liabilities", "Continues balance sheet deleveraging drive.", "POSITIVE"),
                RegulatoryFiling("2026-07-29", "Reg 33", "Q1 FY26 India ARPU hits new milestone of ₹224", "Postpaid customer additions jump 18% YoY.", "POSITIVE")
            ),
            institutionalDeals = listOf(
                InstitutionalDeal("2026-09-01", "Block Deal", "Morgan Stanley Investment Funds", "BUY", 880000, 1715.00, 150.92),
                InstitutionalDeal("2026-08-16", "Bulk Deal", "Temasek Holdings", "BUY", 640000, 1690.00, 108.16)
            ),
            risksAndOpportunities = RisksAndOpportunities(
                growthOpportunities = listOf(
                    "Structural tariff hikes driving ARPU path toward management's target of ₹300.",
                    "Airtel Business Enterprise connectivity and B2B cloud CPaaS margin expansion.",
                    "Home broadband and Airtel Payments Bank achieving profitable standalone scale."
                ),
                strategicRisks = listOf(
                    "Currency devaluation in African subsidiaries impacting consolidated PAT.",
                    "Intense capital expenditure requirements for 5G FWA last-mile equipment."
                ),
                analystConsensus = "89% BUY (37 Analysts)",
                targetConsensusMean = 1950.00,
                quantitativeVerdict = "Dominant mobile duopoly player generating immense operating leverage from tariff hikes."
            ),
            historicalQuotes = history
        )
    }

    private fun createSbi(): BseStock {
        val curPrice = 824.50
        val prevClose = 816.00
        val history = generateHistory(curPrice, 620.0, 915.0)
        val proj = recalculateProjections(curPrice, 14.0, 18.0, 22.5, 11.8)
        return BseStock(
            scripCode = "500112",
            scripId = "SBIN",
            companyName = "State Bank of India",
            sector = "Banking (Public Sector)",
            group = "A",
            marketCapType = MarketCapType.LARGE_CAP,
            currentPrice = curPrice,
            previousClose = prevClose,
            dayOpen = 818.00,
            dayHigh = 832.00,
            dayLow = 815.00,
            volume = 9450000,
            turnoverCr = 778.60,
            fiftyTwoWeekHigh = 912.10,
            fiftyTwoWeekLow = 578.40,
            upperCircuit = 897.60,
            lowerCircuit = 734.40,
            peRatio = 10.4,
            pbRatio = 1.45,
            sectorPe = 12.0,
            dividendYield = 1.70,
            faceValue = 1.0,
            technicals = TechnicalParameters(
                rsi14 = 52.8,
                rsiStatus = "Neutral",
                macdLine = 5.2,
                macdSignal = 4.8,
                macdHistogram = 0.4,
                macdStatus = "Neutral-Positive",
                bollingerUpper = 855.0,
                bollingerMiddle = 820.0,
                bollingerLower = 785.0,
                bollingerBandwidth = 8.5,
                sma20 = 822.0,
                sma50 = 812.0,
                sma200 = 760.0,
                dmaCrossSignal = "Above 200 DMA",
                pivotClassic = PivotLevels(r3 = 842.0, r2 = 835.0, r1 = 830.0, pivot = 824.0, s1 = 818.0, s2 = 812.0, s3 = 802.0),
                pivotFibonacci = PivotLevels(r3 = 840.0, r2 = 834.0, r1 = 828.0, pivot = 824.0, s1 = 819.0, s2 = 814.0, s3 = 804.0),
                stochasticK = 57.0,
                stochasticD = 54.0,
                atr14 = 14.50,
                betaSensex = 1.35,
                sharpeRatio = 1.40,
                alphaRatio = 3.60,
                overallSignal = TechnicalSignal.BUY,
                technicalScore = 71
            ),
            projections = proj,
            ipoMetrics = ListingAndIpoMetrics(
                listingDate = "1970-01-01",
                listingStatus = "Active - Normal T+1 Rolling",
                bseScripGroup = "A - Premier",
                isinCode = "INE062A01020",
                ipoIssueDate = "Jul 1993",
                ipoIssuePrice = 100.0, // Face value was ₹10, sub-divided to ₹1 in 2014
                ipoIssueSizeCr = 1240.0,
                subscriptionQib = 4.5,
                subscriptionHni = 3.2,
                subscriptionRetail = 2.8,
                subscriptionTotal = 3.5,
                listingDayOpen = 140.0,
                listingDayClose = 148.0,
                listingDayGainPercent = 48.0,
                allTimeGainPercent = 8245.0,
                multibaggerFactor = 82.4,
                outperformanceVsSensex = 180.0
            ),
            corporateGovernance = CorporateGovernance(
                corporateGroup = "Government of India / Public Sector",
                cin = "Constitutional Body (SBI Act 1955)",
                registeredOffice = "State Bank Bhavan, Madame Cama Road, Nariman Point, Mumbai 400021",
                website = "www.sbi.co.in",
                auditor = "M/s Khimji Kunverji & Co LLP",
                managementTeam = listOf(
                    ManagementProfile("C. S. Setty", "Chairman", 36, "Bachelor of Science, CAIIB", "Appointed Chairman, championing credit growth and YONO 2.0 digital architecture."),
                    ManagementProfile("Rama Mohan Rao Amara", "Managing Director - Risk, Compliance & SARG", 32, "B.Tech, CAIIB", "Oversight on NPAs and resolution of stressed corporate assets.")
                )
            ),
            shareholding = ShareholdingSummary(
                quarter = "Q1 FY26 (June 2026)",
                promoterPercent = 57.48, // Government of India
                promoterPledgedPercent = 0.0,
                fiiPercent = 11.20,
                diiPercent = 23.90,
                publicRetailPercent = 7.42,
                governmentPercent = 0.0,
                nonInstitutionPercent = 0.0,
                promoterChangeQoQ = 0.0,
                fiiChangeQoQ = 0.25,
                diiChangeQoQ = -0.10,
                majorHolders = listOf(
                    MajorShareholder("President of India (Government)", "Promoter", 57.48, 5130000000),
                    MajorShareholder("Life Insurance Corporation of India (LIC)", "Insurance", 8.85, 790000000),
                    MajorShareholder("HDFC Mutual Fund", "Mutual Fund", 2.90, 259000000),
                    MajorShareholder("Nippon India Growth Fund", "Mutual Fund", 1.80, 160500000)
                )
            ),
            dividendHistory = listOf(
                DividendRecord("2026-05-22", "Final", 14.00, 1.70, 20.0),
                DividendRecord("2025-05-24", "Final", 13.70, 1.82, 20.5),
                DividendRecord("2024-05-22", "Final", 11.30, 1.75, 19.8)
            ),
            regulatoryFilings = listOf(
                RegulatoryFiling("2026-08-16", "Reg 30", "SBI raises ₹10,000 Cr via Basel III compliant Tier-2 bonds at 7.34%", "Oversubscribed nearly 4 times on BSE Electronic Bidding Platform.", "POSITIVE"),
                RegulatoryFiling("2026-07-28", "Reg 33", "Q1 FY26 Net Profit crosses ₹19,200 Cr; Net NPA hits record low 0.53%", "ROA maintained at healthy 1.10%.", "POSITIVE")
            ),
            institutionalDeals = listOf(
                InstitutionalDeal("2026-08-30", "Bulk Deal", "Abu Dhabi Investment Authority", "BUY", 1450000, 822.00, 119.19),
                InstitutionalDeal("2026-07-14", "Block Deal", "Kotak Mahindra Mutual Fund", "BUY", 1100000, 810.00, 89.10)
            ),
            risksAndOpportunities = RisksAndOpportunities(
                growthOpportunities = listOf(
                    "Dominant 23% national banking market share with unmatched deposit franchise.",
                    "YONO platform driving retail loan originations with zero human intervention.",
                    "Substantial value unlocking potential in subsidiaries (SBI Funds Management IPO, SBI General Insurance)."
                ),
                strategicRisks = listOf(
                    "Potential wage bill inflation from tripartite bipartite revisions.",
                    "Higher corporate credit underwriting risk in unsecured personal loans."
                ),
                analystConsensus = "85% BUY (39 Analysts)",
                targetConsensusMean = 980.00,
                quantitativeVerdict = "India's premier banking titan with fortress deposit base and multi-decade high ROEs."
            ),
            historicalQuotes = history
        )
    }

    private fun createTataMotors(): BseStock {
        val curPrice = 968.00
        val prevClose = 952.00
        val history = generateHistory(curPrice, 640.0, 1080.0)
        val proj = recalculateProjections(curPrice, 15.0, 14.0, 26.0, 12.0)
        return BseStock(
            scripCode = "500570",
            scripId = "TATAMOTORS",
            companyName = "Tata Motors Ltd",
            sector = "Automobile (EV & Commercial Vehicles)",
            group = "A",
            marketCapType = MarketCapType.LARGE_CAP,
            currentPrice = curPrice,
            previousClose = prevClose,
            dayOpen = 958.00,
            dayHigh = 976.00,
            dayLow = 954.00,
            volume = 4920000,
            turnoverCr = 475.20,
            fiftyTwoWeekHigh = 1179.05,
            fiftyTwoWeekLow = 625.00,
            upperCircuit = 1047.00,
            lowerCircuit = 857.00,
            peRatio = 11.8,
            pbRatio = 3.6,
            sectorPe = 24.5,
            dividendYield = 0.65,
            faceValue = 2.0,
            technicals = TechnicalParameters(
                rsi14 = 53.6,
                rsiStatus = "Neutral",
                macdLine = 7.4,
                macdSignal = 6.2,
                macdHistogram = 1.2,
                macdStatus = "Bullish Crossover",
                bollingerUpper = 1010.0,
                bollingerMiddle = 960.0,
                bollingerLower = 910.0,
                bollingerBandwidth = 10.4,
                sma20 = 962.0,
                sma50 = 975.0,
                sma200 = 890.0,
                dmaCrossSignal = "Above 200 DMA",
                pivotClassic = PivotLevels(r3 = 992.0, r2 = 982.0, r1 = 974.0, pivot = 965.0, s1 = 956.0, s2 = 948.0, s3 = 938.0),
                pivotFibonacci = PivotLevels(r3 = 990.0, r2 = 980.0, r1 = 972.0, pivot = 965.0, s1 = 958.0, s2 = 950.0, s3 = 940.0),
                stochasticK = 60.5,
                stochasticD = 55.8,
                atr14 = 21.00,
                betaSensex = 1.48,
                sharpeRatio = 1.65,
                alphaRatio = 7.10,
                overallSignal = TechnicalSignal.BUY,
                technicalScore = 73
            ),
            projections = proj,
            ipoMetrics = ListingAndIpoMetrics(
                listingDate = "1955-01-01",
                listingStatus = "Active - Normal T+1 Rolling",
                bseScripGroup = "A - Premier",
                isinCode = "INE155A01022",
                ipoIssueDate = "Jan 1955",
                ipoIssuePrice = 10.0,
                ipoIssueSizeCr = 2.0,
                subscriptionQib = 2.5,
                subscriptionHni = 2.0,
                subscriptionRetail = 3.5,
                subscriptionTotal = 2.8,
                listingDayOpen = 14.0,
                listingDayClose = 15.5,
                listingDayGainPercent = 55.0,
                allTimeGainPercent = 96800.0,
                multibaggerFactor = 968.0,
                outperformanceVsSensex = 290.0
            ),
            corporateGovernance = CorporateGovernance(
                corporateGroup = "Tata Group",
                cin = "L28920MH1945PLC004520",
                registeredOffice = "Bombay House, 24 Homi Mody Street, Mumbai 400001",
                website = "www.tatamotors.com",
                auditor = "B S R & Co. LLP",
                managementTeam = listOf(
                    ManagementProfile("N. Chandrasekaran", "Chairman & Non-Executive Director", 8, "MCA", "Spearheaded EV transformation and balance sheet deleveraging at Tata Motors."),
                    ManagementProfile("Girish Wagh", "Executive Director - Commercial Vehicles", 26, "Mechanical Engg, SPJIMR", "Market leader commanding 40%+ share in Indian commercial truck and bus fleets."),
                    ManagementProfile("Shailesh Chandra", "Managing Director - Passenger & EV", 18, "B.Tech BHU, IIM Calcutta", "Turned Tata Passenger Vehicles into #2 domestic automaker and #1 EV brand.")
                )
            ),
            shareholding = ShareholdingSummary(
                quarter = "Q1 FY26 (June 2026)",
                promoterPercent = 46.36,
                promoterPledgedPercent = 1.20, // Low pledge
                fiiPercent = 19.20,
                diiPercent = 15.60,
                publicRetailPercent = 18.84,
                governmentPercent = 0.0,
                nonInstitutionPercent = 0.0,
                promoterChangeQoQ = 0.0,
                fiiChangeQoQ = 0.40,
                diiChangeQoQ = -0.15,
                majorHolders = listOf(
                    MajorShareholder("Tata Sons Private Limited", "Promoter", 43.72, 1612000000),
                    MajorShareholder("Life Insurance Corporation of India (LIC)", "Insurance", 5.25, 193500000),
                    MajorShareholder("ICICI Prudential Value Discovery Fund", "Mutual Fund", 2.10, 77400000),
                    MajorShareholder("Government of Singapore (GIC)", "FII / FPI", 1.85, 68200000)
                )
            ),
            dividendHistory = listOf(
                DividendRecord("2026-06-12", "Final", 6.00, 0.62, 14.5),
                DividendRecord("2025-06-11", "Final", 6.00, 0.68, 15.0),
                DividendRecord("2024-06-12", "Final", 2.00, 0.25, 8.0)
            ),
            regulatoryFilings = listOf(
                RegulatoryFiling("2026-08-22", "Reg 30", "Demerger update: Passenger Vehicle (PV+EV) & Commercial Vehicle (CV) businesses", "Two distinct listed pure-play entities to be created by FY27.", "POSITIVE"),
                RegulatoryFiling("2026-07-25", "Reg 33", "JLR Wholesale volumes increase 5.4% YoY with record Defender order book", "Net debt at JLR reduced to near-zero levels.", "POSITIVE")
            ),
            institutionalDeals = listOf(
                InstitutionalDeal("2026-08-15", "Block Deal", "Marshall Wace Investment Strategies", "BUY", 680000, 965.00, 65.62),
                InstitutionalDeal("2026-07-10", "Bulk Deal", "SBI Mutual Fund", "BUY", 550000, 950.00, 52.25)
            ),
            risksAndOpportunities = RisksAndOpportunities(
                growthOpportunities = listOf(
                    "Demerger into two focused listed entities (PV+EV and Commercial Vehicles) unlocking conglomerate discount.",
                    "70%+ domestic passenger EV market share led by Nexon.ev, Punch.ev and upcoming Sierra.ev.",
                    "Jaguar Land Rover's luxury electrification transition with Range Rover Electric."
                ),
                strategicRisks = listOf(
                    "European EV demand slowdown and Chinese OEM price wars in UK/EU markets.",
                    "Cyclicality in domestic Indian commercial vehicle infrastructure demand."
                ),
                analystConsensus = "79% BUY (34 Analysts)",
                targetConsensusMean = 1120.00,
                quantitativeVerdict = "Exceptional turnaround story with net auto debt eliminated and EV leadership."
            ),
            historicalQuotes = history
        )
    }

    private fun createSunPharma(): BseStock {
        val curPrice = 1885.00
        val prevClose = 1868.00
        val history = generateHistory(curPrice, 1100.0, 1940.0)
        val proj = recalculateProjections(curPrice, 13.5, 27.0, 16.5, 10.8)
        return BseStock(
            scripCode = "524715",
            scripId = "SUNPHARMA",
            companyName = "Sun Pharmaceutical Industries Ltd",
            sector = "Pharmaceuticals & Healthcare",
            group = "A",
            marketCapType = MarketCapType.LARGE_CAP,
            currentPrice = curPrice,
            previousClose = prevClose,
            dayOpen = 1872.00,
            dayHigh = 1898.00,
            dayLow = 1865.00,
            volume = 1620000,
            turnoverCr = 305.40,
            fiftyTwoWeekHigh = 1960.00,
            fiftyTwoWeekLow = 1120.00,
            upperCircuit = 2055.00,
            lowerCircuit = 1681.00,
            peRatio = 38.4,
            pbRatio = 6.2,
            sectorPe = 34.0,
            dividendYield = 0.85,
            faceValue = 1.0,
            technicals = TechnicalParameters(
                rsi14 = 60.5,
                rsiStatus = "Bullish",
                macdLine = 18.2,
                macdSignal = 14.1,
                macdHistogram = 4.1,
                macdStatus = "Bullish",
                bollingerUpper = 1930.0,
                bollingerMiddle = 1870.0,
                bollingerLower = 1810.0,
                bollingerBandwidth = 6.4,
                sma20 = 1872.0,
                sma50 = 1815.0,
                sma200 = 1580.0,
                dmaCrossSignal = "Above 200 DMA",
                pivotClassic = PivotLevels(r3 = 1930.0, r2 = 1910.0, r1 = 1896.0, pivot = 1882.0, s1 = 1868.0, s2 = 1854.0, s3 = 1840.0),
                pivotFibonacci = PivotLevels(r3 = 1925.0, r2 = 1908.0, r1 = 1894.0, pivot = 1882.0, s1 = 1870.0, s2 = 1856.0, s3 = 1842.0),
                stochasticK = 67.2,
                stochasticD = 62.0,
                atr14 = 28.00,
                betaSensex = 0.62,
                sharpeRatio = 2.10,
                alphaRatio = 8.90,
                overallSignal = TechnicalSignal.BUY,
                technicalScore = 79
            ),
            projections = proj,
            ipoMetrics = ListingAndIpoMetrics(
                listingDate = "1994-11-08",
                listingStatus = "Active - Normal T+1 Rolling",
                bseScripGroup = "A - Premier",
                isinCode = "INE044A01036",
                ipoIssueDate = "Nov 1994",
                ipoIssuePrice = 150.0,
                ipoIssueSizeCr = 55.0,
                subscriptionQib = 8.5,
                subscriptionHni = 6.2,
                subscriptionRetail = 3.8,
                subscriptionTotal = 5.6,
                listingDayOpen = 220.0,
                listingDayClose = 245.0,
                listingDayGainPercent = 63.3,
                allTimeGainPercent = 125600.0,
                multibaggerFactor = 1256.0,
                outperformanceVsSensex = 480.0
            ),
            corporateGovernance = CorporateGovernance(
                corporateGroup = "Sun Pharma Group (Dilip Shanghvi)",
                cin = "L24230GJ1993PLC019050",
                registeredOffice = "SPARC, Tandalja, Vadodara, Gujarat 390012",
                website = "www.sunpharma.com",
                auditor = "S R B C & CO LLP",
                managementTeam = listOf(
                    ManagementProfile("Dilip Shanghvi", "Managing Director", 41, "Commerce Graduate", "Self-made pharma billionaire building India's largest specialty formulation pharmaceutical multinational."),
                    ManagementProfile("Kirti Ganorkar", "CEO - India Business", 28, "B.Pharm, MBA", "Leads #1 domestic pharma sales force with 8.5% market share.")
                )
            ),
            shareholding = ShareholdingSummary(
                quarter = "Q1 FY26 (June 2026)",
                promoterPercent = 54.48,
                promoterPledgedPercent = 0.0,
                fiiPercent = 17.85,
                diiPercent = 19.40,
                publicRetailPercent = 8.27,
                governmentPercent = 0.0,
                nonInstitutionPercent = 0.0,
                promoterChangeQoQ = 0.0,
                fiiChangeQoQ = 0.30,
                diiChangeQoQ = 0.15,
                majorHolders = listOf(
                    MajorShareholder("Dilip S. Shanghvi (Promoter)", "Promoter", 9.60, 230000000),
                    MajorShareholder("Life Insurance Corporation of India (LIC)", "Insurance", 6.80, 163000000),
                    MajorShareholder("ICICI Prudential Pharma Healthcare Fund", "Mutual Fund", 2.40, 57500000),
                    MajorShareholder("Vanguard Emerging Markets Stock Index Fund", "FII / FPI", 1.90, 45500000)
                )
            ),
            dividendHistory = listOf(
                DividendRecord("2026-07-10", "Final", 5.00, 0.27, 28.0),
                DividendRecord("2026-02-06", "Interim", 8.50, 0.46, 32.0),
                DividendRecord("2025-07-12", "Final", 4.50, 0.32, 26.0)
            ),
            regulatoryFilings = listOf(
                RegulatoryFiling("2026-08-25", "Reg 30", "US FDA approval for Deuruxolitinib (Leqselvi) for Alopecia Areata", "Commercial rollout begins in US specialty clinics.", "POSITIVE"),
                RegulatoryFiling("2026-07-26", "Reg 33", "Global Specialty revenue grows 19% YoY to $305 Million", "Ilumya and Cequa maintain double digit volume growth.", "POSITIVE")
            ),
            institutionalDeals = listOf(
                InstitutionalDeal("2026-09-02", "Block Deal", "BlackRock Advisors LLC", "BUY", 340000, 1875.00, 63.75),
                InstitutionalDeal("2026-08-11", "Bulk Deal", "SBI Mutual Fund", "BUY", 280000, 1860.00, 52.08)
            ),
            risksAndOpportunities = RisksAndOpportunities(
                growthOpportunities = listOf(
                    "High-margin Global Specialty portfolio (Ilumya, Cequa, Winlevi, Leqselvi) crossing $1.2B annual run-rate.",
                    "Unrivaled 8.5% market share in high-ROCE domestic branded formulations in India.",
                    "Fortress net cash balance sheet of ₹18,000+ Cr enabling accretive global tuck-in acquisitions."
                ),
                strategicRisks = listOf(
                    "US FDA inspection observations at Halol or Mohali manufacturing facilities.",
                    "Price erosion on legacy oral solid generic formulations in US hospital networks."
                ),
                analystConsensus = "86% BUY (33 Analysts)",
                targetConsensusMean = 2100.00,
                quantitativeVerdict = "Highest quality pharma compounding giant with increasing high-barrier global specialty revenue."
            ),
            historicalQuotes = history
        )
    }

    private fun createZomato(): BseStock {
        val curPrice = 282.50
        val prevClose = 274.00
        val history = generateHistory(curPrice, 102.0, 298.0)
        val proj = recalculateProjections(curPrice, 32.0, 8.5, 38.0, 12.5)
        return BseStock(
            scripCode = "543320",
            scripId = "ZOMATO",
            companyName = "Zomato Ltd (Eternal)",
            sector = "Consumer Internet / Quick Commerce (Blinkit)",
            group = "A",
            marketCapType = MarketCapType.LARGE_CAP,
            currentPrice = curPrice,
            previousClose = prevClose,
            dayOpen = 276.00,
            dayHigh = 286.00,
            dayLow = 275.00,
            volume = 18450000,
            turnoverCr = 518.20,
            fiftyTwoWeekHigh = 298.20,
            fiftyTwoWeekLow = 101.40,
            upperCircuit = 330.00,
            lowerCircuit = 220.00,
            peRatio = 88.5,
            pbRatio = 11.2,
            sectorPe = 65.0,
            dividendYield = 0.0,
            faceValue = 1.0,
            technicals = TechnicalParameters(
                rsi14 = 65.4,
                rsiStatus = "Bullish",
                macdLine = 8.5,
                macdSignal = 6.4,
                macdHistogram = 2.1,
                macdStatus = "Bullish Expansion",
                bollingerUpper = 295.0,
                bollingerMiddle = 272.0,
                bollingerLower = 249.0,
                bollingerBandwidth = 16.9,
                sma20 = 272.0,
                sma50 = 255.0,
                sma200 = 205.0,
                dmaCrossSignal = "Super Bullish Above 200 DMA",
                pivotClassic = PivotLevels(r3 = 294.0, r2 = 289.0, r1 = 285.0, pivot = 281.0, s1 = 277.0, s2 = 273.0, s3 = 268.0),
                pivotFibonacci = PivotLevels(r3 = 292.0, r2 = 288.0, r1 = 284.0, pivot = 281.0, s1 = 278.0, s2 = 274.0, s3 = 269.0),
                stochasticK = 74.0,
                stochasticD = 68.5,
                atr14 = 8.40,
                betaSensex = 1.75,
                sharpeRatio = 2.25,
                alphaRatio = 18.20,
                overallSignal = TechnicalSignal.BUY,
                technicalScore = 84
            ),
            projections = proj,
            ipoMetrics = ListingAndIpoMetrics(
                listingDate = "2021-07-23",
                listingStatus = "Active - Normal T+1 Rolling",
                bseScripGroup = "A - High Liquidity",
                isinCode = "INE758T01015",
                ipoIssueDate = "Jul 2021",
                ipoIssuePrice = 76.0,
                ipoIssueSizeCr = 9375.0,
                subscriptionQib = 51.8,
                subscriptionHni = 32.9,
                subscriptionRetail = 7.4,
                subscriptionTotal = 38.2,
                listingDayOpen = 115.0,
                listingDayClose = 125.85,
                listingDayGainPercent = 65.6,
                allTimeGainPercent = 271.7,
                multibaggerFactor = 3.72,
                outperformanceVsSensex = 220.0
            ),
            corporateGovernance = CorporateGovernance(
                corporateGroup = "Eternal Conglomerate (Deepinder Goyal)",
                cin = "L93030DL2010PLC198141",
                registeredOffice = "Ground Floor, 12A, 94 Meghdoot, Nehru Place, New Delhi 110019",
                website = "www.zomato.com",
                auditor = "Deloitte Haskins & Sells LLP",
                managementTeam = listOf(
                    ManagementProfile("Deepinder Goyal", "Founder, MD & CEO", 16, "IIT Delhi", "Visionary founder of Zomato, architect of Blinkit quick-commerce dominance."),
                    ManagementProfile("Albinder Dhindsa", "Founder & CEO - Blinkit", 11, "IIT Delhi, Columbia Business School", "Pioneered 10-minute quick-commerce dark-store network across Indian metros."),
                    ManagementProfile("Akshant Goyal", "Chief Financial Officer", 8, "IIM Bangalore", "Turned Zomato consolidated EBITDA positive with disciplined cash generation.")
                )
            ),
            shareholding = ShareholdingSummary(
                quarter = "Q1 FY26 (June 2026)",
                promoterPercent = 0.0, // Professionally managed tech company post pre-IPO conversions
                promoterPledgedPercent = 0.0,
                fiiPercent = 54.20,
                diiPercent = 18.60,
                publicRetailPercent = 27.20,
                governmentPercent = 0.0,
                nonInstitutionPercent = 0.0,
                promoterChangeQoQ = 0.0,
                fiiChangeQoQ = 1.80,
                diiChangeQoQ = 0.65,
                majorHolders = listOf(
                    MajorShareholder("Antfin Singapore (Alipay)", "Corporate", 4.10, 360000000),
                    MajorShareholder("Motilal Oswal Focused 25 Fund", "Mutual Fund", 3.85, 338000000),
                    MajorShareholder("Fidelity Management & Research", "FII / FPI", 3.20, 281000000),
                    MajorShareholder("Deepinder Goyal (Founder)", "Management", 4.25, 373000000)
                )
            ),
            dividendHistory = emptyList(), // Growth tech company reinvesting into dark stores
            regulatoryFilings = listOf(
                RegulatoryFiling("2026-08-28", "Reg 30", "Acquisition of Paytm Entertainment Ticketing business completed", "Building District app for live events and movie ticketing.", "POSITIVE"),
                RegulatoryFiling("2026-07-22", "Reg 33", "Blinkit GOV surges 130% YoY; Dark stores cross 1,000 threshold", "Blinkit EBITDA hits break-even milestone ahead of guidance.", "POSITIVE")
            ),
            institutionalDeals = listOf(
                InstitutionalDeal("2026-08-30", "Block Deal", "Morgan Stanley Asia Singapore", "BUY", 2400000, 280.00, 67.20),
                InstitutionalDeal("2026-07-18", "Bulk Deal", "SBI Mutual Fund", "BUY", 1800000, 268.00, 48.24)
            ),
            risksAndOpportunities = RisksAndOpportunities(
                growthOpportunities = listOf(
                    "Blinkit Quick-Commerce expanding SKU range into electronics, apparel, and gifting.",
                    "District app monetizing high-margin live events, concerts, and dining out.",
                    "Advertising revenue from FMCG brands scaling on quick-commerce checkouts."
                ),
                strategicRisks = listOf(
                    "Aggressive dark-store competition from Zepto, Swiggy Instamart, and Flipkart Minutes.",
                    "Potential gig-worker platform regulatory policy and minimum wage compliances."
                ),
                analystConsensus = "88% BUY (31 Analysts)",
                targetConsensusMean = 340.00,
                quantitativeVerdict = "Fastest-growing consumer tech champion with undeniable quick-commerce leadership."
            ),
            historicalQuotes = history
        )
    }

    private fun generateHistory(current: Double, minVal: Double, maxVal: Double): List<HistoricalQuote> {
        val quotes = mutableListOf<HistoricalQuote>()
        val count = 30
        var price = minVal + (current - minVal) * 0.4
        for (i in count downTo 1) {
            val progress = (count - i).toDouble() / count.toDouble()
            val noise = (Math.sin(i * 0.7) * 0.03) + (Math.cos(i * 1.3) * 0.02)
            val base = minVal + (current - minVal) * progress
            val c = (base * (1.0 + noise)).coerceIn(minVal * 0.9, maxVal * 1.05)
            val h = c * (1.0 + (Math.abs(noise) * 0.5 + 0.008))
            val l = c * (1.0 - (Math.abs(noise) * 0.5 + 0.008))
            val o = (h + l) / 2.0
            val vol = (1500000 + (Math.sin(i * 0.5) * 600000)).toLong()
            quotes.add(
                HistoricalQuote(
                    date = "Day -$i",
                    open = round2(o),
                    high = round2(h),
                    low = round2(l),
                    close = if (i == 1) current else round2(c),
                    volume = vol
                )
            )
        }
        return quotes
    }
}
