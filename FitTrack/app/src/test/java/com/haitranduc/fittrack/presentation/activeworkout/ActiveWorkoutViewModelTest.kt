package com.haitranduc.fittrack.presentation.activeworkout

import androidx.lifecycle.SavedStateHandle
import com.haitranduc.fittrack.R
import com.haitranduc.fittrack.domain.model.Exercise
import com.haitranduc.fittrack.domain.model.SetLog
import com.haitranduc.fittrack.domain.model.Workout
import com.haitranduc.fittrack.domain.model.WorkoutSession
import com.haitranduc.fittrack.domain.repository.DataError
import com.haitranduc.fittrack.domain.usecase.CompleteSetUseCase
import com.haitranduc.fittrack.domain.usecase.FinishWorkoutUseCase
import com.haitranduc.fittrack.presentation.util.UiText
import com.haitranduc.fittrack.testing.FakeTimeProvider
import com.haitranduc.fittrack.testing.FakeWorkoutHistoryRepository
import com.haitranduc.fittrack.testing.FakeWorkoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveWorkoutViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var workoutHistoryRepository: FakeWorkoutHistoryRepository
    private lateinit var workoutRepository: FakeWorkoutRepository
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var completeSetUseCase: CompleteSetUseCase
    private lateinit var finishWorkoutUseCase: FinishWorkoutUseCase

    private val sampleExercise = Exercise(
        id = "e1",
        name = "Bench Press",
        bodyPart = "chest",
        equipment = "barbell",
        target = "pectorals",
        muscleGroup = "chest",
        secondaryMuscles = emptyList(),
        instructions = emptyList()
    )

    private val sampleWorkout = Workout(
        id = 1L,
        name = "Push Day",
        createdAt = 1000L,
        updatedAt = 1000L,
        exercises = listOf(sampleExercise)
    )

    private val sampleSession = WorkoutSession(
        id = 10L,
        workoutId = 1L,
        workoutNameSnapshot = "Push Day",
        startedAt = 10_000L,
        finishedAt = null,
        durationSeconds = null,
        sets = emptyList()
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        workoutHistoryRepository = FakeWorkoutHistoryRepository()
        workoutRepository = FakeWorkoutRepository()
        timeProvider = FakeTimeProvider(currentTime = 15_000L) // 5s elapsed
        completeSetUseCase = CompleteSetUseCase(workoutHistoryRepository, timeProvider)
        finishWorkoutUseCase = FinishWorkoutUseCase(workoutHistoryRepository, timeProvider)

        workoutRepository.setWorkouts(listOf(sampleWorkout))
        workoutHistoryRepository.sessions.add(sampleSession)
        workoutHistoryRepository.refreshFlow()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        sessionId: Any? = 10L,
        handle: SavedStateHandle = if (sessionId != null) {
            SavedStateHandle(mapOf("sessionId" to sessionId))
        } else {
            SavedStateHandle()
        }
    ): ActiveWorkoutViewModel {
        return ActiveWorkoutViewModel(
            savedStateHandle = handle,
            workoutHistoryRepository = workoutHistoryRepository,
            workoutRepository = workoutRepository,
            completeSetUseCase = completeSetUseCase,
            finishWorkoutUseCase = finishWorkoutUseCase,
            timeProvider = timeProvider
        )
    }

    @Test
    fun defensiveSessionIdParsing_invalidOrMissingId_producesMissingState() = runTest {
        val vmMissing = createViewModel(sessionId = null)
        advanceUntilIdle()
        assertTrue(vmMissing.uiState.value.isMissing)
        assertFalse(vmMissing.uiState.value.isLoading)

        val vmInvalid = createViewModel(sessionId = "not_a_number")
        advanceUntilIdle()
        assertTrue(vmInvalid.uiState.value.isMissing)

        val vmNegative = createViewModel(sessionId = -1L)
        advanceUntilIdle()
        assertTrue(vmNegative.uiState.value.isMissing)
    }

    @Test
    fun sessionNotFound_producesMissingState() = runTest {
        val vm = createViewModel(sessionId = 999L)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isMissing)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun validSession_loadsSessionAndExercisesAndElapsedTime() = runTest {
        val vm = createViewModel(sessionId = 10L)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isMissing)
        assertNull(state.errorMessage)
        assertEquals("Push Day", state.session?.workoutNameSnapshot)
        assertEquals(1, state.exercises.size)
        assertEquals("Bench Press", state.exercises[0].name)
        // 15000 - 10000 = 5 seconds
        assertEquals(5L, state.elapsedTimeSeconds)
    }

    @Test
    fun onTimerTick_updatesElapsedTimeFromPersistedStartTime() = runTest {
        val vm = createViewModel(sessionId = 10L)
        advanceUntilIdle()

        assertEquals(5L, vm.uiState.value.elapsedTimeSeconds)

        timeProvider.currentTime = 18_000L
        vm.onTimerTick()

        assertEquals(8L, vm.uiState.value.elapsedTimeSeconds)
    }

    @Test
    fun repsAndWeightInput_changesReflectedInState() = runTest {
        val vm = createViewModel(sessionId = 10L)
        advanceUntilIdle()

        vm.onRepsChanged("e1", "12")
        vm.onWeightChanged("e1", "62.5")

        assertEquals("12", vm.uiState.value.inputReps["e1"])
        assertEquals("62.5", vm.uiState.value.inputWeight["e1"])
    }

    @Test
    fun completeSet_invalidInput_showsInlineErrorWithoutWriting() = runTest {
        val vm = createViewModel(sessionId = 10L)
        advanceUntilIdle()

        vm.onRepsChanged("e1", "0") // invalid: < 1
        vm.onWeightChanged("e1", "50")
        vm.onCompleteSetClicked(sampleExercise)
        advanceUntilIdle()

        assertEquals(UiText.StringResource(R.string.error_invalid_reps), vm.uiState.value.inputErrors["e1"])
        assertTrue(workoutHistoryRepository.setLogs.isEmpty())

        // Non-number
        vm.onRepsChanged("e1", "abc")
        vm.onCompleteSetClicked(sampleExercise)
        advanceUntilIdle()

        assertEquals(UiText.StringResource(R.string.error_invalid_reps_number), vm.uiState.value.inputErrors["e1"])
        assertTrue(workoutHistoryRepository.setLogs.isEmpty())
    }

    @Test
    fun completeSet_success_persistsImmediatelyAndIncrementsSetNumber() = runTest {
        val vm = createViewModel(sessionId = 10L)
        advanceUntilIdle()

        vm.onRepsChanged("e1", "10")
        vm.onWeightChanged("e1", "50")
        vm.onCompleteSetClicked(sampleExercise)
        advanceUntilIdle()

        val state1 = vm.uiState.value
        assertNull(state1.inputErrors["e1"])
        assertEquals(1, state1.completedSets.size)
        assertEquals(1, state1.completedSets[0].setNumber)
        assertEquals(10, state1.completedSets[0].reps)
        assertEquals(50.0, state1.completedSets[0].weightKg, 0.001)

        // Complete a second set
        vm.onRepsChanged("e1", "8")
        vm.onWeightChanged("e1", "55")
        vm.onCompleteSetClicked(sampleExercise)
        advanceUntilIdle()

        val state2 = vm.uiState.value
        assertEquals(2, state2.completedSets.size)
        assertEquals(2, state2.completedSets[1].setNumber)
        assertEquals(8, state2.completedSets[1].reps)
        assertEquals(55.0, state2.completedSets[1].weightKg, 0.001)
    }

    @Test
    fun completeSet_persistenceFailure_surfacesErrorMessage() = runTest {
        workoutHistoryRepository.insertSetError = DataError.Database(RuntimeException("Disk error"))
        val vm = createViewModel(sessionId = 10L)
        advanceUntilIdle()

        vm.onRepsChanged("e1", "10")
        vm.onWeightChanged("e1", "50")
        vm.onCompleteSetClicked(sampleExercise)
        advanceUntilIdle()

        assertEquals(UiText.StringResource(R.string.error_database), vm.uiState.value.errorMessage)
        assertTrue(vm.uiState.value.completedSets.isEmpty())
    }

    @Test
    fun finishWorkout_noCompletedSets_rejectedWithErrorMessage() = runTest {
        val vm = createViewModel(sessionId = 10L)
        advanceUntilIdle()

        vm.onFinishClicked()
        advanceUntilIdle()

        assertEquals(UiText.StringResource(R.string.error_no_completed_sets), vm.uiState.value.finishError)
        assertNull(vm.uiState.value.finishedSessionId)
    }

    @Test
    fun finishWorkout_withCompletedSets_successEmitsFinishEvent() = runTest {
        val vm = createViewModel(sessionId = 10L)
        advanceUntilIdle()

        // Complete 1 set
        vm.onRepsChanged("e1", "10")
        vm.onWeightChanged("e1", "50")
        vm.onCompleteSetClicked(sampleExercise)
        advanceUntilIdle()

        timeProvider.currentTime = 20_000L // finished at 20000 -> duration = 10s
        vm.onFinishClicked()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertNull(state.finishError)
        assertEquals(10L, state.finishedSessionId)

        val updatedSession = workoutHistoryRepository.sessions.find { it.id == 10L }
        assertEquals(20_000L, updatedSession?.finishedAt)
        assertEquals(10L, updatedSession?.durationSeconds)
    }

    @Test
    fun duplicateTap_preventsMultipleFinishCalls() = runTest {
        val vm = createViewModel(sessionId = 10L)
        advanceUntilIdle()

        vm.onRepsChanged("e1", "10")
        vm.onWeightChanged("e1", "50")
        vm.onCompleteSetClicked(sampleExercise)
        advanceUntilIdle()

        vm.onFinishClicked()
        vm.onFinishClicked()
        advanceUntilIdle()

        assertEquals(10L, vm.uiState.value.finishedSessionId)
    }

    @Test
    fun completeSet_success_startsRestTimerAt90Seconds() = runTest {
        val handle = SavedStateHandle(mapOf("sessionId" to 10L))
        val vm = createViewModel(sessionId = 10L, handle = handle)
        advanceUntilIdle()

        vm.onRepsChanged("e1", "10")
        vm.onWeightChanged("e1", "50")
        vm.onCompleteSetClicked(sampleExercise)
        advanceUntilIdle()

        assertEquals(90L, vm.uiState.value.restTimerRemainingSeconds)
        assertEquals(15_000L + 90_000L, vm.uiState.value.restTimerEndsAtMillis)
        assertEquals(15_000L + 90_000L, handle.get<Long>("rest_timer_ends_at_millis"))
    }

    @Test
    fun completeSet_persistenceFailure_doesNotStartRestTimer() = runTest {
        workoutHistoryRepository.insertSetError = DataError.Database(RuntimeException("Disk error"))
        val handle = SavedStateHandle(mapOf("sessionId" to 10L))
        val vm = createViewModel(sessionId = 10L, handle = handle)
        advanceUntilIdle()

        vm.onRepsChanged("e1", "10")
        vm.onWeightChanged("e1", "50")
        vm.onCompleteSetClicked(sampleExercise)
        advanceUntilIdle()

        assertEquals(0L, vm.uiState.value.restTimerRemainingSeconds)
        assertNull(vm.uiState.value.restTimerEndsAtMillis)
        assertNull(handle.get<Long>("rest_timer_ends_at_millis"))
    }

    @Test
    fun timerTick_decrementsRemainingRestSeconds() = runTest {
        val vm = createViewModel(sessionId = 10L)
        advanceUntilIdle()

        vm.onRepsChanged("e1", "10")
        vm.onWeightChanged("e1", "50")
        vm.onCompleteSetClicked(sampleExercise)
        advanceUntilIdle()

        assertEquals(90L, vm.uiState.value.restTimerRemainingSeconds)

        timeProvider.currentTime += 30_000L
        vm.onTimerTick()

        assertEquals(60L, vm.uiState.value.restTimerRemainingSeconds)
    }

    @Test
    fun timerTick_atOrPastDeadline_expiresRestTimerAndClearsSavedState() = runTest {
        val handle = SavedStateHandle(mapOf("sessionId" to 10L))
        val vm = createViewModel(sessionId = 10L, handle = handle)
        advanceUntilIdle()

        vm.onRepsChanged("e1", "10")
        vm.onWeightChanged("e1", "50")
        vm.onCompleteSetClicked(sampleExercise)
        advanceUntilIdle()

        timeProvider.currentTime += 90_000L
        vm.onTimerTick()

        assertEquals(0L, vm.uiState.value.restTimerRemainingSeconds)
        assertNull(vm.uiState.value.restTimerEndsAtMillis)
        assertNull(handle.get<Long>("rest_timer_ends_at_millis"))
    }

    @Test
    fun skipRestTimer_clearsTimerAndSavedStateImmediately() = runTest {
        val handle = SavedStateHandle(mapOf("sessionId" to 10L))
        val vm = createViewModel(sessionId = 10L, handle = handle)
        advanceUntilIdle()

        vm.onRepsChanged("e1", "10")
        vm.onWeightChanged("e1", "50")
        vm.onCompleteSetClicked(sampleExercise)
        advanceUntilIdle()

        assertEquals(90L, vm.uiState.value.restTimerRemainingSeconds)

        vm.onSkipRestTimer()

        assertEquals(0L, vm.uiState.value.restTimerRemainingSeconds)
        assertNull(vm.uiState.value.restTimerEndsAtMillis)
        assertNull(handle.get<Long>("rest_timer_ends_at_millis"))
    }

    @Test
    fun anotherSuccessfulSet_resetsRestTimerTo90Seconds() = runTest {
        val handle = SavedStateHandle(mapOf("sessionId" to 10L))
        val vm = createViewModel(sessionId = 10L, handle = handle)
        advanceUntilIdle()

        vm.onRepsChanged("e1", "10")
        vm.onWeightChanged("e1", "50")
        vm.onCompleteSetClicked(sampleExercise)
        advanceUntilIdle()

        assertEquals(90L, vm.uiState.value.restTimerRemainingSeconds)

        timeProvider.currentTime += 40_000L
        vm.onTimerTick()
        assertEquals(50L, vm.uiState.value.restTimerRemainingSeconds)

        // Complete second set
        vm.onRepsChanged("e1", "8")
        vm.onWeightChanged("e1", "55")
        vm.onCompleteSetClicked(sampleExercise)
        advanceUntilIdle()

        assertEquals(90L, vm.uiState.value.restTimerRemainingSeconds)
        assertEquals(timeProvider.currentTime + 90_000L, vm.uiState.value.restTimerEndsAtMillis)
        assertEquals(timeProvider.currentTime + 90_000L, handle.get<Long>("rest_timer_ends_at_millis"))
    }

    @Test
    fun viewModelInit_withSavedRestTimer_restoresActiveRestTimer() = runTest {
        val endsAt = 15_000L + 45_000L
        val handle = SavedStateHandle(mapOf("sessionId" to 10L, "rest_timer_ends_at_millis" to endsAt))
        val vm = createViewModel(sessionId = 10L, handle = handle)
        advanceUntilIdle()

        assertEquals(45L, vm.uiState.value.restTimerRemainingSeconds)
        assertEquals(endsAt, vm.uiState.value.restTimerEndsAtMillis)
    }

    @Test
    fun viewModelInit_withExpiredSavedRestTimer_clearsExpiredTimer() = runTest {
        val pastEndsAt = 10_000L // timeProvider is at 15_000L
        val handle = SavedStateHandle(mapOf("sessionId" to 10L, "rest_timer_ends_at_millis" to pastEndsAt))
        val vm = createViewModel(sessionId = 10L, handle = handle)
        advanceUntilIdle()

        assertEquals(0L, vm.uiState.value.restTimerRemainingSeconds)
        assertNull(vm.uiState.value.restTimerEndsAtMillis)
        assertNull(handle.get<Long>("rest_timer_ends_at_millis"))
    }
}
