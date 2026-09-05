package com.haitranduc.fittrack.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.haitranduc.fittrack.R
import com.haitranduc.fittrack.core.designsystem.FitTrackIcons
import com.haitranduc.fittrack.core.designsystem.theme.FitTrackTheme
import com.haitranduc.fittrack.domain.model.ThemePreference
import com.haitranduc.fittrack.presentation.util.UiText

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsContent(
        uiState = uiState,
        onThemeSelected = viewModel::onThemeSelected,
        onClearError = viewModel::onClearError,
        onRetry = viewModel::retry,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    onThemeSelected: (ThemePreference) -> Unit,
    onClearError: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_settings),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.errorMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = uiState.errorMessage.asString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = onClearError) {
                                    Icon(
                                        imageVector = FitTrackIcons.Close,
                                        contentDescription = stringResource(R.string.cd_dismiss_error),
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                            Button(
                                onClick = onRetry,
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(text = stringResource(R.string.action_retry))
                            }
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.section_theme),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() }
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectableGroup()
                            .padding(vertical = 8.dp)
                    ) {
                        ThemeOptionRow(
                            label = stringResource(R.string.theme_system),
                            selected = uiState.selectedTheme == ThemePreference.SYSTEM,
                            enabled = !uiState.isUpdatingTheme,
                            onClick = { onThemeSelected(ThemePreference.SYSTEM) }
                        )

                        ThemeOptionRow(
                            label = stringResource(R.string.theme_light),
                            selected = uiState.selectedTheme == ThemePreference.LIGHT,
                            enabled = !uiState.isUpdatingTheme,
                            onClick = { onThemeSelected(ThemePreference.LIGHT) }
                        )

                        ThemeOptionRow(
                            label = stringResource(R.string.theme_dark),
                            selected = uiState.selectedTheme == ThemePreference.DARK,
                            enabled = !uiState.isUpdatingTheme,
                            onClick = { onThemeSelected(ThemePreference.DARK) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            enabled = enabled
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    }
}

@Preview(showBackground = true, name = "Settings Screen - System Default")
@Composable
private fun SettingsScreenDefaultPreview() {
    FitTrackTheme {
        SettingsContent(
            uiState = SettingsUiState(
                selectedTheme = ThemePreference.SYSTEM
            ),
            onThemeSelected = {},
            onClearError = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true, name = "Settings Screen - Dark Selected")
@Composable
private fun SettingsScreenDarkPreview() {
    FitTrackTheme(darkTheme = true) {
        SettingsContent(
            uiState = SettingsUiState(
                selectedTheme = ThemePreference.DARK
            ),
            onThemeSelected = {},
            onClearError = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true, name = "Settings Screen - Error State")
@Composable
private fun SettingsScreenErrorPreview() {
    FitTrackTheme {
        SettingsContent(
            uiState = SettingsUiState(
                selectedTheme = ThemePreference.SYSTEM,
                errorMessage = UiText.StringResource(R.string.error_database)
            ),
            onThemeSelected = {},
            onClearError = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true, name = "Settings Screen - Loading")
@Composable
private fun SettingsScreenLoadingPreview() {
    FitTrackTheme {
        SettingsContent(
            uiState = SettingsUiState(
                isLoading = true
            ),
            onThemeSelected = {},
            onClearError = {},
            onRetry = {}
        )
    }
}
