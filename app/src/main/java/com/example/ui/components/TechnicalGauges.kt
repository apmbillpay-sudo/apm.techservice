package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PivotLevels
import com.example.data.model.TechnicalParameters
import com.example.data.model.TechnicalSignal
import com.example.ui.theme.BearRed
import com.example.ui.theme.BullGreen
import com.example.ui.theme.DalalBlueLight
import com.example.ui.theme.DalalGold

@Composable
fun TechnicalScoreMeter(
    signal: TechnicalSignal,
    score: Int,
    modifier: Modifier = Modifier
) {
    val signalColor = when (signal) {
        TechnicalSignal.STRONG_BUY, TechnicalSignal.BUY -> BullGreen
        TechnicalSignal.NEUTRAL -> DalalGold
        TechnicalSignal.SELL, TechnicalSignal.STRONG_SELL -> BearRed
    }

    val signalText = when (signal) {
        TechnicalSignal.STRONG_BUY -> "STRONG BUY"
        TechnicalSignal.BUY -> "BULLISH BUY"
        TechnicalSignal.NEUTRAL -> "NEUTRAL"
        TechnicalSignal.SELL -> "BEARISH SELL"
        TechnicalSignal.STRONG_SELL -> "STRONG SELL"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TECHNICAL OUTLOOK",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = signalText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = signalColor
                    )
                }

                Surface(
                    color = signalColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$score / 100",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = signalColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Continuous gauge bar
            LinearProgressIndicator(
                progress = { (score / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = signalColor,
                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0 Bearish", style = MaterialTheme.typography.labelSmall, color = BearRed, fontSize = 10.sp)
                Text("50 Neutral", style = MaterialTheme.typography.labelSmall, color = DalalGold, fontSize = 10.sp)
                Text("100 Bullish", style = MaterialTheme.typography.labelSmall, color = BullGreen, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun RsiIndicatorCard(
    rsi: Double,
    status: String,
    modifier: Modifier = Modifier
) {
    val rsiColor = when {
        rsi >= 70 -> BearRed // Overbought risk
        rsi <= 30 -> BullGreen // Oversold opportunity
        else -> DalalBlueLight
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RSI (14 Period)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${"%.1f".format(rsi)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = rsiColor
                )
            }

            LinearProgressIndicator(
                progress = { (rsi / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = rsiColor,
                trackColor = MaterialTheme.colorScheme.surface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Oversold (30)", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(status, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = rsiColor)
                Text("Overbought (70)", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun MovingAveragesGrid(
    currentPrice: Double,
    sma20: Double,
    sma50: Double,
    sma200: Double,
    dmaSignal: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MOVING AVERAGES & DMA",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    color = BullGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = dmaSignal,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = BullGreen,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MovingAverageItem(
                    label = "20 SMA",
                    value = sma20,
                    currentPrice = currentPrice,
                    modifier = Modifier.weight(1f)
                )
                MovingAverageItem(
                    label = "50 DMA",
                    value = sma50,
                    currentPrice = currentPrice,
                    modifier = Modifier.weight(1f)
                )
                MovingAverageItem(
                    label = "200 DMA",
                    value = sma200,
                    currentPrice = currentPrice,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MovingAverageItem(
    label: String,
    value: Double,
    currentPrice: Double,
    modifier: Modifier = Modifier
) {
    val isAbove = currentPrice >= value
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "₹${"%.1f".format(value)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (isAbove) "▲ Above" else "▼ Below",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = if (isAbove) BullGreen else BearRed,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun PivotLevelsTable(
    pivotLevels: PivotLevels,
    currentPrice: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "PIVOT POINTS (CLASSIC)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val levels = listOf(
                Pair("R3 (Strong Resistance)", pivotLevels.r3),
                Pair("R2", pivotLevels.r2),
                Pair("R1 (Resistance 1)", pivotLevels.r1),
                Pair("PIVOT POINT", pivotLevels.pivot),
                Pair("S1 (Support 1)", pivotLevels.s1),
                Pair("S2", pivotLevels.s2),
                Pair("S3 (Strong Support)", pivotLevels.s3)
            )

            levels.forEach { (label, value) ->
                val isPivot = label == "PIVOT POINT"
                val isRes = label.startsWith("R")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isPivot) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isPivot) FontWeight.Bold else FontWeight.Normal,
                        color = if (isPivot) DalalBlueLight else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "₹${"%.2f".format(value)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            isPivot -> DalalBlueLight
                            isRes -> BearRed
                            else -> BullGreen
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun VolatilityMetricsCard(
    technicals: TechnicalParameters,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "RISK & VOLATILITY PARAMETERS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricBox(label = "Beta vs SENSEX", value = "${technicals.betaSensex}", modifier = Modifier.weight(1f))
                MetricBox(label = "Sharpe Ratio", value = "${technicals.sharpeRatio}", modifier = Modifier.weight(1f))
                MetricBox(label = "Alpha (Annual)", value = "+${technicals.alphaRatio}%", modifier = Modifier.weight(1f))
                MetricBox(label = "ATR (14D)", value = "₹${technicals.atr14}", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
