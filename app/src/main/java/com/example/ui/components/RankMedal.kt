package com.example.ui.components

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.RankSystem
import com.example.ui.theme.RestDayAccent
import com.example.ui.theme.UpperBodyAccent
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RankMedalBitmapCache {
    private val cache = LruCache<Long, ImageBitmap>(40)
    private val decodeLock = Any()

    fun getOrDecode(resources: Resources, @DrawableRes resId: Int, targetPx: Int): ImageBitmap? {
        val safeTarget = targetPx.coerceAtLeast(32)
        val key = (resId.toLong() shl 32) or (safeTarget.toLong() and 0xFFFFFFFFL)
        cache.get(key)?.let { return it }

        val decoded = synchronized(decodeLock) {
            cache.get(key)?.let { return@synchronized it }
            val result = try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeResource(resources, resId, bounds)
                var sampleSize = 1
                if (bounds.outWidth > 0 && bounds.outHeight > 0) {
                    while ((bounds.outWidth / (sampleSize * 2)) >= safeTarget &&
                        (bounds.outHeight / (sampleSize * 2)) >= safeTarget
                    ) {
                        sampleSize *= 2
                    }
                }
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                BitmapFactory.decodeResource(resources, resId, decodeOpts)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
            if (result != null) {
                cache.put(key, result)
            }
            result
        }
        return decoded
    }

    suspend fun prewarmAll(
        resources: Resources,
        density: Float,
        currentRankIndex: Int
    ) = withContext(Dispatchers.IO) {
        val currentRes = rankMedalDrawableRes(currentRankIndex)
        val px40 = (40f * density).roundToInt()
        val px56 = (56f * density).roundToInt()
        val px96 = (96f * density).roundToInt()
        val px150 = (150f * density).roundToInt()

        // Priority 1: Current rank across all displayed UI sizes
        getOrDecode(resources, currentRes, px56)
        getOrDecode(resources, currentRes, px150)
        getOrDecode(resources, currentRes, px96)
        getOrDecode(resources, currentRes, px40)

        // Priority 2: All 9 rank medals at Jalur Rank compact/expanded row size (40dp)
        for (rankIdx in 0..8) {
            getOrDecode(resources, rankMedalDrawableRes(rankIdx), px40)
        }
    }
}

@DrawableRes
private fun rankMedalDrawableRes(rankIndex: Int): Int = when (rankIndex.coerceIn(0, 8)) {
    0 -> R.drawable.rank_f
    1 -> R.drawable.rank_e
    2 -> R.drawable.rank_d
    3 -> R.drawable.rank_c
    4 -> R.drawable.rank_b
    5 -> R.drawable.rank_a
    6 -> R.drawable.rank_s
    7 -> R.drawable.rank_ss
    else -> R.drawable.rank_sss
}

private fun rankLetterForIndex(rankIndex: Int): String = when (rankIndex.coerceIn(0, 8)) {
    0 -> "F"
    1 -> "E"
    2 -> "D"
    3 -> "C"
    4 -> "B"
    5 -> "A"
    6 -> "S"
    7 -> "SS"
    else -> "SSS"
}

@Composable
fun RankMedal(
    rankIndex: Int,
    size: Dp = 44.dp,
    alpha: Float = 1f,
    modifier: Modifier = Modifier
) {
    val safeIndex = rankIndex.coerceIn(0, 8)
    val drawableRes = rankMedalDrawableRes(safeIndex)
    val letter = rankLetterForIndex(safeIndex)
    val resources = LocalContext.current.resources
    val targetPx = with(LocalDensity.current) { size.roundToPx() }
    val cachedBitmap = remember(drawableRes, targetPx) {
        RankMedalBitmapCache.getOrDecode(resources, drawableRes, targetPx)
    }

    if (cachedBitmap != null) {
        Image(
            bitmap = cachedBitmap,
            contentDescription = "Medali rank $letter",
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.Medium,
            alpha = alpha,
            modifier = modifier.size(size)
        )
    } else {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = "Medali rank $letter",
            contentScale = ContentScale.Fit,
            alpha = alpha,
            modifier = modifier.size(size)
        )
    }
}

@Composable
fun TierStarsRow(
    filledCount: Int,
    modifier: Modifier = Modifier,
    starSize: Dp = 14.dp,
    spacing: Dp = 3.dp,
    filledColor: Color = UpperBodyAccent,
    emptyColor: Color = RestDayAccent.copy(alpha = 0.35f)
) {
    val safeFilled = filledCount.coerceIn(0, RankSystem.TIERS_PER_RANK)
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "Tingkat $safeFilled dari ${RankSystem.TIERS_PER_RANK}"
        },
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..RankSystem.TIERS_PER_RANK) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = if (i <= safeFilled) filledColor else emptyColor,
                modifier = Modifier.size(starSize)
            )
        }
    }
}

fun rankAccentColor(rankIndex: Int): Color = RankSystem.rankPrimaryColor(rankIndex)
