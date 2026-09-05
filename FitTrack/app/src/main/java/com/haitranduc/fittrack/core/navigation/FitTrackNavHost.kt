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
import com.haitranduc.fittrack.presentation.settings.SettingsScreen
import com.haitranduc.fittrack.presentation.startup.FitTrackViewModel
import com.haitranduc.fittrack.presentation.startup.StartupScreen
import com.haitranduc.fittrack.presentation.startup.StartupUiState
import com.haitranduc.fittrack.presentation.workout.WorkoutEditorScreen
import com.haitranduc.fittrack.presentation.workout.WorkoutListScreen
import com.haitranduc.fittrack.presentation.workout.WorkoutListViewModel

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
    if (startupState !is StartupUiState.Ready) {
        StartupScreen(
            startupState = startupState,
            onRetryStartup = onRetryStartup,
            modifier = modifier
        )
        return
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            val topLevelRoutes = setOf(
                FitTrackDestination.EXERCISES,
                FitTrackDestination.WORKOUTS,
                FitTrackDestination.HISTORY,
                FitTrackDestination.SETTINGS
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

                            NavigationBarItem(
                                selected = currentRoute == FitTrackDestination.SETTINGS,
                                onClick = {
                                    navController.navigate(FitTrackDestination.SETTINGS) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = FitTrackIcons.Settings,
                                        contentDescription = stringResource(R.string.nav_settings)
                                    )
                                },
                                label = { Text(text = stringResource(R.string.nav_settings)) }
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

                    composable(FitTrackDestination.WORKOUTS) { backStackEntry ->
                        val workoutSaved by backStackEntry.savedStateHandle
                            .getStateFlow(WorkoutListViewModel.KEY_WORKOUT_SAVED, false)
                            .collectAsStateWithLifecycle()

                        WorkoutListScreen(
                            onCreateWorkout = {
                                navController.navigate(FitTrackDestination.WORKOUT_EDITOR)
                            },
                            onWorkoutClick = { workoutId ->
                                navController.navigate(FitTrackDestination.workoutEditorRoute(workoutId))
                            },
                            workoutSavedResult = workoutSaved,
                            onConsumeWorkoutSavedResult = {
                                backStackEntry.savedStateHandle.remove<Boolean>(WorkoutListViewModel.KEY_WORKOUT_SAVED)
                            }
                        )
                    }

                    composable(FitTrackDestination.WORKOUT_EDITOR) {
                        WorkoutEditorScreen(
                            workoutId = null,
                            onNavigateUp = { navController.navigateUp() },
                            onWorkoutSaved = {
                                navController.previousBackStackEntry?.savedStateHandle?.set(
                                    WorkoutListViewModel.KEY_WORKOUT_SAVED,
                                    true
                                )
                                navController.navigateUp()
                            },
                            onStartWorkout = { sessionId ->
                                navController.navigate(FitTrackDestination.activeWorkoutRoute(sessionId)) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(FitTrackDestination.WORKOUT_EDITOR_WITH_ID) { backStackEntry ->
                        val workoutId = backStackEntry.arguments?.getString("workoutId")?.toLongOrNull()
                        WorkoutEditorScreen(
                            workoutId = workoutId,
                            onNavigateUp = { navController.navigateUp() },
                            onWorkoutSaved = {
                                navController.previousBackStackEntry?.savedStateHandle?.set(
                                    WorkoutListViewModel.KEY_WORKOUT_SAVED,
                                    true
                                )
                                navController.navigateUp()
                            },
                            onStartWorkout = { sessionId ->
                                navController.navigate(FitTrackDestination.activeWorkoutRoute(sessionId)) {
                                    launchSingleTop = true
                                }
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

                    composable(FitTrackDestination.HISTORY_DETAIL) {
                        HistoryDetailScreen(
                            onNavigateUp = { navController.navigateUp() }
                        )
                    }

                    composable(FitTrackDestination.SETTINGS) {
                        SettingsScreen()
                    }
                }
            }
}
