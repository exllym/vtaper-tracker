package com.example.data

import android.content.Context
import com.example.data.model.DayType

object WorkoutOverrideStore {
    private const val PREFS_NAME = "workout_override_prefs"
    private const val KEY_OVERRIDE_DATE = "override_date"
    private const val KEY_OVERRIDE_TYPE = "override_type"

    fun saveOverride(context: Context, date: String, dayType: DayType) {
        if (dayType != DayType.UPPER && dayType != DayType.LOWER) {
            clearOverride(context)
            return
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_OVERRIDE_DATE, date)
            .putString(KEY_OVERRIDE_TYPE, dayType.code)
            .apply()
    }

    fun getValidOverride(context: Context, todayDate: String): DayType? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedDate = prefs.getString(KEY_OVERRIDE_DATE, null) ?: return null
        if (savedDate != todayDate) {
            clearOverride(context)
            return null
        }
        return when (prefs.getString(KEY_OVERRIDE_TYPE, null)) {
            DayType.UPPER.code -> DayType.UPPER
            DayType.LOWER.code -> DayType.LOWER
            else -> {
                clearOverride(context)
                null
            }
        }
    }

    fun clearOverride(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_OVERRIDE_DATE)
            .remove(KEY_OVERRIDE_TYPE)
            .apply()
    }
}
