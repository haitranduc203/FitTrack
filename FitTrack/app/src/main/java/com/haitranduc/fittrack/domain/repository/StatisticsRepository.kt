package com.haitranduc.fittrack.domain.repository

import kotlinx.coroutines.flow.Flow

interface StatisticsRepository {
    fun observeTotalWorkouts(): Flow<DataResult<Long>>
    fun observeTotalCompletedSets(): Flow<DataResult<Long>>
    fun observeTotalTrainingTimeSeconds(): Flow<DataResult<Long>>
}
