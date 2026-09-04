package com.haitranduc.fittrack.testing

import com.haitranduc.fittrack.domain.time.TimeProvider

class FakeTimeProvider(var currentTime: Long = 1000L) : TimeProvider {
    override fun currentTimeMillis(): Long = currentTime
}
