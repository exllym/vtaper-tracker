package com.example.data

import android.content.Context
import com.example.data.model.BodyMeasurement
import com.example.data.model.WeightLog
import com.example.data.model.WorkoutSession
import java.time.DayOfWeek
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class BackupData(
    val sessions: List<WorkoutSession>,
    val weightLogs: List<WeightLog>,
    val bodyMeasurements: List<BodyMeasurement> = emptyList(),
    val startWeightKg: Float,
    val targetWeightKg: Float,
    val reminderHour: Int,
    val reminderMinute: Int,
    val reminderEnabled: Boolean,
    val weighInReminderEnabled: Boolean = true,
    val weighInReminderDay: Int = DayOfWeek.SUNDAY.value,
    val weighInReminderHour: Int = 7,
    val weighInReminderMinute: Int = 0
)

object BackupManager {
    const val CURRENT_BACKUP_VERSION = 2

    suspend fun exportToJson(
        context: Context,
        sessions: List<WorkoutSession>,
        weights: List<WeightLog>,
        measurements: List<BodyMeasurement>,
        startWeightKg: Float,
        targetWeightKg: Float,
        reminderHour: Int,
        reminderMinute: Int,
        reminderEnabled: Boolean,
        weighInReminderEnabled: Boolean = true,
        weighInReminderDay: Int = DayOfWeek.SUNDAY.value,
        weighInReminderHour: Int = 7,
        weighInReminderMinute: Int = 0
    ): String = withContext(Dispatchers.Default) {
        val root = JSONObject()
        root.put("backupVersion", CURRENT_BACKUP_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        val sessionsArray = JSONArray()
        for (session in sessions) {
            val sObj = JSONObject()
            sObj.put("id", session.id)
            sObj.put("date", session.date)
            sObj.put("dayType", session.dayType)
            sObj.put("dayLabel", session.dayLabel)
            sObj.put("timestamp", session.timestamp)
            sObj.put("exercisesJson", session.exercisesJson)
            sessionsArray.put(sObj)
        }
        root.put("sessions", sessionsArray)

        val weightsArray = JSONArray()
        for (w in weights) {
            val wObj = JSONObject()
            wObj.put("id", w.id)
            wObj.put("date", w.date)
            wObj.put("kg", w.kg.toDouble())
            wObj.put("timestamp", w.timestamp)
            weightsArray.put(wObj)
        }
        root.put("weightLogs", weightsArray)

        val measurementsArray = JSONArray()
        for (m in measurements) {
            val mObj = JSONObject()
            mObj.put("id", m.id)
            mObj.put("date", m.date)
            mObj.put("shoulderCm", m.shoulderCm.toDouble())
            mObj.put("waistCm", m.waistCm.toDouble())
            mObj.put("timestamp", m.timestamp)
            measurementsArray.put(mObj)
        }
        root.put("bodyMeasurements", measurementsArray)

        val goalsObj = JSONObject()
        goalsObj.put("start", startWeightKg.toDouble())
        goalsObj.put("target", targetWeightKg.toDouble())
        root.put("weightGoals", goalsObj)

        val reminderObj = JSONObject()
        reminderObj.put("hour", reminderHour)
        reminderObj.put("minute", reminderMinute)
        reminderObj.put("enabled", reminderEnabled)
        root.put("reminderSettings", reminderObj)

        val weighInObj = JSONObject()
        weighInObj.put("enabled", weighInReminderEnabled)
        weighInObj.put("day", weighInReminderDay)
        weighInObj.put("hour", weighInReminderHour)
        weighInObj.put("minute", weighInReminderMinute)
        root.put("weighInReminderSettings", weighInObj)

        root.toString(2)
    }

    suspend fun importFromJson(jsonString: String): BackupData? = withContext(Dispatchers.Default) {
        try {
            if (jsonString.isBlank()) return@withContext null
            val root = JSONObject(jsonString)
            val version = root.optInt("backupVersion", -1)
            if (version < 1 || version > CURRENT_BACKUP_VERSION) {
                return@withContext null
            }

            val sessions = mutableListOf<WorkoutSession>()
            val sessionsArray = root.optJSONArray("sessions")
            if (sessionsArray != null) {
                for (i in 0 until sessionsArray.length()) {
                    val sObj = sessionsArray.getJSONObject(i)
                    sessions.add(
                        WorkoutSession(
                            id = sObj.optInt("id", 0),
                            date = sObj.optString("date", ""),
                            dayType = sObj.optString("dayType", "UPPER"),
                            dayLabel = sObj.optString("dayLabel", ""),
                            timestamp = sObj.optLong("timestamp", System.currentTimeMillis()),
                            exercisesJson = sObj.optString("exercisesJson", "[]")
                        )
                    )
                }
            }

            val weights = mutableListOf<WeightLog>()
            val weightsArray = root.optJSONArray("weightLogs")
            if (weightsArray != null) {
                for (i in 0 until weightsArray.length()) {
                    val wObj = weightsArray.getJSONObject(i)
                    weights.add(
                        WeightLog(
                            id = wObj.optInt("id", 0),
                            date = wObj.optString("date", ""),
                            kg = wObj.optDouble("kg", 0.0).toFloat(),
                            timestamp = wObj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            val measurements = mutableListOf<BodyMeasurement>()
            val measurementsArray = root.optJSONArray("bodyMeasurements")
            if (measurementsArray != null) {
                for (i in 0 until measurementsArray.length()) {
                    val mObj = measurementsArray.getJSONObject(i)
                    measurements.add(
                        BodyMeasurement(
                            id = mObj.optInt("id", 0),
                            date = mObj.optString("date", ""),
                            shoulderCm = mObj.optDouble("shoulderCm", 0.0).toFloat(),
                            waistCm = mObj.optDouble("waistCm", 0.0).toFloat(),
                            timestamp = mObj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            val goalsObj = root.optJSONObject("weightGoals")
            val startKg = goalsObj?.optDouble("start", 38.0)?.toFloat() ?: 38f
            val targetKg = goalsObj?.optDouble("target", 50.0)?.toFloat() ?: 50f

            val reminderObj = root.optJSONObject("reminderSettings")
            val rHour = reminderObj?.optInt("hour", 16) ?: 16
            val rMinute = reminderObj?.optInt("minute", 0) ?: 0
            val rEnabled = reminderObj?.optBoolean("enabled", true) ?: true

            val weighInObj = root.optJSONObject("weighInReminderSettings")
            val wEnabled = if (weighInObj != null && weighInObj.has("enabled")) {
                weighInObj.optBoolean("enabled", true)
            } else {
                true
            }
            val wDay = if (weighInObj != null && weighInObj.has("day")) {
                val rawDay = weighInObj.opt("day")
                when (rawDay) {
                    is Number -> rawDay.toInt().coerceIn(1, 7)
                    is String -> try {
                        DayOfWeek.valueOf(rawDay.uppercase()).value
                    } catch (_: Exception) {
                        rawDay.toIntOrNull()?.coerceIn(1, 7) ?: DayOfWeek.SUNDAY.value
                    }
                    else -> DayOfWeek.SUNDAY.value
                }
            } else {
                DayOfWeek.SUNDAY.value
            }
            val wHour = if (weighInObj != null && weighInObj.has("hour")) {
                weighInObj.optInt("hour", 7).coerceIn(0, 23)
            } else {
                7
            }
            val wMinute = if (weighInObj != null && weighInObj.has("minute")) {
                weighInObj.optInt("minute", 0).coerceIn(0, 59)
            } else {
                0
            }

            BackupData(
                sessions = sessions,
                weightLogs = weights,
                bodyMeasurements = measurements,
                startWeightKg = startKg,
                targetWeightKg = targetKg,
                reminderHour = rHour,
                reminderMinute = rMinute,
                reminderEnabled = rEnabled,
                weighInReminderEnabled = wEnabled,
                weighInReminderDay = wDay,
                weighInReminderHour = wHour,
                weighInReminderMinute = wMinute
            )
        } catch (e: Exception) {
            null
        }
    }
}
