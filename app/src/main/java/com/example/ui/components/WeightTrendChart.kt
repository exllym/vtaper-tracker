package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WeightLog
import com.example.ui.theme.GoldDark
import com.example.ui.theme.OnPrimaryDark
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SuccessColor
import com.example.ui.theme.UpperBodyAccent

@Composable
fun WeightTrendChart(
    weights: List<WeightLog>,
    targetKg: Float = 50f,
    startKg: Float = 38f,
    modifier: Modifier = Modifier
) {
    if (weights.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Belum ada riwayat berat badan untuk grafik",
                color = SecondaryText,
                fontSize = 13.sp
            )
        }
        return
    }

    val density = LocalDensity.current
    val lineColor = UpperBodyAccent
    val gridColor = SecondaryText.copy(alpha = 0.22f)
    val targetLineColor = SecondaryText.copy(alpha = 0.30f)
    val textColor = PrimaryText.toArgb()
    val mutedTextColor = SecondaryText.toArgb()
    val targetLineTextColor = SuccessColor.toArgb()

    val targetLabel = remember(targetKg) {
        if (targetKg % 1f == 0f) "${targetKg.toInt()}kg" else "${targetKg}kg"
    }
    val startLabel = remember(startKg) {
        if (startKg % 1f == 0f) "${startKg.toInt()}kg" else "${startKg}kg"
    }

    val (minKg, maxKg, range) = remember(weights, startKg, targetKg) {
        var min = startKg - 1f
        var max = targetKg + 1f
        for (i in weights.indices) {
            val kg = weights[i].kg
            if (kg < min) min = kg
            if (kg > max) max = kg
        }
        val r = (max - min).coerceAtLeast(1f)
        Triple(min, max, r)
    }

    // Precompute normalized Y ratios (depend only on data & range, independent of canvas size)
    val normalizedY = remember(weights, minKg, range) {
        FloatArray(weights.size) { index ->
            1f - (weights[index].kg - minKg) / range
        }
    }

    // Remember Paint objects and Path/PathEffects outside the Canvas draw lambda
    val targetPaint = remember(density, targetLineTextColor) {
        android.graphics.Paint().apply {
            color = targetLineTextColor
            textSize = with(density) { 10.sp.toPx() }
            isAntiAlias = true
        }
    }

    val startPaint = remember(density, mutedTextColor) {
        android.graphics.Paint().apply {
            color = mutedTextColor
            textSize = with(density) { 10.sp.toPx() }
            isAntiAlias = true
        }
    }

    val singlePointPaint = remember(density, textColor) {
        android.graphics.Paint().apply {
            color = textColor
            textSize = with(density) { 12.sp.toPx() }
            isAntiAlias = true
        }
    }

    val onAccentTextColor = OnPrimaryDark.toArgb()
    val labelPaint = remember(density, onAccentTextColor) {
        android.graphics.Paint().apply {
            color = onAccentTextColor
            textSize = with(density) { 12.sp.toPx() }
            isFakeBoldText = true
            isAntiAlias = true
        }
    }

    val targetDashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 10f)) }
    val startDashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(8f, 8f)) }
    val fillPath = remember { Path() }
    val linePath = remember { Path() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(top = 10.dp, bottom = 10.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val paddingLeft = 45.dp.toPx()
            val paddingRight = 45.dp.toPx()
            val paddingTop = 25.dp.toPx()
            val paddingBottom = 30.dp.toPx()

            val chartWidth = width - paddingLeft - paddingRight
            val chartHeight = height - paddingTop - paddingBottom

            val targetY = paddingTop + (1f - (targetKg - minKg) / range) * chartHeight
            drawLine(
                color = targetLineColor,
                start = Offset(paddingLeft, targetY),
                end = Offset(width - paddingRight, targetY),
                strokeWidth = 1.5f,
                pathEffect = targetDashEffect
            )

            drawContext.canvas.nativeCanvas.drawText(
                "Target $targetLabel",
                paddingLeft + 4.dp.toPx(),
                targetY - 6.dp.toPx(),
                targetPaint
            )

            // Bottom base line
            val startY = paddingTop + (1f - (startKg - minKg) / range) * chartHeight
            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, startY),
                end = Offset(width - paddingRight, startY),
                strokeWidth = 1f,
                pathEffect = startDashEffect
            )
            drawContext.canvas.nativeCanvas.drawText(
                "Mulai $startLabel",
                paddingLeft + 4.dp.toPx(),
                startY - 6.dp.toPx(),
                startPaint
            )

            if (weights.size == 1) {
                val x = paddingLeft + chartWidth / 2f
                val y = paddingTop + normalizedY[0] * chartHeight
                val center = Offset(x, y)

                drawCircle(
                    color = lineColor,
                    radius = 6.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = PrimaryText,
                    radius = 3.dp.toPx(),
                    center = center
                )

                val label = "${weights[0].kg}kg"
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    x - 16.dp.toPx(),
                    y - 12.dp.toPx(),
                    singlePointPaint
                )
                return@Canvas
            }

            // Multiple points: single-pass path construction without intermediate List<Offset> allocation
            val count = weights.size
            val stepX = chartWidth / (count - 1)
            val baseLineY = paddingTop + chartHeight
            var prevX = paddingLeft
            var prevY = paddingTop + normalizedY[0] * chartHeight

            fillPath.reset()
            fillPath.moveTo(prevX, baseLineY)
            fillPath.lineTo(prevX, prevY)

            linePath.reset()
            linePath.moveTo(prevX, prevY)

            for (i in 1 until count) {
                val currX = paddingLeft + i * stepX
                val currY = paddingTop + normalizedY[i] * chartHeight
                val cX = (prevX + currX) / 2f
                fillPath.cubicTo(cX, prevY, cX, currY, currX, currY)
                linePath.cubicTo(cX, prevY, cX, currY, currX, currY)
                prevX = currX
                prevY = currY
            }
            fillPath.lineTo(prevX, baseLineY)
            fillPath.close()

            // Area fill horizontal gradient from GoldDark to UpperBodyAccent
            drawPath(
                path = fillPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        GoldDark.copy(alpha = 0.25f),
                        UpperBodyAccent.copy(alpha = 0.38f)
                    ),
                    startX = paddingLeft,
                    endX = prevX
                )
            )

            // Draw line
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Draw data points
            val outerRadius = 5.dp.toPx()
            val innerRadius = 2.5.dp.toPx()
            for (i in 0 until count) {
                val pt = Offset(paddingLeft + i * stepX, paddingTop + normalizedY[i] * chartHeight)
                drawCircle(
                    color = lineColor,
                    radius = outerRadius,
                    center = pt
                )
                drawCircle(
                    color = PrimaryText,
                    radius = innerRadius,
                    center = pt
                )
            }

            // Latest weight value labeled at the end of the line
            val lastX = prevX
            val lastY = prevY
            val latestKg = weights[count - 1].kg
            val latestLabel = "${latestKg}kg"

            // Label background badge
            val labelWidth = labelPaint.measureText(latestLabel)
            val badgeX = (lastX - labelWidth / 2f).coerceIn(paddingLeft, width - labelWidth - 8.dp.toPx())
            val badgeY = lastY - 14.dp.toPx()

            drawRoundRect(
                color = lineColor,
                topLeft = Offset(badgeX - 6.dp.toPx(), badgeY - 14.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(labelWidth + 12.dp.toPx(), 18.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )

            drawContext.canvas.nativeCanvas.drawText(
                latestLabel,
                badgeX,
                badgeY,
                labelPaint
            )
        }
    }
}
