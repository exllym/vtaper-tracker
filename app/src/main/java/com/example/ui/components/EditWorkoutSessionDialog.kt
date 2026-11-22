package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ExerciseInputType
import com.example.data.model.WorkoutProgram
import com.example.data.model.WorkoutSession
import com.example.ui.theme.BorderDivider
import com.example.ui.theme.CardBackground
import com.example.ui.theme.GuildTitle
import com.example.ui.theme.OnPrimaryDark
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.RestDayAccent
import com.example.ui.theme.SecondaryCardBackground
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.UpperBodyAccent
import com.example.ui.theme.guildPlaque
import org.json.JSONArray
import org.json.JSONObject

@Immutable
private data class EditableSet(
    val setNumber: Int,
    val weightKg: String,
    val reps: String,
    val isDone: Boolean
)

@Immutable
private data class EditableExercise(
    val name: String,
    val inputType: ExerciseInputType,
    val sets: List<EditableSet>
)

@Composable
fun EditWorkoutSessionDialog(
    session: WorkoutSession,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val allDefs = remember {
        (WorkoutProgram.upperBodyExercises + WorkoutProgram.lowerBodyExercises).associateBy { it.name }
    }

    val editableExercises = remember(session.exercisesJson) {
        try {
            val rootArray = JSONArray(session.exercisesJson)
            val list = mutableListOf<EditableExercise>()
            for (i in 0 until rootArray.length()) {
                val exObj = rootArray.getJSONObject(i)
                val exName = exObj.optString("name")
                val setsArray = exObj.optJSONArray("sets") ?: JSONArray()
                val inputType = allDefs[exName]?.inputType ?: when {
                    exName.contains("Pull-up", ignoreCase = true) -> ExerciseInputType.BODYWEIGHT_REPS
                    exName.contains("Core", ignoreCase = true) || exName.contains("Plank", ignoreCase = true) -> ExerciseInputType.DURATION_ONLY
                    else -> ExerciseInputType.WEIGHT_REPS
                }
                val sets = mutableListOf<EditableSet>()
                for (j in 0 until setsArray.length()) {
                    val sObj = setsArray.getJSONObject(j)
                    sets.add(
                        EditableSet(
                            setNumber = sObj.optInt("setNumber", j + 1),
                            weightKg = sObj.optString("weightKg", ""),
                            reps = sObj.optString("reps", ""),
                            isDone = sObj.optBoolean("isDone", true)
                        )
                    )
                }
                list.add(EditableExercise(name = exName, inputType = inputType, sets = sets))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    // Keep state in remember so recompositions reflect edits
    val exercisesState = remember {
        editableExercises.map { ex ->
            ex.name to (ex.inputType to ex.sets.map { s ->
                mutableStateOf(s.weightKg) to mutableStateOf(s.reps)
            })
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .guildPlaque(20.dp)
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        GuildTitle(
                            text = "EDIT SESI LATIHAN",
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${session.dayLabel} • ${session.date}",
                            color = SecondaryText,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable exercise set inputs
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    exercisesState.forEach { (exName, pair) ->
                        val (inputType, setStates) = pair
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SecondaryCardBackground)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = exName,
                                color = PrimaryText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            setStates.forEachIndexed { sIdx, (wState, rState) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Set ${sIdx + 1}",
                                        color = SecondaryText,
                                        fontSize = 12.sp,
                                        modifier = Modifier.width(42.dp)
                                    )

                                    when (inputType) {
                                        ExerciseInputType.DURATION_ONLY -> {
                                            // Only seconds input
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(40.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(CardBackground)
                                                    .border(1.dp, BorderDivider, RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 10.dp),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                if (rState.value.isEmpty()) {
                                                    Text(text = "Detik", color = SecondaryText.copy(alpha = 0.5f), fontSize = 12.sp)
                                                }
                                                BasicTextField(
                                                    value = rState.value,
                                                    onValueChange = { rState.value = it },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    textStyle = androidx.compose.ui.text.TextStyle(color = PrimaryText, fontSize = 13.sp),
                                                    cursorBrush = SolidColor(UpperBodyAccent),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }

                                        ExerciseInputType.BODYWEIGHT_REPS -> {
                                            // Added weight + Reps
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(40.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(CardBackground)
                                                    .border(1.dp, BorderDivider, RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 10.dp),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                if (wState.value.isEmpty()) {
                                                    Text(text = "+kg (beban)", color = SecondaryText.copy(alpha = 0.5f), fontSize = 12.sp)
                                                }
                                                BasicTextField(
                                                    value = wState.value,
                                                    onValueChange = { wState.value = it },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    singleLine = true,
                                                    textStyle = androidx.compose.ui.text.TextStyle(color = PrimaryText, fontSize = 13.sp),
                                                    cursorBrush = SolidColor(UpperBodyAccent),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(40.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(CardBackground)
                                                    .border(1.dp, BorderDivider, RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 10.dp),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                if (rState.value.isEmpty()) {
                                                    Text(text = "Reps", color = SecondaryText.copy(alpha = 0.5f), fontSize = 12.sp)
                                                }
                                                BasicTextField(
                                                    value = rState.value,
                                                    onValueChange = { rState.value = it },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    textStyle = androidx.compose.ui.text.TextStyle(color = PrimaryText, fontSize = 13.sp),
                                                    cursorBrush = SolidColor(UpperBodyAccent),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }

                                        ExerciseInputType.WEIGHT_REPS -> {
                                            // Weight (kg) + Reps
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(40.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(CardBackground)
                                                    .border(1.dp, BorderDivider, RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 10.dp),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                if (wState.value.isEmpty()) {
                                                    Text(text = "Berat (kg)", color = SecondaryText.copy(alpha = 0.5f), fontSize = 12.sp)
                                                }
                                                BasicTextField(
                                                    value = wState.value,
                                                    onValueChange = { wState.value = it },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    singleLine = true,
                                                    textStyle = androidx.compose.ui.text.TextStyle(color = PrimaryText, fontSize = 13.sp),
                                                    cursorBrush = SolidColor(UpperBodyAccent),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(40.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(CardBackground)
                                                    .border(1.dp, BorderDivider, RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 10.dp),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                if (rState.value.isEmpty()) {
                                                    Text(text = "Reps", color = SecondaryText.copy(alpha = 0.5f), fontSize = 12.sp)
                                                }
                                                BasicTextField(
                                                    value = rState.value,
                                                    onValueChange = { rState.value = it },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    textStyle = androidx.compose.ui.text.TextStyle(color = PrimaryText, fontSize = 13.sp),
                                                    cursorBrush = SolidColor(UpperBodyAccent),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: Batal / Simpan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryCardBackground),
                        border = BorderStroke(1.5.dp, RestDayAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text(text = "Batal", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val newRootArray = JSONArray()
                            exercisesState.forEach { (exName, pair) ->
                                val (inputType, setStates) = pair
                                val exObj = JSONObject()
                                exObj.put("name", exName)
                                val setsArr = JSONArray()
                                setStates.forEachIndexed { idx, (wState, rState) ->
                                    val setObj = JSONObject()
                                    setObj.put("setNumber", idx + 1)
                                    val wVal = if (inputType == ExerciseInputType.DURATION_ONLY) "0" else wState.value.trim()
                                    val rVal = rState.value.trim()
                                    setObj.put("weightKg", wVal)
                                    setObj.put("reps", rVal)
                                    // Mark done if reps/duration filled in
                                    val hasInput = rVal.isNotBlank() && (rVal.toIntOrNull() ?: 0) > 0
                                    setObj.put("isDone", hasInput)
                                    setsArr.put(setObj)
                                }
                                exObj.put("sets", setsArr)
                                newRootArray.put(exObj)
                            }
                            onSave(newRootArray.toString())
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = UpperBodyAccent,
                            contentColor = OnPrimaryDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("save_edited_session_button")
                    ) {
                        Text(text = "Simpan", color = OnPrimaryDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
