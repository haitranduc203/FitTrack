package com.haitranduc.fittrack.data.repository

import kotlinx.coroutines.CancellationException

fun Throwable.rethrowIfCancellationOrFatal() {
    if (this is CancellationException) throw this
    if (this is Error) throw this
}
