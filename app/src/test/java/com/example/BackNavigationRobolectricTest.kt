package com.example

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.GamificationEngine
import com.example.ui.screens.AdventureScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackNavigationRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun manifest_hasOnBackInvokedCallbackEnabled() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parser = context.assets.openXmlResourceParser("AndroidManifest.xml")
        var foundEnabled = false
        try {
            var eventType = parser.eventType
            while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                if (eventType == org.xmlpull.v1.XmlPullParser.START_TAG && parser.name == "application") {
                    for (i in 0 until parser.attributeCount) {
                        if (parser.getAttributeName(i) == "enableOnBackInvokedCallback" &&
                            parser.getAttributeValue(i) == "true"
                        ) {
                            foundEnabled = true
                        }
                    }
                    break
                }
                eventType = parser.next()
            }
        } finally {
            parser.close()
        }
        assertTrue("android:enableOnBackInvokedCallback=\"true\" not found in AndroidManifest.xml", foundEnabled)
    }

    @Test
    fun adventureScreen_backHandler_invokesOnBack() {
        var showAdventure by mutableStateOf(true)

        composeTestRule.setContent {
            MyApplicationTheme {
                if (showAdventure) {
                    AdventureScreen(
                        gamificationState = GamificationEngine.emptyState(),
                        onBack = { showAdventure = false }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("adventure_screen").assertIsDisplayed()

        composeTestRule.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()

        assertFalse(showAdventure)
        composeTestRule.onNodeWithTag("adventure_screen").assertDoesNotExist()
    }

    @Test
    fun statsScreen_backHandler_invokesOnBack() {
        var showStats by mutableStateOf(true)

        composeTestRule.setContent {
            MyApplicationTheme {
                if (showStats) {
                    StatsScreen(
                        allSessions = emptyList(),
                        onBack = { showStats = false }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("stats_back_button").assertIsDisplayed()

        composeTestRule.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()

        assertFalse(showStats)
        composeTestRule.onNodeWithTag("stats_back_button").assertDoesNotExist()
    }

    @Test
    fun adventureScreen_whenInactive_doesNotInterceptBack() {
        var backCalled = false

        composeTestRule.setContent {
            MyApplicationTheme {
                AdventureScreen(
                    gamificationState = GamificationEngine.emptyState(),
                    isActive = false,
                    onBack = { backCalled = true }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()

        assertFalse(backCalled)
    }

    @Test
    fun statsScreen_whenInactive_doesNotInterceptBack() {
        var backCalled = false

        composeTestRule.setContent {
            MyApplicationTheme {
                StatsScreen(
                    allSessions = emptyList(),
                    isActive = false,
                    onBack = { backCalled = true }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()

        assertFalse(backCalled)
    }
}
