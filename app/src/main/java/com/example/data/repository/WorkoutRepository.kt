package com.example.data.repository

import com.example.data.db.MeasurementDao
import com.example.data.db.WeightDao
import com.example.data.db.WorkoutDao
import com.example.data.model.BodyMeasurement
import com.example.data.model.WeightLog
import com.example.data.model.WorkoutSession
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(
    private val workoutDao: WorkoutDao,
    private val weightDao: WeightDao,
    private val measurementDao: MeasurementDao
) {
    val allSessions: Flow<List<WorkoutSession>> = workoutDao.getAllSessions()
    val allWeightsAsc: Flow<List<WeightLog>> = weightDao.getAllWeightsAsc()
    val latestWeight: Flow<WeightLog?> = weightDao.getLatestWeight()
    val allMeasurementsAsc: Flow<List<BodyMeasurement>> = measurementDao.getAllMeasurementsAsc()

    suspend fun saveSession(session: WorkoutSession): Long {
        return workoutDao.insertSession(session)
    }

    suspend fun updateSessionJson(id: Int, exercisesJson: String) {
        workoutDao.updateSessionJson(id, exercisesJson)
    }

    /**
     * Queries from a bounded recent set (most recent 60 sessions, covering ~3-4 months of training)
     * instead of fetching the entire history. This bound is intentional: progressive overload
     * calculations only need recent data points (not multi-year history), ensuring scalable and
     * high-performance database queries as history grows.
     */
    suspend fun getRecentSessionsForExercise(exerciseName: String, limit: Int = 10): List<WorkoutSession> {
        val recentSessions = workoutDao.getRecentSessionsList(limit = 60)
        val matching = mutableListOf<WorkoutSession>()
        for (session in recentSessions) {
            val json = session.exercisesJson
            if (json.contains("\"name\":\"$exerciseName\"") || json.contains(exerciseName)) {
                matching.add(session)
                if (matching.size >= limit) break
            }
        }
        return matching
    }

    suspend fun replaceAllSessions(sessions: List<WorkoutSession>) {
        workoutDao.replaceAllSessions(sessions)
    }

    suspend fun replaceAllWeights(weights: List<WeightLog>) {
        weightDao.replaceAllWeights(weights)
    }

    suspend fun replaceAllMeasurements(measurements: List<BodyMeasurement>) {
        measurementDao.replaceAllMeasurements(measurements)
    }

    suspend fun deleteSession(id: Int) {
        workoutDao.deleteSessionById(id)
    }

    suspend fun saveWeight(weight: WeightLog): Long {
        return weightDao.insertWeight(weight)
    }

    suspend fun deleteWeight(id: Int) {
        weightDao.deleteWeightById(id)
    }

    suspend fun saveMeasurement(measurement: BodyMeasurement): Long {
        return measurementDao.insertMeasurement(measurement)
    }

    suspend fun deleteMeasurement(id: Int) {
        measurementDao.deleteMeasurementById(id)
    }
}
