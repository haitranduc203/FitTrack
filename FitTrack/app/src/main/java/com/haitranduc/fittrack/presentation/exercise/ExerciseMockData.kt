package com.haitranduc.fittrack.presentation.exercise

import androidx.annotation.StringRes
import com.haitranduc.fittrack.R

enum class BodyPartFilterKey(@get:StringRes val labelRes: Int) {
    ALL(R.string.filter_all),
    CHEST(R.string.filter_chest),
    SHOULDERS(R.string.filter_shoulders),
    BACK(R.string.filter_back),
    LEGS(R.string.filter_legs)
}

enum class EquipmentFilterKey(@get:StringRes val labelRes: Int) {
    ALL(R.string.filter_all),
    BARBELL(R.string.filter_barbell),
    BODYWEIGHT(R.string.filter_bodyweight),
    DUMBBELL(R.string.filter_dumbbell)
}

data class ExerciseMock(
    val id: String,
    @get:StringRes val nameRes: Int,
    val bodyPartKey: BodyPartFilterKey,
    @get:StringRes val bodyPartRes: Int,
    @get:StringRes val targetRes: Int,
    val equipmentKey: EquipmentFilterKey,
    @get:StringRes val equipmentRes: Int,
    val secondaryMusclesRes: List<Int>,
    val instructionsRes: List<Int>
)

object ExerciseMockData {
    val exercises = listOf(
        ExerciseMock(
            id = "bench_press",
            nameRes = R.string.exercise_name_bench_press,
            bodyPartKey = BodyPartFilterKey.CHEST,
            bodyPartRes = R.string.body_part_chest,
            targetRes = R.string.target_pectorals,
            equipmentKey = EquipmentFilterKey.BARBELL,
            equipmentRes = R.string.equipment_barbell,
            secondaryMusclesRes = listOf(R.string.muscle_triceps, R.string.muscle_anterior_deltoids),
            instructionsRes = listOf(
                R.string.instruction_bench_press_1,
                R.string.instruction_bench_press_2,
                R.string.instruction_bench_press_3,
                R.string.instruction_bench_press_4
            )
        ),
        ExerciseMock(
            id = "overhead_press",
            nameRes = R.string.exercise_name_overhead_press,
            bodyPartKey = BodyPartFilterKey.SHOULDERS,
            bodyPartRes = R.string.body_part_shoulders,
            targetRes = R.string.target_deltoids,
            equipmentKey = EquipmentFilterKey.BARBELL,
            equipmentRes = R.string.equipment_barbell,
            secondaryMusclesRes = listOf(R.string.muscle_triceps, R.string.muscle_upper_chest),
            instructionsRes = listOf(
                R.string.instruction_ohp_1,
                R.string.instruction_ohp_2,
                R.string.instruction_ohp_3
            )
        ),
        ExerciseMock(
            id = "barbell_squat",
            nameRes = R.string.exercise_name_squat,
            bodyPartKey = BodyPartFilterKey.LEGS,
            bodyPartRes = R.string.body_part_legs,
            targetRes = R.string.target_quadriceps,
            equipmentKey = EquipmentFilterKey.BARBELL,
            equipmentRes = R.string.equipment_barbell,
            secondaryMusclesRes = listOf(R.string.muscle_glutes, R.string.muscle_hamstrings, R.string.muscle_lower_back),
            instructionsRes = listOf(
                R.string.instruction_squat_1,
                R.string.instruction_squat_2,
                R.string.instruction_squat_3,
                R.string.instruction_squat_4
            )
        ),
        ExerciseMock(
            id = "deadlift",
            nameRes = R.string.exercise_name_deadlift,
            bodyPartKey = BodyPartFilterKey.BACK,
            bodyPartRes = R.string.body_part_back,
            targetRes = R.string.target_erector_spinae,
            equipmentKey = EquipmentFilterKey.BARBELL,
            equipmentRes = R.string.equipment_barbell,
            secondaryMusclesRes = listOf(R.string.muscle_hamstrings, R.string.muscle_glutes, R.string.muscle_trapezius),
            instructionsRes = listOf(
                R.string.instruction_deadlift_1,
                R.string.instruction_deadlift_2,
                R.string.instruction_deadlift_3,
                R.string.instruction_deadlift_4
            )
        ),
        ExerciseMock(
            id = "pull_up",
            nameRes = R.string.exercise_name_pull_up,
            bodyPartKey = BodyPartFilterKey.BACK,
            bodyPartRes = R.string.body_part_back,
            targetRes = R.string.target_latissimus_dorsi,
            equipmentKey = EquipmentFilterKey.BODYWEIGHT,
            equipmentRes = R.string.equipment_bodyweight,
            secondaryMusclesRes = listOf(R.string.muscle_biceps, R.string.muscle_forearms),
            instructionsRes = listOf(
                R.string.instruction_pullup_1,
                R.string.instruction_pullup_2,
                R.string.instruction_pullup_3,
                R.string.instruction_pullup_4
            )
        )
    )

    fun find(id: String): ExerciseMock? = exercises.find { it.id == id }
}
