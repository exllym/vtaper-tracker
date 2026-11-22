package com.example.data.model

import androidx.compose.runtime.Immutable
import java.time.DayOfWeek
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject

enum class DayType(val code: String, val label: String) {
    UPPER("UPPER", "Upper Body"),
    LOWER("LOWER", "Lower Body"),
    REST("REST", "Rest Day"),
    LIGHT("LIGHT", "Light Activity")
}

@Immutable
data class DayInfo(
    val dayType: DayType,
    val indonesianDay: String,
    val dayLabel: String
)

enum class ExerciseInputType {
    WEIGHT_REPS,
    BODYWEIGHT_REPS,
    DURATION_ONLY
}

@Immutable
data class ExerciseDef(
    val name: String,
    val target: String,
    val setCount: Int,
    val restSeconds: Int,
    val category: String, // "compound" or "isolation"
    val muscleGroup: String,
    val inputType: ExerciseInputType = ExerciseInputType.WEIGHT_REPS
)

@Immutable
data class ExerciseSummary(
    val exerciseName: String,
    val inputType: ExerciseInputType = ExerciseInputType.WEIGHT_REPS,
    val sets: List<Pair<Float, Int>> // weightKg to reps
) {
    fun formatSets(): String {
        if (sets.isEmpty()) return ""
        return sets.joinToString(", ") { (weight, reps) ->
            when (inputType) {
                ExerciseInputType.BODYWEIGHT_REPS -> {
                    if (weight > 0f) {
                        val weightStr = if (weight % 1f == 0f) "${weight.toInt()}" else "$weight"
                        "+${weightStr}kg×$reps"
                    } else {
                        "$reps rep (bodyweight)"
                    }
                }
                ExerciseInputType.DURATION_ONLY -> {
                    "$reps detik"
                }
                ExerciseInputType.WEIGHT_REPS -> {
                    if (weight > 0f) {
                        val weightStr = if (weight % 1f == 0f) "${weight.toInt()}" else "$weight"
                        "${weightStr}kg×$reps"
                    } else {
                        "$reps rep"
                    }
                }
            }
        }
    }
}

@Immutable
data class SetInput(
    val setNumber: Int,
    val weightKg: String = "",
    val reps: String = "",
    val isDone: Boolean = false
)

@Immutable
data class RestTimerStatus(
    val isRunning: Boolean = false,
    val remainingSeconds: Int = 0,
    val isFinished: Boolean = false
)

@Immutable
data class ExerciseState(
    val def: ExerciseDef,
    val sets: List<SetInput>,
    val isExpanded: Boolean = false,
    val timerRemainingSeconds: Int? = null,
    val isTimerRunning: Boolean = false,
    val timerFinishedMessage: Boolean = false
) {
    val isAllSetsDone: Boolean
        get() = sets.isNotEmpty() && sets.all { it.isDone }

    val hasAnyCompletedSet: Boolean
        get() = sets.any { it.isDone || (it.weightKg.isNotBlank() && it.reps.isNotBlank()) }
}

object WorkoutProgram {
    val upperBodyExercises = listOf(
        ExerciseDef(
            name = "Pull-up / Lat Pulldown",
            target = "4 x 8-10",
            setCount = 4,
            restSeconds = 150,
            category = "compound",
            muscleGroup = "Lats, Upper Back",
            inputType = ExerciseInputType.BODYWEIGHT_REPS
        ),
        ExerciseDef(
            name = "Overhead Press",
            target = "4 x 8-10",
            setCount = 4,
            restSeconds = 150,
            category = "compound",
            muscleGroup = "Shoulders"
        ),
        ExerciseDef(
            name = "DB/Barbell Row",
            target = "4 x 8-10",
            setCount = 4,
            restSeconds = 150,
            category = "compound",
            muscleGroup = "Back"
        ),
        ExerciseDef(
            name = "Incline Bench Press",
            target = "4 x 8-10",
            setCount = 4,
            restSeconds = 150,
            category = "compound",
            muscleGroup = "Upper Chest"
        ),
        ExerciseDef(
            name = "Lateral Raise",
            target = "3 x 12-15",
            setCount = 3,
            restSeconds = 60,
            category = "isolation",
            muscleGroup = "Side Delts"
        ),
        ExerciseDef(
            name = "Curl + Tricep Pushdown (superset)",
            target = "3 x 12",
            setCount = 3,
            restSeconds = 60,
            category = "isolation",
            muscleGroup = "Arms"
        )
    )

    val lowerBodyExercises = listOf(
        ExerciseDef(
            name = "Squat",
            target = "4 x 6-8",
            setCount = 4,
            restSeconds = 150,
            category = "compound",
            muscleGroup = "Quads, Glutes"
        ),
        ExerciseDef(
            name = "Romanian Deadlift",
            target = "4 x 8-10",
            setCount = 4,
            restSeconds = 150,
            category = "compound",
            muscleGroup = "Hamstrings"
        ),
        ExerciseDef(
            name = "Leg Press / Lunges",
            target = "3 x 10",
            setCount = 3,
            restSeconds = 90,
            category = "compound",
            muscleGroup = "Quads"
        ),
        ExerciseDef(
            name = "Leg Curl (machine)",
            target = "3 x 12",
            setCount = 3,
            restSeconds = 60,
            category = "isolation",
            muscleGroup = "Hamstrings"
        ),
        ExerciseDef(
            name = "Calf Raise",
            target = "3 x 15",
            setCount = 3,
            restSeconds = 45,
            category = "isolation",
            muscleGroup = "Calves"
        ),
        ExerciseDef(
            name = "Core (Plank/Leg Raise)",
            target = "3 x 30-45 sec",
            setCount = 3,
            restSeconds = 45,
            category = "isolation",
            muscleGroup = "Core",
            inputType = ExerciseInputType.DURATION_ONLY
        )
    )

    fun getDayInfo(date: LocalDate = LocalDate.now()): DayInfo {
        return when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> DayInfo(DayType.UPPER, "SENIN", "Upper Body")
            DayOfWeek.TUESDAY -> DayInfo(DayType.LOWER, "SELASA", "Lower Body")
            DayOfWeek.WEDNESDAY -> DayInfo(DayType.REST, "RABU", "Rest Day")
            DayOfWeek.THURSDAY -> DayInfo(DayType.UPPER, "KAMIS", "Upper Body")
            DayOfWeek.FRIDAY -> DayInfo(DayType.LOWER, "JUMAT", "Lower Body")
            DayOfWeek.SATURDAY -> DayInfo(DayType.LIGHT, "SABTU", "Light Activity")
            DayOfWeek.SUNDAY -> DayInfo(DayType.LIGHT, "MINGGU", "Light Activity")
            null -> DayInfo(DayType.REST, "RABU", "Rest Day")
        }
    }

    fun getInitialExerciseStates(dayType: DayType): List<ExerciseState> {
        val exercises = when (dayType) {
            DayType.UPPER -> upperBodyExercises
            DayType.LOWER -> lowerBodyExercises
            else -> emptyList()
        }

        return exercises.map { def ->
            ExerciseState(
                def = def,
                sets = (1..def.setCount).map { setNum ->
                    SetInput(setNumber = setNum)
                },
                isExpanded = false
            )
        }
    }

    fun serializeExercises(exercises: List<ExerciseState>): String {
        val rootArray = JSONArray()
        for (exercise in exercises) {
            val exObj = JSONObject()
            exObj.put("name", exercise.def.name)
            exObj.put("category", exercise.def.category)
            exObj.put("muscleGroup", exercise.def.muscleGroup)
            val setsArray = JSONArray()
            for (set in exercise.sets) {
                val setObj = JSONObject()
                setObj.put("setNumber", set.setNumber)
                setObj.put("weightKg", set.weightKg)
                setObj.put("reps", set.reps)
                setObj.put("isDone", set.isDone)
                setsArray.put(setObj)
            }
            exObj.put("sets", setsArray)
            rootArray.put(exObj)
        }
        return rootArray.toString()
    }

    fun deserializeExercises(json: String, dayType: DayType): List<ExerciseState> {
        val defaultList = getInitialExerciseStates(dayType)
        if (json.isBlank()) return defaultList
        return try {
            val rootArray = JSONArray(json)
            val jsonMap = mutableMapOf<String, List<SetInput>>()
            for (i in 0 until rootArray.length()) {
                val exObj = rootArray.getJSONObject(i)
                val name = exObj.optString("name")
                val setsArray = exObj.optJSONArray("sets") ?: JSONArray()
                val sets = mutableListOf<SetInput>()
                for (j in 0 until setsArray.length()) {
                    val sObj = setsArray.getJSONObject(j)
                    sets.add(
                        SetInput(
                            setNumber = sObj.optInt("setNumber", j + 1),
                            weightKg = sObj.optString("weightKg", ""),
                            reps = sObj.optString("reps", ""),
                            isDone = sObj.optBoolean("isDone", false)
                        )
                    )
                }
                if (name.isNotBlank()) {
                    jsonMap[name] = sets
                }
            }

            defaultList.map { defaultEx ->
                val savedSets = jsonMap[defaultEx.def.name]
                if (savedSets != null && savedSets.isNotEmpty()) {
                    // Match default setCount
                    val reconciledSets = (1..defaultEx.def.setCount).map { setNum ->
                        savedSets.firstOrNull { it.setNumber == setNum } ?: SetInput(setNumber = setNum)
                    }
                    defaultEx.copy(sets = reconciledSets)
                } else {
                    defaultEx
                }
            }
        } catch (_: Exception) {
            defaultList
        }
    }

    fun countCompletedSets(exercisesJson: String): Int {
        return try {
            val rootArray = JSONArray(exercisesJson)
            var count = 0
            for (i in 0 until rootArray.length()) {
                val exObj = rootArray.getJSONObject(i)
                val setsArray = exObj.optJSONArray("sets") ?: continue
                for (j in 0 until setsArray.length()) {
                    val setObj = setsArray.getJSONObject(j)
                    if (setObj.optBoolean("isDone", false)) {
                        count++
                    }
                }
            }
            count
        } catch (_: Exception) {
            0
        }
    }

    fun parseExerciseSummaries(exercisesJson: String): List<ExerciseSummary> {
        if (exercisesJson.isBlank()) return emptyList()
        return try {
            val rootArray = JSONArray(exercisesJson)
            val summaries = mutableListOf<ExerciseSummary>()
            val allDefs = (upperBodyExercises + lowerBodyExercises).associateBy { it.name }

            for (i in 0 until rootArray.length()) {
                val exObj = rootArray.getJSONObject(i)
                val exName = exObj.optString("name")
                if (exName.isBlank()) continue

                val inputType = allDefs[exName]?.inputType ?: when {
                    exName.contains("Pull-up", ignoreCase = true) -> ExerciseInputType.BODYWEIGHT_REPS
                    exName.contains("Core", ignoreCase = true) || exName.contains("Plank", ignoreCase = true) -> ExerciseInputType.DURATION_ONLY
                    else -> ExerciseInputType.WEIGHT_REPS
                }

                val setsArray = exObj.optJSONArray("sets") ?: continue
                val sets = mutableListOf<Pair<Float, Int>>()
                for (j in 0 until setsArray.length()) {
                    val setObj = setsArray.getJSONObject(j)
                    val isDone = setObj.optBoolean("isDone", false)
                    val weightStr = setObj.optString("weightKg", "").trim()
                    val repsStr = setObj.optString("reps", "").trim()
                    val weight = weightStr.toFloatOrNull() ?: 0f
                    val reps = repsStr.toIntOrNull() ?: 0

                    // Include if marked isDone, or for BODYWEIGHT_REPS if reps is filled in (blank weight treated as 0),
                    // or for DURATION_ONLY if reps/seconds filled in, or for WEIGHT_REPS if both filled in
                    val hasData = when (inputType) {
                        ExerciseInputType.DURATION_ONLY -> reps > 0
                        ExerciseInputType.BODYWEIGHT_REPS -> reps > 0
                        ExerciseInputType.WEIGHT_REPS -> weightStr.isNotBlank() && repsStr.isNotBlank()
                    }

                    if (isDone || hasData) {
                        if (reps > 0 || weight > 0f || isDone) {
                            sets.add(weight to reps)
                        }
                    }
                }

                if (sets.isNotEmpty()) {
                    summaries.add(
                        ExerciseSummary(
                            exerciseName = exName,
                            inputType = inputType,
                            sets = sets
                        )
                    )
                }
            }
            summaries
        } catch (_: Exception) {
            emptyList()
        }
    }
}
