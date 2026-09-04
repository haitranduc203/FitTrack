package com.haitranduc.fittrack.core.time

import com.haitranduc.fittrack.domain.time.TimeProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}
