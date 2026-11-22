package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExerciseInputType
import com.example.data.model.ExerciseState
import com.example.data.model.OverloadConfidence
import com.example.data.model.OverloadSuggestion
import com.example.data.model.RestTimerStatus
import com.example.data.model.SetInput
import com.example.ui.theme.BorderDivider
import com.example.ui.theme.LowerBodyAccent
import com.example.ui.theme.OnPrimaryDark
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryCardBackground
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SuccessColor
import com.example.ui.theme.UpperBodyAccent
import com.example.ui.theme.guildPlaque

private val CardHeaderClickShape = RoundedCornerShape(8.dp)
private val OverloadBannerShape = RoundedCornerShape(8.dp)
private val RestButtonShape = RoundedCornerShape(14.dp)
private val AccentCursorBrush = SolidColor(UpperBodyAccent)
private val DecimalKeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
private val NumberKeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
private val SetNumericTextStyle = TextStyle(
    color = PrimaryText,
    fontSize = 15.sp,
    fontWeight = FontWeight.SemiBold,
    textAlign = TextAlign.Center
)

private fun Modifier.setBadgeBackground(): Modifier = drawWithCache {
    val corner = CornerRadius(10.dp.toPx(), 10.dp.toPx())
    onDrawBehind {
        drawRoundRect(
            color = SecondaryCardBackground,
            cornerRadius = corner
        )
    }
}

private fun Modifier.setInputBox(): Modifier = drawWithCache {
    val radius = 12.dp.toPx()
    val corner = CornerRadius(radius, radius)
    val strokePx = 1.dp.toPx()
    val halfStroke = strokePx * 0.5f
    val borderTopLeft = Offset(halfStroke, halfStroke)
    val borderSize = Size(
        width = (size.width - strokePx).coerceAtLeast(0f),
        height = (size.height - strokePx).coerceAtLeast(0f)
    )
    val stroke = Stroke(width = strokePx)
    onDrawBehind {
        drawRoundRect(
            color = SecondaryCardBackground,
            cornerRadius = corner
        )
        drawRoundRect(
            color = BorderDivider,
            topLeft = borderTopLeft,
            size = borderSize,
            cornerRadius = corner,
            style = stroke
        )
    }
}

@Immutable
private data class SetRowCallbacks(
    val onWeightChange: (String) -> Unit,
    val onRepsChange: (String) -> Unit,
    val onToggleDone: () -> Unit
)

@Immutable
private data class ExerciseSetCallbacksHolder(
    val callbacks: List<SetRowCallbacks>
)

@Immutable
private data class RestButtonDisplay(
    val text: String,
    val backgroundColor: Color,
    val textColor: Color,
    val showBorder: Boolean
)

@Composable
fun ExerciseCard(
    exercise: ExerciseState,
    exerciseIndex: Int,
    onToggleExpand: () -> Unit,
    onWeightChanged: (setIndex: Int, weight: String) -> Unit,
    onRepsChanged: (setIndex: Int, reps: String) -> Unit,
    onToggleSetDone: (setIndex: Int) -> Unit,
    onRestTimerTapped: () -> Unit,
    restTimerStatus: RestTimerStatus = RestTimerStatus(),
    overloadSuggestion: OverloadSuggestion? = null,
    modifier: Modifier = Modifier
) {
    val categoryColor = remember(exercise.def.category) {
        if (exercise.def.category.equals("compound", ignoreCase = true)) {
            UpperBodyAccent
        } else {
            LowerBodyAccent
        }
    }

    val allDone = exercise.isAllSetsDone
    val inputType = exercise.def.inputType

    val weightColumnHeader = remember(inputType) {
        if (inputType == ExerciseInputType.BODYWEIGHT_REPS) "BEBAN TAMBAHAN (KG)" else "BEBAN (KG)"
    }
    val repsColumnHeader = remember(inputType) {
        if (inputType == ExerciseInputType.DURATION_ONLY) "DETIK" else "REPS"
    }
    val weightPlaceholder = remember(inputType) {
        if (inputType == ExerciseInputType.BODYWEIGHT_REPS) "0 jika bodyweight" else "0.0"
    }
    val repsPlaceholder = remember(inputType) {
        if (inputType == ExerciseInputType.DURATION_ONLY) "detik" else "0"
    }
    val muscleGroupNote = remember(exercise.def.muscleGroup) {
        "Target Otot: ${exercise.def.muscleGroup}"
    }
    val overloadReasoning = remember(overloadSuggestion) {
        if (overloadSuggestion != null && overloadSuggestion.confidence != OverloadConfidence.LOW) {
            overloadSuggestion.reasoning
        } else {
            null
        }
    }
    val restButtonDisplay = remember(restTimerStatus, exercise.def.restSeconds) {
        val timerRunning = restTimerStatus.isRunning
        val finishedMessage = restTimerStatus.isFinished
        val remainingSeconds = if (timerRunning) restTimerStatus.remainingSeconds else exercise.def.restSeconds
        val buttonBg = when {
            finishedMessage -> SuccessColor
            timerRunning -> UpperBodyAccent
            else -> SecondaryCardBackground
        }
        val buttonTextColor = when {
            finishedMessage || timerRunning -> OnPrimaryDark
            else -> PrimaryText
        }
        val buttonText = when {
            finishedMessage -> "✓ Done, go on!"
            timerRunning -> "⏸ ${remainingSeconds}s"
            else -> "Rest ${exercise.def.restSeconds}s"
        }
        RestButtonDisplay(
            text = buttonText,
            backgroundColor = buttonBg,
            textColor = buttonTextColor,
            showBorder = !(timerRunning || finishedMessage)
        )
    }

    val setCount = exercise.sets.size
    val setCallbacksHolder = remember(setCount, onWeightChanged, onRepsChanged, onToggleSetDone) {
        ExerciseSetCallbacksHolder(
            callbacks = List(setCount) { setIndex ->
                SetRowCallbacks(
                    onWeightChange = { newWeight -> onWeightChanged(setIndex, newWeight) },
                    onRepsChange = { newReps -> onRepsChanged(setIndex, newReps) },
                    onToggleDone = { onToggleSetDone(setIndex) }
                )
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .guildPlaque(16.dp)
            .padding(16.dp)
            .testTag("exercise_card_$exerciseIndex")
    ) {
        // Collapsed Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardHeaderClickShape)
                .clickable(onClick = onToggleExpand)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Colored category dot (violet for compound, system blue for isolation)
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(categoryColor)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = exercise.def.name,
                        color = PrimaryText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = exercise.def.target,
                        color = SecondaryText,
                        fontSize = 13.sp
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Overall checkmark circle: empty = not all sets done, solid green = all sets checked
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (allDone) SuccessColor else Color.Transparent)
                        .border(
                            width = if (allDone) 0.dp else 1.5.dp,
                            color = if (allDone) Color.Transparent else SecondaryText.copy(alpha = 0.5f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (allDone) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selesai",
                            tint = OnPrimaryDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Chevron icon to indicate expand/collapse
                Icon(
                    imageVector = if (exercise.isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (exercise.isExpanded) "Tutup" else "Buka",
                    tint = SecondaryText,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Expanded Content
        AnimatedVisibility(
            visible = exercise.isExpanded,
            modifier = Modifier.clipToBounds(),
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = tween(durationMillis = 140)),
            exit = shrinkVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeOut(animationSpec = tween(durationMillis = 110))
        ) {
            ExerciseCardExpandedContent(
                exercise = exercise,
                exerciseIndex = exerciseIndex,
                inputType = inputType,
                weightColumnHeader = weightColumnHeader,
                repsColumnHeader = repsColumnHeader,
                weightPlaceholder = weightPlaceholder,
                repsPlaceholder = repsPlaceholder,
                muscleGroupNote = muscleGroupNote,
                overloadReasoning = overloadReasoning,
                restButtonDisplay = restButtonDisplay,
                setCallbacksHolder = setCallbacksHolder,
                onRestTimerTapped = onRestTimerTapped
            )
        }
    }
}

@Composable
private fun ExerciseCardExpandedContent(
    exercise: ExerciseState,
    exerciseIndex: Int,
    inputType: ExerciseInputType,
    weightColumnHeader: String,
    repsColumnHeader: String,
    weightPlaceholder: String,
    repsPlaceholder: String,
    muscleGroupNote: String,
    overloadReasoning: String?,
    restButtonDisplay: RestButtonDisplay,
    setCallbacksHolder: ExerciseSetCallbacksHolder,
    onRestTimerTapped: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        // Header labels for sets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SET",
                color = SecondaryText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.Center
            )
            if (inputType != ExerciseInputType.DURATION_ONLY) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = weightColumnHeader,
                    color = SecondaryText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = repsColumnHeader,
                color = SecondaryText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "DONE",
                color = SecondaryText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(42.dp),
                textAlign = TextAlign.Center
            )
        }

        // Rows for each set
        exercise.sets.forEachIndexed { setIndex, setInput ->
            key(setIndex) {
                val rowCallbacks = setCallbacksHolder.callbacks[setIndex]
                SetInputRow(
                    setInput = setInput,
                    exerciseIndex = exerciseIndex,
                    setIndex = setIndex,
                    inputType = inputType,
                    weightPlaceholder = weightPlaceholder,
                    repsPlaceholder = repsPlaceholder,
                    onWeightChange = rowCallbacks.onWeightChange,
                    onRepsChange = rowCallbacks.onRepsChange,
                    onToggleDone = rowCallbacks.onToggleDone
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Short muscleGroup note below the set inputs
        Text(
            text = muscleGroupNote,
            color = SecondaryText,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        // Progressive Overload Suggestion row (MEDIUM or HIGH confidence)
        if (overloadReasoning != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(OverloadBannerShape)
                    .background(SecondaryCardBackground.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = "Saran Progressive Overload",
                    tint = UpperBodyAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = overloadReasoning,
                    color = PrimaryText.copy(alpha = 0.9f),
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Rest Timer button
        Button(
            onClick = onRestTimerTapped,
            shape = RestButtonShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = restButtonDisplay.backgroundColor,
                contentColor = restButtonDisplay.textColor
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .border(
                    width = if (restButtonDisplay.showBorder) 1.dp else 0.dp,
                    color = BorderDivider,
                    shape = RestButtonShape
                )
                .testTag("rest_timer_$exerciseIndex")
        ) {
            Text(
                text = restButtonDisplay.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SetInputRow(
    setInput: SetInput,
    exerciseIndex: Int,
    setIndex: Int,
    inputType: ExerciseInputType,
    weightPlaceholder: String,
    repsPlaceholder: String,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onToggleDone: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Set number badge
        Box(
            modifier = Modifier
                .size(36.dp)
                .setBadgeBackground(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${setInput.setNumber}",
                color = PrimaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Numeric input "Weight (kg)" (omitted for DURATION_ONLY)
        if (inputType != ExerciseInputType.DURATION_ONLY) {
            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .setInputBox()
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (setInput.weightKg.isEmpty()) {
                    Text(
                        text = weightPlaceholder,
                        color = SecondaryText.copy(alpha = 0.4f),
                        fontSize = if (inputType == ExerciseInputType.BODYWEIGHT_REPS) 11.sp else 15.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
                BasicTextField(
                    value = setInput.weightKg,
                    onValueChange = onWeightChange,
                    keyboardOptions = DecimalKeyboardOptions,
                    textStyle = SetNumericTextStyle,
                    singleLine = true,
                    cursorBrush = AccentCursorBrush,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("set_weight_${exerciseIndex}_$setIndex")
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Numeric input "Reps" or "Detik"
        Box(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .setInputBox()
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            if (setInput.reps.isEmpty()) {
                Text(
                    text = repsPlaceholder,
                    color = SecondaryText.copy(alpha = 0.4f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
            BasicTextField(
                value = setInput.reps,
                onValueChange = onRepsChange,
                keyboardOptions = NumberKeyboardOptions,
                textStyle = SetNumericTextStyle,
                singleLine = true,
                cursorBrush = AccentCursorBrush,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("set_reps_${exerciseIndex}_$setIndex")
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Round checkbox/button to mark this set as done (turns solid green when tapped)
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (setInput.isDone) SuccessColor else SecondaryCardBackground)
                .border(
                    width = if (setInput.isDone) 0.dp else 1.5.dp,
                    color = if (setInput.isDone) Color.Transparent else SecondaryText.copy(alpha = 0.5f),
                    shape = CircleShape
                )
                .clickable(onClick = onToggleDone)
                .testTag("set_done_${exerciseIndex}_$setIndex"),
            contentAlignment = Alignment.Center
        ) {
            if (setInput.isDone) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Set Selesai",
                    tint = OnPrimaryDark,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
