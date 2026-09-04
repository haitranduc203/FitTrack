package com.haitranduc.fittrack.presentation.exercise

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.haitranduc.fittrack.R
import com.haitranduc.fittrack.core.designsystem.FitTrackIcons
import com.haitranduc.fittrack.core.designsystem.theme.FitTrackTheme
import com.haitranduc.fittrack.domain.model.Exercise

enum class BodyPartFilterOption(val labelRes: Int, val queryValue: String?) {
    ALL(R.string.filter_all, null),
    CHEST(R.string.filter_chest, "chest"),
    SHOULDERS(R.string.filter_shoulders, "shoulders"),
    BACK(R.string.filter_back, "back"),
    UPPER_LEGS(R.string.filter_upper_legs, "upper legs"),
    LOWER_LEGS(R.string.filter_lower_legs, "lower legs"),
    UPPER_ARMS(R.string.filter_upper_arms, "upper arms"),
    WAIST(R.string.filter_waist, "waist"),
    CARDIO(R.string.filter_cardio, "cardio")
}

enum class EquipmentFilterOption(val labelRes: Int, val queryValue: String?) {
    ALL(R.string.filter_all, null),
    BARBELL(R.string.filter_barbell, "barbell"),
    BODYWEIGHT(R.string.filter_bodyweight, "body weight"),
    DUMBBELL(R.string.filter_dumbbell, "dumbbell"),
    CABLE(R.string.filter_cable, "cable")
}

@Composable
fun ExerciseListScreen(
    onExerciseClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExerciseListContent(
        uiState = uiState,
        onSearchQueryChange = viewModel::onSearchQueryChanged,
        onBodyPartSelect = viewModel::onBodyPartSelected,
        onEquipmentSelect = viewModel::onEquipmentSelected,
        onToggleFavoritesFilter = viewModel::onFavoritesFilterToggled,
        onToggleFavorite = viewModel::onToggleFavorite,
        onClearFavoriteError = viewModel::onClearFavoriteError,
        onExerciseClick = onExerciseClick,
        onRetry = viewModel::onRetry,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseListContent(
    uiState: ExerciseListUiState,
    onSearchQueryChange: (String) -> Unit,
    onBodyPartSelect: (String?) -> Unit,
    onEquipmentSelect: (String?) -> Unit,
    onToggleFavoritesFilter: (Boolean) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onClearFavoriteError: () -> Unit,
    onExerciseClick: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_exercises),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search input
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text(text = stringResource(R.string.label_search_exercises)) },
                placeholder = { Text(text = stringResource(R.string.hint_search_exercises)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Body Part Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_body_part_filter),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                BodyPartFilterOption.values().forEach { option ->
                    FilterChip(
                        selected = uiState.selectedBodyPart.equals(option.queryValue, ignoreCase = true),
                        onClick = { onBodyPartSelect(option.queryValue) },
                        label = { Text(text = stringResource(option.labelRes)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Equipment Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_equipment_filter),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                EquipmentFilterOption.values().forEach { option ->
                    FilterChip(
                        selected = uiState.selectedEquipment.equals(option.queryValue, ignoreCase = true),
                        onClick = { onEquipmentSelect(option.queryValue) },
                        label = { Text(text = stringResource(option.labelRes)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Favorites Filter Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = uiState.isFavoritesOnly,
                    onClick = { onToggleFavoritesFilter(!uiState.isFavoritesOnly) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (uiState.isFavoritesOnly) FitTrackIcons.Star else FitTrackIcons.StarBorder,
                            contentDescription = null,
                            tint = if (uiState.isFavoritesOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    label = { Text(text = stringResource(R.string.filter_favorites)) }
                )
            }

            if (uiState.favoriteErrorMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.favoriteErrorMessage.asString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onClearFavoriteError) {
                            Icon(
                                imageVector = FitTrackIcons.Close,
                                contentDescription = stringResource(R.string.cd_dismiss_error),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.errorMessage != null -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = uiState.errorMessage.asString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Button(onClick = onRetry) {
                                Text(text = stringResource(R.string.action_retry))
                            }
                        }
                    }
                }
                uiState.exercises.isEmpty() -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.empty_exercises),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.exercises, key = { it.id }) { exercise ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onExerciseClick(exercise.id) },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(14.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = exercise.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = exercise.bodyPart,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                            Surface(
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = exercise.equipment,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = exercise.target,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                    val isFavorite = exercise.id in uiState.favoriteExerciseIds
                                    val isPending = exercise.id in uiState.pendingFavoriteIds
                                    IconButton(
                                        onClick = { onToggleFavorite(exercise.id) },
                                        enabled = !isPending
                                    ) {
                                        Icon(
                                            imageVector = if (isFavorite) FitTrackIcons.Star else FitTrackIcons.StarBorder,
                                            contentDescription = stringResource(
                                                if (isFavorite) R.string.cd_unfavorite_exercise else R.string.cd_favorite_exercise
                                            ),
                                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Exercise List Screen")
@Composable
private fun ExerciseListScreenPreview() {
    FitTrackTheme {
        ExerciseListContent(
            uiState = ExerciseListUiState(
                exercises = listOf(
                    Exercise(
                        id = "0025",
                        name = "barbell bench press",
                        bodyPart = "chest",
                        equipment = "barbell",
                        target = "pectorals",
                        muscleGroup = "triceps",
                        secondaryMuscles = listOf("triceps", "shoulders"),
                        instructions = listOf("Step 1", "Step 2")
                    )
                ),
                favoriteExerciseIds = setOf("0025")
            ),
            onSearchQueryChange = {},
            onBodyPartSelect = {},
            onEquipmentSelect = {},
            onToggleFavoritesFilter = {},
            onToggleFavorite = {},
            onClearFavoriteError = {},
            onExerciseClick = {},
            onRetry = {}
        )
    }
}
