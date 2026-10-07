package ru.doGood.Lynk

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class E2ENavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testBottomNavigation() {
        // Проверяем наличие табов и кликаем по ним
        composeTestRule.onNodeWithText("Файлы").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Система").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Кнопки").performClick()
        composeTestRule.waitForIdle()
    }
}
