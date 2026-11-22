package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardBackground
import com.example.ui.theme.GoldDark
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.UpperBodyAccent

data class DailyVolumePoint(
    val date: String, // yyyy-MM-dd
    val displayDate: String, // "23 Sep"
    val volumeKg: Float,
    val sessionCount: Int = 1,
    val dayLabel: String = ""
)

@Composable
fun VolumeTrendChart(
    dataPoints: List<DailyVolumePoint>,
    modifier: Modifier = Modifier
) {
    if (dataPoints.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Belum ada sesi latihan dalam 30 hari terakhir",
                color = SecondaryText,
                fontSize = 13.sp
            )
        }
        return
    }

    val density = LocalDensity.current
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val accentColor = UpperBodyAccent
    val gridColor = SecondaryText.copy(alpha = 0.22f)
    val mutedTextColor = SecondaryText.toArgb()
    val minVolume = 0f

    val (maxVolume, range) = remember(dataPoints) {
        var rawMax = 0f
        for (i in dataPoints.indices) {
            val v = dataPoints[i].volumeKg
            if (v > rawMax) rawMax = v
        }
        val maxVol = if (rawMax <= 0f) 100f else (rawMax * 1.15f)
        val r = maxVol.coerceAtLeast(1f)
        Pair(maxVol, r)
    }

    // Precompute normalized Y fractions and X-axis label indices outside the draw lambda
    val normalizedY = remember(dataPoints, minVolume, range) {
        FloatArray(dataPoints.size) { index ->
            1f - (dataPoints[index].volumeKg - minVolume) / range
        }
    }

    val indicesToLabel = remember(dataPoints.size) {
        when {
            dataPoints.isEmpty() -> IntArray(0)
            dataPoints.size <= 4 -> IntArray(dataPoints.size) { it }
            else -> intArrayOf(0, dataPoints.size / 2, dataPoints.lastIndex)
        }
    }

    // Remember Paint, PathEffect, and Paths outside the Canvas draw lambda
    val textPaint = remember(density, mutedTextColor) {
        android.graphics.Paint().apply {
            color = mutedTextColor
            textSize = with(density) { 9.5.sp.toPx() }
            isAntiAlias = true
        }
    }
    val gridDashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(8f, 8f)) }
    val fillPath = remember { Path() }
    val linePath = remember { Path() }

    Column(modifier = modifier.fillMaxWidth()) {
        // Interactive detail pill when tapped
        val activePoint = selectedIndex?.let { dataPoints.getOrNull(it) } ?: dataPoints.lastOrNull()
        if (activePoint != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${activePoint.displayDate} • ${activePoint.dayLabel.ifBlank { "Latihan" }}",
                        color = SecondaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                val formattedVol = if (activePoint.volumeKg % 1f == 0f) {
                    "%,d kg".format(activePoint.volumeKg.toInt())
                } else {
                    "%,.1f kg".format(activePoint.volumeKg)
                }
                Text(
                    text = formattedVol,
                    color = PrimaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .testTag("volume_trend_chart_canvas")
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(dataPoints) {
                        detectTapGestures { offset ->
                            val width = size.width
                            val paddingLeft = 52.dp.toPx()
                            val paddingRight = 24.dp.toPx()
                            val chartWidth = width - paddingLeft - paddingRight

                            if (dataPoints.size == 1) {
                                selectedIndex = 0
                            } else {
                                val stepX = chartWidth / (dataPoints.size - 1)
                                val touchX = (offset.x - paddingLeft).coerceIn(0f, chartWidth)
                                val index = (touchX / stepX + 0.5f).toInt().coerceIn(0, dataPoints.lastIndex)
                                selectedIndex = index
                            }
                        }
                    }
            ) {
                val width = size.width
                val height = size.height
                val paddingLeft = 52.dp.toPx()
                val paddingRight = 24.dp.toPx()
                val paddingTop = 24.dp.toPx()
                val paddingBottom = 32.dp.toPx()

                val chartWidth = width - paddingLeft - paddingRight
                val chartHeight = height - paddingTop - paddingBottom

                // 3 horizontal gridlines (0%, 50%, 100%)
                val gridSteps = 3
                for (i in 0..gridSteps) {
                    val ratio = i.toFloat() / gridSteps
                    val y = paddingTop + (1f - ratio) * chartHeight
                    val value = minVolume + ratio * range

                    // Dashed line
                    drawLine(
                        color = gridColor,
                        start = Offset(paddingLeft, y),
                        end = Offset(width - paddingRight, y),
                        strokeWidth = 1f,
                        pathEffect = gridDashEffect
                    )

                    // Y-axis label
                    val label = if (value >= 1000f) {
                        "%.1fk".format(value / 1000f)
                    } else {
                        "${value.toInt()}"
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        8.dp.toPx(),
                        y + 3.dp.toPx(),
                        textPaint
                    )
                }

                val count = dataPoints.size
                val stepX = if (count > 1) chartWidth / (count - 1) else 0f

                // Single-pass path construction for Area Gradient Fill & Smooth Line
                if (count > 1) {
                    val baseLineY = paddingTop + chartHeight
                    val firstX = paddingLeft
                    val firstY = paddingTop + normalizedY[0] * chartHeight

                    fillPath.reset()
                    fillPath.moveTo(firstX, baseLineY)
                    fillPath.lineTo(firstX, firstY)

                    linePath.reset()
                    linePath.moveTo(firstX, firstY)

                    var prevX = firstX
                    var prevY = firstY
                    for (i in 1 until count) {
                        val currX = paddingLeft + i * stepX
                        val currY = paddingTop + normalizedY[i] * chartHeight

                        val midX = (prevX + currX) / 2f
                        fillPath.cubicTo(midX, prevY, midX, currY, currX, currY)
                        linePath.cubicTo(midX, prevY, midX, currY, currX, currY)
                        prevX = currX
                        prevY = currY
                    }
                    fillPath.lineTo(prevX, baseLineY)
                    fillPath.close()

                    drawPath(
                        path = fillPath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                GoldDark.copy(alpha = 0.25f),
                                UpperBodyAccent.copy(alpha = 0.38f)
                            ),
                            startX = firstX,
                            endX = prevX
                        )
                    )

                    drawPath(
                        path = linePath,
                        color = accentColor,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // X-axis date labels (show start, middle, end)
                for (idx in indicesToLabel) {
                    val ptX = if (count == 1) paddingLeft + chartWidth / 2f else paddingLeft + idx * stepX
                    val dateText = dataPoints[idx].displayDate
                    val textWidth = textPaint.measureText(dateText)
                    val textX = (ptX - textWidth / 2f).coerceIn(paddingLeft, width - paddingRight - textWidth)
                    drawContext.canvas.nativeCanvas.drawText(
                        dateText,
                        textX,
                        height - 8.dp.toPx(),
                        textPaint
                    )
                }

                // Dots on points
                val selectedRadius = 6.dp.toPx()
                val unselectedRadius = 3.5.dp.toPx()
                val haloRadius = 12.dp.toPx()
                val innerInset = 1.5.dp.toPx()

                for (i in 0 until count) {
                    val ptX = if (count == 1) paddingLeft + chartWidth / 2f else paddingLeft + i * stepX
                    val ptY = paddingTop + normalizedY[i] * chartHeight
                    val pt = Offset(ptX, ptY)
                    val isSelected = (selectedIndex == i)
                    val radius = if (isSelected) selectedRadius else unselectedRadius

                    if (isSelected) {
                        drawCircle(
                            color = accentColor.copy(alpha = 0.25f),
                            radius = haloRadius,
                            center = pt
                        )
                    }

                    drawCircle(
                        color = CardBackground,
                        radius = radius,
                        center = pt
                    )
                    drawCircle(
                        color = accentColor,
                        radius = radius - innerInset,
                        center = pt
                    )
                }
            }
        }
    }
}
