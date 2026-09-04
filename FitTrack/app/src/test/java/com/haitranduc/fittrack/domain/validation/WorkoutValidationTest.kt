package com.haitranduc.fittrack.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutValidationTest {

    // ==========================================
    // NAME VALIDATION TESTS
    // ==========================================

    @Test
    fun validateName_blankOrWhitespace_returnsBlank() {
        assertEquals(NameResult.Blank, WorkoutValidation.validateName(""))
        assertEquals(NameResult.Blank, WorkoutValidation.validateName("   "))
        assertEquals(NameResult.Blank, WorkoutValidation.validateName("\t\n  "))
    }

    @Test
    fun validateName_validLengths_returnsValidWithTrimmedName() {
        val result1 = WorkoutValidation.validateName("A")
        assertTrue(result1 is NameResult.Valid)
        assertEquals("A", (result1 as NameResult.Valid).trimmedName)

        val result2 = WorkoutValidation.validateName("  Push Day  ")
        assertTrue(result2 is NameResult.Valid)
        assertEquals("Push Day", (result2 as NameResult.Valid).trimmedName)

        val exactly50 = "a".repeat(50)
        val result50 = WorkoutValidation.validateName("  $exactly50  ")
        assertTrue(result50 is NameResult.Valid)
        assertEquals(exactly50, (result50 as NameResult.Valid).trimmedName)
    }

    @Test
    fun validateName_over50CharsAfterTrim_returnsTooLong() {
        val chars51 = "a".repeat(51)
        assertEquals(NameResult.TooLong, WorkoutValidation.validateName(chars51))
        assertEquals(NameResult.TooLong, WorkoutValidation.validateName("  $chars51  "))
    }

    // ==========================================
    // EXERCISE LIST VALIDATION TESTS
    // ==========================================

    @Test
    fun validateExerciseIds_emptyList_returnsEmpty() {
        assertEquals(ExerciseListResult.Empty, WorkoutValidation.validateExerciseIds(emptyList()))
    }

    @Test
    fun validateExerciseIds_singleAndMultipleUnique_returnsValid() {
        assertEquals(ExerciseListResult.Valid, WorkoutValidation.validateExerciseIds(listOf("ex_01")))
        assertEquals(ExerciseListResult.Valid, WorkoutValidation.validateExerciseIds(listOf("ex_01", "ex_02", "ex_03")))
    }

    @Test
    fun validateExerciseIds_duplicateIds_returnsDuplicateWithFirstDuplicatedId() {
        val result = WorkoutValidation.validateExerciseIds(listOf("ex_01", "ex_02", "ex_01", "ex_03", "ex_02"))
        assertTrue(result is ExerciseListResult.Duplicate)
        assertEquals("ex_01", (result as ExerciseListResult.Duplicate).exerciseId)

        val result2 = WorkoutValidation.validateExerciseIds(listOf("bench", "squat", "squat"))
        assertTrue(result2 is ExerciseListResult.Duplicate)
        assertEquals("squat", (result2 as ExerciseListResult.Duplicate).exerciseId)
    }

    // ==========================================
    // SET VALIDATION TESTS (REPS & WEIGHT)
    // ==========================================

    @Test
    fun validateSet_validRepsAndWeight_returnsValid() {
        assertEquals(SetResult.Valid, WorkoutValidation.validateSet(reps = 1, weightKg = 0.0))
        assertEquals(SetResult.Valid, WorkoutValidation.validateSet(reps = 10, weightKg = 50.0))
        assertEquals(SetResult.Valid, WorkoutValidation.validateSet(reps = 100, weightKg = 1000.0))
        assertEquals(SetResult.Valid, WorkoutValidation.validateSet(reps = 15, weightKg = 72.5))
    }

    @Test
    fun validateSet_invalidReps_returnsInvalidReps() {
        assertEquals(SetResult.InvalidReps, WorkoutValidation.validateSet(reps = 0, weightKg = 50.0))
        assertEquals(SetResult.InvalidReps, WorkoutValidation.validateSet(reps = -1, weightKg = 50.0))
        assertEquals(SetResult.InvalidReps, WorkoutValidation.validateSet(reps = -10, weightKg = 50.0))
        assertEquals(SetResult.InvalidReps, WorkoutValidation.validateSet(reps = 101, weightKg = 50.0))
        assertEquals(SetResult.InvalidReps, WorkoutValidation.validateSet(reps = 999, weightKg = 50.0))
    }

    @Test
    fun validateSet_invalidWeight_returnsInvalidWeight() {
        assertEquals(SetResult.InvalidWeight, WorkoutValidation.validateSet(reps = 10, weightKg = -0.1))
        assertEquals(SetResult.InvalidWeight, WorkoutValidation.validateSet(reps = 10, weightKg = -50.0))
        assertEquals(SetResult.InvalidWeight, WorkoutValidation.validateSet(reps = 10, weightKg = 1000.1))
        assertEquals(SetResult.InvalidWeight, WorkoutValidation.validateSet(reps = 10, weightKg = Double.NaN))
        assertEquals(SetResult.InvalidWeight, WorkoutValidation.validateSet(reps = 10, weightKg = Double.POSITIVE_INFINITY))
        assertEquals(SetResult.InvalidWeight, WorkoutValidation.validateSet(reps = 10, weightKg = Double.NEGATIVE_INFINITY))
    }
}
