package com.haitranduc.fittrack.presentation.activeworkout

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.haitranduc.fittrack.R
import com.haitranduc.fittrack.core.designsystem.FitTrackIcons
import com.haitranduc.fittrack.core.designsystem.theme.FitTrackTheme
import com.haitranduc.fittrack.domain.model.Exercise
import com.haitranduc.fittrack.domain.model.SetLog
import com.haitranduc.fittrack.domain.model.WorkoutSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun ActiveWorkoutScreen(
    onNavigateUp: () -> Unit,
    onFinishWorkout: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActiveWorkoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.session?.id, uiState.session?.finishedAt) {
        if (uiState.session != null && uiState.session?.finishedAt == null) {
            while (isActive) {
                delay(1000L)
                viewModel.onTimerTick()
            }
        }
    }

    LaunchedEffect(uiState.finishedSessionId) {
        uiState.finishedSessionId?.let { id ->
            onFinishWorkout(id)
        }
    }

    ActiveWorkoutContent(
        uiState = uiState,
        onRepsChanged = viewModel::onRepsChanged,
        onWeightChanged = viewModel::onWeightChanged,
        onCompleteSetClicked = viewModel::onCompleteSetClicked,
        onFinishClicked = viewModel::onFinishClicked,
        onNavigateUp = onNavigateUp,
        onSkipRestTimer = viewModel::onSkipRestTimer,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutContent(
    uiState: ActiveWorkoutUiState,
    onRepsChanged: (exerciseId: String, reps: String) -> Unit,
    onWeightChanged: (exerciseId: String, weight: String) -> Unit,
    onCompleteSetClicked: (Exercise) -> Unit,
    onFinishClicked: () -> Unit,
    onNavigateUp: () -> Unit,
    onSkipRestTimer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_active_workout),
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
        } else if (uiState.isMissing || uiState.session == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.error_session_not_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onNavigateUp) {
                        Text(stringResource(R.string.cd_navigate_up))
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Timer & Status Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.label_elapsed_time),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formatElapsedTime(uiState.elapsedTimeSeconds),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = uiState.session.workoutNameSnapshot,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Rest Timer Banner (M4)
                if (uiState.restTimerRemainingSeconds > 0L) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.label_rest_timer),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val minutes = uiState.restTimerRemainingSeconds / 60L
                                    val seconds = uiState.restTimerRemainingSeconds % 60L
                                    Text(
                                        text = stringResource(R.string.rest_timer_format, minutes, seconds),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Button(
                                    onClick = onSkipRestTimer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(stringResource(R.string.btn_skip_rest))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            val progress = (uiState.restTimerRemainingSeconds.toFloat() / 90f).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Exercises List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.exercises, key = { it.id }) { exercise ->
                        val exerciseSets = uiState.completedSets.filter { it.exerciseId == exercise.id }
                        val nextSetNumber = (exerciseSets.maxOfOrNull { it.setNumber } ?: 0) + 1
                        val currentReps = uiState.inputReps[exercise.id] ?: "10"
                        val currentWeight = uiState.inputWeight[exercise.id] ?: "50"
                        val errorText = uiState.inputErrors[exercise.id]

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(14.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = exercise.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                // Table Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.label_set_header),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(36.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.label_kg_header),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = stringResource(R.string.label_reps_header),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = stringResource(R.string.label_done_header),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(48.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Completed Sets
                                exerciseSets.forEach { setLog ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${setLog.setNumber}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.width(36.dp)
                                        )
                                        Text(
                                            text = if (setLog.weightKg % 1.0 == 0.0) "${setLog.weightKg.toInt()}" else "${setLog.weightKg}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "${setLog.reps}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        FilledIconButton(
                                            onClick = {},
                                            enabled = false,
                                            modifier = Modifier.width(48.dp),
                                            colors = IconButtonDefaults.filledIconButtonColors(
                                                disabledContainerColor = MaterialTheme.colorScheme.primary
                                            )
                                        ) {
                                            Icon(
                                                imageVector = FitTrackIcons.Check,
                                                contentDescription = stringResource(R.string.cd_set_done),
                                                tint = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                }

                                // Next Set Input Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$nextSetNumber",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(36.dp)
                                    )
                                    OutlinedTextField(
                                        value = currentWeight,
                                        onValueChange = { onWeightChanged(exercise.id, it) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 4.dp)
                                            .semantics {
                                                contentDescription = currentWeight.ifEmpty { "Weight" }
                                            }
                                    )
                                    OutlinedTextField(
                                        value = currentReps,
                                        onValueChange = { onRepsChanged(exercise.id, it) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 4.dp)
                                            .semantics {
                                                contentDescription = currentReps.ifEmpty { "Reps" }
                                            }
                                    )
                                    FilledIconButton(
                                        onClick = { onCompleteSetClicked(exercise) },
                                        enabled = !uiState.isCompletingSet,
                                        modifier = Modifier.width(48.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    ) {
                                        Icon(
                                            imageVector = FitTrackIcons.Check,
                                            contentDescription = stringResource(R.string.cd_set_done)
                                        )
                                    }
                                }

                                if (errorText != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = errorText.asString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.finishError != null) {
                    Text(
                        text = uiState.finishError.asString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage.asString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Button(
                    onClick = onFinishClicked,
                    enabled = !uiState.isFinishing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.btn_finish_workout),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private fun formatElapsedTime(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return "%02d:%02d:%02d".format(hrs, mins, secs)
}

@Preview(showBackground = true, name = "Active Workout - Populated")
@Composable
private fun ActiveWorkoutContentPreview() {
    FitTrackTheme {
        ActiveWorkoutContent(
            uiState = ActiveWorkoutUiState(
                isLoading = false,
                isMissing = false,
                elapsedTimeSeconds = 1455L,
                session = WorkoutSession(
                    id = 1L,
                    workoutId = 1L,
                    workoutNameSnapshot = "Push Day",
                    startedAt = 1000L,
                    finishedAt = null,
                    durationSeconds = null,
                    sets = listOf(
                        SetLog(
                            id = 1L,
                            sessionId = 1L,
                            exerciseId = "e1",
                            exerciseNameSnapshot = "Barbell Bench Press",
                            setNumber = 1,
                            reps = 10,
                            weightKg = 50.0,
                            completedAt = 1100L
                        )
                    )
                ),
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
                    )
                ),
                completedSets = listOf(
                    SetLog(
                        id = 1L,
                        sessionId = 1L,
                        exerciseId = "e1",
                        exerciseNameSnapshot = "Barbell Bench Press",
                        setNumber = 1,
                        reps = 10,
                        weightKg = 50.0,
                        completedAt = 1100L
                    )
                ),
                inputReps = mapOf("e1" to "10"),
                inputWeight = mapOf("e1" to "50")
            ),
            onRepsChanged = { _, _ -> },
            onWeightChanged = { _, _ -> },
            onCompleteSetClicked = {},
            onFinishClicked = {},
            onNavigateUp = {}
        )
    }
}

@Preview(showBackground = true, name = "Active Workout - Loading")
@Composable
private fun ActiveWorkoutContentLoadingPreview() {
    FitTrackTheme {
        ActiveWorkoutContent(
            uiState = ActiveWorkoutUiState(isLoading = true),
            onRepsChanged = { _, _ -> },
            onWeightChanged = { _, _ -> },
            onCompleteSetClicked = {},
            onFinishClicked = {},
            onNavigateUp = {}
        )
    }
}

@Preview(showBackground = true, name = "Active Workout - Rest Timer Active")
@Composable
private fun ActiveWorkoutContentRestTimerPreview() {
    FitTrackTheme {
        ActiveWorkoutContent(
            uiState = ActiveWorkoutUiState(
                isLoading = false,
                elapsedTimeSeconds = 300L,
                restTimerEndsAtMillis = 100_000L,
                restTimerRemainingSeconds = 75L,
                session = WorkoutSession(
                    id = 1L,
                    workoutId = 1L,
                    workoutNameSnapshot = "Chest Day",
                    startedAt = 1000L,
                    finishedAt = null,
                    durationSeconds = null,
                    sets = listOf(
                        SetLog(
                            id = 1L,
                            sessionId = 1L,
                            exerciseId = "e1",
                            exerciseNameSnapshot = "Barbell Bench Press",
                            setNumber = 1,
                            reps = 10,
                            weightKg = 50.0,
                            completedAt = 1100L
                        )
                    )
                ),
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
                    )
                ),
                completedSets = listOf(
                    SetLog(
                        id = 1L,
                        sessionId = 1L,
                        exerciseId = "e1",
                        exerciseNameSnapshot = "Barbell Bench Press",
                        setNumber = 1,
                        reps = 10,
                        weightKg = 50.0,
                        completedAt = 1100L
                    )
                ),
                inputReps = mapOf("e1" to "10"),
                inputWeight = mapOf("e1" to "50")
            ),
            onRepsChanged = { _, _ -> },
            onWeightChanged = { _, _ -> },
            onCompleteSetClicked = {},
            onFinishClicked = {},
            onNavigateUp = {},
            onSkipRestTimer = {}
        )
    }
}

@Preview(showBackground = true, name = "Active Workout - Missing")
@Composable
private fun ActiveWorkoutMissingPreview() {
    FitTrackTheme {
        ActiveWorkoutContent(
            uiState = ActiveWorkoutUiState(
                isLoading = false,
                isMissing = true,
                session = null
            ),
            onRepsChanged = { _, _ -> },
            onWeightChanged = { _, _ -> },
            onCompleteSetClicked = {},
            onFinishClicked = {},
            onNavigateUp = {},
            onSkipRestTimer = {}
        )
    }
}

@Preview(showBackground = true, name = "Active Workout - Dark")
@Composable
private fun ActiveWorkoutDarkPreview() {
    FitTrackTheme(darkTheme = true) {
        ActiveWorkoutContent(
            uiState = ActiveWorkoutUiState(
                isLoading = false,
                session = WorkoutSession(
                    id = 1L,
                    workoutId = 1L,
                    workoutNameSnapshot = "Push Day",
                    startedAt = 1000L,
                    finishedAt = null,
                    durationSeconds = 0L,
                    sets = listOf(
                        SetLog(
                            id = 1L,
                            sessionId = 1L,
                            exerciseId = "e1",
                            exerciseNameSnapshot = "Barbell Bench Press",
                            setNumber = 1,
                            reps = 10,
                            weightKg = 50.0,
                            completedAt = 1100L
                        )
                    )
                ),
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
                    )
                ),
                elapsedTimeSeconds = 125L,
                restTimerRemainingSeconds = 45L,
                completedSets = listOf(
                    SetLog(
                        id = 1L,
                        sessionId = 1L,
                        exerciseId = "e1",
                        exerciseNameSnapshot = "Barbell Bench Press",
                        setNumber = 1,
                        reps = 10,
                        weightKg = 50.0,
                        completedAt = 1100L
                    )
                ),
                inputReps = mapOf("e1" to "10"),
                inputWeight = mapOf("e1" to "50")
            ),
            onRepsChanged = { _, _ -> },
            onWeightChanged = { _, _ -> },
            onCompleteSetClicked = {},
            onFinishClicked = {},
            onNavigateUp = {},
            onSkipRestTimer = {}
        )
    }
}
