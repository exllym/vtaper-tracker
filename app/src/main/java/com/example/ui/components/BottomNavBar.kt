package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DayType
import com.example.ui.theme.CardBackground
import com.example.ui.theme.GoldDark
import com.example.ui.theme.MainBackground
import com.example.ui.theme.SecondaryCardBackground
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.UpperBodyAccent

private val TopRuleBrush = Brush.horizontalGradient(
    listOf(
        GoldDark.copy(alpha = 0.25f),
        UpperBodyAccent.copy(alpha = 0.75f),
        GoldDark.copy(alpha = 0.25f)
    )
)
private val NavBarBgBrush = Brush.verticalGradient(listOf(SecondaryCardBackground, CardBackground))
private val SelectedTabBgBrush = Brush.verticalGradient(
    listOf(UpperBodyAccent.copy(alpha = 0.16f), Color.Transparent)
)
private val ActiveIndicatorShape = RoundedCornerShape(2.dp)

data class NavItem(
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

private fun buildQuestScrollIcon(): ImageVector {
    return ImageVector.Builder(
        name = "GuildQuestScroll",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // Rolled parchment scroll body + crest line
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(6f, 4f)
            horizontalLineTo(17f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 19f, 6f)
            verticalLineTo(17f)
            arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 16f, 20f)
            horizontalLineTo(7f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 5f, 18f)
            verticalLineTo(6f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 7f, 4f)
            close()
            moveTo(9f, 9f)
            horizontalLineTo(15f)
            moveTo(9f, 12.5f)
            horizontalLineTo(15f)
            moveTo(9f, 16f)
            horizontalLineTo(12.5f)
        }
    }.build()
}

private fun buildGuildCalendarIcon(): ImageVector {
    return ImageVector.Builder(
        name = "GuildScheduleSeal",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Calendar tablet with banner rings & wax mark
            moveTo(4f, 6f)
            horizontalLineTo(20f)
            verticalLineTo(20f)
            horizontalLineTo(4f)
            close()
            moveTo(4f, 10f)
            horizontalLineTo(20f)
            moveTo(8f, 3f)
            verticalLineTo(6f)
            moveTo(16f, 3f)
            verticalLineTo(6f)
            // Small diamond quest mark inside
            moveTo(12f, 13f)
            lineTo(14.5f, 15.5f)
            lineTo(12f, 18f)
            lineTo(9.5f, 15.5f)
            close()
        }
    }.build()
}

private fun buildGuildShieldSwordIcon(): ImageVector {
    return ImageVector.Builder(
        name = "GuildAdventureShield",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Heraldic shield silhouette
            moveTo(12f, 3f)
            lineTo(19f, 6f)
            verticalLineTo(11.5f)
            curveTo(19f, 16f, 15.9f, 19.4f, 12f, 21f)
            curveTo(8.1f, 19.4f, 5f, 16f, 5f, 11.5f)
            verticalLineTo(6f)
            close()
            // Upright sword inside shield
            moveTo(12f, 7f)
            verticalLineTo(16.5f)
            moveTo(9.5f, 13.5f)
            horizontalLineTo(14.5f)
        }
    }.build()
}

@Composable
fun BottomNavBar(
    selectedTab: Int,
    dayType: DayType,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val questIcon = remember { buildQuestScrollIcon() }
    val scheduleIcon = remember { buildGuildCalendarIcon() }
    val adventureIcon = remember { buildGuildShieldSwordIcon() }

    val items = remember(questIcon, scheduleIcon, adventureIcon) {
        listOf(
            NavItem("Papan Quest", questIcon, "nav_tab_today"),
            NavItem("Jadwal Misi", scheduleIcon, "nav_tab_schedule"),
            NavItem("Petualangan", adventureIcon, "nav_tab_progress")
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MainBackground)
            .navigationBarsPadding()
    ) {
        // Antique gold top border rule
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .background(TopRuleBrush)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NavBarBgBrush)
                .height(66.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = selectedTab == index
                val itemColor by animateColorAsState(
                    targetValue = if (isSelected) UpperBodyAccent else SecondaryText,
                    animationSpec = tween(durationMillis = 160),
                    label = "nav_item_color_$index"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(66.dp)
                        .then(
                            if (isSelected) {
                                Modifier.background(SelectedTabBgBrush)
                            } else {
                                Modifier
                            }
                        )
                        .clickable { onTabSelected(index) }
                        .testTag(item.testTag),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 1.dp)
                                .width(38.dp)
                                .height(2.5.dp)
                                .clip(ActiveIndicatorShape)
                                .background(UpperBodyAccent)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = itemColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = item.label,
                            fontFamily = FontFamily.Serif,
                            color = itemColor,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            letterSpacing = 0.4.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
