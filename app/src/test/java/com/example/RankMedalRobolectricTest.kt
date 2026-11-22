package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import com.example.data.model.GamificationEngine
import com.example.data.model.RankSystem
import com.example.data.model.WorkoutXpReward
import com.example.ui.components.RankMedal
import com.example.ui.components.WorkoutXpRewardDialog
import com.example.ui.screens.AdventureScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RankMedalRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun allNineRankMedals_renderWithExpectedContentDescriptions() {
        val letters = listOf("F", "E", "D", "C", "B", "A", "S", "SS", "SSS")
        composeTestRule.setContent {
            MyApplicationTheme {
                letters.forEachIndexed { idx, _ ->
                    RankMedal(rankIndex = idx, size = 48.dp)
                }
            }
        }
        letters.forEach { letter ->
            composeTestRule.onNodeWithContentDescription("Medali rank $letter").assertIsDisplayed()
        }
    }

    @Test
    fun adventureScreen_displaysKartuGuildSectionsAndMetalName() {
        val base = GamificationEngine.emptyState()
        val d3Rank = RankSystem.rankFor(7_800) // D-III · Petualang · Perunggu
        val d3State = base.copy(
            totalXp = 7_800,
            rankInfo = d3Rank,
            rankRoad = RankSystem.buildRankRoad(7_800, d3Rank)
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                AdventureScreen(
                    gamificationState = d3State,
                    onBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("KARTU GUILD").assertIsDisplayed()
        composeTestRule.onNodeWithText("Jalur rank, papan misi, medali & catatan perjalanan").assertIsDisplayed()
        composeTestRule.onNodeWithText("D-III · Petualang").assertIsDisplayed()
        composeTestRule.onNodeWithText("Perunggu").assertIsDisplayed()
        composeTestRule.onAllNodesWithContentDescription("Medali rank D")[0].assertIsDisplayed()
        composeTestRule.onNodeWithTag("adventure_screen").performScrollToNode(hasText("JALUR RANK"))
        composeTestRule.onNodeWithText("JALUR RANK").assertIsDisplayed()
        composeTestRule.onNodeWithTag("adventure_screen").performScrollToNode(hasText("PAPAN MISI"))
        composeTestRule.onNodeWithText("PAPAN MISI").assertIsDisplayed()
        composeTestRule.onNodeWithTag("adventure_screen").performScrollToNode(hasText("MEDALI PENCAPAIAN"))
        composeTestRule.onNodeWithText("MEDALI PENCAPAIAN").assertIsDisplayed()
        composeTestRule.onNodeWithTag("adventure_screen").performScrollToNode(hasText("CATATAN PERJALANAN"))
        composeTestRule.onNodeWithText("CATATAN PERJALANAN").assertIsDisplayed()
        composeTestRule.onNodeWithTag("adventure_screen").performScrollToNode(hasText("Penjaga 12 Pekan"))
        composeTestRule.onNodeWithText("Penjaga 12 Pekan").assertIsDisplayed()
        composeTestRule.onNodeWithText("0 / 12 pekan").assertIsDisplayed()
    }

    @Test
    fun workoutXpRewardDialog_showsRankMedalAndNewRankTitleOnRankUp() {
        val oldRank = RankSystem.rankFor(1_400) // F-V
        val newRank = RankSystem.rankFor(1_600) // E-I Pemula
        val reward = WorkoutXpReward(
            xpEarned = 200,
            breakdownItems = listOf("20 set selesai (+200 XP)"),
            oldRankInfo = oldRank,
            newRankInfo = newRank,
            tierChanged = true,
            rankLetterChanged = true,
            newTotalXp = 1_600,
            newLevel = 7
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                WorkoutXpRewardDialog(
                    reward = reward,
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("+200 XP").assertIsDisplayed()
        composeTestRule.onNodeWithText("Naik ke E-I!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pangkat baru: Pemula").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Medali rank E").assertIsDisplayed()
    }

    @Test
    fun rankMedalPrewarmAll_decodesAllNineRanksIntoCache() = kotlinx.coroutines.runBlocking {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        com.example.ui.components.RankMedalBitmapCache.prewarmAll(
            resources = context.resources,
            density = 2.0f,
            currentRankIndex = 2
        )

        val letters = listOf("F", "E", "D", "C", "B", "A", "S", "SS", "SSS")
        composeTestRule.setContent {
            MyApplicationTheme {
                letters.forEachIndexed { idx, _ ->
                    RankMedal(rankIndex = idx, size = 40.dp)
                }
            }
        }

        letters.forEach { letter ->
            composeTestRule.onNodeWithContentDescription("Medali rank $letter").assertIsDisplayed()
        }
    }
}
