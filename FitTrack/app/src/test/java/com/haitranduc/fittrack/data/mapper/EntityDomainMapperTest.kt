package com.haitranduc.fittrack.data.mapper

import com.haitranduc.fittrack.data.local.entity.ExerciseEntity
import com.haitranduc.fittrack.data.local.entity.SetLogEntity
import com.haitranduc.fittrack.data.local.entity.WorkoutEntity
import com.haitranduc.fittrack.data.local.entity.WorkoutSessionEntity
import com.haitranduc.fittrack.data.local.relation.WorkoutExerciseRow
import com.haitranduc.fittrack.data.local.relation.WorkoutSessionWithSets
import com.haitranduc.fittrack.data.local.relation.WorkoutWithExercises
import com.haitranduc.fittrack.domain.model.Exercise
import com.haitranduc.fittrack.domain.model.SetLog
import com.haitranduc.fittrack.domain.model.Workout
import org.junit.Assert.assertEquals
import org.junit.Test

class EntityDomainMapperTest {

    @Test
    fun exerciseEntity_toDomain_mapsAllFields() {
        val entity = ExerciseEntity(
            id = "ex_001",
            name = "Bench Press",
            bodyPart = "Chest",
            equipment = "Barbell",
            target = "Pecs",
            muscleGroup = "Chest",
            secondaryMuscles = listOf("Triceps"),
            instructions = listOf("Step 1", "Step 2")
        )

        val domain = entity.toDomain()
        assertEquals(entity.id, domain.id)
        assertEquals(entity.name, domain.name)
        assertEquals(entity.bodyPart, domain.bodyPart)
        assertEquals(entity.equipment, domain.equipment)
        assertEquals(entity.target, domain.target)
        assertEquals(entity.muscleGroup, domain.muscleGroup)
        assertEquals(entity.secondaryMuscles, domain.secondaryMuscles)
        assertEquals(entity.instructions, domain.instructions)
    }

    @Test
    fun workoutWithExercises_toDomain_preservesOrder() {
        val workoutEntity = WorkoutEntity(id = 1L, name = "Push", createdAt = 100L, updatedAt = 200L)
        val ex1 = ExerciseEntity(id = "e1", name = "Bench", bodyPart = "Chest", equipment = "Barbell", target = "Pecs", muscleGroup = "Chest", secondaryMuscles = emptyList(), instructions = listOf("Push"))
        val ex2 = ExerciseEntity(id = "e2", name = "Incline", bodyPart = "Chest", equipment = "Dumbbell", target = "Pecs", muscleGroup = "Chest", secondaryMuscles = emptyList(), instructions = listOf("Push"))

        val relation = WorkoutWithExercises(
            workout = workoutEntity,
            exercises = listOf(
                WorkoutExerciseRow(exercise = ex2, orderIndex = 0),
                WorkoutExerciseRow(exercise = ex1, orderIndex = 1)
            )
        )

        val domain = relation.toDomain()
        assertEquals(1L, domain.id)
        assertEquals("Push", domain.name)
        assertEquals(2, domain.exercises.size)
        assertEquals("e2", domain.exercises[0].id)
        assertEquals("e1", domain.exercises[1].id)
    }

    @Test
    fun workout_toEntity_mapsBasicFields() {
        val domain = Workout(id = 5L, name = "Legs", createdAt = 500L, updatedAt = 600L, exercises = emptyList())
        val entity = domain.toEntity()
        assertEquals(5L, entity.id)
        assertEquals("Legs", entity.name)
        assertEquals(500L, entity.createdAt)
        assertEquals(600L, entity.updatedAt)
    }

    @Test
    fun workoutSessionWithSets_toDomain_preservesSetsAndSnapshots() {
        val sessionEntity = WorkoutSessionEntity(
            id = 10L,
            workoutId = 1L,
            workoutNameSnapshot = "Push Day Snapshot",
            startedAt = 1000L,
            finishedAt = 2000L,
            durationSeconds = 1000L
        )

        val set1 = SetLogEntity(id = 101L, sessionId = 10L, exerciseId = "e1", exerciseNameSnapshot = "Bench Snapshot", setNumber = 1, reps = 10, weightKg = 80.0, completedAt = 1200L)
        val set2 = SetLogEntity(id = 102L, sessionId = 10L, exerciseId = "e1", exerciseNameSnapshot = "Bench Snapshot", setNumber = 2, reps = 8, weightKg = 85.0, completedAt = 1400L)

        val relation = WorkoutSessionWithSets(session = sessionEntity, sets = listOf(set1, set2))
        val domain = relation.toDomain()

        assertEquals(10L, domain.id)
        assertEquals(1L, domain.workoutId)
        assertEquals("Push Day Snapshot", domain.workoutNameSnapshot)
        assertEquals(1000L, domain.startedAt)
        assertEquals(2000L, domain.finishedAt)
        assertEquals(1000L, domain.durationSeconds)
        assertEquals(2, domain.sets.size)
        assertEquals(101L, domain.sets[0].id)
        assertEquals("Bench Snapshot", domain.sets[0].exerciseNameSnapshot)
    }

    @Test
    fun setLog_roundTripMapping() {
        val domain = SetLog(
            id = 1L,
            sessionId = 10L,
            exerciseId = "ex_01",
            exerciseNameSnapshot = "Squat Snapshot",
            setNumber = 1,
            reps = 10,
            weightKg = 100.0,
            completedAt = 1500L
        )

        val entity = domain.toEntity()
        val mappedBack = entity.toDomain()
        assertEquals(domain, mappedBack)
    }
}
