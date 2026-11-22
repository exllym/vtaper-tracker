package com.example

import com.example.data.model.WeightLog
import com.example.data.model.WeightTrendAnalyzer
import com.example.data.model.WeightTrendStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun weightTrendAnalyzer_notEnoughData() {
        val emptyResult = WeightTrendAnalyzer.analyze(emptyList(), targetWeightKg = 50f)
        assertEquals(WeightTrendStatus.NOT_ENOUGH_DATA, emptyResult.status)
        assertNull(emptyResult.averageWeeklyChangeKg)

        val oneLogResult = WeightTrendAnalyzer.analyze(
            listOf(WeightLog(id = 1, date = "2026-10-01", kg = 40f, timestamp = 1000L)),
            targetWeightKg = 50f
        )
        assertEquals(WeightTrendStatus.NOT_ENOUGH_DATA, oneLogResult.status)
    }

    @Test
    fun weightTrendAnalyzer_tooSlowAdvice() {
        val logs = listOf(
            WeightLog(id = 1, date = "2026-09-01", kg = 40.0f, timestamp = 1000L),
            WeightLog(id = 2, date = "2026-09-15", kg = 40.1f, timestamp = 2000L)
        )
        val result = WeightTrendAnalyzer.analyze(logs, targetWeightKg = 50f)
        assertEquals(WeightTrendStatus.TOO_SLOW, result.status)
        assertEquals(0.05f, result.averageWeeklyChangeKg ?: 0f, 0.01f)
        assertTrue(result.adviceText.contains("di bawah target"))
        assertTrue(result.adviceText.contains("Tambah asupan ±150-200 kalori/hari"))
    }

    @Test
    fun weightTrendAnalyzer_negativeChange_droppingWeightAdvice() {
        val logs = listOf(
            WeightLog(id = 1, date = "2026-09-01", kg = 45.0f, timestamp = 1000L),
            WeightLog(id = 2, date = "2026-09-08", kg = 44.6f, timestamp = 2000L)
        )
        val result = WeightTrendAnalyzer.analyze(logs, targetWeightKg = 50f)
        assertEquals(WeightTrendStatus.TOO_SLOW, result.status)
        assertEquals(-0.4f, result.averageWeeklyChangeKg ?: 0f, 0.01f)
        assertTrue(result.adviceText.contains("Berat rata-rata turun 0.4 kg/minggu"))
        assertTrue(result.adviceText.contains("Untuk bulking, tambah asupan ±200-300 kalori/hari dan pastikan 5x makan tidak bolong."))
        assertTrue(!result.adviceText.contains("Kenaikan"))
    }

    @Test
    fun weightTrendAnalyzer_calendarWeeksGapCalculation() {
        // Gap of 3 weeks (2026-09-07 Monday to 2026-09-28 Monday) with a 3.0 kg change -> 1.0 kg/week
        val logs = listOf(
            WeightLog(id = 1, date = "2026-09-07", kg = 40.0f, timestamp = 1000L),
            WeightLog(id = 2, date = "2026-09-28", kg = 43.0f, timestamp = 2000L)
        )
        val result = WeightTrendAnalyzer.analyze(logs, targetWeightKg = 50f)
        assertEquals(1.0f, result.averageWeeklyChangeKg ?: 0f, 0.01f)
        assertEquals(WeightTrendStatus.TOO_FAST, result.status)
    }

    @Test
    fun weightTrendAnalyzer_thresholdOnTrackMax() {
        // Gap of 2 weeks with 0.7 kg change: 0.35 kg/week is ON_TRACK
        val logsOnTrack = listOf(
            WeightLog(id = 1, date = "2026-09-07", kg = 40.0f, timestamp = 1000L),
            WeightLog(id = 2, date = "2026-09-21", kg = 40.7f, timestamp = 2000L)
        )
        val resultOnTrack = WeightTrendAnalyzer.analyze(logsOnTrack, targetWeightKg = 50f)
        assertEquals(WeightTrendStatus.ON_TRACK, resultOnTrack.status)
        assertEquals(0.35f, resultOnTrack.averageWeeklyChangeKg ?: 0f, 0.01f)

        // 0.40 kg/week is now TOO_FAST (> 0.35)
        val logsTooFast = listOf(
            WeightLog(id = 1, date = "2026-09-07", kg = 40.0f, timestamp = 1000L),
            WeightLog(id = 2, date = "2026-09-14", kg = 40.4f, timestamp = 2000L)
        )
        val resultTooFast = WeightTrendAnalyzer.analyze(logsTooFast, targetWeightKg = 50f)
        assertEquals(WeightTrendStatus.TOO_FAST, resultTooFast.status)
        assertEquals(0.40f, resultTooFast.averageWeeklyChangeKg ?: 0f, 0.01f)
    }

    @Test
    fun weightTrendAnalyzer_reachedTarget() {
        val reachedResult = WeightTrendAnalyzer.analyze(
            listOf(
                WeightLog(id = 1, date = "2026-09-01", kg = 49.5f, timestamp = 1000L),
                WeightLog(id = 2, date = "2026-09-08", kg = 50.2f, timestamp = 2000L)
            ),
            targetWeightKg = 50f
        )
        assertEquals(WeightTrendStatus.REACHED_TARGET, reachedResult.status)
        assertTrue(reachedResult.adviceText.contains("Selamat!"))
    }

    @Test
    fun routeLayerVisualState_frameZeroStartsEnteringAtZeroAlphaAndExitingOpaque() {
        val enteringFrame0 = computeRouteLayerVisualState(
            isActive = true,
            isExiting = false,
            hasExitingRoute = true,
            transitionForward = true,
            isSubTransition = false,
            progress = 0f,
            viewportWidthPx = 900f,
            isWarmupDrawPass = false
        )
        val exitingFrame0 = computeRouteLayerVisualState(
            isActive = false,
            isExiting = true,
            hasExitingRoute = true,
            transitionForward = true,
            isSubTransition = false,
            progress = 0f,
            viewportWidthPx = 900f,
            isWarmupDrawPass = false
        )

        assertEquals(2f, enteringFrame0.zIndex, 0.001f)
        assertEquals(0f, enteringFrame0.alpha, 0.001f)
        assertEquals(100f, enteringFrame0.translationX, 0.001f)

        assertEquals(1f, exitingFrame0.zIndex, 0.001f)
        assertEquals(1f, exitingFrame0.alpha, 0.001f)
        assertEquals(0f, exitingFrame0.translationX, 0.001f)
    }

    @Test
    fun routeLayerVisualState_midAndEndKeepSolidCompositingAndCleanSettle() {
        val enteringMid = computeRouteLayerVisualState(
            isActive = true,
            isExiting = false,
            hasExitingRoute = true,
            transitionForward = false,
            isSubTransition = false,
            progress = 0.5f,
            viewportWidthPx = 900f,
            isWarmupDrawPass = false
        )
        val exitingMid = computeRouteLayerVisualState(
            isActive = false,
            isExiting = true,
            hasExitingRoute = true,
            transitionForward = false,
            isSubTransition = false,
            progress = 0.5f,
            viewportWidthPx = 900f,
            isWarmupDrawPass = false
        )
        val enteringEnd = computeRouteLayerVisualState(
            isActive = true,
            isExiting = false,
            hasExitingRoute = false,
            transitionForward = false,
            isSubTransition = false,
            progress = 1f,
            viewportWidthPx = 900f,
            isWarmupDrawPass = false
        )

        assertEquals(0.5f, enteringMid.alpha, 0.001f)
        assertEquals(-50f, enteringMid.translationX, 0.001f)
        assertEquals(1f, exitingMid.alpha, 0.001f)
        assertEquals(37.5f, exitingMid.translationX, 0.001f)

        assertEquals(1f, enteringEnd.alpha, 0.001f)
        assertEquals(0f, enteringEnd.translationX, 0.001f)
    }
}
