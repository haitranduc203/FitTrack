package com.haitranduc.fittrack.domain.repository

sealed interface SeedImportResult {
    data class Imported(val count: Int) : SeedImportResult
    data class AlreadySeeded(val existingCount: Int) : SeedImportResult
    data class Failure(val error: SeedImportError) : SeedImportResult
}

sealed interface SeedImportError {
    data object FileMissing : SeedImportError
    data class InvalidData(val message: String) : SeedImportError
    data class Database(val cause: Throwable) : SeedImportError
    data class Unknown(val cause: Throwable) : SeedImportError
}
