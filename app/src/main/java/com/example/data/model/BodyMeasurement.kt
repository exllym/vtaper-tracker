package com.example.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "body_measurements")
data class BodyMeasurement(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String, // format "yyyy-MM-dd"
    val shoulderCm: Float,
    val waistCm: Float,
    val timestamp: Long = System.currentTimeMillis()
)
