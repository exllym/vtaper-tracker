package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

private val CustomDarkColorScheme = darkColorScheme(
    primary = UpperBodyAccent,
    onPrimary = OnPrimaryDark,
    secondary = LowerBodyAccent,
    onSecondary = OnPrimaryDark,
    tertiary = RestDayAccent,
    onTertiary = PrimaryText,
    background = MainBackground,
    onBackground = PrimaryText,
    surface = CardBackground,
    onSurface = PrimaryText,
    surfaceVariant = SecondaryCardBackground,
    onSurfaceVariant = SecondaryText,
    outline = BorderDivider,
    outlineVariant = BorderDivider
)

private val GuildPlaqueGradientColors = listOf(SecondaryCardBackground, CardBackground)

fun Modifier.guildPlaque(
    cornerRadius: Dp = 16.dp,
    borderColor: Color = BorderDivider
): Modifier = drawWithCache {
    val radiusPx = cornerRadius.toPx()
    val corner = CornerRadius(radiusPx, radiusPx)
    val strokePx = 1.dp.toPx()
    val halfStroke = strokePx * 0.5f
    val borderTopLeft = Offset(halfStroke, halfStroke)
    val borderSize = Size(
        width = (size.width - strokePx).coerceAtLeast(0f),
        height = (size.height - strokePx).coerceAtLeast(0f)
    )
    val borderStroke = Stroke(width = strokePx)
    val gradientBrush = Brush.verticalGradient(
        colors = GuildPlaqueGradientColors,
        startY = 0f,
        endY = size.height.coerceAtLeast(1f)
    )
    val rivetRadius = 1.5.dp.toPx()
    val inset = 7.5.dp.toPx()
    val topLeftRivet = Offset(inset, inset)
    val topRightRivet = Offset(size.width - inset, inset)
    val bottomLeftRivet = Offset(inset, size.height - inset)
    val bottomRightRivet = Offset(size.width - inset, size.height - inset)

    onDrawBehind {
        drawRoundRect(
            brush = gradientBrush,
            cornerRadius = corner
        )
        drawRoundRect(
            color = borderColor,
            topLeft = borderTopLeft,
            size = borderSize,
            cornerRadius = corner,
            style = borderStroke
        )
        drawCircle(color = GoldDark, radius = rivetRadius, center = topLeftRivet)
        drawCircle(color = GoldDark, radius = rivetRadius, center = topRightRivet)
        drawCircle(color = GoldDark, radius = rivetRadius, center = bottomLeftRivet)
        drawCircle(color = GoldDark, radius = rivetRadius, center = bottomRightRivet)
    }
}

@Composable
fun GuildTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = PrimaryText,
    fontSize: TextUnit = 16.sp,
    letterSpacing: TextUnit = 0.8.sp,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start
) {
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment
    ) {
        Text(
            text = text,
            color = color,
            fontFamily = FontFamily.Serif,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            letterSpacing = letterSpacing
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(1.dp)
                .background(GoldDark)
        )
    }
}

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                @Suppress("DEPRECATION")
                window.statusBarColor = MainBackground.toArgb()
                @Suppress("DEPRECATION")
                window.navigationBarColor = MainBackground.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = CustomDarkColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
