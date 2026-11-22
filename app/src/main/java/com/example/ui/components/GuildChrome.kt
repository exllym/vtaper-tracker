package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderDivider
import com.example.ui.theme.DeepBrown
import com.example.ui.theme.GoldDark
import com.example.ui.theme.MainBackground
import com.example.ui.theme.OnPrimaryDark
import com.example.ui.theme.ParchmentLight
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.RestDayAccent
import com.example.ui.theme.SecondaryCardBackground
import com.example.ui.theme.SuccessColor
import com.example.ui.theme.UpperBodyAccent

private val GoldPlaqueBrush = Brush.verticalGradient(
    colors = listOf(ParchmentLight, UpperBodyAccent, GoldDark)
)

private val XpFillBrush = Brush.horizontalGradient(
    colors = listOf(GoldDark, UpperBodyAccent, ParchmentLight)
)

/**
 * Centered medieval banner heading with ornamental gold lines & small diamonds on both sides.
 * Replaces all Solo Leveling "[ BRACKET ]" headings.
 */
@Composable
fun GuildBannerHeading(
    title: String,
    accentColor: Color = UpperBodyAccent,
    fontSize: TextUnit = 14.sp,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        GuildOrnamentWing(color = accentColor, pointRight = true, modifier = Modifier.weight(1f))
        Text(
            text = title.uppercase(),
            color = accentColor,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            letterSpacing = 1.2.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        GuildOrnamentWing(color = accentColor, pointRight = false, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun GuildOrnamentWing(
    color: Color,
    pointRight: Boolean,
    modifier: Modifier = Modifier
) {
    val diamondPath = remember { Path() }
    Canvas(
        modifier = modifier.height(10.dp)
    ) {
        val midY = size.height / 2f
        val dRadius = 3.2.dp.toPx()
        val lineColor = color.copy(alpha = 0.55f)
        val strokeW = 1.dp.toPx()

        if (pointRight) {
            val diamondX = (size.width - dRadius - 2.dp.toPx()).coerceAtLeast(dRadius)
            drawLine(
                brush = Brush.horizontalGradient(listOf(Color.Transparent, lineColor)),
                start = Offset(0f, midY),
                end = Offset((diamondX - dRadius - 2.dp.toPx()).coerceAtLeast(0f), midY),
                strokeWidth = strokeW
            )
            diamondPath.reset()
            diamondPath.moveTo(diamondX, midY - dRadius)
            diamondPath.lineTo(diamondX + dRadius, midY)
            diamondPath.lineTo(diamondX, midY + dRadius)
            diamondPath.lineTo(diamondX - dRadius, midY)
            diamondPath.close()
            drawPath(path = diamondPath, color = color)
        } else {
            val diamondX = (dRadius + 2.dp.toPx()).coerceAtMost(size.width - dRadius)
            diamondPath.reset()
            diamondPath.moveTo(diamondX, midY - dRadius)
            diamondPath.lineTo(diamondX + dRadius, midY)
            diamondPath.lineTo(diamondX, midY + dRadius)
            diamondPath.lineTo(diamondX - dRadius, midY)
            diamondPath.close()
            drawPath(path = diamondPath, color = color)
            drawLine(
                brush = Brush.horizontalGradient(listOf(lineColor, Color.Transparent)),
                start = Offset((diamondX + dRadius + 2.dp.toPx()).coerceAtMost(size.width), midY),
                end = Offset(size.width, midY),
                strokeWidth = strokeW
            )
        }
    }
}

/**
 * Gold divider line with a small engraved diamond ornament in the center.
 */
@Composable
fun GuildDivider(
    color: Color = UpperBodyAccent,
    modifier: Modifier = Modifier
) {
    val diamondPath = remember { Path() }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
    ) {
        val midX = size.width / 2f
        val midY = size.height / 2f
        val r = 3.5.dp.toPx()
        val lineColor = color.copy(alpha = 0.4f)
        val strokeW = 1.dp.toPx()

        drawLine(
            brush = Brush.horizontalGradient(listOf(Color.Transparent, lineColor)),
            start = Offset(0f, midY),
            end = Offset(midX - r - 4.dp.toPx(), midY),
            strokeWidth = strokeW
        )
        drawLine(
            brush = Brush.horizontalGradient(listOf(lineColor, Color.Transparent)),
            start = Offset(midX + r + 4.dp.toPx(), midY),
            end = Offset(size.width, midY),
            strokeWidth = strokeW
        )

        diamondPath.reset()
        diamondPath.moveTo(midX, midY - r)
        diamondPath.lineTo(midX + r, midY)
        diamondPath.lineTo(midX, midY + r)
        diamondPath.lineTo(midX - r, midY)
        diamondPath.close()
        drawPath(path = diamondPath, color = color.copy(alpha = 0.85f))
    }
}

/**
 * Metallic gold/bronze plaque button with Cinzel dark brown text (WCAG AA contrast).
 */
@Composable
fun GuildGoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    fontSize: TextUnit = 14.sp,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = OnPrimaryDark
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(10.dp))
            .background(GoldPlaqueBrush)
            .border(1.dp, UpperBodyAccent.copy(alpha = 0.75f), RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.ExtraBold,
                fontSize = fontSize,
                letterSpacing = 0.8.sp,
                color = OnPrimaryDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Dark carved-wood secondary button with antique gold outline.
 */
@Composable
fun GuildOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color = UpperBodyAccent,
    textColor: Color = PrimaryText,
    height: Dp = 46.dp,
    fontSize: TextUnit = 13.sp,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.2.dp, borderColor.copy(alpha = 0.75f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = SecondaryCardBackground,
            contentColor = textColor
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
        modifier = modifier.height(height)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                letterSpacing = 0.6.sp,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Scroll/potion-style XP bar with antique gold fill, gloss line, and tip jewel.
 */
@Composable
fun GuildXpBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 12.dp
) {
    val clamped = progress.coerceIn(0f, 1f)
    val jewelPath = remember { Path() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0F0B08))
            .border(1.dp, BorderDivider, RoundedCornerShape(6.dp))
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(clamped)
                    .clip(RoundedCornerShape(6.dp))
                    .background(XpFillBrush)
            ) {
                // Glossy top specular line + tip gem
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawLine(
                        color = ParchmentLight.copy(alpha = 0.65f),
                        start = Offset(2.dp.toPx(), 2.dp.toPx()),
                        end = Offset((size.width - 3.dp.toPx()).coerceAtLeast(2.dp.toPx()), 2.dp.toPx()),
                        strokeWidth = 1.2.dp.toPx()
                    )
                    if (size.width > 12.dp.toPx()) {
                        val tipX = size.width - 4.dp.toPx()
                        val cy = size.height / 2f
                        val r = (size.height * 0.34f).coerceAtLeast(2.dp.toPx())
                        jewelPath.reset()
                        jewelPath.moveTo(tipX, cy - r)
                        jewelPath.lineTo(tipX + r, cy)
                        jewelPath.lineTo(tipX, cy + r)
                        jewelPath.lineTo(tipX - r, cy)
                        jewelPath.close()
                        drawPath(path = jewelPath, color = Color.White.copy(alpha = 0.9f))
                        drawPath(path = jewelPath, color = GoldDark, style = Stroke(width = 0.8.dp.toPx()))
                    }
                }
            }
        }
    }
}

enum class WaxSealStyle {
    REACHED,
    CURRENT,
    LOCKED
}

/**
 * Stamped wax seal status badge for Guild rank road ("Tercapai", "Sekarang", "Terkunci").
 */
@Composable
fun WaxSealBadge(
    label: String,
    style: WaxSealStyle,
    accentColor: Color = UpperBodyAccent,
    modifier: Modifier = Modifier
) {
    val bgColor = when (style) {
        WaxSealStyle.REACHED -> RestDayAccent.copy(alpha = 0.28f)
        WaxSealStyle.CURRENT -> DeepBrown.copy(alpha = 0.88f)
        WaxSealStyle.LOCKED -> MainBackground.copy(alpha = 0.9f)
    }
    val rimColor = when (style) {
        WaxSealStyle.REACHED -> SuccessColor.copy(alpha = 0.85f)
        WaxSealStyle.CURRENT -> accentColor
        WaxSealStyle.LOCKED -> RestDayAccent.copy(alpha = 0.45f)
    }
    val textColor = when (style) {
        WaxSealStyle.REACHED -> SuccessColor
        WaxSealStyle.CURRENT -> PrimaryText
        WaxSealStyle.LOCKED -> RestDayAccent
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, rimColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(rimColor)
        )
        Text(
            text = label,
            color = textColor,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}
