package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HistoricalQuote
import com.example.data.model.PriceProjectionModel
import com.example.ui.theme.BearRed
import com.example.ui.theme.BullGreen
import com.example.ui.theme.DalalBlueLight
import com.example.ui.theme.DalalGold

@Composable
fun InteractiveStockChart(
    quotes: List<HistoricalQuote>,
    isCandleMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (quotes.isEmpty()) return

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(quotes) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis = 600))
    }

    val selectedQuote = selectedIndex?.let { if (it in quotes.indices) quotes[it] else null }

    Column(modifier = modifier) {
        // Scrub header display
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedQuote != null) {
                Column {
                    Text(
                        text = "₹${"%.2f".format(selectedQuote.close)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "O: ${selectedQuote.open}  H: ${selectedQuote.high}  L: ${selectedQuote.low}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = selectedQuote.date,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                val latest = quotes.last()
                val first = quotes.first()
                val change = latest.close - first.close
                val pct = if (first.close > 0) (change / first.close) * 100 else 0.0
                Column {
                    Text(
                        text = "₹${"%.2f".format(latest.close)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${if (change >= 0) "+" else ""}${"%.2f".format(change)} (${"%.2f".format(pct)}%)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (change >= 0) BullGreen else BearRed
                    )
                }
                Text(
                    text = "Touch & drag to inspect",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .pointerInput(quotes) {
                    detectTapGestures(
                        onPress = { offset ->
                            val width = size.width
                            val step = width / quotes.size
                            val idx = (offset.x / step).toInt().coerceIn(0, quotes.size - 1)
                            selectedIndex = idx
                            tryAwaitRelease()
                            selectedIndex = null
                        }
                    )
                }
                .pointerInput(quotes) {
                    detectDragGestures(
                        onDrag = { change, _ ->
                            val width = size.width
                            val step = width / quotes.size
                            val idx = (change.position.x / step).toInt().coerceIn(0, quotes.size - 1)
                            selectedIndex = idx
                        },
                        onDragEnd = { selectedIndex = null },
                        onDragCancel = { selectedIndex = null }
                    )
                }
        ) {
            val isBullish = quotes.last().close >= quotes.first().close
            val lineColor = if (isBullish) BullGreen else BearRed
            val areaBrush = Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = 0.35f),
                    lineColor.copy(alpha = 0.02f)
                )
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val minPrice = quotes.minOf { it.low } * 0.99
                val maxPrice = quotes.maxOf { it.high } * 1.01
                val priceRange = if (maxPrice > minPrice) maxPrice - minPrice else 1.0

                if (isCandleMode) {
                    drawCandlestickChart(
                        quotes = quotes,
                        minPrice = minPrice,
                        priceRange = priceRange,
                        canvasWidth = canvasWidth,
                        canvasHeight = canvasHeight
                    )
                } else {
                    drawAreaLineChart(
                        quotes = quotes,
                        minPrice = minPrice,
                        priceRange = priceRange,
                        canvasWidth = canvasWidth,
                        canvasHeight = canvasHeight,
                        lineColor = lineColor,
                        areaBrush = areaBrush,
                        animProgress = progress.value
                    )
                }

                // Draw scrub indicator line if user is touching
                selectedIndex?.let { idx ->
                    if (idx in quotes.indices) {
                        val x = (idx.toFloat() / (quotes.size - 1).coerceAtLeast(1)) * canvasWidth
                        val q = quotes[idx]
                        val y = canvasHeight - (((q.close - minPrice) / priceRange).toFloat() * canvasHeight)

                        drawLine(
                            color = Color.White.copy(alpha = 0.6f),
                            start = Offset(x, 0f),
                            end = Offset(x, canvasHeight),
                            strokeWidth = 1.5.dp.toPx()
                        )
                        drawCircle(
                            color = lineColor,
                            radius = 5.dp.toPx(),
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.5.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawAreaLineChart(
    quotes: List<HistoricalQuote>,
    minPrice: Double,
    priceRange: Double,
    canvasWidth: Float,
    canvasHeight: Float,
    lineColor: Color,
    areaBrush: Brush,
    animProgress: Float
) {
    val count = quotes.size
    if (count < 2) return

    val path = Path()
    val fillPath = Path()

    val points = quotes.mapIndexed { index, quote ->
        val x = (index.toFloat() / (count - 1)) * canvasWidth
        val normalized = ((quote.close - minPrice) / priceRange).toFloat()
        val y = canvasHeight - (normalized * canvasHeight * animProgress)
        Offset(x, y)
    }

    path.moveTo(points.first().x, points.first().y)
    fillPath.moveTo(points.first().x, canvasHeight)
    fillPath.lineTo(points.first().x, points.first().y)

    for (i in 1 until points.size) {
        val prev = points[i - 1]
        val curr = points[i]
        val cX = (prev.x + curr.x) / 2f
        path.cubicTo(cX, prev.y, cX, curr.y, curr.x, curr.y)
        fillPath.cubicTo(cX, prev.y, cX, curr.y, curr.x, curr.y)
    }

    fillPath.lineTo(points.last().x, canvasHeight)
    fillPath.close()

    drawPath(path = fillPath, brush = areaBrush)
    drawPath(
        path = path,
        color = lineColor,
        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawCandlestickChart(
    quotes: List<HistoricalQuote>,
    minPrice: Double,
    priceRange: Double,
    canvasWidth: Float,
    canvasHeight: Float
) {
    val count = quotes.size
    val candleSlotWidth = canvasWidth / count
    val candleBodyWidth = (candleSlotWidth * 0.65f).coerceAtLeast(2f)

    quotes.forEachIndexed { i, q ->
        val xCenter = i * candleSlotWidth + candleSlotWidth / 2f
        val isGreen = q.close >= q.open
        val color = if (isGreen) BullGreen else BearRed

        val yHigh = canvasHeight - (((q.high - minPrice) / priceRange).toFloat() * canvasHeight)
        val yLow = canvasHeight - (((q.low - minPrice) / priceRange).toFloat() * canvasHeight)
        val yOpen = canvasHeight - (((q.open - minPrice) / priceRange).toFloat() * canvasHeight)
        val yClose = canvasHeight - (((q.close - minPrice) / priceRange).toFloat() * canvasHeight)

        // Draw Wick
        drawLine(
            color = color,
            start = Offset(xCenter, yHigh),
            end = Offset(xCenter, yLow),
            strokeWidth = 1.2.dp.toPx()
        )

        // Draw Body
        val top = minOf(yOpen, yClose)
        val bodyHeight = maxOf(Math.abs(yClose - yOpen), 2.5f)

        drawRect(
            color = color,
            topLeft = Offset(xCenter - candleBodyWidth / 2f, top),
            size = Size(candleBodyWidth, bodyHeight)
        )
    }
}

@Composable
fun MonteCarloProjectionFanChart(
    projections: PriceProjectionModel,
    currentPrice: Double,
    modifier: Modifier = Modifier
) {
    val p5 = projections.monteCarloP5
    val p25 = projections.monteCarloP25
    val p50 = projections.monteCarloP50
    val p75 = projections.monteCarloP75
    val p95 = projections.monteCarloP95

    if (p50.isEmpty()) return

    Column(modifier = modifier) {
        // Legend row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = DalalBlueLight, label = "P50 Expected")
            LegendItem(color = BullGreen, label = "P95 Bull (+2σ)")
            LegendItem(color = BearRed, label = "P5 Bear (-2σ)")
            LegendItem(color = DalalGold, label = "Interquartile")
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                val allValues = (p5 + p95)
                val minVal = (allValues.minOrNull() ?: currentPrice) * 0.95
                val maxVal = (allValues.maxOrNull() ?: currentPrice) * 1.05
                val range = if (maxVal > minVal) maxVal - minVal else 1.0

                fun getY(price: Double): Float {
                    val norm = ((price - minVal) / range).toFloat()
                    return h - (norm * h)
                }

                fun getX(idx: Int, total: Int): Float {
                    return (idx.toFloat() / (total - 1).coerceAtLeast(1)) * w
                }

                val steps = p50.size

                // Draw outer fan ribbon (P5 to P95)
                val outerRibbon = Path()
                outerRibbon.moveTo(getX(0, steps), getY(p95[0]))
                for (i in 1 until steps) {
                    outerRibbon.lineTo(getX(i, steps), getY(p95[i]))
                }
                for (i in (steps - 1) downTo 0) {
                    outerRibbon.lineTo(getX(i, steps), getY(p5[i]))
                }
                outerRibbon.close()
                drawPath(path = outerRibbon, color = DalalBlueLight.copy(alpha = 0.08f))

                // Draw inner quartile ribbon (P25 to P75)
                val innerRibbon = Path()
                innerRibbon.moveTo(getX(0, steps), getY(p75[0]))
                for (i in 1 until steps) {
                    innerRibbon.lineTo(getX(i, steps), getY(p75[i]))
                }
                for (i in (steps - 1) downTo 0) {
                    innerRibbon.lineTo(getX(i, steps), getY(p25[i]))
                }
                innerRibbon.close()
                drawPath(path = innerRibbon, color = DalalGold.copy(alpha = 0.15f))

                // Draw trajectory lines
                fun drawTrajectory(points: List<Double>, color: Color, strokeWidth: Float, isDashed: Boolean = false) {
                    val p = Path()
                    p.moveTo(getX(0, steps), getY(points[0]))
                    for (i in 1 until steps) {
                        p.lineTo(getX(i, steps), getY(points[i]))
                    }
                    drawPath(
                        path = p,
                        color = color,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                drawTrajectory(p95, BullGreen.copy(alpha = 0.8f), 1.5.dp.toPx())
                drawTrajectory(p5, BearRed.copy(alpha = 0.8f), 1.5.dp.toPx())
                drawTrajectory(p50, DalalBlueLight, 2.5.dp.toPx())

                // Draw year grid vertical markers (Year 1, 2, 3, 4, 5)
                for (yr in 1..5) {
                    val mIndex = (yr * 12).coerceAtMost(steps - 1)
                    val xYr = getX(mIndex, steps)
                    drawLine(
                        color = Color.White.copy(alpha = 0.12f),
                        start = Offset(xYr, 0f),
                        end = Offset(xYr, h),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
        }

        // Years horizontal axis labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Now", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Yr 1", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Yr 2", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Yr 3", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Yr 4", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Yr 5", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

@Composable
fun ShareholdingDonutChart(
    promoter: Double,
    fii: Double,
    dii: Double,
    publicRetail: Double,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Triple("Promoter", promoter, DalalBlueLight),
        Triple("DII (MF/Ins)", dii, BullGreen),
        Triple("FII / FPI", fii, DalalGold),
        Triple("Public/Retail", publicRetail, Color(0xFFA78BFA))
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = -90f
                val total = (promoter + fii + dii + publicRetail).coerceAtLeast(1.0)
                val strokeW = 18.dp.toPx()

                items.forEach { (_, pct, color) ->
                    val sweep = ((pct / total) * 360f).toFloat()
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                    )
                    startAngle += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${"%.0f".format(promoter)}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = DalalBlueLight
                )
                Text(
                    text = "Promoter",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEach { (label, pct, color) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(color, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${"%.2f".format(pct)}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
