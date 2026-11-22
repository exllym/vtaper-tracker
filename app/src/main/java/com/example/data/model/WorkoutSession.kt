package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String, // format "yyyy-MM-dd"
    val dayType: String, // "UPPER" / "LOWER"
    val dayLabel: String,
    val timestamp: Long = System.currentTimeMillis(),
    val exercisesJson: String
)
