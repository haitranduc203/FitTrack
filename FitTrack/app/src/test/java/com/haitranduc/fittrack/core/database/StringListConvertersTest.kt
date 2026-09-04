package com.haitranduc.fittrack.core.database

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class StringListConvertersTest {

    private lateinit var converters: StringListConverters

    @Before
    fun setUp() {
        converters = StringListConverters()
    }

    @Test
    fun list_roundTripsWithoutDelimiterLoss() {
        val original = listOf("upper back", "quote \"inside\"", "line\nstep")
        assertEquals(original, converters.toList(converters.fromList(original)))
    }

    @Test
    fun emptyList_roundTripsCorrectly() {
        val original = emptyList<String>()
        assertEquals(original, converters.toList(converters.fromList(original)))
    }

    @Test
    fun unicodeAndSpecialCharacters_roundTripCorrectly() {
        val original = listOf("Triceps brachii", "Dumbbell — 15kg", "日本語", "🔥")
        assertEquals(original, converters.toList(converters.fromList(original)))
    }

    @Test
    fun emptyJsonString_returnsEmptyList() {
        assertEquals(emptyList<String>(), converters.toList(""))
        assertEquals(emptyList<String>(), converters.toList("[]"))
    }

    @Test(expected = kotlinx.serialization.SerializationException::class)
    fun malformedJson_throwsSerializationException() {
        converters.toList("{not_valid_json}")
    }
}
