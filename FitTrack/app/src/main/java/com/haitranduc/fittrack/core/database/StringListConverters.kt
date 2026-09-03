package com.haitranduc.fittrack.core.database

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class StringListConverters {

    @TypeConverter
    fun fromList(list: List<String>?): String {
        return if (list.isNullOrEmpty()) "[]" else Json.encodeToString(list)
    }

    @TypeConverter
    fun toList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return Json.decodeFromString(value)
    }
}
