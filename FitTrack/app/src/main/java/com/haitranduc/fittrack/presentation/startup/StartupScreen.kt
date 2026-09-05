package com.haitranduc.fittrack.presentation.startup

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.haitranduc.fittrack.R
import com.haitranduc.fittrack.core.designsystem.theme.FitTrackTheme
import com.haitranduc.fittrack.presentation.util.UiText

@Composable
fun StartupScreen(
    startupState: StartupUiState,
    onRetryStartup: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (startupState) {
        is StartupUiState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.loading_startup),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        is StartupUiState.Error -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.error_startup),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    startupState.message?.let {
                        Text(
                            text = it.asString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(onClick = onRetryStartup) {
                        Text(text = stringResource(R.string.action_retry))
                    }
                }
            }
        }
        is StartupUiState.Ready -> { /* no-op */ }
    }
}

@Preview(showBackground = true, name = "Startup Loading - Light")
@Composable
private fun StartupLoadingPreview() {
    FitTrackTheme(darkTheme = false) {
        Surface {
            StartupScreen(
                startupState = StartupUiState.Loading,
                onRetryStartup = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Startup Loading - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StartupLoadingDarkPreview() {
    FitTrackTheme(darkTheme = true) {
        Surface {
            StartupScreen(
                startupState = StartupUiState.Loading,
                onRetryStartup = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Startup Error - Light")
@Composable
private fun StartupErrorPreview() {
    FitTrackTheme(darkTheme = false) {
        Surface {
            StartupScreen(
                startupState = StartupUiState.Error(UiText.StringResource(R.string.error_database)),
                onRetryStartup = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Startup Error - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StartupErrorDarkPreview() {
    FitTrackTheme(darkTheme = true) {
        Surface {
            StartupScreen(
                startupState = StartupUiState.Error(UiText.StringResource(R.string.error_database)),
                onRetryStartup = {}
            )
        }
    }
}
