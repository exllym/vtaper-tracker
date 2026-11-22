package com.example.data.model

import androidx.compose.runtime.Immutable
import org.json.JSONArray
import kotlin.math.roundToInt

enum class OverloadConfidence {
    LOW,
    MEDIUM,
    HIGH
}

@Immutable
data class OverloadSuggestion(
    val suggestedWeightKg: Float? = null,
    val reasoning: String,
    val confidence: OverloadConfidence = OverloadConfidence.LOW,
    val monthlyProjection: String? = null
)

object ProgressiveOverloadCalculator {

    private data class LoggedSet(
        val weightKg: Float,
        val reps: Int
    )

    private data class SessionExerciseData(
        val date: String,
        val timestamp: Long,
        val validSets: List<LoggedSet>
    )

    /**
     * Parse target rep range string, e.g.:
     * "4 x 8-10" -> 10
     * "3 x 15-20" -> 20
     * "3 x 12" -> 12
     * "3 x 30-45 sec" -> 45
     */
    fun extractTopRep(targetRepRange: String): Int {
        val parts = targetRepRange.split("x")
        val repPart = if (parts.size >= 2) parts[1].trim() else targetRepRange.trim()
        val cleaned = repPart.replace("sec", "").replace("detik", "").trim()
        val dashParts = cleaned.split("-")
        return if (dashParts.size >= 2) {
            dashParts[1].filter { it.isDigit() }.toIntOrNull() ?: 10
        } else {
            cleaned.filter { it.isDigit() }.toIntOrNull() ?: 10
        }
    }

    fun suggestNextWeight(
        recentSessions: List<WorkoutSession>,
        exerciseName: String,
        currentTargetRepRange: String,
        inputType: ExerciseInputType = ExerciseInputType.WEIGHT_REPS
    ): OverloadSuggestion {
        val topRep = extractTopRep(currentTargetRepRange)

        // Parse valid sets for this exercise per session
        val parsedSessions = mutableListOf<SessionExerciseData>()
        for (session in recentSessions) {
            val validSets = extractSetsForExercise(session.exercisesJson, exerciseName, inputType)
            if (validSets.isNotEmpty()) {
                parsedSessions.add(
                    SessionExerciseData(
                        date = session.date,
                        timestamp = session.timestamp,
                        validSets = validSets
                    )
                )
            }
        }

        if (parsedSessions.size < 2) {
            val emptyReason = when (inputType) {
                ExerciseInputType.DURATION_ONLY -> "Belum cukup data, catat durasi latihan minimal 2 sesi dulu"
                else -> "Belum cukup data, catat progres latihan minimal 2 sesi dulu"
            }
            return OverloadSuggestion(
                suggestedWeightKg = null,
                reasoning = emptyReason,
                confidence = OverloadConfidence.LOW
            )
        }

        // Ensure sessions are evaluated newest-first regardless of caller ordering
        val orderedSessions = parsedSessions.sortedWith(
            compareByDescending<SessionExerciseData> { it.date }
                .thenByDescending { it.timestamp }
        )

        val sessionCount = orderedSessions.size
        val confidence = if (sessionCount >= 4) OverloadConfidence.HIGH else OverloadConfidence.MEDIUM

        val mostRecent = orderedSessions[0]
        val secondRecent = orderedSessions[1]

        val latestAllHitTop = mostRecent.validSets.all { it.reps >= topRep }
        val secondAllHitTop = secondRecent.validSets.all { it.reps >= topRep }
        val latestMinRep = mostRecent.validSets.minOfOrNull { it.reps } ?: 0
        val latestMaxRep = mostRecent.validSets.maxOfOrNull { it.reps } ?: 0
        val setsHitTop = mostRecent.validSets.count { it.reps >= topRep }
        val totalValidSets = mostRecent.validSets.size

        // Specific handling for DURATION_ONLY (e.g. Plank)
        if (inputType == ExerciseInputType.DURATION_ONLY) {
            if (latestAllHitTop && secondAllHitTop) {
                val nextTargetSeconds = topRep + 5
                val reason = "Sudah 2 sesi capai $topRep detik — coba $nextTargetSeconds detik minggu depan! ⏱️"
                return OverloadSuggestion(
                    suggestedWeightKg = null,
                    reasoning = reason,
                    confidence = confidence
                )
            } else if (latestAllHitTop && !secondAllHitTop) {
                val achievedText = if (latestMinRep > topRep) "$latestMinRep+ detik (target $topRep detik)" else "$topRep detik"
                val reason = "Sesi terakhir sukses capai $achievedText! Ulangi 1 sesi lagi dengan target ini sebelum menambah durasi."
                return OverloadSuggestion(
                    suggestedWeightKg = null,
                    reasoning = reason,
                    confidence = confidence
                )
            } else {
                val reason = if (setsHitTop > 0) {
                    "$setsHitTop/$totalValidSets set capai $topRep+ detik (terendah $latestMinRep/$topRep detik) — samakan semua set minimal $topRep detik sebelum menambah durasi."
                } else {
                    "Masih di bawah target ($latestMaxRep/$topRep detik) — pertahankan durasi saat ini hingga mampu capai $topRep detik."
                }
                return OverloadSuggestion(
                    suggestedWeightKg = null,
                    reasoning = reason,
                    confidence = confidence
                )
            }
        }

        val latestAvgWeight = mostRecent.validSets.map { it.weightKg }.average().toFloat()
        val secondAvgWeight = secondRecent.validSets.map { it.weightKg }.average().toFloat()

        // Specific handling for BODYWEIGHT_REPS with 0 added weight (pure bodyweight)
        if (inputType == ExerciseInputType.BODYWEIGHT_REPS && latestAvgWeight < 0.25f) {
            val secondPureBodyweight = secondAvgWeight < 0.25f
            if (latestAllHitTop && secondAllHitTop && secondPureBodyweight) {
                val reason = "Sudah 2 sesi berturut-turut capai $topRep reps bodyweight — saatnya coba tambah beban 1-2 kg ekstra (atau variasi lebih sulit) minggu depan! 💪"
                return OverloadSuggestion(
                    suggestedWeightKg = 1.0f,
                    reasoning = reason,
                    confidence = confidence
                )
            } else if (latestAllHitTop) {
                val achievedText = if (latestMinRep > topRep) "$latestMinRep+ reps (target $topRep)" else "$topRep reps"
                val reason = "Sesi terakhir sukses capai $achievedText bodyweight! Ulangi 1 sesi lagi dengan repetisi ini sebelum tambah beban."
                return OverloadSuggestion(
                    suggestedWeightKg = 0f,
                    reasoning = reason,
                    confidence = confidence
                )
            } else {
                val reason = if (setsHitTop > 0) {
                    "$setsHitTop/$totalValidSets set capai $topRep+ reps bodyweight (terendah $latestMinRep/$topRep reps) — samakan semua set minimal $topRep reps sebelum tambah beban."
                } else {
                    "Masih di bawah rep atas target ($latestMaxRep/$topRep reps) — fokus capai $topRep reps bodyweight dulu sebelum tambah beban."
                }
                return OverloadSuggestion(
                    suggestedWeightKg = 0f,
                    reasoning = reason,
                    confidence = confidence
                )
            }
        }

        // Standard WEIGHT_REPS or BODYWEIGHT_REPS with added weight > 0
        val weightsSimilar = kotlin.math.abs(latestAvgWeight - secondAvgWeight) <= 1.0f
        val projection = computeMonthlyProjection(orderedSessions)
        val formattedLatest = if (latestAvgWeight % 1f == 0f) "${latestAvgWeight.toInt()}" else "$latestAvgWeight"
        val prefix = if (inputType == ExerciseInputType.BODYWEIGHT_REPS) "beban tambahan +" else ""

        if (latestAllHitTop && secondAllHitTop && weightsSimilar) {
            // Suggest 5-10% increase, rounded to 0.5kg or 1kg
            val increment = when {
                latestAvgWeight < 10f -> 0.5f
                latestAvgWeight < 30f -> 1.0f
                latestAvgWeight < 60f -> 2.0f
                else -> 2.5f
            }
            val newSuggestedWeight = (latestAvgWeight + increment).let {
                (it * 2).roundToInt() / 2.0f
            }
            val formattedWeight = if (newSuggestedWeight % 1f == 0f) "${newSuggestedWeight.toInt()}" else "$newSuggestedWeight"
            val achievedSummary = if (latestMinRep > topRep) "minimal $latestMinRep reps, target $topRep" else "$topRep reps"

            val reason = "Sudah 2 sesi berturut-turut capai rep atas ($achievedSummary) di ${prefix}${formattedLatest}kg — saatnya naik ke ${prefix}${formattedWeight}kg minggu depan! 💪"
            return OverloadSuggestion(
                suggestedWeightKg = newSuggestedWeight,
                reasoning = if (projection != null) "$reason ($projection)" else reason,
                confidence = confidence,
                monthlyProjection = projection
            )
        } else if (latestAllHitTop) {
            val achievedSummary = if (latestMinRep > topRep) "$latestMinRep+ reps (target $topRep)" else "$topRep reps"
            val reason = if (secondAllHitTop && !weightsSimilar) {
                "Sesi terakhir sukses capai $achievedSummary di beban baru ${prefix}${formattedLatest}kg — ulangi 1 sesi lagi di beban ini sebelum naik beban."
            } else {
                "Sesi terakhir sukses capai $achievedSummary di ${prefix}${formattedLatest}kg! Ulangi 1 sesi lagi di beban ini sebelum naik beban."
            }
            return OverloadSuggestion(
                suggestedWeightKg = latestAvgWeight,
                reasoning = if (projection != null) "$reason ($projection)" else reason,
                confidence = confidence,
                monthlyProjection = projection
            )
        } else {
            val reason = if (setsHitTop > 0) {
                "$setsHitTop/$totalValidSets set capai $topRep+ reps (terendah $latestMinRep/$topRep reps di ${prefix}${formattedLatest}kg) — samakan semua set minimal $topRep reps sebelum naik beban."
            } else {
                "Masih di bawah rep atas target ($latestMaxRep/$topRep reps di ${prefix}${formattedLatest}kg) — fokus tambah rep dulu sebelum naik beban."
            }
            return OverloadSuggestion(
                suggestedWeightKg = latestAvgWeight,
                reasoning = if (projection != null) "$reason ($projection)" else reason,
                confidence = confidence,
                monthlyProjection = projection
            )
        }
    }

    private fun computeMonthlyProjection(sessions: List<SessionExerciseData>): String? {
        if (sessions.size < 3) return null
        val chronological = sessions.sortedBy { it.timestamp }
        val firstWeight = chronological.first().validSets.map { it.weightKg }.average().toFloat()
        val lastWeight = chronological.last().validSets.map { it.weightKg }.average().toFloat()

        if (firstWeight <= 0f && lastWeight <= 0f) return null

        val timeDiffMillis = chronological.last().timestamp - chronological.first().timestamp
        val timeDiffWeeks = (timeDiffMillis / (1000.0 * 60 * 60 * 24 * 7)).coerceAtLeast(1.0)
        val weightDiff = lastWeight - firstWeight

        if (weightDiff > 0.4f) {
            val weeklyRate = (weightDiff / timeDiffWeeks).toFloat()
            val fourWeekProjected = lastWeight + (weeklyRate * 4f)
            val rounded = (fourWeekProjected * 2).roundToInt() / 2.0f
            val formatted = if (rounded % 1f == 0f) "${rounded.toInt()}" else "$rounded"
            return "Perkiraan ~1 bulan lagi: sekitar ${formatted}kg"
        }
        return null
    }

    private fun extractSetsForExercise(
        exercisesJson: String,
        targetExerciseName: String,
        inputType: ExerciseInputType
    ): List<LoggedSet> {
        val result = mutableListOf<LoggedSet>()
        try {
            val rootArray = JSONArray(exercisesJson)
            for (i in 0 until rootArray.length()) {
                val exObj = rootArray.getJSONObject(i)
                val name = exObj.optString("name")
                if (name.equals(targetExerciseName, ignoreCase = true) ||
                    name.contains(targetExerciseName, ignoreCase = true) ||
                    targetExerciseName.contains(name, ignoreCase = true)) {
                    val setsArray = exObj.optJSONArray("sets") ?: continue
                    for (j in 0 until setsArray.length()) {
                        val setObj = setsArray.getJSONObject(j)
                        val weightStr = setObj.optString("weightKg", "").trim()
                        val repsStr = setObj.optString("reps", "").trim()
                        val isDone = setObj.optBoolean("isDone", false)
                        val w = weightStr.toFloatOrNull() ?: 0f
                        val r = repsStr.toIntOrNull() ?: 0

                        val isValid = when (inputType) {
                            ExerciseInputType.DURATION_ONLY -> r > 0 || (isDone && r > 0)
                            ExerciseInputType.BODYWEIGHT_REPS -> r > 0 || isDone
                            ExerciseInputType.WEIGHT_REPS -> (w > 0f && r > 0) || (isDone && w > 0f && r > 0)
                        }

                        if (isValid && r > 0) {
                            result.add(
                                LoggedSet(
                                    weightKg = if (inputType == ExerciseInputType.DURATION_ONLY) 0f else w,
                                    reps = r
                                )
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) { }
        return result
    }
}
