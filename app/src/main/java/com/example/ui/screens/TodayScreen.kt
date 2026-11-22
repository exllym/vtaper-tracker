package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DayInfo
import com.example.data.model.DayType
import com.example.data.model.ExerciseState
import com.example.data.model.OverloadSuggestion
import com.example.data.model.RestTimerStatus
import com.example.ui.components.ExerciseCard
import com.example.ui.theme.BorderDivider
import com.example.ui.theme.GuildTitle
import com.example.ui.theme.LowerBodyAccent
import com.example.ui.theme.MainBackground
import com.example.ui.theme.OnPrimaryDark
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.RestDayAccent
import com.example.ui.theme.SecondaryCardBackground
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.UpperBodyAccent
import com.example.ui.theme.guildPlaque
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun TodayScreen(
    dayInfo: DayInfo,
    exerciseStates: List<ExerciseState>,
    onToggleExpand: (Int) -> Unit,
    onWeightChanged: (exerciseIndex: Int, setIndex: Int, weight: String) -> Unit,
    onRepsChanged: (exerciseIndex: Int, setIndex: Int, reps: String) -> Unit,
    onToggleSetDone: (exerciseIndex: Int, setIndex: Int) -> Unit,
    onRestTimerTapped: (Int) -> Unit,
    onFinishWorkout: () -> Unit,
    selectedWorkoutOverride: DayType? = null,
    onSelectWorkoutOverride: (DayType?) -> Unit = {},
    restTimerStateFlow: StateFlow<Map<Int, RestTimerStatus>>? = null,
    restTimerState: Map<Int, RestTimerStatus> = emptyMap(),
    overloadSuggestions: Map<String, OverloadSuggestion> = emptyMap(),
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MainBackground)
    ) {
        TodayWorkoutSelector(
            selectedOverride = selectedWorkoutOverride,
            onSelectOverride = onSelectWorkoutOverride,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (dayInfo.dayType == DayType.REST || dayInfo.dayType == DayType.LIGHT) {
            // CONDITION: If day type is REST or LIGHT: Show large centered card + missed workout hint
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                val emoji = if (dayInfo.dayType == DayType.REST) "😴" else "🚶"
                val title = if (dayInfo.dayType == DayType.REST) "FULL REST DAY" else "LIGHT ACTIVITY"
                val desc = if (dayInfo.dayType == DayType.REST) {
                    "Hari istirahat penuh! Otot bertumbuh saat kamu beristirahat dan makan cukup, bukan saat mengangkat beban. Pastikan asupan protein, kalori surplus lean bulk, dan tidur 7-9 jam berkualitas."
                } else {
                    "Hari aktivitas ringan. Luangkan waktu untuk jalan santai 20-30 menit, peregangan dinamis, atau foam rolling untuk melancarkan sirkulasi nutrisi ke otot tanpa menambah kelelahan sistem saraf."
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .guildPlaque(16.dp)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 64.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        GuildTitle(
                            text = title,
                            fontSize = 21.sp,
                            horizontalAlignment = Alignment.CenterHorizontally
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = desc,
                            color = SecondaryText,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Ada latihan yang terlewat? Pilih Upper atau Lower di atas.",
                        color = SecondaryText,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .testTag("missed_workout_hint")
                    )
                }
            }
        } else {
            // CONDITION: If day type is UPPER or LOWER, show list of exercise cards
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "today_top_spacer", contentType = "spacer") {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                itemsIndexed(
                    items = exerciseStates,
                    key = { _, item -> item.def.name },
                    contentType = { _, _ -> "exercise_card" }
                ) { index, exerciseState ->
                    ExerciseCardItem(
                        exerciseState = exerciseState,
                        exerciseIndex = index,
                        restTimerStateFlow = restTimerStateFlow,
                        fallbackRestTimerStatus = restTimerState[index] ?: RestTimerStatus(),
                        overloadSuggestion = overloadSuggestions[exerciseState.def.name],
                        onToggleExpand = onToggleExpand,
                        onWeightChanged = onWeightChanged,
                        onRepsChanged = onRepsChanged,
                        onToggleSetDone = onToggleSetDone,
                        onRestTimerTapped = onRestTimerTapped
                    )
                }

                item(key = "today_finish_footer", contentType = "finish_button") {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Large full-width gold accent button labeled "Selesai Latihan & Simpan"
                    Button(
                        onClick = onFinishWorkout,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = UpperBodyAccent,
                            contentColor = OnPrimaryDark
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("finish_workout_button")
                    ) {
                        Text(
                            text = "Selesai Latihan & Simpan",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(36.dp))
                }
            }
        }
    }
}

@Composable
private fun TodayWorkoutSelector(
    selectedOverride: DayType?,
    onSelectOverride: (DayType?) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeOverrideColor = when (selectedOverride) {
        DayType.UPPER -> UpperBodyAccent
        DayType.LOWER -> LowerBodyAccent
        else -> RestDayAccent
    }

    Column(
        modifier = modifier
            .guildPlaque(14.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("today_workout_selector")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Latihan hari ini",
                color = PrimaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (selectedOverride != null) {
                Text(
                    text = "Menggantikan jadwal hari ini",
                    color = activeOverrideColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .testTag("override_active_note")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WorkoutTypeChip(
                label = "Sesuai jadwal",
                isSelected = selectedOverride == null,
                selectedContainerColor = SecondaryCardBackground,
                selectedBorderColor = RestDayAccent,
                selectedTextColor = PrimaryText,
                onClick = { onSelectOverride(null) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("workout_override_chip_schedule")
            )
            WorkoutTypeChip(
                label = "Upper",
                isSelected = selectedOverride == DayType.UPPER,
                selectedContainerColor = UpperBodyAccent,
                selectedBorderColor = UpperBodyAccent,
                selectedTextColor = OnPrimaryDark,
                onClick = { onSelectOverride(DayType.UPPER) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("workout_override_chip_upper")
            )
            WorkoutTypeChip(
                label = "Lower",
                isSelected = selectedOverride == DayType.LOWER,
                selectedContainerColor = LowerBodyAccent,
                selectedBorderColor = LowerBodyAccent,
                selectedTextColor = OnPrimaryDark,
                onClick = { onSelectOverride(DayType.LOWER) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("workout_override_chip_lower")
            )
        }
    }
}

@Composable
private fun WorkoutTypeChip(
    label: String,
    isSelected: Boolean,
    selectedContainerColor: Color,
    selectedBorderColor: Color,
    selectedTextColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) selectedContainerColor else MainBackground
    val borderColor = if (isSelected) selectedBorderColor else BorderDivider
    val textColor = if (isSelected) selectedTextColor else SecondaryText

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ExerciseCardItem(
    exerciseState: ExerciseState,
    exerciseIndex: Int,
    restTimerStateFlow: StateFlow<Map<Int, RestTimerStatus>>?,
    fallbackRestTimerStatus: RestTimerStatus,
    overloadSuggestion: OverloadSuggestion?,
    onToggleExpand: (Int) -> Unit,
    onWeightChanged: (Int, Int, String) -> Unit,
    onRepsChanged: (Int, Int, String) -> Unit,
    onToggleSetDone: (Int, Int) -> Unit,
    onRestTimerTapped: (Int) -> Unit
) {
    val restTimerStatus = if (restTimerStateFlow != null) {
        val timerFlow = remember(restTimerStateFlow, exerciseIndex) {
            restTimerStateFlow
                .map { it[exerciseIndex] ?: RestTimerStatus() }
                .distinctUntilChanged()
        }
        val status by timerFlow.collectAsStateWithLifecycle(
            initialValue = restTimerStateFlow.value[exerciseIndex] ?: RestTimerStatus()
        )
        status
    } else {
        fallbackRestTimerStatus
    }

    val cardToggleExpand = remember(onToggleExpand, exerciseIndex) { { onToggleExpand(exerciseIndex) } }
    val cardWeightChanged = remember(onWeightChanged, exerciseIndex) { { setIdx: Int, w: String -> onWeightChanged(exerciseIndex, setIdx, w) } }
    val cardRepsChanged = remember(onRepsChanged, exerciseIndex) { { setIdx: Int, r: String -> onRepsChanged(exerciseIndex, setIdx, r) } }
    val cardToggleSetDone = remember(onToggleSetDone, exerciseIndex) { { setIdx: Int -> onToggleSetDone(exerciseIndex, setIdx) } }
    val cardRestTimerTapped = remember(onRestTimerTapped, exerciseIndex) { { onRestTimerTapped(exerciseIndex) } }

    ExerciseCard(
        exercise = exerciseState,
        exerciseIndex = exerciseIndex,
        onToggleExpand = cardToggleExpand,
        onWeightChanged = cardWeightChanged,
        onRepsChanged = cardRepsChanged,
        onToggleSetDone = cardToggleSetDone,
        onRestTimerTapped = cardRestTimerTapped,
        restTimerStatus = restTimerStatus,
        overloadSuggestion = overloadSuggestion
    )
}
