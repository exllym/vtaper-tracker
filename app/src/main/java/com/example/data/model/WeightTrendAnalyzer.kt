package com.example.data.model

import androidx.compose.runtime.Immutable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.ceil
import kotlin.math.roundToInt

// Thresholds for weekly body weight changes (kg/week)
const val THRESHOLD_TOO_SLOW = 0.1f // below 0.1 kg/week is TOO_SLOW
const val THRESHOLD_ON_TRACK_MAX = 0.35f // 0.1 to 0.35 kg/week is ON_TRACK, above 0.35 is TOO_FAST

enum class WeightTrendStatus {
    NOT_ENOUGH_DATA,
    TOO_SLOW,
    ON_TRACK,
    TOO_FAST,
    REACHED_TARGET
}

@Immutable
data class WeeklyAverage(
    val weekStartDate: String,
    val averageKg: Float
)

@Immutable
data class WeightTrendResult(
    val weeklyAverages: List<WeeklyAverage> = emptyList(),
    val averageWeeklyChangeKg: Float? = null,
    val status: WeightTrendStatus = WeightTrendStatus.NOT_ENOUGH_DATA,
    val estimatedWeeksToTarget: Int? = null,
    val adviceText: String = ""
)

object WeightTrendAnalyzer {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun analyze(
        weightLogs: List<WeightLog>,
        targetWeightKg: Float
    ): WeightTrendResult {
        if (weightLogs.isEmpty()) {
            return WeightTrendResult(
                weeklyAverages = emptyList(),
                averageWeeklyChangeKg = null,
                status = WeightTrendStatus.NOT_ENOUGH_DATA,
                estimatedWeeksToTarget = null,
                adviceText = "Butuh minimal 2 minggu data penimbangan untuk analisis."
            )
        }

        // Sort weight logs chronologically
        val sortedLogs = weightLogs.sortedWith(
            compareBy<WeightLog> { it.date }.thenBy { it.timestamp }
        )

        // Group by calendar week (Monday as start of week)
        val groupedByWeek = sortedLogs.groupBy { log ->
            try {
                val parsedDate = LocalDate.parse(log.date, dateFormatter)
                val monday = parsedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                monday.format(dateFormatter)
            } catch (_: Exception) {
                log.date
            }
        }

        val weeklyAverages = groupedByWeek.map { (weekStart, logs) ->
            val avg = logs.map { it.kg }.average().toFloat()
            val roundedAvg = (avg * 10).roundToInt() / 10.0f
            WeeklyAverage(weekStartDate = weekStart, averageKg = roundedAvg)
        }.sortedBy { it.weekStartDate }

        if (weeklyAverages.size < 2) {
            return WeightTrendResult(
                weeklyAverages = weeklyAverages,
                averageWeeklyChangeKg = null,
                status = WeightTrendStatus.NOT_ENOUGH_DATA,
                estimatedWeeksToTarget = null,
                adviceText = "Butuh minimal 2 minggu data penimbangan untuk analisis."
            )
        }

        // Most recent up to 4 weeks with data
        // Compute delta divided by the real number of calendar weeks between Mondays (minimum 1)
        val recentWeeks = weeklyAverages.takeLast(4)
        val perWeekRates = recentWeeks.zipWithNext { a, b ->
            val calendarWeeks = try {
                val aDate = LocalDate.parse(a.weekStartDate, dateFormatter)
                val bDate = LocalDate.parse(b.weekStartDate, dateFormatter)
                ChronoUnit.WEEKS.between(aDate, bDate).coerceAtLeast(1L)
            } catch (_: Exception) {
                1L
            }
            (b.averageKg - a.averageKg) / calendarWeeks.toFloat()
        }
        val rawWeeklyChange = if (perWeekRates.isNotEmpty()) perWeekRates.average().toFloat() else 0f
        val roundedWeeklyChange = (rawWeeklyChange * 100).roundToInt() / 100.0f

        val latestAvg = weeklyAverages.last().averageKg

        // Format change string for Indonesian advice text
        val changeFormatted = if (kotlin.math.abs(roundedWeeklyChange) < 0.01f) {
            "0"
        } else {
            String.format(java.util.Locale.US, "%.2f", roundedWeeklyChange).trimEnd('0').trimEnd('.')
        }

        // Check if target reached
        if (latestAvg >= targetWeightKg) {
            val targetStr = if (targetWeightKg % 1f == 0f) "${targetWeightKg.toInt()}" else "$targetWeightKg"
            return WeightTrendResult(
                weeklyAverages = weeklyAverages,
                averageWeeklyChangeKg = roundedWeeklyChange,
                status = WeightTrendStatus.REACHED_TARGET,
                estimatedWeeksToTarget = 0,
                adviceText = "Selamat! Target berat badan ${targetStr}kg sudah tercapai. Pertahankan komposisi tubuh dan fokus pada progres angkatan."
            )
        }

        val status = when {
            roundedWeeklyChange < THRESHOLD_TOO_SLOW -> WeightTrendStatus.TOO_SLOW
            roundedWeeklyChange <= THRESHOLD_ON_TRACK_MAX -> WeightTrendStatus.ON_TRACK
            else -> WeightTrendStatus.TOO_FAST
        }

        val adviceText = when (status) {
            WeightTrendStatus.TOO_SLOW -> {
                if (roundedWeeklyChange < 0f) {
                    val absChange = kotlin.math.abs(roundedWeeklyChange)
                    val absFormatted = if (absChange < 0.01f) "0" else String.format(java.util.Locale.US, "%.2f", absChange).trimEnd('0').trimEnd('.')
                    "Berat rata-rata turun $absFormatted kg/minggu. Untuk bulking, tambah asupan ±200-300 kalori/hari dan pastikan 5x makan tidak bolong."
                } else {
                    "Kenaikan rata-rata $changeFormatted kg/minggu, di bawah target. Tambah asupan ±150-200 kalori/hari dan pastikan 5x makan tidak bolong."
                }
            }
            WeightTrendStatus.ON_TRACK ->
                "Kenaikan $changeFormatted kg/minggu — sudah sesuai jalur. Pertahankan."
            WeightTrendStatus.TOO_FAST ->
                "Kenaikan $changeFormatted kg/minggu agak cepat, berisiko lebih banyak lemak. Kurangi porsi sedikit dan cek apakah beban latihan ikut naik."
            WeightTrendStatus.REACHED_TARGET, WeightTrendStatus.NOT_ENOUGH_DATA -> ""
        }

        val remainingKg = targetWeightKg - latestAvg
        val estimatedWeeks = if (remainingKg > 0f && roundedWeeklyChange > 0.01f) {
            ceil(remainingKg / roundedWeeklyChange).toInt()
        } else {
            null
        }

        return WeightTrendResult(
            weeklyAverages = weeklyAverages,
            averageWeeklyChangeKg = roundedWeeklyChange,
            status = status,
            estimatedWeeksToTarget = estimatedWeeks,
            adviceText = adviceText
        )
    }
}
