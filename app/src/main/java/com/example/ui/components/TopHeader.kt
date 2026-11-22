package com.example.ui.components

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DayInfo
import com.example.data.model.DayType
import com.example.ui.theme.BorderDivider
import com.example.ui.theme.CardBackground
import com.example.ui.theme.LowerBodyAccent
import com.example.ui.theme.MainBackground
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.RestDayAccent
import com.example.ui.theme.SecondaryCardBackground
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.UpperBodyAccent

private val SettingsButtonBrush = Brush.verticalGradient(listOf(SecondaryCardBackground, CardBackground))
private val BadgePillShape = RoundedCornerShape(20.dp)
private val HeaderTitleText = buildAnnotatedString {
    withStyle(
        SpanStyle(
            color = PrimaryText,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
    ) {
        append("V-TAPER ")
    }
    withStyle(
        SpanStyle(
            color = UpperBodyAccent,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
    ) {
        append("GRIND")
    }
}

@Composable
fun TopHeader(
    dayInfo: DayInfo,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = when (dayInfo.dayType) {
        DayType.UPPER -> UpperBodyAccent
        DayType.LOWER -> LowerBodyAccent
        DayType.REST, DayType.LIGHT -> RestDayAccent
    }

    val badgeText = when (dayInfo.dayType) {
        DayType.UPPER -> "UPPER BODY"
        DayType.LOWER -> "LOWER BODY"
        DayType.REST -> "REST"
        DayType.LIGHT -> "LIGHT"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MainBackground)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // App title "V-TAPER GRIND" (V-TAPER PrimaryText, GRIND in gold accent)
                Text(
                    text = HeaderTitleText,
                    fontSize = 23.sp
                )

                Spacer(modifier = Modifier.height(3.dp))

                // Small line below: "[DAY NAME] • PROJECT ABBAS V2"
                Text(
                    text = "${dayInfo.indonesianDay} • PROJECT ABBAS V2",
                    color = SecondaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Pill/badge showing day type colored by accent
                Box(
                    modifier = Modifier
                        .clip(BadgePillShape)
                        .background(accentColor.copy(alpha = 0.16f))
                        .border(1.dp, accentColor.copy(alpha = 0.45f), BadgePillShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeText,
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                }

                // Quick gear icon for reminder settings
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SettingsButtonBrush)
                        .border(1.dp, BorderDivider, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Pengaturan Pengingat",
                        tint = SecondaryText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
