package com.haitranduc.fittrack.testing

import com.haitranduc.fittrack.domain.repository.DataError
import com.haitranduc.fittrack.domain.repository.DataResult
import com.haitranduc.fittrack.domain.repository.StatisticsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class FakeStatisticsRepository(
    initialTotalWorkouts: Long = 0L
) : StatisticsRepository {

    private val totalWorkoutsFlow = MutableStateFlow(initialTotalWorkouts)
    var observeTotalWorkoutsError: DataError? = null

    fun setTotalWorkouts(count: Long) {
        totalWorkoutsFlow.value = count
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
}
