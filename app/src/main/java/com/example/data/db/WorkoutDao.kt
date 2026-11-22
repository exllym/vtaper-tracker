package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.WorkoutSession
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions ORDER BY timestamp DESC")
    suspend fun getAllSessionsList(): List<WorkoutSession>

    @Query("SELECT * FROM workout_sessions ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentSessionsList(limit: Int = 60): List<WorkoutSession>

    @Query("SELECT * FROM workout_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Int): WorkoutSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSessions(sessions: List<WorkoutSession>)

    @Query("UPDATE workout_sessions SET exercisesJson = :exercisesJson WHERE id = :id")
    suspend fun updateSessionJson(id: Int, exercisesJson: String)

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Int)

    @Query("DELETE FROM workout_sessions")
    suspend fun deleteAllSessions()

    @Transaction
    suspend fun replaceAllSessions(sessions: List<WorkoutSession>) {
        deleteAllSessions()
        insertAllSessions(sessions)
    }
}
