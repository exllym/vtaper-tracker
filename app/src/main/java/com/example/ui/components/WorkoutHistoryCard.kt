package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WorkoutProgram
import com.example.data.model.WorkoutSession
import com.example.ui.theme.BorderDivider
import com.example.ui.theme.GuildTitle
import com.example.ui.theme.LowerBodyAccent
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryCardBackground
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.UpperBodyAccent
import com.example.ui.theme.guildPlaque

private val HistoryRowClickShape = RoundedCornerShape(8.dp)
private val HistoryDetailShape = RoundedCornerShape(8.dp)
private val LoadMoreButtonShape = RoundedCornerShape(12.dp)

@Composable
fun WorkoutHistoryCard(
    allSessions: List<WorkoutSession>,
    onEditSession: (WorkoutSession) -> Unit,
    onDeleteSession: (Int) -> Unit,
    visibleCountState: Int? = null,
    onVisibleCountChange: ((Int) -> Unit)? = null,
    expandedSessionIdsState: Set<Int>? = null,
    onExpandedSessionIdsChange: ((Set<Int>) -> Unit)? = null,
    completedSetsBySessionId: Map<Int, Int> = emptyMap(),
    modifier: Modifier = Modifier
) {
    var localVisibleCount by remember { mutableStateOf(8) }
    var localExpandedSessionIds by remember { mutableStateOf(setOf<Int>()) }

    val visibleCount = visibleCountState ?: localVisibleCount
    val setVisibleCount: (Int) -> Unit = onVisibleCountChange ?: { localVisibleCount = it }
    val expandedSessionIds = expandedSessionIdsState ?: localExpandedSessionIds
    val setExpandedSessionIds: (Set<Int>) -> Unit = onExpandedSessionIdsChange ?: { localExpandedSessionIds = it }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .guildPlaque(16.dp)
            .padding(16.dp)
            .testTag("workout_history_card")
    ) {
        GuildTitle(
            text = "WORKOUT HISTORY",
            fontSize = 17.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (allSessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No sessions saved yet",
                    color = SecondaryText,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            val displayedSessions = allSessions.take(visibleCount)
            displayedSessions.forEachIndexed { idx, session ->
                key(session.id) {
                    val dotColor = if (session.dayType.equals("UPPER", ignoreCase = true)) {
                        UpperBodyAccent
                    } else {
                        LowerBodyAccent
                    }
                    val completedSets = completedSetsBySessionId[session.id]
                        ?: remember(session.id, session.exercisesJson) {
                            WorkoutProgram.countCompletedSets(session.exercisesJson)
                        }
                    val isExpanded = expandedSessionIds.contains(session.id)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(HistoryRowClickShape)
                                .clickable {
                                    setExpandedSessionIds(
                                        if (isExpanded) {
                                            expandedSessionIds - session.id
                                        } else {
                                            expandedSessionIds + session.id
                                        }
                                    )
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${session.dayLabel} • ${session.date}",
                                        color = PrimaryText,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "$completedSets set selesai",
                                        color = SecondaryText,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = { onEditSession(session) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Riwayat",
                                        tint = UpperBodyAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteSession(session.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Hapus Riwayat",
                                        tint = SecondaryText.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) "Tutup rincian" else "Lihat rincian",
                                    tint = SecondaryText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Expanded exercise breakdown
                        AnimatedVisibility(
                            visible = isExpanded,
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
                            val summaries = remember(session.id, session.exercisesJson) {
                                WorkoutProgram.parseExerciseSummaries(session.exercisesJson)
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 22.dp, top = 6.dp, bottom = 8.dp)
                                    .clip(HistoryDetailShape)
                                    .background(SecondaryCardBackground)
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (summaries.isEmpty()) {
                                    Text(
                                        text = "Tidak ada detail set yang tersimpan",
                                        color = SecondaryText,
                                        fontSize = 12.sp
                                    )
                                } else {
                                    summaries.forEach { summary ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                text = "${summary.exerciseName}:",
                                                color = PrimaryText,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = summary.formatSets(),
                                                color = UpperBodyAccent,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                textAlign = TextAlign.End,
                                                modifier = Modifier.weight(1.3f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (idx < displayedSessions.size - 1) {
                        HorizontalDivider(color = BorderDivider.copy(alpha = 0.6f), thickness = 0.8.dp)
                    }
                }
            }

            if (allSessions.size > displayedSessions.size) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { setVisibleCount(visibleCount + 10) },
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryCardBackground),
                    border = BorderStroke(1.5.dp, UpperBodyAccent),
                    shape = LoadMoreButtonShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("load_more_history_button")
                ) {
                    Text(
                        text = "Tampilkan lebih banyak",
                        color = UpperBodyAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
