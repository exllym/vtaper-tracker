package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.BackupManager
import com.example.data.model.ExerciseInputType
import com.example.data.model.ProgressiveOverloadCalculator
import com.example.data.model.WeightLog
import com.example.data.model.WorkoutProgram
import com.example.data.model.WorkoutSession
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("V-Taper Guild", appName)
  }

  @Test
  fun `backup export and import works cleanly`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sessions = listOf(
      WorkoutSession(
        id = 1,
        date = "2026-09-29",
        dayType = "UPPER",
        dayLabel = "Upper Body A",
        timestamp = 1727580000000L,
        exercisesJson = "[]"
      )
    )
    val weights = listOf(
      WeightLog(
        id = 1,
        date = "2026-09-29",
        kg = 39.5f,
        timestamp = 1727580000000L
      )
    )
    val measurements = listOf(
      com.example.data.model.BodyMeasurement(
        id = 1,
        date = "2026-09-29",
        shoulderCm = 112.5f,
        waistCm = 74.0f,
        timestamp = 1727580000000L
      )
    )

    val json = BackupManager.exportToJson(
      context = context,
      sessions = sessions,
      weights = weights,
      measurements = measurements,
      startWeightKg = 38f,
      targetWeightKg = 52f,
      reminderHour = 17,
      reminderMinute = 30,
      reminderEnabled = true
    )

    assertNotNull(json)
    val imported = BackupManager.importFromJson(json)
    assertNotNull(imported)
    assertEquals(1, imported!!.sessions.size)
    assertEquals("Upper Body A", imported.sessions[0].dayLabel)
    assertEquals(1, imported.weightLogs.size)
    assertEquals(39.5f, imported.weightLogs[0].kg, 0.01f)
    assertEquals(1, imported.bodyMeasurements.size)
    assertEquals(112.5f, imported.bodyMeasurements[0].shoulderCm, 0.01f)
    assertEquals(74.0f, imported.bodyMeasurements[0].waistCm, 0.01f)
    assertEquals(38f, imported.startWeightKg, 0.01f)
    assertEquals(52f, imported.targetWeightKg, 0.01f)
    assertEquals(17, imported.reminderHour)
    assertEquals(30, imported.reminderMinute)
    assertEquals(true, imported.reminderEnabled)
  }

  @Test
  fun `backward compatibility - old backup without bodyMeasurements imports successfully`() = runBlocking {
    val oldBackupJson = """
      {
        "backupVersion": 1,
        "exportedAt": 1727580000000,
        "sessions": [],
        "weightLogs": [],
        "weightGoals": {
          "start": 40.0,
          "target": 55.0
        },
        "reminderSettings": {
          "hour": 8,
          "minute": 0,
          "enabled": true
        }
      }
    """.trimIndent()

    val imported = BackupManager.importFromJson(oldBackupJson)
    assertNotNull(imported)
    assertEquals(0, imported!!.bodyMeasurements.size)
    assertEquals(40.0f, imported.startWeightKg, 0.01f)
  }

  @Test
  fun `progressive overload for duration only exercises`() {
    val plankJson = """
        [
          {
            "name": "Core (Plank/Leg Raise)",
            "sets": [
              {"weightKg": "", "reps": "45", "isDone": true},
              {"weightKg": "", "reps": "45", "isDone": true},
              {"weightKg": "", "reps": "45", "isDone": true}
            ]
          }
        ]
    """.trimIndent()

    val sessions = listOf(
      WorkoutSession(id = 1, date = "2026-10-08", timestamp = 2000L, dayType = "LOWER", dayLabel = "Lower A", exercisesJson = plankJson),
      WorkoutSession(id = 2, date = "2026-10-01", timestamp = 1000L, dayType = "LOWER", dayLabel = "Lower A", exercisesJson = plankJson)
    )

    val suggestion = ProgressiveOverloadCalculator.suggestNextWeight(
      recentSessions = sessions,
      exerciseName = "Core (Plank/Leg Raise)",
      currentTargetRepRange = "3 x 30-45 detik",
      inputType = ExerciseInputType.DURATION_ONLY
    )

    assertNull(suggestion.suggestedWeightKg)
    assertTrue(suggestion.reasoning.contains("50 detik"))
  }

  @Test
  fun `progressive overload for bodyweight reps exercises`() {
    val pullUpJson = """
        [
          {
            "name": "Pull-up / Lat Pulldown",
            "sets": [
              {"weightKg": "0", "reps": "10", "isDone": true},
              {"weightKg": "0", "reps": "10", "isDone": true},
              {"weightKg": "0", "reps": "10", "isDone": true}
            ]
          }
        ]
    """.trimIndent()

    val sessions = listOf(
      WorkoutSession(id = 1, date = "2026-10-08", timestamp = 2000L, dayType = "UPPER", dayLabel = "Upper A", exercisesJson = pullUpJson),
      WorkoutSession(id = 2, date = "2026-10-01", timestamp = 1000L, dayType = "UPPER", dayLabel = "Upper A", exercisesJson = pullUpJson)
    )

    val suggestion = ProgressiveOverloadCalculator.suggestNextWeight(
      recentSessions = sessions,
      exerciseName = "Pull-up / Lat Pulldown",
      currentTargetRepRange = "3 x 8-10",
      inputType = ExerciseInputType.BODYWEIGHT_REPS
    )

    assertEquals(1.0f, suggestion.suggestedWeightKg ?: 0f, 0.01f)
    assertTrue(suggestion.reasoning.contains("1-2 kg ekstra"))
  }

  @Test
  fun `progressive overload asks to repeat 1 session when weight changed and all sets exceed top rep`() {
    val olderSquatJson = """
        [
          {
            "name": "Squat",
            "sets": [
              {"weightKg": "50", "reps": "10", "isDone": true},
              {"weightKg": "50", "reps": "10", "isDone": true},
              {"weightKg": "50", "reps": "10", "isDone": true},
              {"weightKg": "50", "reps": "10", "isDone": true}
            ]
          }
        ]
    """.trimIndent()
    val newerSquatJson = """
        [
          {
            "name": "Squat",
            "sets": [
              {"weightKg": "55", "reps": "10", "isDone": true},
              {"weightKg": "55", "reps": "10", "isDone": true},
              {"weightKg": "55", "reps": "10", "isDone": true},
              {"weightKg": "55", "reps": "10", "isDone": true}
            ]
          }
        ]
    """.trimIndent()

    // Also pass in oldest-first order to verify chronological sorting
    val sessions = listOf(
      WorkoutSession(id = 1, date = "2026-10-01", timestamp = 1000L, dayType = "LOWER", dayLabel = "Lower A", exercisesJson = olderSquatJson),
      WorkoutSession(id = 2, date = "2026-10-08", timestamp = 2000L, dayType = "LOWER", dayLabel = "Lower A", exercisesJson = newerSquatJson)
    )

    val suggestion = ProgressiveOverloadCalculator.suggestNextWeight(
      recentSessions = sessions,
      exerciseName = "Squat",
      currentTargetRepRange = "4 x 6-8",
      inputType = ExerciseInputType.WEIGHT_REPS
    )

    assertEquals(55f, suggestion.suggestedWeightKg ?: 0f, 0.01f)
    assertTrue(suggestion.reasoning.contains("ulangi 1 sesi lagi", ignoreCase = true))
    assertTrue(suggestion.reasoning.contains("beban baru 55kg"))
    assertTrue(!suggestion.reasoning.contains("Masih di bawah rep atas target"))
  }

  @Test
  fun `progressive overload reports lowest rep and completed set count when some sets are below top rep`() {
    val prevSquatJson = """
        [
          {
            "name": "Squat",
            "sets": [
              {"weightKg": "50", "reps": "8", "isDone": true},
              {"weightKg": "50", "reps": "8", "isDone": true},
              {"weightKg": "50", "reps": "8", "isDone": true},
              {"weightKg": "50", "reps": "8", "isDone": true}
            ]
          }
        ]
    """.trimIndent()
    val partialSquatJson = """
        [
          {
            "name": "Squat",
            "sets": [
              {"weightKg": "50", "reps": "10", "isDone": true},
              {"weightKg": "50", "reps": "10", "isDone": true},
              {"weightKg": "50", "reps": "9", "isDone": true},
              {"weightKg": "50", "reps": "7", "isDone": true}
            ]
          }
        ]
    """.trimIndent()

    val sessions = listOf(
      WorkoutSession(id = 2, date = "2026-10-08", timestamp = 2000L, dayType = "LOWER", dayLabel = "Lower A", exercisesJson = partialSquatJson),
      WorkoutSession(id = 1, date = "2026-10-01", timestamp = 1000L, dayType = "LOWER", dayLabel = "Lower A", exercisesJson = prevSquatJson)
    )

    val suggestion = ProgressiveOverloadCalculator.suggestNextWeight(
      recentSessions = sessions,
      exerciseName = "Squat",
      currentTargetRepRange = "4 x 6-8",
      inputType = ExerciseInputType.WEIGHT_REPS
    )

    assertEquals(50f, suggestion.suggestedWeightKg ?: 0f, 0.01f)
    assertTrue(suggestion.reasoning.contains("3/4 set capai 8+ reps"))
    assertTrue(suggestion.reasoning.contains("terendah 7/8 reps"))
    assertTrue(!suggestion.reasoning.contains("10/8"))
  }

  @Test
  fun `workout program parseExerciseSummaries parses blank weight bodyweight set`() {
    val pullUpJson = """
        [
          {
            "name": "Pull-up / Lat Pulldown",
            "sets": [
              {"weightKg": "", "reps": "8", "isDone": false},
              {"weightKg": "5", "reps": "6", "isDone": true}
            ]
          }
        ]
    """.trimIndent()

    val summaries = WorkoutProgram.parseExerciseSummaries(pullUpJson)
    assertEquals(1, summaries.size)
    val sets = summaries[0].sets
    assertEquals(2, sets.size)
    assertEquals(0f, sets[0].first, 0.01f)
    assertEquals(8, sets[0].second)
    assertEquals(5f, sets[1].first, 0.01f)
    assertEquals(6, sets[1].second)
  }

  @Test
  fun `room database and measurementDao operations and session update`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
      .addMigrations(com.example.data.db.AppDatabase.MIGRATION_1_2)
      .build()
    try {
      val measurement = com.example.data.model.BodyMeasurement(
        date = "2026-10-10",
        shoulderCm = 115.0f,
        waistCm = 76.0f,
        timestamp = 1728550000000L
      )
      val id = db.measurementDao().insert(measurement)
      assertTrue(id > 0)
      val all = db.measurementDao().getAll()
      assertEquals(1, all.size)
      assertEquals(115.0f, all[0].shoulderCm, 0.01f)
      assertEquals(76.0f, all[0].waistCm, 0.01f)

      val session = WorkoutSession(
        date = "2026-10-10",
        dayType = "UPPER",
        dayLabel = "Upper Body A",
        timestamp = 1728550000000L,
        exercisesJson = "[]"
      )
      val sId = db.workoutDao().insertSession(session).toInt()
      db.workoutDao().updateSessionJson(sId, "[{\"name\":\"Incline DB Press\"}]")
      val updated = db.workoutDao().getSessionById(sId)
      assertNotNull(updated)
      assertEquals("[{\"name\":\"Incline DB Press\"}]", updated!!.exercisesJson)
    } finally {
      db.close()
    }
  }

  @Test
  fun `workoutOverrideStore returns override only on matching date and clears on rollover`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.data.WorkoutOverrideStore.clearOverride(context)

    com.example.data.WorkoutOverrideStore.saveOverride(
      context,
      "2026-10-14",
      com.example.data.model.DayType.LOWER
    )
    assertEquals(
      com.example.data.model.DayType.LOWER,
      com.example.data.WorkoutOverrideStore.getValidOverride(context, "2026-10-14")
    )
    assertNull(
      com.example.data.WorkoutOverrideStore.getValidOverride(context, "2026-10-15")
    )
  }

  @Test
  fun `draftWorkoutStore isolates Upper and Lower drafts on same date and discards old dates`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.data.DraftWorkoutStore.clearAllDrafts(context)

    com.example.data.DraftWorkoutStore.saveDraft(
      context,
      "2026-10-14",
      com.example.data.model.DayType.UPPER,
      "[{\"name\":\"Overhead Press\"}]"
    )
    com.example.data.DraftWorkoutStore.saveDraft(
      context,
      "2026-10-14",
      com.example.data.model.DayType.LOWER,
      "[{\"name\":\"Squat\"}]"
    )

    assertEquals(
      "[{\"name\":\"Overhead Press\"}]",
      com.example.data.DraftWorkoutStore.getDraft(context, "2026-10-14", com.example.data.model.DayType.UPPER)
    )
    assertEquals(
      "[{\"name\":\"Squat\"}]",
      com.example.data.DraftWorkoutStore.getDraft(context, "2026-10-14", com.example.data.model.DayType.LOWER)
    )

    // Clearing LOWER leaves UPPER intact
    com.example.data.DraftWorkoutStore.clearDraft(context, com.example.data.model.DayType.LOWER)
    assertNull(
      com.example.data.DraftWorkoutStore.getDraft(context, "2026-10-14", com.example.data.model.DayType.LOWER)
    )
    assertEquals(
      "[{\"name\":\"Overhead Press\"}]",
      com.example.data.DraftWorkoutStore.getDraft(context, "2026-10-14", com.example.data.model.DayType.UPPER)
    )

    // Querying on a different date discards stale draft
    assertNull(
      com.example.data.DraftWorkoutStore.getDraft(context, "2026-10-15", com.example.data.model.DayType.UPPER)
    )
  }
}
