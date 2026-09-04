package com.haitranduc.fittrack.testing

import com.haitranduc.fittrack.domain.repository.DataError
import com.haitranduc.fittrack.domain.repository.DataResult
import com.haitranduc.fittrack.domain.repository.StatisticsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class FakeStatisticsRepository(
    initialTotalWorkouts: Long = 0L,
    initialTotalCompletedSets: Long = 0L,
    initialTotalTrainingTimeSeconds: Long = 0L
) : StatisticsRepository {

    private val totalWorkoutsFlow = MutableStateFlow(initialTotalWorkouts)
    var observeTotalWorkoutsError: DataError? = null

    private val totalCompletedSetsFlow = MutableStateFlow(initialTotalCompletedSets)
    var observeTotalCompletedSetsError: DataError? = null

    private val totalTrainingTimeSecondsFlow = MutableStateFlow(initialTotalTrainingTimeSeconds)
    var observeTotalTrainingTimeSecondsError: DataError? = null

    fun setTotalWorkouts(count: Long) {
        totalWorkoutsFlow.value = count
    }

    fun setTotalCompletedSets(count: Long) {
        totalCompletedSetsFlow.value = count
    }

    fun setTotalTrainingTimeSeconds(seconds: Long) {
        totalTrainingTimeSecondsFlow.value = seconds
    }

    override fun observeTotalWorkouts(): Flow<DataResult<Long>> {
        return totalWorkoutsFlow.asStateFlow().map { count ->
            val err = observeTotalWorkoutsError
            if (err != null) {
                DataResult.Failure(err)
            } else {
                DataResult.Success(count)
            }
        }
    }

    override fun observeTotalCompletedSets(): Flow<DataResult<Long>> {
        return totalCompletedSetsFlow.asStateFlow().map { count ->
            val err = observeTotalCompletedSetsError
            if (err != null) {
                DataResult.Failure(err)
            } else {
                DataResult.Success(count)
            }
        }
    }

    override fun observeTotalTrainingTimeSeconds(): Flow<DataResult<Long>> {
        return totalTrainingTimeSecondsFlow.asStateFlow().map { seconds ->
            val err = observeTotalTrainingTimeSecondsError
            if (err != null) {
                DataResult.Failure(err)
            } else {
                DataResult.Success(seconds)
            }
        }
    }
}
