package com.haitranduc.fittrack.data.repository

import com.haitranduc.fittrack.data.local.dao.StatisticsDao
import com.haitranduc.fittrack.domain.repository.DataError
import com.haitranduc.fittrack.domain.repository.DataResult
import com.haitranduc.fittrack.domain.repository.StatisticsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatisticsRepositoryImpl @Inject constructor(
    private val statisticsDao: StatisticsDao
) : StatisticsRepository {

    override fun observeTotalWorkouts(): Flow<DataResult<Long>> {
        return statisticsDao.observeTotalWorkouts()
            .map<Long, DataResult<Long>> { count -> DataResult.Success(count) }
            .catch { e -> emit(DataResult.Failure(DataError.Database(e))) }
    }
}
