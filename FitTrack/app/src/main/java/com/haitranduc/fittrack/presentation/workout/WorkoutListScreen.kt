package com.haitranduc.fittrack.presentation.workout

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.haitranduc.fittrack.domain.model.Workout

@Composable
fun WorkoutListScreen(
    onCreateWorkout: () -> Unit,
    onWorkoutClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    WorkoutListContent(
        uiState = uiState,
        onCreateWorkout = onCreateWorkout,
        onWorkoutClick = onWorkoutClick,
        onDeleteClick = viewModel::onDeleteRequested,
        onConfirmDelete = viewModel::onDeleteConfirmed,
        onDismissDelete = viewModel::onDeleteDismissed,
        onRetry = viewModel::retry,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutListContent(
    uiState: WorkoutListUiState,
    onCreateWorkout: () -> Unit,
    onWorkoutClick: (Long) -> Unit,
    onDeleteClick: (Workout) -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_workouts),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateWorkout,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = FitTrackIcons.Add,
                    contentDescription = stringResource(R.string.cd_create_workout)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.errorMessage != null) {
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
            } else if (uiState.workouts.isEmpty()) {
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
                            text = stringResource(R.string.empty_workouts),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.workouts, key = { it.id }) { workout ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onWorkoutClick(workout.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(14.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = workout.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = stringResource(
                                                    R.string.label_exercises_count,
                                                    workout.exercises.size
                                                ),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onDeleteClick(workout) }
                                        ) {
                                            Icon(
                                                imageVector = FitTrackIcons.Delete,
                                                contentDescription = stringResource(R.string.cd_delete_workout),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                val exerciseNames = workout.exercises.map { it.name }
                                Text(
                                    text = if (exerciseNames.isEmpty()) {
                                        stringResource(R.string.error_workout_exercises_empty)
                                    } else {
                                        exerciseNames.joinToString(", ")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    if (uiState.workoutToDelete != null) {
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = {
                Text(
                    text = stringResource(R.string.dialog_delete_workout_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.dialog_delete_workout_message),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmDelete,
                    enabled = !uiState.isDeleting
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissDelete,
                    enabled = !uiState.isDeleting
                ) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Preview(showBackground = true, name = "Workout List - Populated")
@Composable
private fun WorkoutListContentPopulatedPreview() {
    FitTrackTheme {
        WorkoutListContent(
            uiState = WorkoutListUiState(
                isLoading = false,
                workouts = listOf(
                    Workout(
                        id = 1L,
                        name = "Push Day",
                        createdAt = 1000L,
                        updatedAt = 1000L,
                        exercises = listOf(
                            Exercise(
                                id = "e1",
                                name = "Barbell Bench Press",
                                bodyPart = "chest",
                                equipment = "barbell",
                                target = "pectorals",
                                muscleGroup = "chest",
                                secondaryMuscles = emptyList(),
                                instructions = emptyList()
                            ),
                            Exercise(
                                id = "e2",
                                name = "Overhead Press",
                                bodyPart = "shoulders",
                                equipment = "barbell",
                                target = "deltoids",
                                muscleGroup = "shoulders",
                                secondaryMuscles = emptyList(),
                                instructions = emptyList()
                            )
                        )
                    ),
                    Workout(
                        id = 2L,
                        name = "Pull Day",
                        createdAt = 2000L,
                        updatedAt = 2000L,
                        exercises = listOf(
                            Exercise(
                                id = "e3",
                                name = "Pull Up",
                                bodyPart = "back",
                                equipment = "bodyweight",
                                target = "latissimus dorsi",
                                muscleGroup = "back",
                                secondaryMuscles = emptyList(),
                                instructions = emptyList()
                            )
                        )
                    )
                )
            ),
            onCreateWorkout = {},
            onWorkoutClick = {},
            onDeleteClick = {},
            onConfirmDelete = {},
            onDismissDelete = {}
        )
    }
}

@Preview(showBackground = true, name = "Workout List - Empty")
@Composable
private fun WorkoutListContentEmptyPreview() {
    FitTrackTheme {
        WorkoutListContent(
            uiState = WorkoutListUiState(
                isLoading = false,
                workouts = emptyList()
            ),
            onCreateWorkout = {},
            onWorkoutClick = {},
            onDeleteClick = {},
            onConfirmDelete = {},
            onDismissDelete = {}
        )
    }
}

@Preview(showBackground = true, name = "Workout List - Loading")
@Composable
private fun WorkoutListContentLoadingPreview() {
    FitTrackTheme {
        WorkoutListContent(
            uiState = WorkoutListUiState(
                isLoading = true,
                workouts = emptyList()
            ),
            onCreateWorkout = {},
            onWorkoutClick = {},
            onDeleteClick = {},
            onConfirmDelete = {},
            onDismissDelete = {}
        )
    }
}
