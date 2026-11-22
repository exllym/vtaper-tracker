package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weight_logs")
data class WeightLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String, // format "yyyy-MM-dd"
    val kg: Float,
    val timestamp: Long = System.currentTimeMillis()
)
