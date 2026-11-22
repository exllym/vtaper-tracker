package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.BodyMeasurement
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM body_measurements ORDER BY timestamp ASC")
    fun getAllMeasurementsAsc(): Flow<List<BodyMeasurement>>

    @Query("SELECT * FROM body_measurements ORDER BY timestamp DESC")
    fun getAllMeasurementsDesc(): Flow<List<BodyMeasurement>>

    @Query("SELECT * FROM body_measurements ORDER BY timestamp DESC")
    suspend fun getAllMeasurementsList(): List<BodyMeasurement>

    @Query("SELECT * FROM body_measurements ORDER BY timestamp ASC")
    suspend fun getAll(): List<BodyMeasurement>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(measurement: BodyMeasurement): Long

    @Query("DELETE FROM body_measurements WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurement(measurement: BodyMeasurement): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMeasurements(measurements: List<BodyMeasurement>)

    @Query("DELETE FROM body_measurements WHERE id = :id")
    suspend fun deleteMeasurementById(id: Int)

    @Query("DELETE FROM body_measurements")
    suspend fun deleteAllMeasurements()

    @Transaction
    suspend fun replaceAllMeasurements(measurements: List<BodyMeasurement>) {
        deleteAllMeasurements()
        insertAllMeasurements(measurements)
    }
}
