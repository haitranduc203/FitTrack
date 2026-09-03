package com.haitranduc.fittrack

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.haitranduc.fittrack.core.designsystem.theme.FitTrackTheme
import com.haitranduc.fittrack.presentation.StartupScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupScreenSmokeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun startupScreen_displaysAppNameAndStatus() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val expectedStatus = context.getString(R.string.status_m0_complete)
        val expectedDetail = context.getString(R.string.status_detail_architecture)

        composeTestRule.setContent {
            FitTrackTheme {
                StartupScreen()
            }
        }

        composeTestRule.onNodeWithText(expectedStatus).assertIsDisplayed()
        composeTestRule.onNodeWithText(expectedDetail).assertIsDisplayed()
    }
}
