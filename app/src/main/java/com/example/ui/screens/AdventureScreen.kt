package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AchievementStatus
import com.example.data.model.AchievementTier
import com.example.data.model.GamificationState
import com.example.data.model.RankInfo
import com.example.data.model.RankRoadItem
import com.example.data.model.RankSystem
import com.example.data.model.RankTierRoadItem
import com.example.ui.components.RankMedal
import com.example.ui.components.TierStarsRow
import com.example.ui.components.rankAccentColor
import com.example.ui.theme.BorderDivider
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GuildTitle
import com.example.ui.theme.LowerBodyAccent
import com.example.ui.theme.MainBackground
import com.example.ui.theme.ParchmentLight
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.RestDayAccent
import com.example.ui.theme.SecondaryCardBackground
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SuccessColor
import com.example.ui.theme.UpperBodyAccent
import com.example.ui.theme.guildPlaque

private val XpProgressBrush = Brush.horizontalGradient(
    colors = listOf(GoldDark, UpperBodyAccent)
)
private val LeftTransformOrigin = TransformOrigin(0f, 0.5f)
private val HeroXpTrackShape = RoundedCornerShape(5.dp)
private val HeroXpBadgeShape = RoundedCornerShape(8.dp)
private val AchievementBarShape = RoundedCornerShape(3.dp)
private val AchievementTierBadgeShape = RoundedCornerShape(6.dp)
private val XpEventBadgeShape = RoundedCornerShape(8.dp)
private val TierRowShape = RoundedCornerShape(10.dp)
private val QuestRowShape = RoundedCornerShape(12.dp)

@Composable
fun AdventureScreen(
    gamificationState: GamificationState,
    onBack: () -> Unit,
    isActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = isActive) { onBack() }

    val lazyListState = rememberLazyListState()
    var wasActive by remember { mutableStateOf(isActive) }
    LaunchedEffect(isActive) {
        if (isActive && !wasActive) {
            lazyListState.scrollToItem(0)
        }
        wasActive = isActive
    }

    val rankInfo = gamificationState.rankInfo
    val achievementRows = remember(gamificationState.achievements) {
        gamificationState.achievements.chunked(2)
    }
    val unlockedCount = remember(gamificationState.achievements) {
        gamificationState.achievements.count { it.isUnlocked }
    }

    val rawFraction = if (rankInfo.xpNeededForNextTier <= 0) {
        1f
    } else {
        (rankInfo.xpIntoTier.toFloat() / rankInfo.xpNeededForNextTier.toFloat()).coerceIn(0f, 1f)
    }
    val animatedProgress by animateFloatAsState(
        targetValue = rawFraction,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "xp_bar_progress"
    )

    LazyColumn(
        state = lazyListState,
        modifier = modifier
            .fillMaxSize()
            .background(MainBackground)
            .testTag("adventure_screen"),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item(key = "adventure_header", contentType = "adventure_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("adventure_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = PrimaryText
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    GuildTitle(
                        text = "KARTU GUILD",
                        fontSize = 19.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Jalur rank, papan misi, medali & catatan perjalanan",
                        color = SecondaryText,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Hero Guild Membership Card
        item(key = "adventure_hero_card", contentType = "adventure_hero_card") {
            val rankLabelText = if (rankInfo.isEndless) {
                "${rankInfo.rankLetter} · ${rankInfo.displayLabel}"
            } else {
                "${rankInfo.displayLabel} · ${rankInfo.rankTitle}"
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .guildPlaque(16.dp)
                    .padding(20.dp)
                    .testTag("adventure_hero_card"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                RankMedal(
                    rankIndex = rankInfo.rankIndex,
                    size = 150.dp
                )

                Spacer(modifier = Modifier.height(10.dp))

                TierStarsRow(
                    filledCount = rankInfo.tierIndex + 1,
                    starSize = 18.dp,
                    spacing = 4.dp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = rankLabelText,
                    color = ParchmentLight,
                    fontFamily = FontFamily.Serif,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center
                )

                if (rankInfo.metal.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = rankInfo.metal,
                        color = SecondaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .clip(HeroXpBadgeShape)
                        .background(UpperBodyAccent.copy(alpha = 0.16f))
                        .border(1.dp, UpperBodyAccent.copy(alpha = 0.45f), HeroXpBadgeShape)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Total ${RankSystem.formatXp(gamificationState.totalXp)} XP",
                        color = UpperBodyAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // XP Progress Bar toward next tier / Monarch level (GPU draw-phase scale)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(HeroXpTrackShape)
                        .background(MainBackground)
                        .border(1.dp, BorderDivider, HeroXpTrackShape)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = animatedProgress
                                transformOrigin = LeftTransformOrigin
                            }
                            .background(XpProgressBrush)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${rankInfo.xpIntoTier} / ${rankInfo.xpNeededForNextTier} XP ke ${rankInfo.nextLabel}",
                    color = SecondaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Jalur Rank Header
        item(key = "adventure_rank_road_header", contentType = "section_header") {
            GuildTitle(
                text = "JALUR RANK",
                fontSize = 15.sp
            )
        }

        // Jalur Rank Rows (current rank expanded with 5 tiers; other ranks compact)
        items(
            items = gamificationState.rankRoad,
            key = { item -> "rank_road_${item.rankIndex}" },
            contentType = { item -> if (item.isExpanded) "rank_road_expanded" else "rank_road_compact" }
        ) { item ->
            if (item.isExpanded) {
                ExpandedCurrentRankCard(
                    item = item,
                    rankInfo = rankInfo
                )
            } else {
                CompactRankRoadRow(item = item)
            }
        }

        // Weekly Quests Card
        item(key = "adventure_quests_card", contentType = "adventure_quests") {
            val quests = gamificationState.weeklyQuests
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .guildPlaque(16.dp)
                    .padding(16.dp)
                    .testTag("adventure_quests_card")
            ) {
                GuildTitle(
                    text = "PAPAN MISI",
                    fontSize = 15.sp
                )

                if (quests.sessionsCompleted == 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Pekan ini belum mulai. Mulai kapan saja.",
                        color = UpperBodyAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuestRow(
                        label = "Selesaikan 4 sesi",
                        current = quests.sessionsCompleted,
                        target = quests.sessionsTarget
                    )
                    QuestRow(
                        label = "Timbang badan 1x",
                        current = quests.weighInCompleted,
                        target = quests.weighInTarget
                    )
                    QuestRow(
                        label = "Pecahkan 1 rekor",
                        current = quests.prCompleted,
                        target = quests.prTarget
                    )
                }
            }
        }

        // Achievements Header
        item(key = "adventure_achievements_header", contentType = "section_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GuildTitle(
                    text = "MEDALI PENCAPAIAN",
                    fontSize = 15.sp
                )
                Text(
                    text = "$unlockedCount / ${gamificationState.achievements.size} terbuka",
                    color = UpperBodyAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Achievements Grid (2 columns per row — single-pass measure without IntrinsicSize)
        items(
            count = achievementRows.size,
            key = { rowIndex -> "achievement_row_$rowIndex" },
            contentType = { "achievement_row" }
        ) { rowIndex ->
            val pair = achievementRows[rowIndex]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                AchievementGridCard(
                    achievement = pair[0],
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 160.dp)
                )
                if (pair.size > 1) {
                    AchievementGridCard(
                        achievement = pair[1],
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 160.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // Recent XP Header
        item(key = "adventure_xp_history_header", contentType = "section_header") {
            GuildTitle(
                text = "CATATAN PERJALANAN",
                fontSize = 15.sp
            )
        }

        if (gamificationState.recentXpEvents.isEmpty()) {
            item(key = "adventure_xp_empty", contentType = "xp_empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .guildPlaque(14.dp)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada catatan perjalanan. Selesaikan sesi latihan untuk mulai mengumpulkan XP.",
                        color = SecondaryText,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(
                items = gamificationState.recentXpEvents,
                key = { event -> "xp_event_${event.date}" },
                contentType = { "xp_event_row" }
            ) { event ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .guildPlaque(12.dp)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = event.date,
                            color = PrimaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = event.breakdown,
                            color = SecondaryText,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(UpperBodyAccent.copy(alpha = 0.16f))
                            .border(1.dp, UpperBodyAccent.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "+${event.xpEarned} XP",
                            color = UpperBodyAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandedCurrentRankCard(
    item: RankRoadItem,
    rankInfo: RankInfo
) {
    val rankColor = rankAccentColor(item.rankIndex)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .guildPlaque(16.dp, borderColor = rankColor.copy(alpha = 0.65f))
            .padding(14.dp)
            .testTag("rank_road_current_${item.rankIndex}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RankMedal(
                    rankIndex = item.rankIndex,
                    size = 40.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "${item.letter} · ${item.title}",
                        color = ParchmentLight,
                        fontFamily = FontFamily.Serif,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp
                    )
                    val subtitleText = if (item.metal.isNotEmpty()) {
                        "${item.metal} · ${item.xpRangeText}"
                    } else {
                        item.xpRangeText
                    }
                    Text(
                        text = subtitleText,
                        color = SecondaryText,
                        fontSize = 11.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(rankColor.copy(alpha = 0.16f))
                    .border(1.dp, rankColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = item.statusLabel,
                    color = rankColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item.tiers.forEach { tier ->
                ExpandedTierRow(
                    tier = tier,
                    rankColor = rankColor
                )
            }

            if (rankInfo.isEndless) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(rankColor.copy(alpha = 0.14f))
                        .border(1.dp, rankColor.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = rankInfo.displayLabel,
                        color = ParchmentLight,
                        fontFamily = FontFamily.Serif,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp
                    )
                    Text(
                        text = "Sekarang (+15.000 XP/level)",
                        color = PrimaryText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandedTierRow(
    tier: RankTierRoadItem,
    rankColor: androidx.compose.ui.graphics.Color
) {
    val bgColor = if (tier.isCurrent) rankColor.copy(alpha = 0.14f) else SecondaryCardBackground
    val borderColor = when {
        tier.isCurrent -> rankColor.copy(alpha = 0.65f)
        tier.isReached -> SuccessColor.copy(alpha = 0.4f)
        else -> BorderDivider
    }
    val icon = when {
        tier.isReached -> Icons.Default.CheckCircle
        tier.isCurrent -> Icons.Default.RadioButtonChecked
        else -> Icons.Default.RadioButtonUnchecked
    }
    val iconTint = when {
        tier.isReached -> SuccessColor
        tier.isCurrent -> rankColor
        else -> RestDayAccent
    }
    val statusText = when {
        tier.isCurrent -> "Sekarang"
        tier.isReached -> "Tercapai"
        else -> "Terkunci"
    }
    val statusColor = when {
        tier.isCurrent -> rankColor
        tier.isReached -> SuccessColor
        else -> RestDayAccent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tier.tierLabel,
                        color = if (tier.isCurrent || tier.isReached) ParchmentLight else SecondaryText,
                        fontSize = 12.sp,
                        fontWeight = if (tier.isCurrent) FontWeight.Bold else FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    TierStarsRow(
                        filledCount = tier.tierIndex + 1,
                        starSize = 11.dp,
                        spacing = 1.5.dp
                    )
                }
                Text(
                    text = tier.xpRangeText,
                    color = SecondaryText,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = statusText,
            color = statusColor,
            fontSize = 11.sp,
            fontWeight = if (tier.isCurrent) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun CompactRankRoadRow(item: RankRoadItem) {
    val rankColor = rankAccentColor(item.rankIndex)
    val isLocked = !item.isReached && !item.isCurrent
    val stateColor = when {
        item.isReached -> SuccessColor
        item.isCurrent -> rankColor
        else -> RestDayAccent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .guildPlaque(12.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("rank_road_compact_${item.rankIndex}"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RankMedal(
                rankIndex = item.rankIndex,
                size = 40.dp,
                alpha = if (isLocked) 0.4f else 1f
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "${item.letter} · ${item.title}",
                    color = if (item.isReached) ParchmentLight else SecondaryText,
                    fontFamily = FontFamily.Serif,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
                val subtitleText = if (item.metal.isNotEmpty()) {
                    "${item.metal} · ${item.xpRangeText}"
                } else {
                    item.xpRangeText
                }
                Text(
                    text = subtitleText,
                    color = RestDayAccent,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = item.statusLabel,
            color = stateColor,
            fontSize = 11.sp,
            fontWeight = if (item.isReached) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
private fun QuestRow(
    label: String,
    current: Int,
    target: Int
) {
    val isCompleted = current >= target
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SecondaryCardBackground)
            .border(
                width = 1.dp,
                color = if (isCompleted) SuccessColor.copy(alpha = 0.45f) else BorderDivider,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isCompleted) SuccessColor else RestDayAccent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                color = if (isCompleted) PrimaryText else SecondaryText,
                fontSize = 13.sp,
                fontWeight = if (isCompleted) FontWeight.SemiBold else FontWeight.Medium
            )
        }

        Text(
            text = "$current/$target",
            color = if (isCompleted) SuccessColor else UpperBodyAccent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AchievementGridCard(
    achievement: AchievementStatus,
    modifier: Modifier = Modifier
) {
    val isUnlocked = achievement.isUnlocked
    val tierColor = when (achievement.tier) {
        AchievementTier.DASAR -> RestDayAccent
        AchievementTier.MENENGAH -> LowerBodyAccent
        AchievementTier.TANTANGAN -> UpperBodyAccent
        AchievementTier.LEGENDARIS -> ParchmentLight
    }
    val borderColor = when {
        isUnlocked -> UpperBodyAccent.copy(alpha = 0.65f)
        achievement.tier == AchievementTier.LEGENDARIS -> GoldDark.copy(alpha = 0.6f)
        else -> BorderDivider
    }
    val fraction = if (achievement.targetProgress <= 0) {
        if (isUnlocked) 1f else 0f
    } else {
        (achievement.currentProgress.toFloat() / achievement.targetProgress.toFloat()).coerceIn(0f, 1f)
    }

    Column(
        modifier = modifier
            .guildPlaque(14.dp, borderColor = borderColor)
            .padding(12.dp)
            .testTag("achievement_card_${achievement.id}"),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (isUnlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (isUnlocked) UpperBodyAccent else tierColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = achievement.name,
                    color = if (isUnlocked) ParchmentLight else PrimaryText,
                    fontFamily = FontFamily.Serif,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .clip(AchievementTierBadgeShape)
                    .background(tierColor.copy(alpha = 0.14f))
                    .border(1.dp, tierColor.copy(alpha = 0.4f), AchievementTierBadgeShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = achievement.tier.label,
                    color = tierColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = achievement.description,
                color = if (isUnlocked) SecondaryText else RestDayAccent,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }

        Column(modifier = Modifier.padding(top = 8.dp)) {
            if (achievement.progressText.isNotEmpty()) {
                Text(
                    text = achievement.progressText,
                    color = if (isUnlocked) SuccessColor else SecondaryText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("achievement_progress_text_${achievement.id}")
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(AchievementBarShape)
                    .background(MainBackground)
                    .border(1.dp, BorderDivider, AchievementBarShape)
            ) {
                if (fraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = fraction
                                transformOrigin = LeftTransformOrigin
                            }
                            .then(
                                if (isUnlocked) {
                                    Modifier.background(SuccessColor)
                                } else {
                                    Modifier.background(XpProgressBrush)
                                }
                            )
                    )
                }
            }

            if (isUnlocked && achievement.unlockedDate != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Terbuka: ${achievement.unlockedDate}",
                    color = SuccessColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
