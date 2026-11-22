package com.example.data.model

import androidx.compose.runtime.Immutable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sqrt
import org.json.JSONArray

@Immutable
data class XpEventItem(
    val date: String,
    val xpEarned: Int,
    val breakdown: String
)

enum class AchievementTier(val label: String) {
    DASAR("Dasar"),
    MENENGAH("Menengah"),
    TANTANGAN("Tantangan"),
    LEGENDARIS("Legendaris")
}

@Immutable
data class AchievementStatus(
    val id: String,
    val name: String,
    val description: String,
    val isUnlocked: Boolean,
    val unlockedDate: String? = null,
    val tier: AchievementTier = AchievementTier.DASAR,
    val currentProgress: Int = if (isUnlocked) 1 else 0,
    val targetProgress: Int = 1,
    val progressText: String = ""
)

@Immutable
data class WeeklyQuestProgress(
    val sessionsCompleted: Int,
    val sessionsTarget: Int = 4,
    val weighInCompleted: Int,
    val weighInTarget: Int = 1,
    val prCompleted: Int,
    val prTarget: Int = 1
)

@Immutable
data class GamificationCounts(
    val totalSessions: Int = 0,
    val totalCountedSets: Int = 0,
    val totalPersonalRecords: Int = 0,
    val distinctWeighInWeeks: Int = 0,
    val totalMeasurements: Int = 0,
    val totalVolumeKg: Float = 0f,
    val totalPullUpReps: Int = 0
)

@Immutable
data class GamificationState(
    val totalXp: Int = 0,
    val level: Int = 1,
    val rankInfo: RankInfo = RankSystem.rankFor(0),
    val rankRoad: List<RankRoadItem> = RankSystem.buildRankRoad(0),
    val recentXpEvents: List<XpEventItem> = emptyList(),
    val achievements: List<AchievementStatus> = emptyList(),
    val weeklyQuests: WeeklyQuestProgress = WeeklyQuestProgress(
        sessionsCompleted = 0,
        sessionsTarget = 4,
        weighInCompleted = 0,
        weighInTarget = 1,
        prCompleted = 0,
        prTarget = 1
    ),
    val counts: GamificationCounts = GamificationCounts()
)

@Immutable
data class WorkoutXpReward(
    val xpEarned: Int,
    val breakdownItems: List<String>,
    val oldRankInfo: RankInfo,
    val newRankInfo: RankInfo,
    val tierChanged: Boolean,
    val rankLetterChanged: Boolean,
    val newTotalXp: Int,
    val newLevel: Int
)

object GamificationEngine {
    // Tuneable XP & cap constants
    const val XP_PER_COMPLETED_SET = 10
    const val MAX_COUNTED_SETS_PER_DATE = 30
    const val XP_SESSION_BONUS = 50
    const val MIN_SETS_FOR_SESSION_BONUS = 12
    const val XP_WEEKLY_3_SESSIONS = 60
    const val XP_WEEKLY_4_PLUS_SESSIONS = 100
    const val XP_PERSONAL_RECORD = 30
    const val MAX_RECORD_BONUSES_PER_DATE = 3
    const val XP_WEIGH_IN_WEEKLY = 20
    const val XP_MEASUREMENT_MONTHLY = 30
    const val LEVEL_XP_DIVISOR = 40.0

    const val PULL_UP_EXERCISE_NAME = "Pull-up / Lat Pulldown"
    const val VOLUME_MILESTONE_KG = 10_000f
    const val ELITE_VOLUME_MILESTONE_KG = 100_000f
    const val PULL_UP_REPS_MILESTONE = 100
    const val SETS_1500_MILESTONE = 1_500
    const val PR_75_MILESTONE = 75
    const val WEIGH_IN_16_WEEKS_MILESTONE = 16
    const val MEASUREMENT_6_MONTHS_MILESTONE = 6

    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val EXERCISE_INPUT_TYPES: Map<String, ExerciseInputType> by lazy {
        (WorkoutProgram.upperBodyExercises + WorkoutProgram.lowerBodyExercises)
            .associate { it.name to it.inputType }
    }

    private data class ExerciseBest(
        val inputType: ExerciseInputType,
        val weightKg: Float,
        val reps: Int
    ) {
        fun beats(other: ExerciseBest): Boolean {
            return when (inputType) {
                ExerciseInputType.WEIGHT_REPS -> {
                    val myScore = weightKg * reps
                    val otherScore = other.weightKg * other.reps
                    myScore > otherScore + 0.001f
                }
                ExerciseInputType.BODYWEIGHT_REPS -> {
                    if (weightKg > other.weightKg + 0.001f) {
                        true
                    } else if (abs(weightKg - other.weightKg) <= 0.001f) {
                        reps > other.reps
                    } else {
                        false
                    }
                }
                ExerciseInputType.DURATION_ONLY -> reps > other.reps
            }
        }
    }

    private data class DateXpAccumulator(
        var countedSets: Int = 0,
        var hasSessionBonus: Boolean = false,
        var weeklyBonusXp: Int = 0,
        var prCount: Int = 0,
        var hasWeighInBonus: Boolean = false,
        var hasMeasurementBonus: Boolean = false
    )

    private data class InternalComputeResult(
        val state: GamificationState,
        val dateAccumulators: Map<LocalDate, DateXpAccumulator>,
        val weekBonusByWeek: Map<LocalDate, Int>
    )

    fun emptyState(): GamificationState {
        return compute(
            sessions = emptyList(),
            weightLogs = emptyList(),
            measurements = emptyList(),
            today = LocalDate.now()
        )
    }

    fun compute(
        sessions: List<WorkoutSession>,
        weightLogs: List<WeightLog>,
        measurements: List<BodyMeasurement>,
        today: LocalDate = LocalDate.now()
    ): GamificationState {
        return computeInternal(sessions, weightLogs, measurements, today).state
    }

    fun computePostWorkoutReward(
        beforeSessions: List<WorkoutSession>,
        newSession: WorkoutSession,
        weightLogs: List<WeightLog>,
        measurements: List<BodyMeasurement>,
        today: LocalDate = LocalDate.now()
    ): WorkoutXpReward {
        val before = computeInternal(beforeSessions, weightLogs, measurements, today)
        val after = computeInternal(beforeSessions + newSession, weightLogs, measurements, today)

        val xpEarned = (after.state.totalXp - before.state.totalXp).coerceAtLeast(0)
        val sessionDate = parseDate(newSession.date) ?: today
        val weekStart = sessionDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        val beforeDay = before.dateAccumulators[sessionDate] ?: DateXpAccumulator()
        val afterDay = after.dateAccumulators[sessionDate] ?: DateXpAccumulator()

        val breakdown = mutableListOf<String>()
        val deltaSets = (afterDay.countedSets - beforeDay.countedSets).coerceAtLeast(0)
        if (deltaSets > 0) {
            breakdown.add("+${deltaSets * XP_PER_COMPLETED_SET} XP ($deltaSets set tuntas)")
        }
        if (!beforeDay.hasSessionBonus && afterDay.hasSessionBonus) {
            breakdown.add("+$XP_SESSION_BONUS XP (Bonus misi tuntas)")
        }
        val beforeWeekXp = before.weekBonusByWeek[weekStart] ?: 0
        val afterWeekXp = after.weekBonusByWeek[weekStart] ?: 0
        val deltaWeekXp = (afterWeekXp - beforeWeekXp).coerceAtLeast(0)
        if (deltaWeekXp > 0) {
            breakdown.add("+$deltaWeekXp XP (Bonus konsistensi pekan)")
        }
        val deltaPr = (afterDay.prCount - beforeDay.prCount).coerceAtLeast(0)
        if (deltaPr > 0) {
            breakdown.add("+${deltaPr * XP_PERSONAL_RECORD} XP ($deltaPr rekor pribadi)")
        }
        if (breakdown.isEmpty()) {
            breakdown.add("Misi tersimpan (batas XP harian sudah penuh)")
        }

        val oldInfo = before.state.rankInfo
        val newInfo = after.state.rankInfo
        val rankLetterChanged = newInfo.rankIndex > oldInfo.rankIndex
        val tierChanged = rankLetterChanged ||
            (newInfo.rankIndex == oldInfo.rankIndex &&
                (newInfo.tierIndex > oldInfo.tierIndex || newInfo.monarchLevel > oldInfo.monarchLevel))

        return WorkoutXpReward(
            xpEarned = xpEarned,
            breakdownItems = breakdown,
            oldRankInfo = oldInfo,
            newRankInfo = newInfo,
            tierChanged = tierChanged,
            rankLetterChanged = rankLetterChanged,
            newTotalXp = after.state.totalXp,
            newLevel = after.state.level
        )
    }

    private fun computeInternal(
        sessions: List<WorkoutSession>,
        weightLogs: List<WeightLog>,
        measurements: List<BodyMeasurement>,
        today: LocalDate
    ): InternalComputeResult {
        val sortedSessions = sessions
            .mapNotNull { s -> parseDate(s.date)?.let { d -> Triple(s, d, s.timestamp) } }
            .sortedWith(compareBy<Triple<WorkoutSession, LocalDate, Long>> { it.second }.thenBy { it.third }.thenBy { it.first.id })

        val sortedWeights = weightLogs
            .mapNotNull { w -> parseDate(w.date)?.let { d -> Triple(w, d, w.timestamp) } }
            .sortedWith(compareBy<Triple<WeightLog, LocalDate, Long>> { it.second }.thenBy { it.third }.thenBy { it.first.id })

        val sortedMeasurements = measurements
            .mapNotNull { m -> parseDate(m.date)?.let { d -> Triple(m, d, m.timestamp) } }
            .sortedWith(compareBy<Triple<BodyMeasurement, LocalDate, Long>> { it.second }.thenBy { it.third }.thenBy { it.first.id })

        val dateMap = sortedMapOf<LocalDate, DateXpAccumulator>()
        fun accFor(date: LocalDate): DateXpAccumulator = dateMap.getOrPut(date) { DateXpAccumulator() }

        val bestByExercise = mutableMapOf<String, ExerciseBest>()
        val prExercisesByDate = mutableMapOf<LocalDate, MutableSet<String>>()
        val qualifyingDatesByWeek = mutableMapOf<LocalDate, MutableSet<LocalDate>>()
        val weekBonusByWeek = mutableMapOf<LocalDate, Int>()
        val sessionsByWeek = mutableMapOf<LocalDate, Int>()
        val prCountByWeek = mutableMapOf<LocalDate, Int>()

        var totalCountedSets = 0
        var totalPersonalRecords = 0
        var totalVolumeKg = 0f
        var totalPullUpReps = 0

        var firstPrDate: LocalDate? = null
        var twentyFifthPrDate: LocalDate? = null
        var seventyFifthPrDate: LocalDate? = null
        var perfectWeekDate: LocalDate? = null
        var fourWeekChainDate: LocalDate? = null
        var eightWeekChainDate: LocalDate? = null
        var twelveWeekChainDate: LocalDate? = null
        var fourPerfectWeeksDate: LocalDate? = null
        var thousandKgDate: LocalDate? = null
        var hundredThousandKgDate: LocalDate? = null
        var pullUp100Date: LocalDate? = null
        var sets1500Date: LocalDate? = null

        var bestThreeSessionStreak = 0
        var bestFourSessionStreak = 0
        var maxSingleWeekSessions = 0

        for ((session, date, _) in sortedSessions) {
            val weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val weekSessionsCount = (sessionsByWeek[weekStart] ?: 0) + 1
            sessionsByWeek[weekStart] = weekSessionsCount
            if (weekSessionsCount > maxSingleWeekSessions) {
                maxSingleWeekSessions = weekSessionsCount
            }

            if (weekSessionsCount == 3) {
                var streak3 = 1
                while ((sessionsByWeek[weekStart.minusWeeks(streak3.toLong())] ?: 0) >= 3) {
                    streak3++
                }
                if (streak3 > bestThreeSessionStreak) {
                    bestThreeSessionStreak = streak3
                }
                if (streak3 >= 4 && fourWeekChainDate == null) {
                    fourWeekChainDate = date
                }
                if (streak3 >= 8 && eightWeekChainDate == null) {
                    eightWeekChainDate = date
                }
                if (streak3 >= 12 && twelveWeekChainDate == null) {
                    twelveWeekChainDate = date
                }
            }
            if (weekSessionsCount == 4) {
                if (perfectWeekDate == null) {
                    perfectWeekDate = date
                }
                var streak4 = 1
                while ((sessionsByWeek[weekStart.minusWeeks(streak4.toLong())] ?: 0) >= 4) {
                    streak4++
                }
                if (streak4 > bestFourSessionStreak) {
                    bestFourSessionStreak = streak4
                }
                if (streak4 >= 4 && fourPerfectWeeksDate == null) {
                    fourPerfectWeeksDate = date
                }
            }

            val dayAcc = accFor(date)
            val prExercisesToday = prExercisesByDate.getOrPut(date) { mutableSetOf() }
            val sessionNewBests = mutableMapOf<String, ExerciseBest>()

            try {
                val rootArray = JSONArray(session.exercisesJson)
                for (i in 0 until rootArray.length()) {
                    val exObj = rootArray.getJSONObject(i)
                    val exName = exObj.optString("name", "").trim()
                    if (exName.isEmpty()) continue

                    val inputType = resolveInputType(exName)
                    val setsArray = exObj.optJSONArray("sets") ?: continue

                    for (j in 0 until setsArray.length()) {
                        val setObj = setsArray.getJSONObject(j)
                        val isDone = setObj.optBoolean("isDone", false)
                        val reps = setObj.optString("reps", "").trim().toIntOrNull() ?: 0
                        if (!isDone || reps <= 0) continue

                        if (dayAcc.countedSets >= MAX_COUNTED_SETS_PER_DATE) {
                            continue
                        }

                        dayAcc.countedSets++
                        totalCountedSets++
                        if (sets1500Date == null && totalCountedSets >= SETS_1500_MILESTONE) {
                            sets1500Date = date
                        }

                        val rawWeight = setObj.optString("weightKg", "").trim().toFloatOrNull() ?: 0f
                        val weightKg = if (rawWeight > 0f) rawWeight else 0f

                        if (inputType != ExerciseInputType.DURATION_ONLY && weightKg > 0f) {
                            totalVolumeKg += weightKg * reps
                            if (thousandKgDate == null && totalVolumeKg >= VOLUME_MILESTONE_KG) {
                                thousandKgDate = date
                            }
                            if (hundredThousandKgDate == null && totalVolumeKg >= ELITE_VOLUME_MILESTONE_KG) {
                                hundredThousandKgDate = date
                            }
                        }

                        if (exName == PULL_UP_EXERCISE_NAME) {
                            totalPullUpReps += reps
                            if (pullUp100Date == null && totalPullUpReps >= PULL_UP_REPS_MILESTONE) {
                                pullUp100Date = date
                            }
                        }

                        val isEligibleForPr = when (inputType) {
                            ExerciseInputType.WEIGHT_REPS -> weightKg > 0f && reps > 0
                            ExerciseInputType.BODYWEIGHT_REPS -> reps > 0
                            ExerciseInputType.DURATION_ONLY -> reps > 0
                        }

                        if (isEligibleForPr) {
                            val candidate = ExerciseBest(inputType = inputType, weightKg = weightKg, reps = reps)
                            val existingSessionBest = sessionNewBests[exName]
                            if (existingSessionBest == null || candidate.beats(existingSessionBest)) {
                                sessionNewBests[exName] = candidate
                            }

                            val prevBest = bestByExercise[exName]
                            if (prevBest != null && candidate.beats(prevBest)) {
                                if (dayAcc.prCount < MAX_RECORD_BONUSES_PER_DATE && !prExercisesToday.contains(exName)) {
                                    prExercisesToday.add(exName)
                                    dayAcc.prCount++
                                    totalPersonalRecords++
                                    prCountByWeek[weekStart] = (prCountByWeek[weekStart] ?: 0) + 1
                                    if (firstPrDate == null) firstPrDate = date
                                    if (totalPersonalRecords == 25 && twentyFifthPrDate == null) {
                                        twentyFifthPrDate = date
                                    }
                                    if (totalPersonalRecords == PR_75_MILESTONE && seventyFifthPrDate == null) {
                                        seventyFifthPrDate = date
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Skip malformed JSON safely
            }

            // Commit bests after evaluating this session chronologically
            for ((exName, sessionBest) in sessionNewBests) {
                val existing = bestByExercise[exName]
                if (existing == null || sessionBest.beats(existing)) {
                    bestByExercise[exName] = sessionBest
                }
            }

            // Session bonus & weekly consistency bonus check
            if (!dayAcc.hasSessionBonus && dayAcc.countedSets >= MIN_SETS_FOR_SESSION_BONUS) {
                dayAcc.hasSessionBonus = true
                val qualDates = qualifyingDatesByWeek.getOrPut(weekStart) { mutableSetOf() }
                qualDates.add(date)
                val targetWeekBonus = when {
                    qualDates.size >= 4 -> XP_WEEKLY_4_PLUS_SESSIONS
                    qualDates.size == 3 -> XP_WEEKLY_3_SESSIONS
                    else -> 0
                }
                val alreadyAwardedWeekBonus = weekBonusByWeek[weekStart] ?: 0
                if (targetWeekBonus > alreadyAwardedWeekBonus) {
                    val delta = targetWeekBonus - alreadyAwardedWeekBonus
                    dayAcc.weeklyBonusXp += delta
                    weekBonusByWeek[weekStart] = targetWeekBonus
                }
            }
        }

        // Weigh-in XP: +20 XP for first weight log per calendar week
        val weighInWeeksSeen = mutableSetOf<LocalDate>()
        var fourthWeighInDate: LocalDate? = null
        var sixteenthWeighInDate: LocalDate? = null
        for ((_, date, _) in sortedWeights) {
            val weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            if (weighInWeeksSeen.add(weekStart)) {
                accFor(date).hasWeighInBonus = true
                if (weighInWeeksSeen.size == 4 && fourthWeighInDate == null) {
                    fourthWeighInDate = date
                }
                if (weighInWeeksSeen.size == WEIGH_IN_16_WEEKS_MILESTONE && sixteenthWeighInDate == null) {
                    sixteenthWeighInDate = date
                }
            }
        }

        // Measurement XP: +30 XP for first measurement per calendar month
        val measurementMonthsSeen = mutableSetOf<YearMonth>()
        var sixthMeasurementMonthDate: LocalDate? = null
        for ((_, date, _) in sortedMeasurements) {
            val ym = YearMonth.from(date)
            if (measurementMonthsSeen.add(ym)) {
                accFor(date).hasMeasurementBonus = true
                if (measurementMonthsSeen.size == MEASUREMENT_6_MONTHS_MILESTONE && sixthMeasurementMonthDate == null) {
                    sixthMeasurementMonthDate = date
                }
            }
        }

        // Aggregate total XP & recent XP events
        var totalXp = 0
        val allEventsDesc = mutableListOf<XpEventItem>()
        for (date in dateMap.keys.sortedDescending()) {
            val acc = dateMap[date] ?: continue
            val dayXp = (acc.countedSets * XP_PER_COMPLETED_SET) +
                (if (acc.hasSessionBonus) XP_SESSION_BONUS else 0) +
                acc.weeklyBonusXp +
                (acc.prCount * XP_PERSONAL_RECORD) +
                (if (acc.hasWeighInBonus) XP_WEIGH_IN_WEEKLY else 0) +
                (if (acc.hasMeasurementBonus) XP_MEASUREMENT_MONTHLY else 0)

            if (dayXp > 0) {
                totalXp += dayXp
                allEventsDesc.add(
                    XpEventItem(
                        date = date.format(DATE_FORMATTER),
                        xpEarned = dayXp,
                        breakdown = formatDayBreakdown(acc)
                    )
                )
            }
        }

        val level = 1 + floor(sqrt(totalXp / LEVEL_XP_DIVISOR)).toInt()
        val rankInfo = RankSystem.rankFor(totalXp)
        val rankRoad = RankSystem.buildRankRoad(totalXp, rankInfo)

        val currentWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weeklyQuests = WeeklyQuestProgress(
            sessionsCompleted = (sessionsByWeek[currentWeekStart] ?: 0).coerceAtMost(4),
            sessionsTarget = 4,
            weighInCompleted = if (weighInWeeksSeen.contains(currentWeekStart)) 1 else 0,
            weighInTarget = 1,
            prCompleted = if ((prCountByWeek[currentWeekStart] ?: 0) >= 1) 1 else 0,
            prTarget = 1
        )

        val totalSessions = sortedSessions.size
        val firstSessionDate = sortedSessions.getOrNull(0)?.second
        val tenthSessionDate = sortedSessions.getOrNull(9)?.second
        val fiftiethSessionDate = sortedSessions.getOrNull(49)?.second
        val hundredthSessionDate = sortedSessions.getOrNull(99)?.second
        val twoHundredthSessionDate = sortedSessions.getOrNull(199)?.second
        val firstMeasurementDate = sortedMeasurements.firstOrNull()?.second

        val volumeInt = totalVolumeKg.toInt().coerceAtLeast(0)
        val achievements = listOf(
            // --- DASAR (6) ---
            AchievementStatus(
                id = "langkah_pertama",
                name = "Langkah Pertama",
                description = "Selesaikan 1 sesi latihan",
                isUnlocked = firstSessionDate != null,
                unlockedDate = firstSessionDate?.format(DATE_FORMATTER),
                tier = AchievementTier.DASAR,
                currentProgress = totalSessions.coerceAtMost(1),
                targetProgress = 1,
                progressText = "${totalSessions.coerceAtMost(1)} / 1 sesi"
            ),
            AchievementStatus(
                id = "pekan_sempurna",
                name = "Pekan Sempurna",
                description = "Selesaikan 4 sesi atau lebih dalam satu pekan",
                isUnlocked = perfectWeekDate != null,
                unlockedDate = perfectWeekDate?.format(DATE_FORMATTER),
                tier = AchievementTier.DASAR,
                currentProgress = if (perfectWeekDate != null) 4 else maxSingleWeekSessions.coerceAtMost(4),
                targetProgress = 4,
                progressText = "${if (perfectWeekDate != null) 4 else maxSingleWeekSessions.coerceAtMost(4)} / 4 sesi"
            ),
            AchievementStatus(
                id = "pemecah_rekor",
                name = "Pemecah Rekor",
                description = "Cetak rekor pribadi pertamamu",
                isUnlocked = firstPrDate != null,
                unlockedDate = firstPrDate?.format(DATE_FORMATTER),
                tier = AchievementTier.DASAR,
                currentProgress = totalPersonalRecords.coerceAtMost(1),
                targetProgress = 1,
                progressText = "${totalPersonalRecords.coerceAtMost(1)} / 1 rekor"
            ),
            AchievementStatus(
                id = "penimbang_setia",
                name = "Penimbang Setia",
                description = "Timbang badan di 4 pekan berbeda",
                isUnlocked = fourthWeighInDate != null,
                unlockedDate = fourthWeighInDate?.format(DATE_FORMATTER),
                tier = AchievementTier.DASAR,
                currentProgress = weighInWeeksSeen.size.coerceAtMost(4),
                targetProgress = 4,
                progressText = "${weighInWeeksSeen.size.coerceAtMost(4)} / 4 pekan"
            ),
            AchievementStatus(
                id = "pemahat_tubuh",
                name = "Pemahat Tubuh",
                description = "Catat pengukuran tubuh pertamamu",
                isUnlocked = firstMeasurementDate != null,
                unlockedDate = firstMeasurementDate?.format(DATE_FORMATTER),
                tier = AchievementTier.DASAR,
                currentProgress = sortedMeasurements.size.coerceAtMost(1),
                targetProgress = 1,
                progressText = "${sortedMeasurements.size.coerceAtMost(1)} / 1 ukur"
            ),
            AchievementStatus(
                id = "beban_ribuan",
                name = "Beban Ribuan",
                description = "Capai total volume angkatan 10.000 kg",
                isUnlocked = thousandKgDate != null,
                unlockedDate = thousandKgDate?.format(DATE_FORMATTER),
                tier = AchievementTier.DASAR,
                currentProgress = volumeInt.coerceAtMost(10_000),
                targetProgress = 10_000,
                progressText = "${RankSystem.formatXp(volumeInt.coerceAtMost(10_000))} / 10.000 kg"
            ),

            // --- MENENGAH (5) ---
            AchievementStatus(
                id = "sepuluh_sesi",
                name = "Sepuluh Sesi",
                description = "Selesaikan 10 sesi latihan",
                isUnlocked = tenthSessionDate != null,
                unlockedDate = tenthSessionDate?.format(DATE_FORMATTER),
                tier = AchievementTier.MENENGAH,
                currentProgress = totalSessions.coerceAtMost(10),
                targetProgress = 10,
                progressText = "${totalSessions.coerceAtMost(10)} / 10 sesi"
            ),
            AchievementStatus(
                id = "lima_puluh_sesi",
                name = "Lima Puluh Sesi",
                description = "Selesaikan 50 sesi latihan",
                isUnlocked = fiftiethSessionDate != null,
                unlockedDate = fiftiethSessionDate?.format(DATE_FORMATTER),
                tier = AchievementTier.MENENGAH,
                currentProgress = totalSessions.coerceAtMost(50),
                targetProgress = 50,
                progressText = "${totalSessions.coerceAtMost(50)} / 50 sesi"
            ),
            AchievementStatus(
                id = "rantai_4_pekan",
                name = "Rantai 4 Pekan",
                description = "Minimal 3 sesi per pekan selama 4 pekan beruntun",
                isUnlocked = fourWeekChainDate != null,
                unlockedDate = fourWeekChainDate?.format(DATE_FORMATTER),
                tier = AchievementTier.MENENGAH,
                currentProgress = bestThreeSessionStreak.coerceAtMost(4),
                targetProgress = 4,
                progressText = "${bestThreeSessionStreak.coerceAtMost(4)} / 4 pekan"
            ),
            AchievementStatus(
                id = "kolektor_rekor",
                name = "Kolektor Rekor",
                description = "Cetak total 25 rekor pribadi",
                isUnlocked = twentyFifthPrDate != null,
                unlockedDate = twentyFifthPrDate?.format(DATE_FORMATTER),
                tier = AchievementTier.MENENGAH,
                currentProgress = totalPersonalRecords.coerceAtMost(25),
                targetProgress = 25,
                progressText = "${totalPersonalRecords.coerceAtMost(25)} / 25 rekor"
            ),
            AchievementStatus(
                id = "penarik_sejati",
                name = "Penarik Sejati",
                description = "Capai 100 repetisi Pull-up / Lat Pulldown",
                isUnlocked = pullUp100Date != null,
                unlockedDate = pullUp100Date?.format(DATE_FORMATTER),
                tier = AchievementTier.MENENGAH,
                currentProgress = totalPullUpReps.coerceAtMost(100),
                targetProgress = 100,
                progressText = "${totalPullUpReps.coerceAtMost(100)} / 100 rep"
            ),

            // --- TANTANGAN (6) ---
            AchievementStatus(
                id = "rantai_8_pekan",
                name = "Rantai 8 Pekan",
                description = "Minimal 3 sesi per pekan selama 8 pekan beruntun",
                isUnlocked = eightWeekChainDate != null,
                unlockedDate = eightWeekChainDate?.format(DATE_FORMATTER),
                tier = AchievementTier.TANTANGAN,
                currentProgress = bestThreeSessionStreak.coerceAtMost(8),
                targetProgress = 8,
                progressText = "${bestThreeSessionStreak.coerceAtMost(8)} / 8 pekan"
            ),
            AchievementStatus(
                id = "bulan_disiplin",
                name = "Empat Pekan Sempurna",
                description = "Minimal 4 sesi per pekan selama 4 pekan beruntun",
                isUnlocked = fourPerfectWeeksDate != null,
                unlockedDate = fourPerfectWeeksDate?.format(DATE_FORMATTER),
                tier = AchievementTier.TANTANGAN,
                currentProgress = bestFourSessionStreak.coerceAtMost(4),
                targetProgress = 4,
                progressText = "${bestFourSessionStreak.coerceAtMost(4)} / 4 pekan"
            ),
            AchievementStatus(
                id = "seratus_sesi",
                name = "Ksatria 100 Sesi",
                description = "Selesaikan total 100 sesi latihan",
                isUnlocked = hundredthSessionDate != null,
                unlockedDate = hundredthSessionDate?.format(DATE_FORMATTER),
                tier = AchievementTier.TANTANGAN,
                currentProgress = totalSessions.coerceAtMost(100),
                targetProgress = 100,
                progressText = "${totalSessions.coerceAtMost(100)} / 100 sesi"
            ),
            AchievementStatus(
                id = "seribu_set",
                name = "Penempa 1.500 Set",
                description = "Tuntaskan total 1.500 set latihan terhitung",
                isUnlocked = sets1500Date != null,
                unlockedDate = sets1500Date?.format(DATE_FORMATTER),
                tier = AchievementTier.TANTANGAN,
                currentProgress = totalCountedSets.coerceAtMost(SETS_1500_MILESTONE),
                targetProgress = SETS_1500_MILESTONE,
                progressText = "${RankSystem.formatXp(totalCountedSets.coerceAtMost(SETS_1500_MILESTONE))} / 1.500 set"
            ),
            AchievementStatus(
                id = "penimbang_tahunan",
                name = "Penimbang 16 Pekan",
                description = "Timbang badan di 16 pekan berbeda",
                isUnlocked = sixteenthWeighInDate != null,
                unlockedDate = sixteenthWeighInDate?.format(DATE_FORMATTER),
                tier = AchievementTier.TANTANGAN,
                currentProgress = weighInWeeksSeen.size.coerceAtMost(WEIGH_IN_16_WEEKS_MILESTONE),
                targetProgress = WEIGH_IN_16_WEEKS_MILESTONE,
                progressText = "${weighInWeeksSeen.size.coerceAtMost(WEIGH_IN_16_WEEKS_MILESTONE)} / 16 pekan"
            ),
            AchievementStatus(
                id = "arsitek_v_taper",
                name = "Arsitek Enam Bulan",
                description = "Catat pengukuran tubuh di 6 bulan berbeda",
                isUnlocked = sixthMeasurementMonthDate != null,
                unlockedDate = sixthMeasurementMonthDate?.format(DATE_FORMATTER),
                tier = AchievementTier.TANTANGAN,
                currentProgress = measurementMonthsSeen.size.coerceAtMost(MEASUREMENT_6_MONTHS_MILESTONE),
                targetProgress = MEASUREMENT_6_MONTHS_MILESTONE,
                progressText = "${measurementMonthsSeen.size.coerceAtMost(MEASUREMENT_6_MONTHS_MILESTONE)} / 6 bulan"
            ),

            // --- LEGENDARIS (4) ---
            AchievementStatus(
                id = "rantai_12_pekan",
                name = "Penjaga 12 Pekan",
                description = "Minimal 3 sesi per pekan selama 12 pekan beruntun",
                isUnlocked = twelveWeekChainDate != null,
                unlockedDate = twelveWeekChainDate?.format(DATE_FORMATTER),
                tier = AchievementTier.LEGENDARIS,
                currentProgress = bestThreeSessionStreak.coerceAtMost(12),
                targetProgress = 12,
                progressText = "${bestThreeSessionStreak.coerceAtMost(12)} / 12 pekan"
            ),
            AchievementStatus(
                id = "dua_ratus_sesi",
                name = "Veteran 200 Sesi",
                description = "Selesaikan total 200 sesi latihan",
                isUnlocked = twoHundredthSessionDate != null,
                unlockedDate = twoHundredthSessionDate?.format(DATE_FORMATTER),
                tier = AchievementTier.LEGENDARIS,
                currentProgress = totalSessions.coerceAtMost(200),
                targetProgress = 200,
                progressText = "${totalSessions.coerceAtMost(200)} / 200 sesi"
            ),
            AchievementStatus(
                id = "penguasa_volume",
                name = "Beban Ratusan Ton",
                description = "Capai total volume angkatan 100.000 kg",
                isUnlocked = hundredThousandKgDate != null,
                unlockedDate = hundredThousandKgDate?.format(DATE_FORMATTER),
                tier = AchievementTier.LEGENDARIS,
                currentProgress = volumeInt.coerceAtMost(100_000),
                targetProgress = 100_000,
                progressText = "${RankSystem.formatXp(volumeInt.coerceAtMost(100_000))} / 100.000 kg"
            ),
            AchievementStatus(
                id = "legenda_rekor",
                name = "Pemburu 75 Rekor",
                description = "Cetak total 75 rekor pribadi",
                isUnlocked = seventyFifthPrDate != null,
                unlockedDate = seventyFifthPrDate?.format(DATE_FORMATTER),
                tier = AchievementTier.LEGENDARIS,
                currentProgress = totalPersonalRecords.coerceAtMost(PR_75_MILESTONE),
                targetProgress = PR_75_MILESTONE,
                progressText = "${totalPersonalRecords.coerceAtMost(PR_75_MILESTONE)} / 75 rekor"
            )
        )

        val state = GamificationState(
            totalXp = totalXp,
            level = level,
            rankInfo = rankInfo,
            rankRoad = rankRoad,
            recentXpEvents = allEventsDesc.take(10),
            achievements = achievements,
            weeklyQuests = weeklyQuests,
            counts = GamificationCounts(
                totalSessions = totalSessions,
                totalCountedSets = totalCountedSets,
                totalPersonalRecords = totalPersonalRecords,
                distinctWeighInWeeks = weighInWeeksSeen.size,
                totalMeasurements = sortedMeasurements.size,
                totalVolumeKg = totalVolumeKg,
                totalPullUpReps = totalPullUpReps
            )
        )

        return InternalComputeResult(
            state = state,
            dateAccumulators = dateMap,
            weekBonusByWeek = weekBonusByWeek
        )
    }

    private fun resolveInputType(exerciseName: String): ExerciseInputType {
        return EXERCISE_INPUT_TYPES[exerciseName] ?: when {
            exerciseName.contains("Pull-up", ignoreCase = true) -> ExerciseInputType.BODYWEIGHT_REPS
            exerciseName.contains("Core", ignoreCase = true) ||
                exerciseName.contains("Plank", ignoreCase = true) -> ExerciseInputType.DURATION_ONLY
            else -> ExerciseInputType.WEIGHT_REPS
        }
    }

    private fun parseDate(dateStr: String): LocalDate? {
        if (dateStr.isBlank()) return null
        return try {
            LocalDate.parse(dateStr.trim(), DATE_FORMATTER)
        } catch (_: Exception) {
            null
        }
    }

    private fun formatDayBreakdown(acc: DateXpAccumulator): String {
        val parts = mutableListOf<String>()
        if (acc.countedSets > 0) parts.add("${acc.countedSets} set")
        if (acc.hasSessionBonus) parts.add("bonus sesi")
        if (acc.weeklyBonusXp > 0) parts.add("bonus pekan")
        if (acc.prCount > 0) parts.add("${acc.prCount} rekor")
        if (acc.hasWeighInBonus) parts.add("timbang badan")
        if (acc.hasMeasurementBonus) parts.add("ukur tubuh")
        return parts.joinToString(", ")
    }
}
