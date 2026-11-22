package com.example.data.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.LowerBodyAccent
import com.example.ui.theme.ParchmentLight
import com.example.ui.theme.RestDayAccent
import com.example.ui.theme.SuccessColor
import com.example.ui.theme.UpperBodyAccent
import java.text.NumberFormat
import java.util.Locale

// Rank accent color constants
val RankFColor = RestDayAccent
val RankEColor = Color(0xFFCD7F32)
val RankDColor = Color(0xFFC8C2B8)
val RankCColor = SuccessColor
val RankBColor = LowerBodyAccent
val RankAColor = Color(0xFFD97745)
val RankSColor = UpperBodyAccent
val RankSSColor = Color(0xFFF3C65F)
val RankSSSStartColor = Color(0xFFFFD875)
val RankSSSEndColor = ParchmentLight

@Immutable
data class RankDefinition(
    val index: Int,
    val letter: String,
    val title: String,
    val metal: String,
    val startXp: Int,
    val spanXp: Int
) {
    val endXp: Int get() = startXp + spanXp
    val tierWidthXp: Int get() = spanXp / RankSystem.TIERS_PER_RANK
}

@Immutable
data class RankInfo(
    val rankIndex: Int,
    val rankLetter: String,
    val rankTitle: String,
    val metal: String = "",
    val tierIndex: Int,
    val tierRoman: String,
    val displayLabel: String,
    val xpIntoTier: Int,
    val xpNeededForNextTier: Int,
    val nextLabel: String,
    val isEndless: Boolean,
    val monarchLevel: Int = 0
)

@Immutable
data class RankTierRoadItem(
    val tierIndex: Int,
    val tierLabel: String,
    val xpRangeText: String,
    val isReached: Boolean,
    val isCurrent: Boolean
)

@Immutable
data class RankRoadItem(
    val rankIndex: Int,
    val letter: String,
    val title: String,
    val metal: String = "",
    val xpRangeText: String,
    val statusLabel: String,
    val isReached: Boolean,
    val isCurrent: Boolean,
    val isExpanded: Boolean,
    val tiers: List<RankTierRoadItem>
)

object RankSystem {
    // Tuneable rank thresholds, tier counts, and endless scaling constants
    const val TIERS_PER_RANK = 5
    val TIER_ROMAN: List<String> = listOf("I", "II", "III", "IV", "V")

    const val RANK_F_START_XP = 0
    const val RANK_E_START_XP = 1_500
    const val RANK_D_START_XP = 5_000
    const val RANK_C_START_XP = 12_000
    const val RANK_B_START_XP = 25_000
    const val RANK_A_START_XP = 45_000
    const val RANK_S_START_XP = 75_000
    const val RANK_SS_START_XP = 120_000
    const val RANK_SSS_START_XP = 180_000

    const val RANK_SSS_SPAN_XP = 80_000
    const val ENDLESS_MONARCH_START_XP = RANK_SSS_START_XP + RANK_SSS_SPAN_XP // 260000
    const val ENDLESS_MONARCH_STEP_XP = 15_000

    val RANKS: List<RankDefinition> = listOf(
        RankDefinition(0, "F", "Rekrut", "Kayu", RANK_F_START_XP, RANK_E_START_XP - RANK_F_START_XP),
        RankDefinition(1, "E", "Pemula", "Besi", RANK_E_START_XP, RANK_D_START_XP - RANK_E_START_XP),
        RankDefinition(2, "D", "Petualang", "Perunggu", RANK_D_START_XP, RANK_C_START_XP - RANK_D_START_XP),
        RankDefinition(3, "C", "Veteran", "Perak", RANK_C_START_XP, RANK_B_START_XP - RANK_C_START_XP),
        RankDefinition(4, "B", "Elit", "Emas", RANK_B_START_XP, RANK_A_START_XP - RANK_B_START_XP),
        RankDefinition(5, "A", "Pahlawan", "Platinum", RANK_A_START_XP, RANK_S_START_XP - RANK_A_START_XP),
        RankDefinition(6, "S", "Legenda", "Mithril", RANK_S_START_XP, RANK_SS_START_XP - RANK_S_START_XP),
        RankDefinition(7, "SS", "Mitos", "Orichalcum", RANK_SS_START_XP, RANK_SSS_START_XP - RANK_SS_START_XP),
        RankDefinition(8, "SSS", "Raja Naga", "Adamantium", RANK_SSS_START_XP, RANK_SSS_SPAN_XP)
    )

    private val numberFormat: NumberFormat = NumberFormat.getIntegerInstance(Locale("id", "ID"))

    fun rankFor(totalXp: Int): RankInfo {
        val safeXp = totalXp.coerceAtLeast(0)

        if (safeXp >= ENDLESS_MONARCH_START_XP) {
            val sssDef = RANKS.last()
            val xpPastEnd = safeXp - ENDLESS_MONARCH_START_XP
            val monarchLevel = 1 + (xpPastEnd / ENDLESS_MONARCH_STEP_XP)
            val xpIntoLevel = xpPastEnd % ENDLESS_MONARCH_STEP_XP
            return RankInfo(
                rankIndex = sssDef.index,
                rankLetter = sssDef.letter,
                rankTitle = sssDef.title,
                metal = sssDef.metal,
                tierIndex = TIERS_PER_RANK - 1,
                tierRoman = TIER_ROMAN.last(),
                displayLabel = "Raja Naga Lv $monarchLevel",
                xpIntoTier = xpIntoLevel,
                xpNeededForNextTier = ENDLESS_MONARCH_STEP_XP,
                nextLabel = "Raja Naga Lv ${monarchLevel + 1}",
                isEndless = true,
                monarchLevel = monarchLevel
            )
        }

        val rankIndex = RANKS.indexOfLast { safeXp >= it.startXp }.coerceAtLeast(0)
        val rankDef = RANKS[rankIndex]
        val tierWidth = (rankDef.spanXp / TIERS_PER_RANK).coerceAtLeast(1)
        val xpInRank = safeXp - rankDef.startXp
        val tierIndex = (xpInRank / tierWidth).coerceIn(0, TIERS_PER_RANK - 1)
        val tierRoman = TIER_ROMAN[tierIndex]
        val tierStartXp = rankDef.startXp + (tierIndex * tierWidth)
        val tierEndXp = if (tierIndex == TIERS_PER_RANK - 1) {
            rankDef.endXp
        } else {
            tierStartXp + tierWidth
        }
        val xpIntoTier = (safeXp - tierStartXp).coerceAtLeast(0)
        val xpNeededForNextTier = (tierEndXp - tierStartXp).coerceAtLeast(1)

        val nextLabel = when {
            tierIndex < TIERS_PER_RANK - 1 -> "${rankDef.letter}-${TIER_ROMAN[tierIndex + 1]}"
            rankIndex < RANKS.lastIndex -> "${RANKS[rankIndex + 1].letter}-${TIER_ROMAN[0]}"
            else -> "Raja Naga Lv 1"
        }

        return RankInfo(
            rankIndex = rankDef.index,
            rankLetter = rankDef.letter,
            rankTitle = rankDef.title,
            metal = rankDef.metal,
            tierIndex = tierIndex,
            tierRoman = tierRoman,
            displayLabel = "${rankDef.letter}-$tierRoman",
            xpIntoTier = xpIntoTier,
            xpNeededForNextTier = xpNeededForNextTier,
            nextLabel = nextLabel,
            isEndless = false,
            monarchLevel = 0
        )
    }

    fun buildRankRoad(totalXp: Int, currentInfo: RankInfo = rankFor(totalXp)): List<RankRoadItem> {
        return RANKS.map { def ->
            val isCurrent = def.index == currentInfo.rankIndex
            val isReached = def.index < currentInfo.rankIndex || (isCurrent && currentInfo.isEndless)
            val statusLabel = when {
                isCurrent -> "Sekarang"
                isReached -> "Tercapai"
                else -> "Terkunci"
            }

            val rangeText = if (def.index == RANKS.lastIndex) {
                "${formatXp(def.startXp)} – ${formatXp(def.endXp - 1)} XP (+Raja Naga)"
            } else {
                "${formatXp(def.startXp)} – ${formatXp(def.endXp - 1)} XP"
            }

            val tiers = if (isCurrent) {
                val tierWidth = def.tierWidthXp
                (0 until TIERS_PER_RANK).map { tIdx ->
                    val tStart = def.startXp + tIdx * tierWidth
                    val tEnd = if (tIdx == TIERS_PER_RANK - 1) def.endXp else tStart + tierWidth
                    val tierReached = currentInfo.isEndless || tIdx < currentInfo.tierIndex
                    val tierCurrent = !currentInfo.isEndless && tIdx == currentInfo.tierIndex
                    RankTierRoadItem(
                        tierIndex = tIdx,
                        tierLabel = "${def.letter}-${TIER_ROMAN[tIdx]}",
                        xpRangeText = "${formatXp(tStart)} – ${formatXp(tEnd - 1)} XP",
                        isReached = tierReached,
                        isCurrent = tierCurrent
                    )
                }
            } else {
                emptyList()
            }

            RankRoadItem(
                rankIndex = def.index,
                letter = def.letter,
                title = def.title,
                metal = def.metal,
                xpRangeText = rangeText,
                statusLabel = statusLabel,
                isReached = isReached,
                isCurrent = isCurrent,
                isExpanded = isCurrent,
                tiers = tiers
            )
        }
    }

    fun rankPrimaryColor(rankIndex: Int): Color {
        return when (rankIndex) {
            0 -> RankFColor
            1 -> RankEColor
            2 -> RankDColor
            3 -> RankCColor
            4 -> RankBColor
            5 -> RankAColor
            6 -> RankSColor
            7 -> RankSSColor
            else -> RankSSSStartColor
        }
    }

    fun formatXp(xp: Int): String = numberFormat.format(xp)
}
