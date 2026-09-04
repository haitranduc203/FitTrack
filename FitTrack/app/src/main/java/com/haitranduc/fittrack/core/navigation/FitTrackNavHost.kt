package com.haitranduc.fittrack.core.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.haitranduc.fittrack.R
import com.haitranduc.fittrack.core.designsystem.FitTrackIcons
import com.haitranduc.fittrack.presentation.activeworkout.ActiveWorkoutScreen
import com.haitranduc.fittrack.presentation.exercise.ExerciseDetailScreen
import com.haitranduc.fittrack.presentation.exercise.ExerciseListScreen
import com.haitranduc.fittrack.presentation.history.HistoryDetailScreen
import com.haitranduc.fittrack.presentation.history.HistoryScreen
import com.haitranduc.fittrack.presentation.startup.FitTrackViewModel
import com.haitranduc.fittrack.presentation.startup.StartupUiState
import com.haitranduc.fittrack.presentation.workout.WorkoutEditorScreen
import com.haitranduc.fittrack.presentation.workout.WorkoutListScreen

@Composable
fun FitTrackNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startupViewModel: FitTrackViewModel = hiltViewModel()
) {
    val startupState by startupViewModel.uiState.collectAsStateWithLifecycle()
    FitTrackNavHostContent(
        startupState = startupState,
        onRetryStartup = { startupViewModel.retry() },
        modifier = modifier,
        navController = navController
    )
}

@Composable
fun FitTrackNavHostContent(
    startupState: StartupUiState,
    onRetryStartup: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
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
                            text = it,
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
        is StartupUiState.Ready -> {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            val topLevelRoutes = setOf(
                FitTrackDestination.EXERCISES,
                FitTrackDestination.WORKOUTS,
                FitTrackDestination.HISTORY
            )

            val showBottomBar = currentRoute in topLevelRoutes

            Scaffold(
                modifier = modifier.fillMaxSize(),
                bottomBar = {
                    if (showBottomBar) {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentRoute == FitTrackDestination.EXERCISES,
                                onClick = {
                                    navController.navigate(FitTrackDestination.EXERCISES) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = FitTrackIcons.Exercises,
                                        contentDescription = stringResource(R.string.nav_exercises)
                                    )
                                },
                                label = { Text(text = stringResource(R.string.nav_exercises)) }
                            )

                            NavigationBarItem(
                                selected = currentRoute == FitTrackDestination.WORKOUTS,
                                onClick = {
                                    navController.navigate(FitTrackDestination.WORKOUTS) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = FitTrackIcons.Workouts,
                                        contentDescription = stringResource(R.string.nav_workouts)
                                    )
                                },
                                label = { Text(text = stringResource(R.string.nav_workouts)) }
                            )

                            NavigationBarItem(
                                selected = currentRoute == FitTrackDestination.HISTORY,
                                onClick = {
                                    navController.navigate(FitTrackDestination.HISTORY) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = FitTrackIcons.History,
                                        contentDescription = stringResource(R.string.nav_history)
                                    )
                                },
                                label = { Text(text = stringResource(R.string.nav_history)) }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = FitTrackDestination.EXERCISES,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(FitTrackDestination.EXERCISES) {
                        ExerciseListScreen(
                            onExerciseClick = { exerciseId ->
                                navController.navigate(FitTrackDestination.exerciseDetailRoute(exerciseId))
                            }
                        )
                    }

                    composable(FitTrackDestination.EXERCISE_DETAIL) { backStackEntry ->
                        val exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: ""
                        ExerciseDetailScreen(
                            exerciseId = exerciseId,
                            onNavigateUp = { navController.navigateUp() }
                        )
                    }

                    composable(FitTrackDestination.WORKOUTS) {
                        WorkoutListScreen(
                            onCreateWorkout = {
                                navController.navigate(FitTrackDestination.WORKOUT_EDITOR)
                            },
                            onWorkoutClick = { workoutId ->
                                navController.navigate(FitTrackDestination.workoutEditorRoute(workoutId))
                            }
                        )
                    }

                    composable(FitTrackDestination.WORKOUT_EDITOR) {
                        WorkoutEditorScreen(
                            workoutId = null,
                            onNavigateUp = { navController.navigateUp() },
                            onStartWorkout = { sessionId ->
                                navController.navigate(FitTrackDestination.activeWorkoutRoute(sessionId))
                            }
                        )
                    }

                    composable(FitTrackDestination.WORKOUT_EDITOR_WITH_ID) { backStackEntry ->
                        val workoutId = backStackEntry.arguments?.getString("workoutId")?.toLongOrNull()
                        WorkoutEditorScreen(
                            workoutId = workoutId,
                            onNavigateUp = { navController.navigateUp() },
                            onStartWorkout = { sessionId ->
                                navController.navigate(FitTrackDestination.activeWorkoutRoute(sessionId))
                            }
                        )
                    }

                    composable(FitTrackDestination.ACTIVE_WORKOUT) {
                        ActiveWorkoutScreen(
                            onNavigateUp = { navController.navigateUp() },
                            onFinishWorkout = { finishedSessionId ->
                                navController.navigate(FitTrackDestination.historyDetailRoute(finishedSessionId)) {
                                    popUpTo(FitTrackDestination.WORKOUTS) {
                                        inclusive = false
                                    }
                                }
                            }
                        )
                    }

                    composable(FitTrackDestination.HISTORY) {
                        HistoryScreen(
                            onSessionClick = { sessionId ->
                                navController.navigate(FitTrackDestination.historyDetailRoute(sessionId))
                            }
                        )
                    }

                    composable(FitTrackDestination.HISTORY_DETAIL) { backStackEntry ->
                        val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
                        HistoryDetailScreen(
                            sessionId = sessionId,
                            onNavigateUp = { navController.navigateUp() }
                        )
                    }
                }
            }
        }
    }
}
