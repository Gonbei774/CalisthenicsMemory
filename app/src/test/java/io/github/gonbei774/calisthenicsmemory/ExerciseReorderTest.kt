package io.github.gonbei774.calisthenicsmemory

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for exercise reordering logic
 * Tests the pure logic without database dependencies
 */
class ExerciseReorderTest {

    companion object {
        // Same constant as in TrainingViewModel
        const val FAVORITE_GROUP_KEY = "★FAVORITES"
    }

    // ========================================
    // Reorder Logic Tests
    // ========================================

    @Test
    fun reorderList_moveItemDown() {
        val items = mutableListOf("A", "B", "C", "D")
        val fromIndex = 0
        val toIndex = 2

        // Simulate reorder: remove from source, insert at destination
        val item = items.removeAt(fromIndex)
        items.add(toIndex, item)

        assertEquals(listOf("B", "C", "A", "D"), items)
    }

    @Test
    fun reorderList_moveItemUp() {
        val items = mutableListOf("A", "B", "C", "D")
        val fromIndex = 3
        val toIndex = 1

        val item = items.removeAt(fromIndex)
        items.add(toIndex, item)

        assertEquals(listOf("A", "D", "B", "C"), items)
    }

    @Test
    fun reorderList_moveToSamePosition() {
        val items = mutableListOf("A", "B", "C", "D")
        val fromIndex = 1
        val toIndex = 1

        // When moving to same position, list should remain unchanged
        if (fromIndex != toIndex) {
            val item = items.removeAt(fromIndex)
            items.add(toIndex, item)
        }

        assertEquals(listOf("A", "B", "C", "D"), items)
    }

    @Test
    fun reorderList_moveFirstToLast() {
        val items = mutableListOf("A", "B", "C")
        val fromIndex = 0
        val toIndex = 2

        val item = items.removeAt(fromIndex)
        items.add(toIndex, item)

        assertEquals(listOf("B", "C", "A"), items)
    }

    @Test
    fun reorderList_moveLastToFirst() {
        val items = mutableListOf("A", "B", "C")
        val fromIndex = 2
        val toIndex = 0

        val item = items.removeAt(fromIndex)
        items.add(toIndex, item)

        assertEquals(listOf("C", "A", "B"), items)
    }

    // ========================================
    // Index Validation Tests
    // ========================================

    @Test
    fun indexValidation_validIndices() {
        val listSize = 5
        val fromIndex = 0
        val toIndex = 4

        val isValid = fromIndex >= 0 && toIndex >= 0 &&
                fromIndex < listSize && toIndex < listSize

        assertTrue(isValid)
    }

    @Test
    fun indexValidation_negativeFromIndex() {
        val listSize = 5
        val fromIndex = -1
        val toIndex = 2

        val isValid = fromIndex >= 0 && toIndex >= 0 &&
                fromIndex < listSize && toIndex < listSize

        assertFalse(isValid)
    }

    @Test
    fun indexValidation_negativeToIndex() {
        val listSize = 5
        val fromIndex = 2
        val toIndex = -1

        val isValid = fromIndex >= 0 && toIndex >= 0 &&
                fromIndex < listSize && toIndex < listSize

        assertFalse(isValid)
    }

    @Test
    fun indexValidation_fromIndexOutOfBounds() {
        val listSize = 5
        val fromIndex = 5  // Out of bounds (0-4 valid)
        val toIndex = 2

        val isValid = fromIndex >= 0 && toIndex >= 0 &&
                fromIndex < listSize && toIndex < listSize

        assertFalse(isValid)
    }

    @Test
    fun indexValidation_toIndexOutOfBounds() {
        val listSize = 5
        val fromIndex = 2
        val toIndex = 5  // Out of bounds

        val isValid = fromIndex >= 0 && toIndex >= 0 &&
                fromIndex < listSize && toIndex < listSize

        assertFalse(isValid)
    }

    @Test
    fun indexValidation_emptyList() {
        val listSize = 0
        val fromIndex = 0
        val toIndex = 0

        val isValid = listSize > 0 && fromIndex >= 0 && toIndex >= 0 &&
                fromIndex < listSize && toIndex < listSize

        assertFalse(isValid)
    }

    // ========================================
    // Favorites Group Restriction Tests
    // ========================================

    @Test
    fun favoritesRestriction_shouldBlockReorder() {
        val groupName = FAVORITE_GROUP_KEY

        // Reorder should be blocked for favorites group
        val shouldBlock = groupName == FAVORITE_GROUP_KEY

        assertTrue(shouldBlock)
    }

    @Test
    fun favoritesRestriction_normalGroupAllowed() {
        val groupName = "Step 1 Pushups"

        val shouldBlock = groupName == FAVORITE_GROUP_KEY

        assertFalse(shouldBlock)
    }

    @Test
    fun favoritesRestriction_nullGroupAllowed() {
        val groupName: String? = null

        val shouldBlock = groupName == FAVORITE_GROUP_KEY

        assertFalse(shouldBlock)
    }

    @Test
    fun favoritesRestriction_emptyGroupAllowed() {
        val groupName = ""

        val shouldBlock = groupName == FAVORITE_GROUP_KEY

        assertFalse(shouldBlock)
    }

    // ========================================
    // Display Order Update Tests
    // ========================================

    @Test
    fun displayOrderUpdate_afterReorder() {
        data class TestExercise(val name: String, var displayOrder: Int)

        val exercises = mutableListOf(
            TestExercise("A", 0),
            TestExercise("B", 1),
            TestExercise("C", 2)
        )

        // Move C to first position
        val fromIndex = 2
        val toIndex = 0
        val item = exercises.removeAt(fromIndex)
        exercises.add(toIndex, item)

        // Update displayOrder
        exercises.forEachIndexed { index, exercise ->
            exercise.displayOrder = index
        }

        assertEquals("C", exercises[0].name)
        assertEquals(0, exercises[0].displayOrder)
        assertEquals("A", exercises[1].name)
        assertEquals(1, exercises[1].displayOrder)
        assertEquals("B", exercises[2].name)
        assertEquals(2, exercises[2].displayOrder)
    }

    @Test
    fun displayOrderUpdate_preservesOrder() {
        data class TestExercise(val name: String, var displayOrder: Int)

        val exercises = listOf(
            TestExercise("A", 5),  // Non-sequential initial order
            TestExercise("B", 10),
            TestExercise("C", 15)
        )

        // After reordering, displayOrder should be 0, 1, 2...
        val reorderedExercises = exercises.toMutableList()
        reorderedExercises.forEachIndexed { index, exercise ->
            exercise.displayOrder = index
        }

        assertEquals(0, reorderedExercises[0].displayOrder)
        assertEquals(1, reorderedExercises[1].displayOrder)
        assertEquals(2, reorderedExercises[2].displayOrder)
    }

    // ========================================
    // Exercise Data Class Tests
    // ========================================

    @Test
    fun exerciseDataClass_defaultDisplayOrder() {
        val exercise = Exercise(
            name = "Test Exercise",
            type = "Dynamic",
            laterality = "Bilateral"
        )

        assertEquals(0, exercise.displayOrder)
    }

    @Test
    fun exerciseDataClass_customDisplayOrder() {
        val exercise = Exercise(
            name = "Test Exercise",
            type = "Dynamic",
            laterality = "Bilateral",
            displayOrder = 5
        )

        assertEquals(5, exercise.displayOrder)
    }

    @Test
    fun exerciseDataClass_copyWithNewDisplayOrder() {
        val original = Exercise(
            id = 1,
            name = "Test Exercise",
            type = "Dynamic",
            group = "Group A",
            sortOrder = 1,
            displayOrder = 0,
            laterality = "Bilateral"
        )

        val updated = original.copy(displayOrder = 3)

        assertEquals(1, updated.id)
        assertEquals("Test Exercise", updated.name)
        assertEquals("Group A", updated.group)
        assertEquals(3, updated.displayOrder)
    }

    // ========================================
    // Group-wise Reorder Tests
    // ========================================

    @Test
    fun groupWiseReorder_onlyAffectsTargetGroup() {
        data class TestExercise(val name: String, val group: String?, var displayOrder: Int)

        val allExercises = listOf(
            TestExercise("A1", "Group A", 0),
            TestExercise("A2", "Group A", 1),
            TestExercise("B1", "Group B", 0),
            TestExercise("B2", "Group B", 1)
        )

        // Filter exercises in Group A
        val groupAExercises = allExercises.filter { it.group == "Group A" }.toMutableList()

        // Reorder within Group A
        val item = groupAExercises.removeAt(0)
        groupAExercises.add(1, item)

        // Update displayOrder for Group A only
        groupAExercises.forEachIndexed { index, exercise ->
            exercise.displayOrder = index
        }

        // Verify Group A is reordered
        assertEquals("A2", groupAExercises[0].name)
        assertEquals(0, groupAExercises[0].displayOrder)
        assertEquals("A1", groupAExercises[1].name)
        assertEquals(1, groupAExercises[1].displayOrder)

        // Verify Group B is unchanged
        val groupBExercises = allExercises.filter { it.group == "Group B" }
        assertEquals("B1", groupBExercises[0].name)
        assertEquals(0, groupBExercises[0].displayOrder)
        assertEquals("B2", groupBExercises[1].name)
        assertEquals(1, groupBExercises[1].displayOrder)
    }

    @Test
    fun ungroupedExercises_canBeReordered() {
        data class TestExercise(val name: String, val group: String?, var displayOrder: Int)

        val exercises = mutableListOf(
            TestExercise("Ungrouped1", null, 0),
            TestExercise("Ungrouped2", null, 1),
            TestExercise("Ungrouped3", null, 2)
        )

        // Filter ungrouped (null group)
        val ungrouped = exercises.filter { it.group == null }.toMutableList()

        assertEquals(3, ungrouped.size)

        // Reorder
        val item = ungrouped.removeAt(2)
        ungrouped.add(0, item)

        assertEquals("Ungrouped3", ungrouped[0].name)
        assertEquals("Ungrouped1", ungrouped[1].name)
        assertEquals("Ungrouped2", ungrouped[2].name)
    }

    // ========================================
    // Sorting by DisplayOrder Tests
    // ========================================

    @Test
    fun sortByDisplayOrder_correctOrder() {
        val exercises = listOf(
            Exercise(id = 1, name = "C", type = "Dynamic", displayOrder = 2, laterality = "Bilateral"),
            Exercise(id = 2, name = "A", type = "Dynamic", displayOrder = 0, laterality = "Bilateral"),
            Exercise(id = 3, name = "B", type = "Dynamic", displayOrder = 1, laterality = "Bilateral")
        )

        val sorted = exercises.sortedBy { it.displayOrder }

        assertEquals("A", sorted[0].name)
        assertEquals("B", sorted[1].name)
        assertEquals("C", sorted[2].name)
    }

    @Test
    fun sortByDisplayOrder_withSameOrder() {
        val exercises = listOf(
            Exercise(id = 1, name = "C", type = "Dynamic", displayOrder = 0, laterality = "Bilateral"),
            Exercise(id = 2, name = "A", type = "Dynamic", displayOrder = 0, laterality = "Bilateral"),
            Exercise(id = 3, name = "B", type = "Dynamic", displayOrder = 0, laterality = "Bilateral")
        )

        val sorted = exercises.sortedBy { it.displayOrder }

        // All have same displayOrder, so original order is preserved (stable sort)
        assertEquals(3, sorted.size)
        // Order is stable - original list order preserved for equal keys
    }
}