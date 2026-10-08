package ru.doGood.Lynk

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class NavigationE2ETest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() {
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    @Test
    fun testAppBottomNavigationFlow() {
        // Wait for the app to load and the initial screen (Files/Файлы) to appear.
        // It might take a moment, so we wait until "Файлы" or "Files" is displayed.
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(hasText("Файлы") or hasText("Files")).fetchSemanticsNodes().isNotEmpty()
        }

        // Find "System" / "Система" tab and click it
        val systemTab = composeTestRule.onNode(hasText("Система") or hasText("System"))
        systemTab.assertIsDisplayed()
        systemTab.performClick()

        // Verify subtabs "Характеристики" / "Specs", "Логи системы" / "System Logs", "Права доступа" / "Rights"
        composeTestRule.onNode(hasText("Характеристики") or hasText("Specs")).assertIsDisplayed()
        composeTestRule.onNode(hasText("Логи системы") or hasText("System Logs")).assertIsDisplayed()
        composeTestRule.onNode(hasText("Права доступа") or hasText("Rights")).assertIsDisplayed()

        // Find "Buttons" / "Кнопки" tab and click it
        val buttonsTab = composeTestRule.onNode(hasText("Кнопки") or hasText("Buttons"))
        buttonsTab.assertIsDisplayed()
        buttonsTab.performClick()

        // Verify that "Навигатор Домой" or "Home Navigator" appears
        composeTestRule.onNode(hasText("Навигатор Домой") or hasText("Home Navigator")).assertIsDisplayed()
    }

    @Test
    fun testLandscapeNavigationFlow() {
        // Set to Landscape
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        composeTestRule.waitForIdle()

        // Wait for the app to load and the initial screen (Files/Файлы) to appear.
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(hasText("Файлы") or hasText("Files")).fetchSemanticsNodes().isNotEmpty()
        }

        // Find "System" / "Система" tab and click it (now should be in NavigationRail)
        val systemTab = composeTestRule.onNode(hasText("Система") or hasText("System"))
        systemTab.assertIsDisplayed()
        systemTab.performClick()

        // Verify subtabs "Характеристики" / "Specs", "Логи системы" / "System Logs", "Права доступа" / "Rights"
        composeTestRule.onNode(hasText("Характеристики") or hasText("Specs")).assertIsDisplayed()
        composeTestRule.onNode(hasText("Логи системы") or hasText("System Logs")).assertIsDisplayed()
        composeTestRule.onNode(hasText("Права доступа") or hasText("Rights")).assertIsDisplayed()

        // Find "Buttons" / "Кнопки" tab and click it
        val buttonsTab = composeTestRule.onNode(hasText("Кнопки") or hasText("Buttons"))
        buttonsTab.assertIsDisplayed()
        buttonsTab.performClick()

        // Verify that "Навигатор Домой" or "Home Navigator" appears
        composeTestRule.onNode(hasText("Навигатор Домой") or hasText("Home Navigator")).assertIsDisplayed()
    }
}
