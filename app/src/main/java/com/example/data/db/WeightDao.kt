package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.WeightLog
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_logs ORDER BY timestamp ASC")
    fun getAllWeightsAsc(): Flow<List<WeightLog>>

    @Query("SELECT * FROM weight_logs ORDER BY timestamp DESC")
    fun getAllWeightsDesc(): Flow<List<WeightLog>>

    @Query("SELECT * FROM weight_logs ORDER BY timestamp DESC LIMIT 1")
    fun getLatestWeight(): Flow<WeightLog?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(weight: WeightLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllWeights(weights: List<WeightLog>)

    @Query("DELETE FROM weight_logs WHERE id = :id")
    suspend fun deleteWeightById(id: Int)

    @Query("DELETE FROM weight_logs")
    suspend fun deleteAllWeights()

    @Transaction
    suspend fun replaceAllWeights(weights: List<WeightLog>) {
        deleteAllWeights()
        insertAllWeights(weights)
    }
}
