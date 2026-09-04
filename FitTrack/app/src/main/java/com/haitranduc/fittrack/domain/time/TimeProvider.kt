package com.haitranduc.fittrack.domain.time

interface TimeProvider {
    fun currentTimeMillis(): Long
}
