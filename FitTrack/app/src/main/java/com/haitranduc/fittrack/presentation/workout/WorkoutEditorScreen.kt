package com.haitranduc.fittrack.presentation.workout

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.haitranduc.fittrack.domain.model.Exercise

@Composable
fun WorkoutEditorScreen(
    workoutId: Long?,
    onNavigateUp: () -> Unit,
    onStartWorkout: (Long) -> Unit,
    onWorkoutSaved: () -> Unit = onNavigateUp,
    modifier: Modifier = Modifier,
    viewModel: WorkoutEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }

    val handleBack: () -> Unit = {
        if (uiState.hasUnsavedChanges) {
            showDiscardDialog = true
        } else {
            onNavigateUp()
        }
    }

    BackHandler(enabled = uiState.isPickerOpen) {
        viewModel.closeExercisePicker()
    }

    BackHandler(enabled = !uiState.isPickerOpen && uiState.hasUnsavedChanges) {
        showDiscardDialog = true
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is WorkoutEditorEvent.NavigateBack -> onWorkoutSaved()
                is WorkoutEditorEvent.NavigateToActiveWorkout -> onStartWorkout(event.sessionId)
                is WorkoutEditorEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message.asString(context))
                }
            }
        }
    }

    WorkoutEditorContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        showDiscardDialog = showDiscardDialog,
        onConfirmDiscard = {
            showDiscardDialog = false
            onNavigateUp()
        },
        onDismissDiscard = {
            showDiscardDialog = false
        },
        onNameChanged = viewModel::onNameChanged,
        onAddExerciseClick = viewModel::openExercisePicker,
        onRemoveExercise = viewModel::onRemoveExercise,
        onMoveUp = viewModel::onMoveExerciseUp,
        onMoveDown = viewModel::onMoveExerciseDown,
        onSaveClick = viewModel::onSaveClicked,
        onStartClick = viewModel::onStartClicked,
        onPickerDismiss = viewModel::closeExercisePicker,
        onPickerQueryChange = viewModel::onPickerQueryChanged,
        onPickerExerciseSelect = viewModel::onExerciseSelected,
        onNavigateUp = handleBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutEditorContent(
    uiState: WorkoutEditorUiState,
    onNameChanged: (String) -> Unit,
    onAddExerciseClick: () -> Unit,
    onRemoveExercise: (Int) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onSaveClick: () -> Unit,
    onStartClick: () -> Unit,
    onPickerDismiss: () -> Unit,
    onPickerQueryChange: (String) -> Unit,
    onPickerExerciseSelect: (Exercise) -> Unit,
    onNavigateUp: () -> Unit,
    showDiscardDialog: Boolean = false,
    onConfirmDiscard: () -> Unit = {},
    onDismissDiscard: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.workoutId == null) {
                            stringResource(R.string.title_new_workout)
                        } else {
                            uiState.workoutName.ifEmpty { stringResource(R.string.title_edit_workout) }
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = FitTrackIcons.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_up)
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = onSaveClick,
                        enabled = !uiState.isSaving && !uiState.isLoading && !uiState.isMissing
                    ) {
                        Text(
                            text = stringResource(R.string.btn_save_workout),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.isMissing -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.error_workout_not_found),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Button(onClick = onNavigateUp) {
                                Text(text = stringResource(R.string.cd_navigate_up))
                            }
                        }
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    uiState.errorMessage?.let { error ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Text(
                                text = error.asString(),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Workout Name Field
                    OutlinedTextField(
                        value = uiState.workoutName,
                        onValueChange = onNameChanged,
                        label = { Text(text = stringResource(R.string.label_workout_name)) },
                        isError = uiState.nameErrorRes != null,
                        supportingText = uiState.nameErrorRes?.let {
                            { Text(text = stringResource(it)) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.section_exercises),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.semantics { heading() }
                        )
                        OutlinedButton(
                            onClick = onAddExerciseClick,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = stringResource(R.string.btn_add_exercise))
                        }
                    }

                    uiState.exerciseErrorRes?.let { errorRes ->
                        Text(
                            text = stringResource(errorRes),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (uiState.exercises.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stringResource(R.string.error_workout_exercises_empty),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            itemsIndexed(
                                items = uiState.exercises,
                                key = { _, exercise -> exercise.id },
                                contentType = { _, _ -> "editor_exercise_item" }
                            ) { index, exercise ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = exercise.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = exercise.bodyPart,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Surface(
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = exercise.equipment,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { onMoveUp(index) },
                                            enabled = index > 0
                                        ) {
                                            Icon(
                                                imageVector = FitTrackIcons.ArrowUp,
                                                contentDescription = stringResource(R.string.cd_move_up)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onMoveDown(index) },
                                            enabled = index < uiState.exercises.size - 1
                                        ) {
                                            Icon(
                                                imageVector = FitTrackIcons.ArrowDown,
                                                contentDescription = stringResource(R.string.cd_move_down)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onRemoveExercise(index) }
                                        ) {
                                            Icon(
                                                imageVector = FitTrackIcons.Close,
                                                contentDescription = stringResource(R.string.cd_remove_exercise),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Start Workout Button (validates, saves if needed, and starts session)
                    Button(
                        onClick = onStartClick,
                        enabled = !uiState.isSaving && !uiState.isLoading && !uiState.isMissing && uiState.exercises.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = FitTrackIcons.Play,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_start_workout),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    ExercisePickerDialog(
        isOpen = uiState.isPickerOpen,
        exercises = uiState.pickerExercises,
        searchQuery = uiState.pickerQuery,
        onSearchQueryChange = onPickerQueryChange,
        onExerciseSelect = onPickerExerciseSelect,
        onDismiss = onPickerDismiss
    )

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = onDismissDiscard,
            title = {
                Text(
                    text = stringResource(R.string.dialog_discard_changes_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.dialog_discard_changes_message),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmDiscard) {
                    Text(
                        text = stringResource(R.string.action_discard),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDiscard) {
                    Text(text = stringResource(R.string.action_keep_editing))
                }
            }
        )
    }
}

@Preview(showBackground = true, name = "Workout Editor - Create Mode")
@Composable
private fun WorkoutEditorCreatePreview() {
    FitTrackTheme {
        WorkoutEditorContent(
            uiState = WorkoutEditorUiState(
                workoutName = "",
                exercises = emptyList()
            ),
            onNameChanged = {},
            onAddExerciseClick = {},
            onRemoveExercise = {},
            onMoveUp = {},
            onMoveDown = {},
            onSaveClick = {},
            onStartClick = {},
            onPickerDismiss = {},
            onPickerQueryChange = {},
            onPickerExerciseSelect = {},
            onNavigateUp = {}
        )
    }
}

@Preview(showBackground = true, name = "Workout Editor - Edit Mode")
@Composable
private fun WorkoutEditorEditPreview() {
    FitTrackTheme {
        WorkoutEditorContent(
            uiState = WorkoutEditorUiState(
                workoutId = 1L,
                workoutName = "Push Day",
                exercises = listOf(
                    Exercise(
                        id = "0025",
                        name = "barbell bench press",
                        bodyPart = "chest",
                        equipment = "barbell",
                        target = "pectorals",
                        muscleGroup = "triceps",
                        secondaryMuscles = listOf("triceps", "shoulders"),
                        instructions = listOf("Lie on bench", "Press bar")
                    )
                )
            ),
            onNameChanged = {},
            onAddExerciseClick = {},
            onRemoveExercise = {},
            onMoveUp = {},
            onMoveDown = {},
            onSaveClick = {},
            onStartClick = {},
            onPickerDismiss = {},
            onPickerQueryChange = {},
            onPickerExerciseSelect = {},
            onNavigateUp = {}
        )
    }
}

@Preview(showBackground = true, name = "Workout Editor - Loading")
@Composable
private fun WorkoutEditorLoadingPreview() {
    FitTrackTheme {
        WorkoutEditorContent(
            uiState = WorkoutEditorUiState(
                isLoading = true
            ),
            onNameChanged = {},
            onAddExerciseClick = {},
            onRemoveExercise = {},
            onMoveUp = {},
            onMoveDown = {},
            onSaveClick = {},
            onStartClick = {},
            onPickerDismiss = {},
            onPickerQueryChange = {},
            onPickerExerciseSelect = {},
            onNavigateUp = {}
        )
    }
}

@Preview(showBackground = true, name = "Workout Editor - Dark")
@Composable
private fun WorkoutEditorDarkPreview() {
    FitTrackTheme(darkTheme = true) {
        WorkoutEditorContent(
            uiState = WorkoutEditorUiState(
                workoutId = 1L,
                workoutName = "Push Day",
                exercises = listOf(
                    Exercise(
                        id = "0025",
                        name = "barbell bench press",
                        bodyPart = "chest",
                        equipment = "barbell",
                        target = "pectorals",
                        muscleGroup = "triceps",
                        secondaryMuscles = listOf("triceps", "shoulders"),
                        instructions = listOf("Lie on bench", "Press bar")
                    )
                )
            ),
            onNameChanged = {},
            onAddExerciseClick = {},
            onRemoveExercise = {},
            onMoveUp = {},
            onMoveDown = {},
            onSaveClick = {},
            onStartClick = {},
            onPickerDismiss = {},
            onPickerQueryChange = {},
            onPickerExerciseSelect = {},
            onNavigateUp = {}
        )
    }
}
