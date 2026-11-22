package com.example.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WorkoutProgram
import com.example.ui.theme.BorderDivider
import com.example.ui.theme.GuildTitle
import com.example.ui.theme.LowerBodyAccent
import com.example.ui.theme.MainBackground
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.RestDayAccent
import com.example.ui.theme.SecondaryCardBackground
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.UpperBodyAccent
import com.example.ui.theme.guildPlaque

data class DayMapItem(
    val dayAbbr: String,
    val typeCode: String,
    val color: Color
)

private val WeeklyMapItems = listOf(
    DayMapItem("Mon", "UP", UpperBodyAccent),
    DayMapItem("Tue", "LOW", LowerBodyAccent),
    DayMapItem("Wed", "OFF", RestDayAccent),
    DayMapItem("Thu", "UP", UpperBodyAccent),
    DayMapItem("Fri", "LOW", LowerBodyAccent),
    DayMapItem("Sat-Sun", "LT", RestDayAccent)
)

private val ScheduleNotes = listOf(
    "Wednesday = full rest. Saturday-Sunday = light walk / light activity.",
    "Rest between sets: 90-150 sec for compound, 45-60 sec for isolation.",
    "Push close to failure at the top of the rep range, not just hitting the rep number.",
    "Progressive overload: if the top rep target is hit with clean form, add weight."
)

private val DayChipShape = RoundedCornerShape(12.dp)
private val DayCodeShape = RoundedCornerShape(8.dp)

@Composable
fun ScheduleScreen(
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MainBackground)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. "Weekly Map" card: 6-column grid showing day abbreviation and type label (UP/LOW/OFF/LT) colored accordingly
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .guildPlaque(16.dp)
                .padding(16.dp)
                .testTag("weekly_map_card")
        ) {
            GuildTitle(
                text = "WEEKLY MAP",
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WeeklyMapItems.forEach { item ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(DayChipShape)
                            .background(SecondaryCardBackground)
                            .border(1.dp, BorderDivider, DayChipShape)
                            .padding(vertical = 10.dp, horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = item.dayAbbr,
                            color = SecondaryText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(DayCodeShape)
                                .background(item.color.copy(alpha = 0.18f))
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.typeCode,
                                color = item.color,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }

        // 2. "Upper Body" card (gold title): 2-column table (Exercise Name | Set x Rep) listing all 6 upper exercises
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .guildPlaque(16.dp)
                .padding(16.dp)
                .testTag("upper_body_schedule_card")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GuildTitle(
                    text = "UPPER BODY",
                    color = UpperBodyAccent,
                    fontSize = 17.sp
                )
                Text(
                    text = "Senin & Kamis",
                    color = SecondaryText,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    text = "Exercise Name",
                    color = SecondaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Set x Rep",
                    color = SecondaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(90.dp)
                )
            }

            HorizontalDivider(color = BorderDivider, thickness = 1.dp)

            WorkoutProgram.upperBodyExercises.forEachIndexed { idx, ex ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${idx + 1}. ${ex.name}",
                            color = PrimaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = ex.muscleGroup,
                            color = SecondaryText,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = ex.target,
                        color = UpperBodyAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(90.dp)
                    )
                }
                if (idx < WorkoutProgram.upperBodyExercises.size - 1) {
                    HorizontalDivider(color = BorderDivider.copy(alpha = 0.5f), thickness = 0.8.dp)
                }
            }
        }

        // 3. "Lower Body" card (royal blue title): same 2-column table, listing all 6 lower exercises
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .guildPlaque(16.dp)
                .padding(16.dp)
                .testTag("lower_body_schedule_card")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GuildTitle(
                    text = "LOWER BODY",
                    color = LowerBodyAccent,
                    fontSize = 17.sp
                )
                Text(
                    text = "Selasa & Jumat",
                    color = SecondaryText,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    text = "Exercise Name",
                    color = SecondaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Set x Rep",
                    color = SecondaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(90.dp)
                )
            }

            HorizontalDivider(color = BorderDivider, thickness = 1.dp)

            WorkoutProgram.lowerBodyExercises.forEachIndexed { idx, ex ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${idx + 1}. ${ex.name}",
                            color = PrimaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = ex.muscleGroup,
                            color = SecondaryText,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = ex.target,
                        color = LowerBodyAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(90.dp)
                    )
                }
                if (idx < WorkoutProgram.lowerBodyExercises.size - 1) {
                    HorizontalDivider(color = BorderDivider.copy(alpha = 0.5f), thickness = 0.8.dp)
                }
            }
        }

        // 4. "Notes" card: bullet points containing the 4 required points
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .guildPlaque(16.dp)
                .padding(16.dp)
                .testTag("notes_schedule_card")
        ) {
            GuildTitle(
                text = "NOTES",
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            ScheduleNotes.forEach { note ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 7.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(UpperBodyAccent)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = note,
                        color = SecondaryText,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
