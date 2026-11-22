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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WeightTrendResult
import com.example.data.model.WeightTrendStatus
import com.example.ui.theme.GuildTitle
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SuccessColor
import com.example.ui.theme.UpperBodyAccent
import com.example.ui.theme.guildPlaque
import java.util.Locale

@Composable
fun WeeklyTrendAnalysisBlock(
    weightTrendResult: WeightTrendResult,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .guildPlaque(12.dp)
            .padding(12.dp)
            .testTag("weekly_trend_analysis_block")
    ) {
        val (statusLabel, badgeBgColor, badgeTextColor) = when (weightTrendResult.status) {
            WeightTrendStatus.ON_TRACK -> Triple("Sesuai Jalur", SuccessColor.copy(alpha = 0.18f), SuccessColor)
            WeightTrendStatus.REACHED_TARGET -> Triple("Target Tercapai", SuccessColor.copy(alpha = 0.18f), SuccessColor)
            WeightTrendStatus.TOO_SLOW -> Triple("Di Bawah Target", UpperBodyAccent.copy(alpha = 0.18f), UpperBodyAccent)
            WeightTrendStatus.TOO_FAST -> Triple("Terlalu Cepat", Color(0x2EFF453A), Color(0xFFFF453A))
            WeightTrendStatus.NOT_ENOUGH_DATA -> Triple("Data Belum Cukup", SecondaryText.copy(alpha = 0.14f), SecondaryText)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = badgeTextColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                GuildTitle(
                    text = "ANALISIS MINGGUAN",
                    fontSize = 12.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeBgColor)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = statusLabel,
                    color = badgeTextColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        val avgChange = weightTrendResult.averageWeeklyChangeKg
        if (avgChange != null) {
            Spacer(modifier = Modifier.height(8.dp))
            val sign = if (avgChange > 0f) "+" else ""
            val changeStr = if (kotlin.math.abs(avgChange) < 0.01f) {
                "0"
            } else {
                String.format(Locale.US, "%.2f", avgChange).trimEnd('0').trimEnd('.')
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rata-rata perubahan:",
                    color = SecondaryText,
                    fontSize = 12.sp
                )
                Text(
                    text = "$sign$changeStr kg / minggu",
                    color = PrimaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            val estWeeks = weightTrendResult.estimatedWeeksToTarget
            if (estWeeks != null && estWeeks > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Estimasi capai target:",
                        color = SecondaryText,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "~$estWeeks minggu lagi",
                        color = UpperBodyAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (weightTrendResult.adviceText.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = weightTrendResult.adviceText,
                color = if (weightTrendResult.status == WeightTrendStatus.NOT_ENOUGH_DATA) SecondaryText else PrimaryText,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}
