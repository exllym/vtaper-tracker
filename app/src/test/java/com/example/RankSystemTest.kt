package com.example

import com.example.data.model.GamificationEngine
import com.example.data.model.RankSystem
import com.example.data.model.WorkoutSession
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RankSystemTest {

    @Test
    fun boundaries_allSpecifiedXpValues() {
        // 0 -> F-I (0 / 300)
        val r0 = RankSystem.rankFor(0)
        assertEquals(0, r0.rankIndex)
        assertEquals("F", r0.rankLetter)
        assertEquals("Rekrut", r0.rankTitle)
        assertEquals("Kayu", r0.metal)
        assertEquals(0, r0.tierIndex)
        assertEquals("I", r0.tierRoman)
        assertEquals("F-I", r0.displayLabel)
        assertEquals(0, r0.xpIntoTier)
        assertEquals(300, r0.xpNeededForNextTier)
        assertEquals("F-II", r0.nextLabel)
        assertFalse(r0.isEndless)

        // 299 -> still F-I (299 / 300)
        val r299 = RankSystem.rankFor(299)
        assertEquals("F-I", r299.displayLabel)
        assertEquals(0, r299.tierIndex)
        assertEquals(299, r299.xpIntoTier)
        assertEquals(300, r299.xpNeededForNextTier)

        // 300 -> F-II (0 / 300)
        val r300 = RankSystem.rankFor(300)
        assertEquals("F-II", r300.displayLabel)
        assertEquals(1, r300.tierIndex)
        assertEquals(0, r300.xpIntoTier)
        assertEquals(300, r300.xpNeededForNextTier)
        assertEquals("F-III", r300.nextLabel)

        // 1499 -> F-V (299 / 300 -> E-I)
        val r1499 = RankSystem.rankFor(1499)
        assertEquals("F-V", r1499.displayLabel)
        assertEquals(4, r1499.tierIndex)
        assertEquals(299, r1499.xpIntoTier)
        assertEquals(300, r1499.xpNeededForNextTier)
        assertEquals("E-I", r1499.nextLabel)

        // 1500 -> E-I (0 / 700 -> E-II)
        val r1500 = RankSystem.rankFor(1500)
        assertEquals(1, r1500.rankIndex)
        assertEquals("E", r1500.rankLetter)
        assertEquals("Pemula", r1500.rankTitle)
        assertEquals("Besi", r1500.metal)
        assertEquals(0, r1500.tierIndex)
        assertEquals("E-I", r1500.displayLabel)
        assertEquals(0, r1500.xpIntoTier)
        assertEquals(700, r1500.xpNeededForNextTier)
        assertEquals("E-II", r1500.nextLabel)

        // 4999 -> E-V (699 / 700 -> D-I)
        val r4999 = RankSystem.rankFor(4999)
        assertEquals("E-V", r4999.displayLabel)
        assertEquals(4, r4999.tierIndex)
        assertEquals(699, r4999.xpIntoTier)
        assertEquals(700, r4999.xpNeededForNextTier)
        assertEquals("D-I", r4999.nextLabel)

        // 5000 -> D-I (0 / 1400 -> D-II)
        val r5000 = RankSystem.rankFor(5000)
        assertEquals(2, r5000.rankIndex)
        assertEquals("D", r5000.rankLetter)
        assertEquals("Petualang", r5000.rankTitle)
        assertEquals("Perunggu", r5000.metal)
        assertEquals(0, r5000.tierIndex)
        assertEquals("D-I", r5000.displayLabel)
        assertEquals(0, r5000.xpIntoTier)
        assertEquals(1400, r5000.xpNeededForNextTier)
        assertEquals("D-II", r5000.nextLabel)

        // 179999 -> SS-V (11999 / 12000 -> SSS-I)
        val r179999 = RankSystem.rankFor(179_999)
        assertEquals(7, r179999.rankIndex)
        assertEquals("SS", r179999.rankLetter)
        assertEquals("Mitos", r179999.rankTitle)
        assertEquals("Orichalcum", r179999.metal)
        assertEquals(4, r179999.tierIndex)
        assertEquals("SS-V", r179999.displayLabel)
        assertEquals(11_999, r179999.xpIntoTier)
        assertEquals(12_000, r179999.xpNeededForNextTier)
        assertEquals("SSS-I", r179999.nextLabel)

        // 180000 -> SSS-I (0 / 16000 -> SSS-II)
        val r180000 = RankSystem.rankFor(180_000)
        assertEquals(8, r180000.rankIndex)
        assertEquals("SSS", r180000.rankLetter)
        assertEquals("Raja Naga", r180000.rankTitle)
        assertEquals("Adamantium", r180000.metal)
        assertEquals(0, r180000.tierIndex)
        assertEquals("SSS-I", r180000.displayLabel)
        assertEquals(0, r180000.xpIntoTier)
        assertEquals(16_000, r180000.xpNeededForNextTier)
        assertEquals("SSS-II", r180000.nextLabel)
        assertFalse(r180000.isEndless)

        // 259999 -> SSS-V (15999 / 16000 -> Raja Naga Lv 1)
        val r259999 = RankSystem.rankFor(259_999)
        assertEquals(8, r259999.rankIndex)
        assertEquals(4, r259999.tierIndex)
        assertEquals("SSS-V", r259999.displayLabel)
        assertEquals(15_999, r259999.xpIntoTier)
        assertEquals(16_000, r259999.xpNeededForNextTier)
        assertEquals("Raja Naga Lv 1", r259999.nextLabel)
        assertFalse(r259999.isEndless)

        // 260000 -> Raja Naga Lv 1 (0 / 15000 -> Raja Naga Lv 2)
        val r260000 = RankSystem.rankFor(260_000)
        assertEquals(8, r260000.rankIndex)
        assertEquals(4, r260000.tierIndex)
        assertEquals("Raja Naga Lv 1", r260000.displayLabel)
        assertEquals(0, r260000.xpIntoTier)
        assertEquals(15_000, r260000.xpNeededForNextTier)
        assertEquals("Raja Naga Lv 2", r260000.nextLabel)
        assertTrue(r260000.isEndless)
        assertEquals(1, r260000.monarchLevel)

        // 274999 -> Raja Naga Lv 1 (14999 / 15000 -> Raja Naga Lv 2)
        val r274999 = RankSystem.rankFor(274_999)
        assertEquals("Raja Naga Lv 1", r274999.displayLabel)
        assertEquals(14_999, r274999.xpIntoTier)
        assertEquals(15_000, r274999.xpNeededForNextTier)
        assertTrue(r274999.isEndless)
        assertEquals(1, r274999.monarchLevel)

        // 275000 -> Raja Naga Lv 2 (0 / 15000 -> Raja Naga Lv 3)
        val r275000 = RankSystem.rankFor(275_000)
        assertEquals("Raja Naga Lv 2", r275000.displayLabel)
        assertEquals(0, r275000.xpIntoTier)
        assertEquals(15_000, r275000.xpNeededForNextTier)
        assertEquals("Raja Naga Lv 3", r275000.nextLabel)
        assertTrue(r275000.isEndless)
        assertEquals(2, r275000.monarchLevel)
    }

    @Test
    fun tierWidths_forEveryRank_matchSpecification() {
        val expectedWidths = listOf(
            "F" to 300,
            "E" to 700,
            "D" to 1_400,
            "C" to 2_600,
            "B" to 4_000,
            "A" to 6_000,
            "S" to 9_000,
            "SS" to 12_000,
            "SSS" to 16_000
        )

        assertEquals(9, RankSystem.RANKS.size)
        for ((index, def) in RankSystem.RANKS.withIndex()) {
            val (letter, width) = expectedWidths[index]
            assertEquals(letter, def.letter)
            assertEquals(width, def.tierWidthXp)
            assertEquals(0, def.spanXp % 5)
            // Verify tier start produces tier I..V in exact increments of width
            for (tier in 0 until 5) {
                val xp = def.startXp + tier * width
                val info = RankSystem.rankFor(xp)
                assertEquals(index, info.rankIndex)
                assertEquals(tier, info.tierIndex)
                assertEquals("$letter-${RankSystem.TIER_ROMAN[tier]}", info.displayLabel)
                assertEquals(width, info.xpNeededForNextTier)
            }
        }
    }

    @Test
    fun tierIndex_alwaysBetween0And4_and_monotonicAcrossXpIncreases() {
        val sampleXps = (
            (0..310_000 step 250).toList() + listOf(
                299, 300, 1499, 1500, 4999, 5000, 11_999, 12_000,
                24_999, 25_000, 44_999, 45_000, 74_999, 75_000,
                119_999, 120_000, 179_999, 180_000, 259_999, 260_000,
                274_999, 275_000, 290_000
            )
            ).sorted()

        var prevRankIdx = -1
        var prevTierIdx = -1
        var prevMonarchLv = -1

        for (xp in sampleXps) {
            val info = RankSystem.rankFor(xp)
            assertTrue("tierIndex out of range at xp=$xp", info.tierIndex in 0..4)

            if (prevRankIdx >= 0) {
                assertTrue(
                    "Rank decreased at xp=$xp: ${info.rankIndex} < $prevRankIdx",
                    info.rankIndex >= prevRankIdx
                )
                if (info.rankIndex == prevRankIdx) {
                    assertTrue(
                        "Tier decreased at xp=$xp: ${info.tierIndex} < $prevTierIdx",
                        info.tierIndex >= prevTierIdx
                    )
                    assertTrue(
                        "Monarch level decreased at xp=$xp: ${info.monarchLevel} < $prevMonarchLv",
                        info.monarchLevel >= prevMonarchLv
                    )
                }
            }
            prevRankIdx = info.rankIndex
            prevTierIdx = info.tierIndex
            prevMonarchLv = info.monarchLevel
        }
    }

    @Test
    fun endlessMonarchLevels_continueIndefinitelyAfter260000() {
        val lv3 = RankSystem.rankFor(260_000 + 2 * 15_000 + 5_000) // 295000 -> Lv 3, 5000/15000
        assertEquals("Raja Naga Lv 3", lv3.displayLabel)
        assertEquals(3, lv3.monarchLevel)
        assertEquals(5_000, lv3.xpIntoTier)
        assertEquals(15_000, lv3.xpNeededForNextTier)
        assertEquals("Raja Naga Lv 4", lv3.nextLabel)
        assertTrue(lv3.isEndless)
        assertEquals(4, lv3.tierIndex)

        val lv10 = RankSystem.rankFor(260_000 + 9 * 15_000)
        assertEquals("Raja Naga Lv 10", lv10.displayLabel)
        assertEquals(10, lv10.monarchLevel)
    }

    @Test
    fun displayLabelFormats_examplesAndNegativeClamp() {
        assertEquals("F-I", RankSystem.rankFor(-50).displayLabel)
        // D starts at 5000, width = 1400 -> D-III starts at 5000 + 2 * 1400 = 7800
        assertEquals("D-III", RankSystem.rankFor(7_800).displayLabel)
        assertEquals("C-IV", RankSystem.rankFor(12_000 + 3 * 2_600).displayLabel)
        assertEquals("Raja Naga Lv 3", RankSystem.rankFor(290_000).displayLabel)
    }

    @Test
    fun deletingSession_lowersDerivedRankLabelConsistently() {
        val sets = (1..30).map {
            JSONObject().apply {
                put("weightKg", "20")
                put("reps", "10")
                put("isDone", true)
            }
        }
        val exJson = JSONArray(
            listOf(
                JSONObject().apply {
                    put("name", "Incline Dumbbell Press")
                    put("note", "")
                    put("sets", JSONArray(sets))
                }
            )
        ).toString()

        // Day 1: 300 + 50 = 350 XP -> F-II
        val s1 = WorkoutSession(
            id = 1,
            date = "2026-10-05",
            dayType = "UPPER",
            dayLabel = "Upper Body",
            timestamp = 1000L,
            exercisesJson = exJson
        )
        // Day 2: 350 XP -> total 700 XP -> F-III
        val s2 = WorkoutSession(
            id = 2,
            date = "2026-10-06",
            dayType = "LOWER",
            dayLabel = "Lower Body",
            timestamp = 2000L,
            exercisesJson = exJson
        )

        val both = GamificationEngine.compute(listOf(s1, s2), emptyList(), emptyList(), LocalDate.of(2026, 10, 6))
        assertEquals(700, both.totalXp)
        assertEquals("F-III", both.rankInfo.displayLabel)

        val afterDelete = GamificationEngine.compute(listOf(s1), emptyList(), emptyList(), LocalDate.of(2026, 10, 6))
        assertEquals(350, afterDelete.totalXp)
        assertEquals("F-II", afterDelete.rankInfo.displayLabel)
    }
}
