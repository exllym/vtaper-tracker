package com.example.data

import android.content.Context
import com.example.data.model.DayType

object DraftWorkoutStore {
    private const val PREFS_NAME = "draft_workout_prefs"
    private const val KEY_LEGACY_DRAFT_DATE = "draft_date"
    private const val KEY_LEGACY_DRAFT_EXERCISES_JSON = "draft_exercises_json"

    private fun dateKey(dayType: DayType): String = "draft_date_${dayType.code}"
    private fun jsonKey(dayType: DayType): String = "draft_exercises_json_${dayType.code}"

    fun saveDraft(context: Context, date: String, dayType: DayType, exercisesJson: String) {
        if (dayType != DayType.UPPER && dayType != DayType.LOWER) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
            .remove(KEY_LEGACY_DRAFT_DATE)
            .remove(KEY_LEGACY_DRAFT_EXERCISES_JSON)
            .putString(dateKey(dayType), date)
            .putString(jsonKey(dayType), exercisesJson)

        // Discard any stale drafts from other dates for the opposite type
        val otherType = if (dayType == DayType.UPPER) DayType.LOWER else DayType.UPPER
        val otherDate = prefs.getString(dateKey(otherType), null)
        if (otherDate != null && otherDate != date) {
            editor.remove(dateKey(otherType)).remove(jsonKey(otherType))
        }

        editor.apply()
    }

    fun getDraft(context: Context, expectedDate: String, dayType: DayType): String? {
        if (dayType != DayType.UPPER && dayType != DayType.LOWER) return null
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedDate = prefs.getString(dateKey(dayType), null)
        if (savedDate == expectedDate) {
            return prefs.getString(jsonKey(dayType), null)
        }
        if (savedDate != null) {
            prefs.edit()
                .remove(dateKey(dayType))
                .remove(jsonKey(dayType))
                .apply()
        }
        return null
    }

    fun clearDraft(context: Context, dayType: DayType) {
        if (dayType != DayType.UPPER && dayType != DayType.LOWER) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(dateKey(dayType))
            .remove(jsonKey(dayType))
            .apply()
    }

    fun clearAllDrafts(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
