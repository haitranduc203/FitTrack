package com.haitranduc.fittrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.haitranduc.fittrack.core.designsystem.theme.FitTrackTheme
import com.haitranduc.fittrack.core.navigation.FitTrackNavHost
import com.haitranduc.fittrack.domain.model.ThemePreference
import com.haitranduc.fittrack.presentation.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themePreference by themeViewModel.themePreference.collectAsStateWithLifecycle()
            val useDarkTheme = when (themePreference) {
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }

            FitTrackTheme(darkTheme = useDarkTheme) {
                FitTrackNavHost()
            }
        }
    }
}
