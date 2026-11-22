package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.WorkoutXpReward
import com.example.ui.theme.BorderDivider
import com.example.ui.theme.GuildTitle
import com.example.ui.theme.OnPrimaryDark
import com.example.ui.theme.ParchmentLight
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryCardBackground
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SuccessColor
import com.example.ui.theme.UpperBodyAccent
import com.example.ui.theme.guildPlaque

@Composable
fun WorkoutXpRewardDialog(
    reward: WorkoutXpReward,
    onDismiss: () -> Unit
) {
    val newRank = reward.newRankInfo
    val rankColor = rankAccentColor(newRank.rankIndex)
    val emblemSize = when {
        reward.rankLetterChanged -> 96.dp
        reward.tierChanged -> 64.dp
        else -> 56.dp
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .testTag("workout_xp_reward_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .guildPlaque(
                        cornerRadius = 20.dp,
                        borderColor = if (reward.rankLetterChanged) rankColor.copy(alpha = 0.7f) else BorderDivider
                    )
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GuildTitle(
                    text = if (reward.rankLetterChanged) "RANK NAIK!" else "SESI SELESAI",
                    color = if (reward.rankLetterChanged) rankColor else SecondaryText,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    horizontalAlignment = Alignment.CenterHorizontally
                )

                Spacer(modifier = Modifier.height(12.dp))

                RankMedal(
                    rankIndex = newRank.rankIndex,
                    size = emblemSize
                )

                Spacer(modifier = Modifier.height(8.dp))

                TierStarsRow(
                    filledCount = newRank.tierIndex + 1,
                    starSize = 14.dp,
                    spacing = 3.dp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "+${reward.xpEarned} XP",
                    color = if (reward.xpEarned > 0) UpperBodyAccent else PrimaryText,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.testTag("reward_xp_amount")
                )

                if (reward.rankLetterChanged) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(rankColor.copy(alpha = 0.14f))
                            .border(1.dp, rankColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Naik ke ${newRank.displayLabel}!",
                            color = rankColor,
                            fontFamily = FontFamily.Serif,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("reward_rank_up_label")
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Pangkat baru: ${newRank.rankTitle}",
                            color = ParchmentLight,
                            fontFamily = FontFamily.Serif,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (reward.tierChanged) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(SuccessColor.copy(alpha = 0.16f))
                            .border(1.dp, SuccessColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Naik ke ${newRank.displayLabel}!",
                            color = SuccessColor,
                            fontFamily = FontFamily.Serif,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("reward_tier_up_label")
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = newRank.rankTitle,
                        color = ParchmentLight,
                        fontFamily = FontFamily.Serif,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp
                    )
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    val labelText = if (newRank.isEndless) {
                        "${newRank.rankLetter} · ${newRank.displayLabel}"
                    } else {
                        "${newRank.displayLabel} · ${newRank.rankTitle}"
                    }
                    Text(
                        text = labelText,
                        color = ParchmentLight,
                        fontFamily = FontFamily.Serif,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = BorderDivider, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SecondaryCardBackground)
                        .border(1.dp, BorderDivider, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    reward.breakdownItems.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "• $item",
                                color = PrimaryText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Total XP: ${reward.newTotalXp} XP (${newRank.xpIntoTier} / ${newRank.xpNeededForNextTier} ke ${newRank.nextLabel})",
                    color = SecondaryText,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = UpperBodyAccent,
                        contentColor = OnPrimaryDark
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("dismiss_xp_reward_button")
                ) {
                    Text(
                        text = "Lanjutkan",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
