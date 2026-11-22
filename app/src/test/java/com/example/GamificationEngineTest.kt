package com.example

import com.example.data.model.BodyMeasurement
import com.example.data.model.GamificationEngine
import com.example.data.model.WeightLog
import com.example.data.model.WorkoutSession
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GamificationEngineTest {

    private fun setJson(weightKg: String, reps: String, isDone: Boolean = true): JSONObject {
        return JSONObject().apply {
            put("weightKg", weightKg)
            put("reps", reps)
            put("isDone", isDone)
        }
    }

    private fun exerciseJson(name: String, sets: List<JSONObject>): JSONObject {
        return JSONObject().apply {
            put("name", name)
            put("note", "")
            put("sets", JSONArray(sets))
        }
    }

    private fun makeSession(
        id: Int = 1,
        date: String,
        timestamp: Long = 1000L,
        dayType: String = "UPPER",
        exercises: List<JSONObject>
    ): WorkoutSession {
        return WorkoutSession(
            id = id,
            date = date,
            dayType = dayType,
            dayLabel = if (dayType == "UPPER") "Upper Body" else "Lower Body",
            timestamp = timestamp,
            exercisesJson = JSONArray(exercises).toString()
        )
    }

    private fun uniformSession(
        id: Int,
        date: String,
        setCount: Int,
        exerciseName: String = "Incline Dumbbell Press",
        weightKg: String = "20",
        reps: String = "10",
        timestamp: Long = 1000L
    ): WorkoutSession {
        val sets = (1..setCount).map { setJson(weightKg, reps, isDone = true) }
        return makeSession(
            id = id,
            date = date,
            timestamp = timestamp,
            exercises = listOf(exerciseJson(exerciseName, sets))
        )
    }

    @Test
    fun emptyData_returnsInitialRankLevelAndPositiveQuests() {
        val state = GamificationEngine.compute(
            sessions = emptyList(),
            weightLogs = emptyList(),
            measurements = emptyList(),
            today = LocalDate.of(2026, 10, 7)
        )

        assertEquals(0, state.totalXp)
        assertEquals(1, state.level)
        assertEquals("F-I", state.rankInfo.displayLabel)
        assertEquals("Rekrut", state.rankInfo.rankTitle)
        assertEquals("F-II", state.rankInfo.nextLabel)
        assertEquals(0, state.rankInfo.xpIntoTier)
        assertEquals(300, state.rankInfo.xpNeededForNextTier)
        assertEquals(0, state.weeklyQuests.sessionsCompleted)
        assertEquals(0, state.weeklyQuests.weighInCompleted)
        assertEquals(0, state.weeklyQuests.prCompleted)
        assertTrue(state.achievements.all { !it.isUnlocked })
    }

    @Test
    fun completedSets_respects30SetCapPerCalendarDate() {
        // Session 1: 20 done sets + 2 undone sets + 1 zero-rep set (only 20 valid)
        val sets1 = (1..20).map { setJson("20", "8", true) } +
            listOf(
                setJson("20", "8", false),
                setJson("20", "0", true)
            )
        // Session 2 on same date: 15 done sets -> only 10 should count (20 + 10 = 30 cap)
        val sets2 = (1..15).map { setJson("15", "10", true) }

        val sessions = listOf(
            makeSession(id = 1, date = "2026-10-05", timestamp = 1000L, exercises = listOf(exerciseJson("Incline Dumbbell Press", sets1))),
            makeSession(id = 2, date = "2026-10-05", timestamp = 2000L, exercises = listOf(exerciseJson("Lateral Raise", sets2)))
        )

        val state = GamificationEngine.compute(sessions, emptyList(), emptyList(), LocalDate.of(2026, 10, 5))

        // 30 counted sets * 10 = 300 XP, + 50 session bonus (once per date) = 350 XP
        assertEquals(30, state.counts.totalCountedSets)
        assertEquals(350, state.totalXp)
    }

    @Test
    fun sessionBonus_requiresAtLeast12CountedSetsAndGrantsOncePerDate() {
        val elevenSets = uniformSession(id = 1, date = "2026-10-05", setCount = 11)
        val state11 = GamificationEngine.compute(listOf(elevenSets), emptyList(), emptyList(), LocalDate.of(2026, 10, 5))
        assertEquals(110, state11.totalXp)

        val twelveSets = uniformSession(id = 1, date = "2026-10-05", setCount = 12)
        val secondTwelveSameDay = uniformSession(id = 2, date = "2026-10-05", setCount = 12, timestamp = 2000L)
        val state24SameDay = GamificationEngine.compute(
            listOf(twelveSets, secondTwelveSameDay),
            emptyList(),
            emptyList(),
            LocalDate.of(2026, 10, 5)
        )
        // 24 sets * 10 = 240 + 50 (only 1 session bonus per date) = 290 XP
        assertEquals(290, state24SameDay.totalXp)
    }

    @Test
    fun weeklyConsistencyBonus_nonStackingAndMondayToSundayBoundaries() {
        // Week 2026-10-05 (Mon) to 2026-10-11 (Sun)
        // Use identical weight/reps on different exercises or same weight/reps so no accidental PR XP complicates the arithmetic.
        val mon = uniformSession(id = 1, date = "2026-10-05", setCount = 12)
        val tue = uniformSession(id = 2, date = "2026-10-06", setCount = 12)
        val thu = uniformSession(id = 3, date = "2026-10-08", setCount = 12)
        val fri = uniformSession(id = 4, date = "2026-10-09", setCount = 12)
        val sun = uniformSession(id = 5, date = "2026-10-11", setCount = 12)

        // 2 qualifying dates -> 0 weekly bonus: 2 * (120 + 50) = 340
        val twoDays = GamificationEngine.compute(listOf(mon, tue), emptyList(), emptyList(), LocalDate.of(2026, 10, 11))
        assertEquals(340, twoDays.totalXp)

        // 3 qualifying dates -> +60 weekly bonus: 3 * 170 + 60 = 570
        val threeDays = GamificationEngine.compute(listOf(mon, tue, thu), emptyList(), emptyList(), LocalDate.of(2026, 10, 11))
        assertEquals(570, threeDays.totalXp)

        // 4 qualifying dates -> +100 total weekly bonus (NOT 60 + 100): 4 * 170 + 100 = 780
        val fourDays = GamificationEngine.compute(listOf(mon, tue, thu, fri), emptyList(), emptyList(), LocalDate.of(2026, 10, 11))
        assertEquals(780, fourDays.totalXp)

        // 5 qualifying dates in same week -> still +100 weekly bonus: 5 * 170 + 100 = 950
        val fiveDays = GamificationEngine.compute(listOf(mon, tue, thu, fri, sun), emptyList(), emptyList(), LocalDate.of(2026, 10, 11))
        assertEquals(950, fiveDays.totalXp)
    }

    @Test
    fun personalRecords_weightedBodyweightDurationAndDateCaps() {
        // Day 1 (2026-10-01): sets baselines for 4 exercises (no PR XP on first-ever baseline)
        val day1 = makeSession(
            id = 1,
            date = "2026-10-01",
            exercises = listOf(
                exerciseJson("Incline Dumbbell Press", listOf(setJson("20", "10"))), // score = 200
                exerciseJson("Pull-up / Lat Pulldown", listOf(setJson("5", "8"))),  // addedWeight=5, reps=8
                exerciseJson("Core (Plank/Leg Raise)", listOf(setJson("0", "45"))), // duration=45s
                exerciseJson("Lateral Raise", listOf(setJson("10", "10")))          // score = 100
            )
        )

        // Day 2 (2026-10-03):
        // - Bodyweight: addedWeight=0, reps=15 should NOT beat addedWeight=5, reps=8!
        //   Then addedWeight=5, reps=9 SHOULD beat it (1 PR).
        // - Core: duration=60 SHOULD beat 45 (2 PRs).
        // - Incline Dumbbell Press: 22x10 (beats) AND 25x10 (same exercise, same date -> max 1 PR per exercise per date!) -> counts once (3 PRs).
        // - Lateral Raise: 12x10 (beats 10x10), BUT date already reached MAX 3 PR bonuses per date -> blocked by date cap!
        val day2 = makeSession(
            id = 2,
            date = "2026-10-03",
            exercises = listOf(
                exerciseJson(
                    "Pull-up / Lat Pulldown",
                    listOf(
                        setJson("0", "15"),
                        setJson("5", "9")
                    )
                ),
                exerciseJson("Core (Plank/Leg Raise)", listOf(setJson("0", "60"))),
                exerciseJson(
                    "Incline Dumbbell Press",
                    listOf(
                        setJson("22", "10"),
                        setJson("25", "10")
                    )
                ),
                exerciseJson("Lateral Raise", listOf(setJson("12", "10")))
            )
        )

        val state = GamificationEngine.compute(listOf(day1, day2), emptyList(), emptyList(), LocalDate.of(2026, 10, 3))

        // Counted sets: 4 on day1 + 6 on day2 = 10 sets -> 100 XP
        // PRs awarded on day2: Pull-up (1), Core (1), Incline (1). Lateral Raise capped at 3/date -> 3 PRs * 30 = 90 XP
        assertEquals(3, state.counts.totalPersonalRecords)
        assertEquals(190, state.totalXp)
        assertTrue(state.achievements.first { it.id == "pemecah_rekor" }.isUnlocked)
        assertEquals("2026-10-03", state.achievements.first { it.id == "pemecah_rekor" }.unlockedDate)
    }

    @Test
    fun personalRecords_bodyweightLowerAddedWeightIgnoredEvenWithMoreReps() {
        val day1 = makeSession(
            id = 1,
            date = "2026-10-01",
            exercises = listOf(exerciseJson("Pull-up / Lat Pulldown", listOf(setJson("10", "5"))))
        )
        val day2 = makeSession(
            id = 2,
            date = "2026-10-02",
            exercises = listOf(exerciseJson("Pull-up / Lat Pulldown", listOf(setJson("5", "20"))))
        )
        val state = GamificationEngine.compute(listOf(day1, day2), emptyList(), emptyList(), LocalDate.of(2026, 10, 2))
        assertEquals(0, state.counts.totalPersonalRecords)
        assertEquals(20, state.totalXp) // 2 sets * 10
    }

    @Test
    fun weighInWeeklyCap_and_measurementMonthlyCap() {
        val weights = listOf(
            // Week 1 (2026-10-05 Mon .. 2026-10-11 Sun): two logs -> +20 XP once
            WeightLog(id = 1, date = "2026-10-05", kg = 60.0f, timestamp = 1000L),
            WeightLog(id = 2, date = "2026-10-09", kg = 60.2f, timestamp = 2000L),
            // Week 2 (2026-10-12 Mon): +20 XP
            WeightLog(id = 3, date = "2026-10-12", kg = 60.5f, timestamp = 3000L)
        )
        val measurements = listOf(
            // Oct 2026: two measurements -> +30 XP once
            BodyMeasurement(id = 1, date = "2026-10-05", shoulderCm = 110f, waistCm = 75f, timestamp = 1000L),
            BodyMeasurement(id = 2, date = "2026-10-20", shoulderCm = 111f, waistCm = 75f, timestamp = 2000L),
            // Nov 2026: +30 XP
            BodyMeasurement(id = 3, date = "2026-11-01", shoulderCm = 112f, waistCm = 75f, timestamp = 3000L)
        )

        val state = GamificationEngine.compute(emptyList(), weights, measurements, LocalDate.of(2026, 11, 1))
        // 2 weigh-in weeks * 20 = 40; 2 measurement months * 30 = 60 -> 100 XP total
        assertEquals(100, state.totalXp)
        assertEquals(2, state.counts.distinctWeighInWeeks)
        assertEquals(3, state.counts.totalMeasurements)
        assertTrue(state.achievements.first { it.id == "pemahat_tubuh" }.isUnlocked)
        assertFalse(state.achievements.first { it.id == "penimbang_setia" }.isUnlocked)
    }

    @Test
    fun deletingSession_recomputesLowerXpLevelAndAchievementsConsistently() {
        val s1 = uniformSession(id = 1, date = "2026-10-05", setCount = 12, weightKg = "50", reps = "10") // 6000 kg volume
        val s2 = uniformSession(id = 2, date = "2026-10-06", setCount = 12, weightKg = "50", reps = "10") // +6000 = 12000 kg

        val withTwo = GamificationEngine.compute(listOf(s1, s2), emptyList(), emptyList(), LocalDate.of(2026, 10, 6))
        assertEquals(340, withTwo.totalXp)
        assertTrue(withTwo.achievements.first { it.id == "beban_ribuan" }.isUnlocked)

        // Delete s2 -> XP, counts, and Beban Ribuan revert cleanly
        val afterDelete = GamificationEngine.compute(listOf(s1), emptyList(), emptyList(), LocalDate.of(2026, 10, 6))
        assertEquals(170, afterDelete.totalXp)
        assertEquals(1, afterDelete.counts.totalSessions)
        assertFalse(afterDelete.achievements.first { it.id == "beban_ribuan" }.isUnlocked)
        assertNull(afterDelete.achievements.first { it.id == "beban_ribuan" }.unlockedDate)
    }

    @Test
    fun levelFormula_and_rankThresholds() {
        // Construct enough distinct dates with 30 sets each (each date gives 300 + 50 = 350 XP)
        // 5 dates + 100 weekly bonus = 1850 XP -> Level = 1 + floor(sqrt(1850 / 40)) = 7, Rank = E-I (1500..2199)
        val fiveDays = (0 until 5).map { idx ->
            val d = LocalDate.of(2026, 1, 5).plusDays(idx.toLong()) // Week 1 Mon-Fri (+100 weekly bonus too)
            uniformSession(id = idx + 1, date = d.toString(), setCount = 30)
        }
        val state5 = GamificationEngine.compute(fiveDays, emptyList(), emptyList(), LocalDate.of(2026, 1, 9))
        assertEquals(1850, state5.totalXp)
        assertEquals(7, state5.level)
        assertEquals("E-I", state5.rankInfo.displayLabel)
        assertEquals("Pemula", state5.rankInfo.rankTitle)
        assertEquals("E-II", state5.rankInfo.nextLabel)
        assertEquals(350, state5.rankInfo.xpIntoTier)
        assertEquals(700, state5.rankInfo.xpNeededForNextTier)

        // Deleting 1 session drops XP to 1400 (< 1500 -> F-V)
        val afterDelete = GamificationEngine.compute(fiveDays.dropLast(1), emptyList(), emptyList(), LocalDate.of(2026, 1, 9))
        assertEquals(1500, afterDelete.totalXp)
        assertEquals("E-I", afterDelete.rankInfo.displayLabel)
        val afterDeleteTwo = GamificationEngine.compute(fiveDays.dropLast(2), emptyList(), emptyList(), LocalDate.of(2026, 1, 9))
        assertEquals(1110, afterDeleteTwo.totalXp)
        assertEquals("F-IV", afterDeleteTwo.rankInfo.displayLabel)
    }

    @Test
    fun achievements_fourWeekChain_pullUp100_and_penimbangSetia() {
        // 4 consecutive weeks with 3 sessions per week, plus Pull-up reps and 4 distinct weigh-in weeks
        val sessions = mutableListOf<WorkoutSession>()
        val weights = mutableListOf<WeightLog>()
        var nextId = 1
        val startMonday = LocalDate.of(2026, 8, 3)

        for (w in 0 until 4) {
            val weekMon = startMonday.plusWeeks(w.toLong())
            weights.add(WeightLog(id = w + 1, date = weekMon.toString(), kg = 55f + w * 0.2f, timestamp = (w + 1) * 1000L))
            for (d in 0 until 3) {
                val date = weekMon.plusDays(d.toLong())
                // 3 sessions * 4 weeks = 12 sessions (>= 10 sessions achievement)
                // Each session has 10 reps of Pull-up -> 12 * 10 = 120 pull-up reps (>= 100)
                sessions.add(
                    makeSession(
                        id = nextId++,
                        date = date.toString(),
                        exercises = listOf(
                            exerciseJson("Pull-up / Lat Pulldown", listOf(setJson("0", "10")))
                        )
                    )
                )
            }
        }

        val state = GamificationEngine.compute(sessions, weights, emptyList(), LocalDate.of(2026, 8, 30))

        assertTrue(state.achievements.first { it.id == "langkah_pertama" }.isUnlocked)
        assertTrue(state.achievements.first { it.id == "sepuluh_sesi" }.isUnlocked)
        assertTrue(state.achievements.first { it.id == "rantai_4_pekan" }.isUnlocked)
        assertTrue(state.achievements.first { it.id == "penimbang_setia" }.isUnlocked)
        assertTrue(state.achievements.first { it.id == "penarik_sejati" }.isUnlocked)
        assertFalse(state.achievements.first { it.id == "pekan_sempurna" }.isUnlocked)
        assertEquals(120, state.counts.totalPullUpReps)
        assertEquals(4, state.counts.distinctWeighInWeeks)
    }

    @Test
    fun computePostWorkoutReward_detectsRankUpAtThreshold() {
        // Build 4 dates of 30 sets in 1 week = 4 * 350 + 100 = 1500 XP? Wait:
        // 4 dates of 25 sets = 4 * (250 + 50) + 100 = 1300 XP (< 1500, still Rakyat Jelata).
        val baseSessions = (0 until 4).map { idx ->
            val d = LocalDate.of(2026, 9, 7).plusDays(idx.toLong())
            uniformSession(id = idx + 1, date = d.toString(), setCount = 25)
        }
        // New session on Friday adds 25 sets = +250 XP + 50 session bonus = +300 -> total 1600 XP (>= 1500 -> Murid Petualang!)
        val newSession = uniformSession(id = 5, date = "2026-09-11", setCount = 25)

        val reward = GamificationEngine.computePostWorkoutReward(
            beforeSessions = baseSessions,
            newSession = newSession,
            weightLogs = emptyList(),
            measurements = emptyList(),
            today = LocalDate.of(2026, 9, 11)
        )

        assertEquals(300, reward.xpEarned)
        assertTrue(reward.tierChanged)
        assertTrue(reward.rankLetterChanged)
        assertEquals("F-V", reward.oldRankInfo.displayLabel)
        assertEquals("E-I", reward.newRankInfo.displayLabel)
        assertEquals("Pemula", reward.newRankInfo.rankTitle)
        assertEquals(1600, reward.newTotalXp)
    }

    @Test
    fun challengingConsistencyAchievements_eightAndTwelveWeekChainsAndFourPerfectWeeks() {
        val sessions = mutableListOf<WorkoutSession>()
        var nextId = 1
        val startMonday = LocalDate.of(2026, 1, 5)

        // 12 consecutive weeks: first 4 weeks have 4 sessions/week (unlocks bulan_disiplin at week 4),
        // weeks 5..12 have 3 sessions/week (unlocks rantai_4_pekan at week 4, rantai_8_pekan at week 8, rantai_12_pekan at week 12).
        for (w in 0 until 12) {
            val weekMon = startMonday.plusWeeks(w.toLong())
            val sessionsThisWeek = if (w < 4) 4 else 3
            for (d in 0 until sessionsThisWeek) {
                sessions.add(
                    uniformSession(
                        id = nextId++,
                        date = weekMon.plusDays(d.toLong()).toString(),
                        setCount = 12
                    )
                )
            }
        }

        val state12 = GamificationEngine.compute(sessions, emptyList(), emptyList(), LocalDate.of(2026, 3, 29))
        assertEquals(21, state12.achievements.size)

        val rantai4 = state12.achievements.first { it.id == "rantai_4_pekan" }
        val rantai8 = state12.achievements.first { it.id == "rantai_8_pekan" }
        val rantai12 = state12.achievements.first { it.id == "rantai_12_pekan" }
        val bulanDisiplin = state12.achievements.first { it.id == "bulan_disiplin" }

        assertTrue(rantai4.isUnlocked)
        assertTrue(rantai8.isUnlocked)
        assertTrue(rantai12.isUnlocked)
        assertTrue(bulanDisiplin.isUnlocked)
        assertEquals("12 / 12 pekan", rantai12.progressText)
        assertEquals("4 / 4 pekan", bulanDisiplin.progressText)

        // Check partial progress when only 7 weeks exist -> rantai_8_pekan is locked at 7 / 8 pekan
        val sevenWeeksSessions = sessions.filter {
            LocalDate.parse(it.date).isBefore(startMonday.plusWeeks(7))
        }
        val state7 = GamificationEngine.compute(sevenWeeksSessions, emptyList(), emptyList(), LocalDate.of(2026, 3, 29))
        val rantai8Locked = state7.achievements.first { it.id == "rantai_8_pekan" }
        assertFalse(rantai8Locked.isUnlocked)
        assertEquals(7, rantai8Locked.currentProgress)
        assertEquals(8, rantai8Locked.targetProgress)
        assertEquals("7 / 8 pekan", rantai8Locked.progressText)
    }

    @Test
    fun challengingEnduranceMilestones_100And200Sessions_1500Sets_100kVolume_16WeighIns_6Months() {
        val sessions = mutableListOf<WorkoutSession>()
        val startDate = LocalDate.of(2025, 1, 1)
        // 200 distinct dates, each with 10 sets of 100kg x 5 reps (= 5,000 kg/session -> 1,000,000 kg total, 2,000 sets)
        for (i in 0 until 200) {
            val d = startDate.plusDays((i * 2).toLong())
            sessions.add(
                uniformSession(
                    id = i + 1,
                    date = d.toString(),
                    setCount = 10,
                    weightKg = "100",
                    reps = "5"
                )
            )
        }
        val weights = (0 until 16).map { w ->
            WeightLog(
                id = w + 1,
                date = startDate.plusWeeks(w.toLong()).toString(),
                kg = 55f,
                timestamp = (w + 1) * 1000L
            )
        }
        val measurements = (0 until 6).map { m ->
            BodyMeasurement(
                id = m + 1,
                date = startDate.plusMonths(m.toLong()).toString(),
                shoulderCm = 112f,
                waistCm = 74f,
                timestamp = (m + 1) * 1000L
            )
        }

        val fullState = GamificationEngine.compute(sessions, weights, measurements, LocalDate.of(2026, 3, 1))
        assertTrue(fullState.achievements.first { it.id == "seratus_sesi" }.isUnlocked)
        assertTrue(fullState.achievements.first { it.id == "dua_ratus_sesi" }.isUnlocked)
        assertTrue(fullState.achievements.first { it.id == "seribu_set" }.isUnlocked)
        assertTrue(fullState.achievements.first { it.id == "penguasa_volume" }.isUnlocked)
        assertTrue(fullState.achievements.first { it.id == "penimbang_tahunan" }.isUnlocked)
        assertTrue(fullState.achievements.first { it.id == "arsitek_v_taper" }.isUnlocked)

        // Check partial state at 100 sessions (1000 sets < 1500, 100 < 200 sessions)
        val halfState = GamificationEngine.compute(sessions.take(100), weights.take(10), measurements.take(3), LocalDate.of(2026, 3, 1))
        assertTrue(halfState.achievements.first { it.id == "seratus_sesi" }.isUnlocked)
        val s200 = halfState.achievements.first { it.id == "dua_ratus_sesi" }
        assertFalse(s200.isUnlocked)
        assertEquals(100, s200.currentProgress)
        assertEquals("100 / 200 sesi", s200.progressText)

        val set1500 = halfState.achievements.first { it.id == "seribu_set" }
        assertFalse(set1500.isUnlocked)
        assertEquals(1000, set1500.currentProgress)

        val weigh16 = halfState.achievements.first { it.id == "penimbang_tahunan" }
        assertFalse(weigh16.isUnlocked)
        assertEquals("10 / 16 pekan", weigh16.progressText)

        val meas6 = halfState.achievements.first { it.id == "arsitek_v_taper" }
        assertFalse(meas6.isUnlocked)
        assertEquals("3 / 6 bulan", meas6.progressText)
    }
}
