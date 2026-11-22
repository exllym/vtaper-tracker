package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.data.model.WorkoutSession
import com.example.ui.components.DailyVolumePoint
import com.example.ui.components.VolumeTrendChart
import com.example.ui.theme.GuildTitle
import com.example.ui.theme.LowerBodyAccent
import com.example.ui.theme.MainBackground
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SuccessColor
import com.example.ui.theme.UpperBodyAccent
import com.example.ui.theme.guildPlaque
import org.json.JSONArray
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class StatsSummaryState(
    val dailyPoints: List<DailyVolumePoint> = emptyList(),
    val totalVolume30Days: Float = 0f,
    val averageVolumePerSession: Float = 0f,
    val bestSessionVolume: Float = 0f
)

private val StatsDisplayDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

fun buildStatsSummary30Days(
    allSessions: List<WorkoutSession>,
    nowMillis: Long = System.currentTimeMillis()
): StatsSummaryState {
    val thirtyDaysAgoMillis = nowMillis - (30L * 24 * 60 * 60 * 1000)
    val last30DaysSessions = allSessions
        .filter { it.timestamp >= thirtyDaysAgoMillis }
        .sortedBy { it.timestamp }

    val groupedByDate = LinkedHashMap<String, MutableList<WorkoutSession>>()
    for (session in last30DaysSessions) {
        groupedByDate.getOrPut(session.date) { mutableListOf() }.add(session)
    }

    val dailyPoints = groupedByDate.map { (dateStr, sessions) ->
        var totalVolumeForDay = 0f
        var label = ""
        for (s in sessions) {
            if (label.isEmpty() && s.dayLabel.isNotBlank()) {
                label = s.dayLabel
            }
            totalVolumeForDay += calculateSessionVolume(s.exercisesJson)
        }

        val display = try {
            val parsed = LocalDate.parse(dateStr)
            parsed.format(StatsDisplayDateFormatter)
        } catch (_: Exception) {
            dateStr.takeLast(5)
        }

        DailyVolumePoint(
            date = dateStr,
            displayDate = display,
            volumeKg = totalVolumeForDay,
            sessionCount = sessions.size,
            dayLabel = label
        )
    }

    val totalVolume30Days = dailyPoints.sumOf { it.volumeKg.toDouble() }.toFloat()
    val averageVolumePerSession = if (dailyPoints.isNotEmpty()) totalVolume30Days / dailyPoints.size else 0f
    val bestSessionVolume = dailyPoints.maxOfOrNull { it.volumeKg } ?: 0f

    return StatsSummaryState(
        dailyPoints = dailyPoints,
        totalVolume30Days = totalVolume30Days,
        averageVolumePerSession = averageVolumePerSession,
        bestSessionVolume = bestSessionVolume
    )
}

@Composable
fun StatsScreen(
    allSessions: List<WorkoutSession>,
    onBack: (() -> Unit)? = null,
    precomputedSummary: StatsSummaryState? = null,
    isActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (onBack != null) {
        BackHandler(enabled = isActive) { onBack() }
    }

    val lazyListState = rememberLazyListState()
    var wasActive by remember { mutableStateOf(isActive) }
    LaunchedEffect(isActive) {
        if (isActive && !wasActive) {
            lazyListState.scrollToItem(0)
        }
        wasActive = isActive
    }

    val summary = precomputedSummary ?: remember(allSessions) {
        buildStatsSummary30Days(allSessions)
    }
    val dailyPoints = summary.dailyPoints
    val totalVolume30Days = summary.totalVolume30Days
    val averageVolumePerSession = summary.averageVolumePerSession
    val bestSessionVolume = summary.bestSessionVolume

    LazyColumn(
        state = lazyListState,
        modifier = modifier
            .fillMaxSize()
            .background(MainBackground),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with optional back navigation
        item(key = "stats_header", contentType = "stats_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("stats_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = PrimaryText
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Column {
                    GuildTitle(
                        text = "Statistik Beban & Volume",
                        fontSize = 20.sp,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tren total volume angkatan 30 hari terakhir",
                        color = SecondaryText,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Summary metric cards
        item(key = "stats_summary_cards", contentType = "stats_summary_cards") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
            // Card 1: Total Volume (30D)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .guildPlaque(16.dp)
                    .padding(vertical = 14.dp, horizontal = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TOTAL VOLUME",
                    color = SecondaryText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                val formattedTotal = if (totalVolume30Days >= 1000f) {
                    "%.1fk".format(totalVolume30Days / 1000f)
                } else {
                    "${totalVolume30Days.toInt()}"
                }
                Text(
                    text = "${formattedTotal} kg",
                    color = UpperBodyAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // Card 2: Average Per Session
            Column(
                modifier = Modifier
                    .weight(1f)
                    .guildPlaque(16.dp)
                    .padding(vertical = 14.dp, horizontal = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "RATA-RATA / SESI",
                    color = SecondaryText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                val formattedAvg = if (averageVolumePerSession >= 1000f) {
                    "%.1fk".format(averageVolumePerSession / 1000f)
                } else {
                    "${averageVolumePerSession.toInt()}"
                }
                Text(
                    text = "${formattedAvg} kg",
                    color = LowerBodyAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // Card 3: Best Session
            Column(
                modifier = Modifier
                    .weight(1f)
                    .guildPlaque(16.dp)
                    .padding(vertical = 14.dp, horizontal = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SESI TERTINGGI",
                    color = SecondaryText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                val best = bestSessionVolume
                val formattedBest = if (best >= 1000f) {
                    "%.1fk".format(best / 1000f)
                } else {
                    "${best.toInt()}"
                }
                Text(
                    text = "${formattedBest} kg",
                    color = SuccessColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                }
            }
        }

        // Line Chart Card
        item(key = "stats_volume_chart_card", contentType = "stats_volume_chart_card") {
            Column(
            modifier = Modifier
                .fillMaxWidth()
                .guildPlaque(16.dp)
                .padding(16.dp)
                .testTag("stats_volume_chart_card")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    GuildTitle(
                        text = "TREN VOLUME (30 HARI)",
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Formula: Beban (kg) × Reps untuk tiap set yang terselesaikan",
                        color = SecondaryText,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            VolumeTrendChart(
                dataPoints = dailyPoints
            )
        }
        }

        // Educational Insight Card
        item(key = "stats_insight_card", contentType = "stats_insight_card") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .guildPlaque(16.dp)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = UpperBodyAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    GuildTitle(
                        text = "PRINSIP VOLUME BULKING",
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Kenaikan total volume secara bertahap (progressive overload) adalah faktor utama hipertrofi otot. Usahakan grafik volume menunjukkan tren meningkat seiring waktu.",
                    color = SecondaryText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

/**
 * Calculates total volume for a session (sum of weightKg * reps for all completed/filled sets)
 */
fun calculateSessionVolume(exercisesJson: String): Float {
    return try {
        val rootArray = JSONArray(exercisesJson)
        var totalVolume = 0f
        for (i in 0 until rootArray.length()) {
            val exObj = rootArray.getJSONObject(i)
            val setsArray = exObj.optJSONArray("sets") ?: continue
            for (j in 0 until setsArray.length()) {
                val setObj = setsArray.getJSONObject(j)
                val weightStr = setObj.optString("weightKg", "")
                val repsStr = setObj.optString("reps", "")
                val isDone = setObj.optBoolean("isDone", false)
                val w = weightStr.toFloatOrNull()
                val r = repsStr.toIntOrNull()
                if (w != null && r != null && w > 0f && r > 0) {
                    if (isDone || (w > 0f && r > 0)) {
                        totalVolume += (w * r)
                    }
                }
            }
        }
        totalVolume
    } catch (_: Exception) {
        0f
    }
}
