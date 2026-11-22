package com.example.data

import android.content.Context

object WeightGoalManager {
    private const val PREFS_NAME = "weight_goals_prefs"
    private const val KEY_START_WEIGHT = "start_weight_kg"
    private const val KEY_TARGET_WEIGHT = "target_weight_kg"

    const val DEFAULT_START_WEIGHT = 38f
    const val DEFAULT_TARGET_WEIGHT = 50f

    fun getStartWeight(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(KEY_START_WEIGHT, DEFAULT_START_WEIGHT)
    }

    fun getTargetWeight(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(KEY_TARGET_WEIGHT, DEFAULT_TARGET_WEIGHT)
    }

    fun saveWeightGoals(context: Context, startWeight: Float, targetWeight: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat(KEY_START_WEIGHT, startWeight)
            .putFloat(KEY_TARGET_WEIGHT, targetWeight)
            .apply()
    }
}
