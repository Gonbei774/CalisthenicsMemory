package io.github.gonbei774.calisthenicsmemory

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for timer settings priority logic
 * Tests the fallback behavior: Exercise-specific > Global settings
 */
class TimerSettingsPriorityTest {

    companion object {
        // Default values from WorkoutPreferences
        const val DEFAULT_START_COUNTDOWN = 5
        const val DEFAULT_SET_INTERVAL = 240
        const val MAX_SET_INTERVAL = 600
    }

    // ========================================
    // Rest Interval Priority Tests
    // ========================================

    @Test
    fun restInterval_exerciseSettingTakesPriority() {
        val exerciseRestInterval: Int? = 120
        val globalRestInterval = DEFAULT_SET_INTERVAL

        val effectiveRestInterval = exerciseRestInterval ?: globalRestInterval

        assertEquals(120, effectiveRestInterval)
    }

    @Test
    fun restInterval_fallbackToGlobalWhenNull() {
        val exerciseRestInterval: Int? = null
        val globalRestInterval = DEFAULT_SET_INTERVAL

        val effectiveRestInterval = exerciseRestInterval ?: globalRestInterval

        assertEquals(DEFAULT_SET_INTERVAL, effectiveRestInterval)
    }

    @Test
    fun restInterval_exerciseZeroUsesZero() {
        // 0 is a valid value (disabled), not null
        val exerciseRestInterval: Int? = 0
        val globalRestInterval = DEFAULT_SET_INTERVAL

        val effectiveRestInterval = exerciseRestInterval ?: globalRestInterval

        assertEquals(0, effectiveRestInterval)
    }

    @Test
    fun restInterval_customGlobalValue() {
        val exerciseRestInterval: Int? = null
        val globalRestInterval = 180  // Custom global setting

        val effectiveRestInterval = exerciseRestInterval ?: globalRestInterval

        assertEquals(180, effectiveRestInterval)
    }

    // ========================================
    // Rep Duration Priority Tests
    // ========================================

    @Test
    fun repDuration_exerciseSettingTakesPriority() {
        val exerciseRepDuration: Int? = 3
        val defaultRepDuration = 5

        val effectiveRepDuration = exerciseRepDuration ?: defaultRepDuration

        assertEquals(3, effectiveRepDuration)
    }

    @Test
    fun repDuration_fallbackToDefaultWhenNull() {
        val exerciseRepDuration: Int? = null
        val defaultRepDuration = 5

        val effectiveRepDuration = exerciseRepDuration ?: defaultRepDuration

        assertEquals(5, effectiveRepDuration)
    }

    @Test
    fun repDuration_onlyUsedForDynamicType() {
        val exerciseType = "Dynamic"
        val exerciseRepDuration: Int? = 3

        val shouldUseRepDuration = exerciseType == "Dynamic"
        val effectiveRepDuration = if (shouldUseRepDuration) {
            exerciseRepDuration ?: 5
        } else {
            null
        }

        assertTrue(shouldUseRepDuration)
        assertEquals(3, effectiveRepDuration)
    }

    @Test
    fun repDuration_notUsedForIsometricType() {
        val exerciseType = "Isometric"
        val exerciseRepDuration: Int? = 3

        val shouldUseRepDuration = exerciseType == "Dynamic"
        val effectiveRepDuration = if (shouldUseRepDuration) {
            exerciseRepDuration ?: 5
        } else {
            null
        }

        assertFalse(shouldUseRepDuration)
        assertNull(effectiveRepDuration)
    }

    // ========================================
    // Exercise Data Class Tests
    // ========================================

    @Test
    fun exerciseDataClass_defaultTimerValues() {
        val exercise = Exercise(
            name = "Test Exercise",
            type = "Dynamic",
            laterality = "Bilateral"
        )

        assertNull(exercise.restInterval)
        assertNull(exercise.repDuration)
    }

    @Test
    fun exerciseDataClass_customTimerValues() {
        val exercise = Exercise(
            name = "Test Exercise",
            type = "Dynamic",
            laterality = "Bilateral",
            restInterval = 180,
            repDuration = 3
        )

        assertEquals(180, exercise.restInterval)
        assertEquals(3, exercise.repDuration)
    }

    @Test
    fun exerciseDataClass_partialTimerSettings() {
        val exercise = Exercise(
            name = "Test Exercise",
            type = "Dynamic",
            laterality = "Bilateral",
            restInterval = 120,
            repDuration = null
        )

        assertEquals(120, exercise.restInterval)
        assertNull(exercise.repDuration)
    }

    // ========================================
    // Timer Settings Calculation Tests
    // ========================================

    @Test
    fun calculateEffectiveRestInterval_withExerciseSetting() {
        fun getEffectiveRestInterval(
            exercise: Exercise,
            globalInterval: Int
        ): Int {
            return exercise.restInterval ?: globalInterval
        }

        val exercise = Exercise(
            name = "Test",
            type = "Dynamic",
            laterality = "Bilateral",
            restInterval = 120
        )

        val result = getEffectiveRestInterval(exercise, DEFAULT_SET_INTERVAL)

        assertEquals(120, result)
    }

    @Test
    fun calculateEffectiveRestInterval_withoutExerciseSetting() {
        fun getEffectiveRestInterval(
            exercise: Exercise,
            globalInterval: Int
        ): Int {
            return exercise.restInterval ?: globalInterval
        }

        val exercise = Exercise(
            name = "Test",
            type = "Dynamic",
            laterality = "Bilateral",
            restInterval = null
        )

        val result = getEffectiveRestInterval(exercise, DEFAULT_SET_INTERVAL)

        assertEquals(DEFAULT_SET_INTERVAL, result)
    }

    @Test
    fun calculateEffectiveRepDuration_dynamicWithSetting() {
        fun getEffectiveRepDuration(
            exercise: Exercise,
            defaultDuration: Int = 5
        ): Int? {
            return if (exercise.type == "Dynamic") {
                exercise.repDuration ?: defaultDuration
            } else {
                null
            }
        }

        val exercise = Exercise(
            name = "Test",
            type = "Dynamic",
            laterality = "Bilateral",
            repDuration = 3
        )

        val result = getEffectiveRepDuration(exercise)

        assertEquals(3, result)
    }

    @Test
    fun calculateEffectiveRepDuration_dynamicWithoutSetting() {
        fun getEffectiveRepDuration(
            exercise: Exercise,
            defaultDuration: Int = 5
        ): Int? {
            return if (exercise.type == "Dynamic") {
                exercise.repDuration ?: defaultDuration
            } else {
                null
            }
        }

        val exercise = Exercise(
            name = "Test",
            type = "Dynamic",
            laterality = "Bilateral",
            repDuration = null
        )

        val result = getEffectiveRepDuration(exercise)

        assertEquals(5, result)
    }

    @Test
    fun calculateEffectiveRepDuration_isometric() {
        fun getEffectiveRepDuration(
            exercise: Exercise,
            defaultDuration: Int = 5
        ): Int? {
            return if (exercise.type == "Dynamic") {
                exercise.repDuration ?: defaultDuration
            } else {
                null
            }
        }

        val exercise = Exercise(
            name = "Test",
            type = "Isometric",
            laterality = "Bilateral",
            repDuration = 3  // Should be ignored for Isometric
        )

        val result = getEffectiveRepDuration(exercise)

        assertNull(result)
    }

    // ========================================
    // Value Validation Tests
    // ========================================

    @Test
    fun validation_restIntervalRange() {
        val minValue = 0
        val maxValue = MAX_SET_INTERVAL

        assertTrue(0 in minValue..maxValue)
        assertTrue(60 in minValue..maxValue)
        assertTrue(240 in minValue..maxValue)
        assertTrue(600 in minValue..maxValue)
        assertFalse(-1 in minValue..maxValue)
        assertFalse(601 in minValue..maxValue)
    }

    @Test
    fun validation_repDurationPositive() {
        val validValues = listOf(1, 2, 3, 5, 10)
        val invalidValues = listOf(0, -1, -5)

        validValues.forEach { assertTrue("$it should be valid", it > 0) }
        invalidValues.forEach { assertFalse("$it should be invalid", it > 0) }
    }

    // ========================================
    // Workout Session Tests
    // ========================================

    @Test
    fun workoutSession_timerSettingsApplied() {
        data class WorkoutSession(
            val exerciseName: String,
            val exerciseType: String,
            val restInterval: Int,
            val repDuration: Int?
        )

        val exercise = Exercise(
            name = "Wall Push-up",
            type = "Dynamic",
            laterality = "Bilateral",
            restInterval = 180,
            repDuration = 3
        )

        val globalRestInterval = DEFAULT_SET_INTERVAL

        val session = WorkoutSession(
            exerciseName = exercise.name,
            exerciseType = exercise.type,
            restInterval = exercise.restInterval ?: globalRestInterval,
            repDuration = if (exercise.type == "Dynamic") {
                exercise.repDuration ?: 5
            } else {
                null
            }
        )

        assertEquals("Wall Push-up", session.exerciseName)
        assertEquals(180, session.restInterval)  // Exercise-specific
        assertEquals(3, session.repDuration)     // Exercise-specific
    }

    @Test
    fun workoutSession_fallbackToGlobalSettings() {
        data class WorkoutSession(
            val exerciseName: String,
            val exerciseType: String,
            val restInterval: Int,
            val repDuration: Int?
        )

        val exercise = Exercise(
            name = "Wall Push-up",
            type = "Dynamic",
            laterality = "Bilateral",
            restInterval = null,
            repDuration = null
        )

        val globalRestInterval = DEFAULT_SET_INTERVAL

        val session = WorkoutSession(
            exerciseName = exercise.name,
            exerciseType = exercise.type,
            restInterval = exercise.restInterval ?: globalRestInterval,
            repDuration = if (exercise.type == "Dynamic") {
                exercise.repDuration ?: 5
            } else {
                null
            }
        )

        assertEquals("Wall Push-up", session.exerciseName)
        assertEquals(DEFAULT_SET_INTERVAL, session.restInterval)  // Global fallback
        assertEquals(5, session.repDuration)                      // Default fallback
    }

    // ========================================
    // Multiple Exercises with Different Settings
    // ========================================

    @Test
    fun multipleExercises_differentSettings() {
        val exercises = listOf(
            Exercise(
                id = 1,
                name = "Exercise 1",
                type = "Dynamic",
                laterality = "Bilateral",
                restInterval = 60,
                repDuration = 2
            ),
            Exercise(
                id = 2,
                name = "Exercise 2",
                type = "Dynamic",
                laterality = "Bilateral",
                restInterval = null,
                repDuration = null
            ),
            Exercise(
                id = 3,
                name = "Exercise 3",
                type = "Isometric",
                laterality = "Bilateral",
                restInterval = 300,
                repDuration = null
            )
        )

        val globalRestInterval = DEFAULT_SET_INTERVAL

        fun getEffectiveSettings(exercise: Exercise): Pair<Int, Int?> {
            val rest = exercise.restInterval ?: globalRestInterval
            val rep = if (exercise.type == "Dynamic") {
                exercise.repDuration ?: 5
            } else {
                null
            }
            return Pair(rest, rep)
        }

        // Exercise 1: Custom settings
        val (rest1, rep1) = getEffectiveSettings(exercises[0])
        assertEquals(60, rest1)
        assertEquals(2, rep1)

        // Exercise 2: Fallback to defaults
        val (rest2, rep2) = getEffectiveSettings(exercises[1])
        assertEquals(DEFAULT_SET_INTERVAL, rest2)
        assertEquals(5, rep2)

        // Exercise 3: Custom rest, no rep (Isometric)
        val (rest3, rep3) = getEffectiveSettings(exercises[2])
        assertEquals(300, rest3)
        assertNull(rep3)
    }

    // ========================================
    // Edge Cases
    // ========================================

    @Test
    fun edgeCase_zeroRestIntervalIsValid() {
        // 0 means "no rest interval" - should use the value, not fall back
        val exercise = Exercise(
            name = "Test",
            type = "Dynamic",
            laterality = "Bilateral",
            restInterval = 0
        )

        val effective = exercise.restInterval ?: DEFAULT_SET_INTERVAL

        assertEquals(0, effective)
    }

    @Test
    fun edgeCase_maxRestInterval() {
        val exercise = Exercise(
            name = "Test",
            type = "Dynamic",
            laterality = "Bilateral",
            restInterval = MAX_SET_INTERVAL
        )

        assertTrue(exercise.restInterval!! <= MAX_SET_INTERVAL)
        assertEquals(600, exercise.restInterval)
    }

    @Test
    fun edgeCase_copyExercisePreservesTimerSettings() {
        val original = Exercise(
            id = 1,
            name = "Original",
            type = "Dynamic",
            laterality = "Bilateral",
            restInterval = 120,
            repDuration = 3
        )

        val copy = original.copy(name = "Copy")

        assertEquals("Copy", copy.name)
        assertEquals(120, copy.restInterval)
        assertEquals(3, copy.repDuration)
    }
}