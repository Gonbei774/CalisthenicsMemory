package io.github.gonbei774.calisthenicsmemory

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.util.SearchUtils
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for SearchUtils
 */
class SearchUtilsTest {

    private val exercises = listOf(
        Exercise(id = 1, name = "Push-up", type = "Dynamic"),
        Exercise(id = 2, name = "Pull-up", type = "Dynamic"),
        Exercise(id = 3, name = "Push-up Variation", type = "Dynamic"),
        Exercise(id = 4, name = "Diamond Push-up", type = "Dynamic"),
        Exercise(id = 5, name = "Squats", type = "Dynamic"),
        Exercise(id = 6, name = "Push Press", type = "Dynamic")
    )

    @Test
    fun `empty query returns empty list`() {
        val result = SearchUtils.searchExercises(exercises, "")
        assertTrue(result.isEmpty())

        val result2 = SearchUtils.searchExercises(exercises, "   ")
        assertTrue(result2.isEmpty())
    }

    @Test
    fun `exact match appears first`() {
        val result = SearchUtils.searchExercises(exercises, "Push-up")
        assertEquals(3, result.size)
        assertEquals("Push-up", result[0].name) // Exact match first
        assertEquals("Push-up Variation", result[1].name) // Starts with query
        assertEquals("Diamond Push-up", result[2].name) // Contains query
    }

    @Test
    fun `starts with query ranks higher than contains`() {
        val result = SearchUtils.searchExercises(exercises, "Push")
        assertEquals(4, result.size)
        assertEquals("Push Press", result[0].name) // Starts with, alphabetical
        assertEquals("Push-up", result[1].name) // Starts with
        assertEquals("Push-up Variation", result[2].name) // Starts with
        assertEquals("Diamond Push-up", result[3].name) // Contains
    }

    @Test
    fun `case insensitive search works`() {
        val result = SearchUtils.searchExercises(exercises, "push")
        assertEquals(4, result.size)
        assertEquals("Push Press", result[0].name)
    }

    @Test
    fun `word boundary matching works`() {
        val exercisesWithWords = listOf(
            Exercise(id = 1, name = "Push-up", type = "Dynamic"),
            Exercise(id = 2, name = "Bench Press", type = "Dynamic"),
            Exercise(id = 3, name = "Push Press", type = "Dynamic")
        )

        val result = SearchUtils.searchExercises(exercisesWithWords, "Press")
        assertEquals(2, result.size)
        assertEquals("Bench Press", result[0].name) // Word boundary match
        assertEquals("Push Press", result[1].name) // Contains match
    }

    @Test
    fun `hierarchical search filters groups correctly`() {
        val group1 = TrainingViewModel.GroupWithExercises(
            groupName = "Upper Body",
            exercises = listOf(
                Exercise(id = 1, name = "Push-up", type = "Dynamic"),
                Exercise(id = 2, name = "Pull-up", type = "Dynamic")
            )
        )
        val group2 = TrainingViewModel.GroupWithExercises(
            groupName = "Lower Body",
            exercises = listOf(
                Exercise(id = 3, name = "Squats", type = "Dynamic")
            )
        )
        val hierarchicalData = listOf(group1, group2)

        val result = SearchUtils.searchHierarchicalExercises(hierarchicalData, "Push")
        assertEquals(1, result.size) // Only Upper Body group should remain
        assertEquals("Upper Body", result[0].groupName)
        assertEquals(1, result[0].exercises.size) // Only Push-up
        assertEquals("Push-up", result[0].exercises[0].name)
    }

    @Test
    fun `hierarchical search ranks exercises within groups`() {
        val group = TrainingViewModel.GroupWithExercises(
            groupName = "Upper Body",
            exercises = listOf(
                Exercise(id = 1, name = "Push-up Variation", type = "Dynamic"),
                Exercise(id = 2, name = "Push-up", type = "Dynamic"),
                Exercise(id = 3, name = "Diamond Push-up", type = "Dynamic")
            )
        )
        val hierarchicalData = listOf(group)

        val result = SearchUtils.searchHierarchicalExercises(hierarchicalData, "Push-up")
        assertEquals(1, result.size)
        assertEquals(3, result[0].exercises.size)
        // Exact match first
        assertEquals("Push-up", result[0].exercises[0].name)
        // Starts with second
        assertEquals("Push-up Variation", result[0].exercises[1].name)
        // Contains third
        assertEquals("Diamond Push-up", result[0].exercises[2].name)
    }
}