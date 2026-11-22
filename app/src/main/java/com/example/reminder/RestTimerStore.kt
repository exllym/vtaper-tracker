package com.example.reminder

import android.content.Context

object RestTimerStore {
    private const val PREFS_NAME = "rest_timer_prefs"
    private const val KEY_PREFIX_TIMESTAMP = "rest_end_ts_"
    private const val KEY_PREFIX_TOTAL = "rest_total_sec_"

    fun saveTimer(context: Context, exerciseIndex: Int, endTimestampMillis: Long, totalSeconds: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong("$KEY_PREFIX_TIMESTAMP$exerciseIndex", endTimestampMillis)
            .putInt("$KEY_PREFIX_TOTAL$exerciseIndex", totalSeconds)
            .apply()
    }

    fun getTimerEndTimestamp(context: Context, exerciseIndex: Int): Long? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ts = prefs.getLong("$KEY_PREFIX_TIMESTAMP$exerciseIndex", -1L)
        return if (ts > 0L) ts else null
    }

    fun getAllActiveTimers(context: Context): Map<Int, Long> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val map = mutableMapOf<Int, Long>()
        for ((key, value) in prefs.all) {
            if (key.startsWith(KEY_PREFIX_TIMESTAMP) && value is Long) {
                val indexStr = key.removePrefix(KEY_PREFIX_TIMESTAMP)
                val idx = indexStr.toIntOrNull()
                if (idx != null) {
                    map[idx] = value
                }
            }
        }
        return map
    }

    fun clearTimer(context: Context, exerciseIndex: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove("$KEY_PREFIX_TIMESTAMP$exerciseIndex")
            .remove("$KEY_PREFIX_TOTAL$exerciseIndex")
            .apply()
    }

    fun clearAllTimers(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
