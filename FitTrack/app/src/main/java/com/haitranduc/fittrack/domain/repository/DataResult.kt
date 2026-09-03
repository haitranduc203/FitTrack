package com.haitranduc.fittrack.domain.repository

sealed interface DataError {
    data class Database(val cause: Throwable) : DataError
    data class Unknown(val cause: Throwable) : DataError
}

sealed interface DataResult<out T> {
    data class Success<out T>(val data: T) : DataResult<T>
    data class Failure(val error: DataError) : DataResult<Nothing>
}
