package io.github.gonbei774.calisthenicsmemory

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for CSV Exercise import/export with DB v10 format compatibility
 * Tests both old (8-column) and new (11-column) CSV formats
 */
class CsvExerciseV10Test {

    // ========================================
    // CSV Format Detection Tests
    // ========================================

    @Test
    fun detectCsvVersion_8columns_returnsV9() {
        val header = "name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite"
        val columnCount = header.split(",").size

        val version = when {
            columnCount >= 11 -> 10
            columnCount >= 8 -> 9
            else -> 0
        }

        assertEquals(9, version)
    }

    @Test
    fun detectCsvVersion_11columns_returnsV10() {
        val header = "name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite,displayOrder,restInterval,repDuration"
        val columnCount = header.split(",").size

        val version = when {
            columnCount >= 11 -> 10
            columnCount >= 8 -> 9
            else -> 0
        }

        assertEquals(10, version)
    }

    @Test
    fun detectCsvVersion_lessThan8columns_returnsUnknown() {
        val header = "name,type,group"
        val columnCount = header.split(",").size

        val version = when {
            columnCount >= 11 -> 10
            columnCount >= 8 -> 9
            else -> 0
        }

        assertEquals(0, version)
    }

    // ========================================
    // V8 Format Parsing Tests (Backward Compatibility)
    // ========================================

    @Test
    fun parseV8Format_allFieldsFilled() {
        val line = "Wall Push-up,Dynamic,Step 1 Pushups,1,Bilateral,3,50,false"
        val columns = line.split(",")

        assertEquals(8, columns.size)
        assertEquals("Wall Push-up", columns[0])
        assertEquals("Dynamic", columns[1])
        assertEquals("Step 1 Pushups", columns[2])
        assertEquals("1", columns[3])
        assertEquals("Bilateral", columns[4])
        assertEquals("3", columns[5])
        assertEquals("50", columns[6])
        assertEquals("false", columns[7])
    }

    @Test
    fun parseV8Format_withEmptyOptionalFields() {
        val line = "Simple Exercise,Dynamic,,0,Bilateral,,,false"
        val columns = line.split(",")

        assertEquals(8, columns.size)
        assertEquals("Simple Exercise", columns[0])
        assertEquals("", columns[2])  // group empty
        assertEquals("", columns[5])  // targetSets empty
        assertEquals("", columns[6])  // targetValue empty
    }

    @Test
    fun parseV8Format_applyDefaultsForNewFields() {
        val line = "Test,Dynamic,Group,1,Bilateral,3,50,false"
        val columns = line.split(",")

        // Parse old format
        val name = columns[0]
        val type = columns[1]
        val group = columns[2].ifEmpty { null }
        val sortOrder = columns[3].toIntOrNull() ?: 0
        val laterality = columns[4]
        val targetSets = columns[5].toIntOrNull()
        val targetValue = columns[6].toIntOrNull()
        val isFavorite = columns[7].toBooleanStrictOrNull() ?: false

        // Apply defaults for new fields (v10)
        val displayOrder = columns.getOrNull(8)?.toIntOrNull() ?: 0
        val restInterval = columns.getOrNull(9)?.toIntOrNull()
        val repDuration = columns.getOrNull(10)?.toIntOrNull()

        assertEquals("Test", name)
        assertEquals("Dynamic", type)
        assertEquals("Group", group)
        assertEquals(1, sortOrder)
        assertEquals("Bilateral", laterality)
        assertEquals(3, targetSets)
        assertEquals(50, targetValue)
        assertEquals(false, isFavorite)
        assertEquals(0, displayOrder)  // Default
        assertNull(restInterval)       // Default
        assertNull(repDuration)        // Default
    }

    // ========================================
    // V10 Format Parsing Tests
    // ========================================

    @Test
    fun parseV10Format_allFieldsFilled() {
        val line = "Wall Push-up,Dynamic,Step 1 Pushups,1,Bilateral,3,50,false,0,120,5"
        val columns = line.split(",")

        assertEquals(11, columns.size)
        assertEquals("Wall Push-up", columns[0])
        assertEquals("Dynamic", columns[1])
        assertEquals("Step 1 Pushups", columns[2])
        assertEquals("1", columns[3])
        assertEquals("Bilateral", columns[4])
        assertEquals("3", columns[5])
        assertEquals("50", columns[6])
        assertEquals("false", columns[7])
        assertEquals("0", columns[8])    // displayOrder
        assertEquals("120", columns[9])  // restInterval
        assertEquals("5", columns[10])   // repDuration
    }

    @Test
    fun parseV10Format_withEmptyTimerFields() {
        val line = "Test Exercise,Dynamic,Group,1,Bilateral,3,50,true,2,,"
        val columns = line.split(",")

        assertEquals(11, columns.size)

        val displayOrder = columns[8].toIntOrNull() ?: 0
        val restInterval = columns[9].ifEmpty { null }?.toIntOrNull()
        val repDuration = columns[10].ifEmpty { null }?.toIntOrNull()

        assertEquals(2, displayOrder)
        assertNull(restInterval)
        assertNull(repDuration)
    }

    @Test
    fun parseV10Format_partialTimerSettings() {
        val line = "Test Exercise,Dynamic,Group,1,Bilateral,3,50,false,1,180,"
        val columns = line.split(",")

        val restInterval = columns[9].ifEmpty { null }?.toIntOrNull()
        val repDuration = columns[10].ifEmpty { null }?.toIntOrNull()

        assertEquals(180, restInterval)
        assertNull(repDuration)
    }

    // ========================================
    // CSV Export Format Tests
    // ========================================

    @Test
    fun exportV10Format_header() {
        val header = "name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite,displayOrder,restInterval,repDuration"
        val columns = header.split(",")

        assertEquals(11, columns.size)
        assertEquals("name", columns[0])
        assertEquals("displayOrder", columns[8])
        assertEquals("restInterval", columns[9])
        assertEquals("repDuration", columns[10])
    }

    @Test
    fun exportV10Format_dataLine() {
        // Simulate export
        val name = "Wall Push-up"
        val type = "Dynamic"
        val group = "Step 1 Pushups"
        val sortOrder = 1
        val laterality = "Bilateral"
        val targetSets: Int? = 3
        val targetValue: Int? = 50
        val isFavorite = false
        val displayOrder = 0
        val restInterval: Int? = 120
        val repDuration: Int? = 5

        val csvLine = "$name,$type,${group ?: ""},$sortOrder,$laterality," +
                "${targetSets ?: ""},${targetValue ?: ""},$isFavorite," +
                "$displayOrder,${restInterval ?: ""},${repDuration ?: ""}"

        assertEquals(
            "Wall Push-up,Dynamic,Step 1 Pushups,1,Bilateral,3,50,false,0,120,5",
            csvLine
        )
    }

    @Test
    fun exportV10Format_nullableFieldsAsEmpty() {
        val name = "Simple Exercise"
        val type = "Dynamic"
        val group: String? = null
        val sortOrder = 0
        val laterality = "Bilateral"
        val targetSets: Int? = null
        val targetValue: Int? = null
        val isFavorite = false
        val displayOrder = 0
        val restInterval: Int? = null
        val repDuration: Int? = null

        val csvLine = "$name,$type,${group ?: ""},$sortOrder,$laterality," +
                "${targetSets ?: ""},${targetValue ?: ""},$isFavorite," +
                "$displayOrder,${restInterval ?: ""},${repDuration ?: ""}"

        assertEquals(
            "Simple Exercise,Dynamic,,0,Bilateral,,,false,0,,",
            csvLine
        )
    }

    // ========================================
    // Field Validation Tests
    // ========================================

    @Test
    fun validation_typeField() {
        val validTypes = listOf("Dynamic", "Isometric")

        assertTrue("Dynamic" in validTypes)
        assertTrue("Isometric" in validTypes)
        assertFalse("Invalid" in validTypes)
        assertFalse("dynamic" in validTypes)  // Case sensitive
    }

    @Test
    fun validation_lateralityField() {
        val validLaterality = listOf("Bilateral", "Unilateral")

        assertTrue("Bilateral" in validLaterality)
        assertTrue("Unilateral" in validLaterality)
        assertFalse("Invalid" in validLaterality)
    }

    @Test
    fun validation_displayOrderField() {
        // displayOrder should be non-negative
        val validValues = listOf(0, 1, 5, 100)
        val invalidValues = listOf(-1, -10)

        validValues.forEach { assertTrue("$it should be valid", it >= 0) }
        invalidValues.forEach { assertFalse("$it should be invalid", it >= 0) }
    }

    @Test
    fun validation_restIntervalField() {
        // restInterval: 0-600 seconds (0 = disabled, max 10 minutes)
        val maxRestInterval = 600

        val validValues = listOf(0, 60, 120, 240, 600)
        val invalidValues = listOf(-1, 601, 1000)

        validValues.forEach { assertTrue("$it should be valid", it in 0..maxRestInterval) }
        invalidValues.forEach { assertFalse("$it should be invalid", it in 0..maxRestInterval) }
    }

    @Test
    fun validation_repDurationField() {
        // repDuration: positive integer (seconds per rep)
        val validValues = listOf(1, 3, 5, 10)
        val invalidValues = listOf(0, -1)

        validValues.forEach { assertTrue("$it should be valid", it > 0) }
        invalidValues.forEach { assertFalse("$it should be invalid", it > 0) }
    }

    // ========================================
    // Group Auto-Creation Tests
    // ========================================

    @Test
    fun groupAutoCreation_detectNewGroup() {
        val existingGroups = setOf("Group A", "Group B")
        val csvGroup = "Group C"

        val needsCreation = csvGroup !in existingGroups

        assertTrue(needsCreation)
    }

    @Test
    fun groupAutoCreation_existingGroupNoCreation() {
        val existingGroups = setOf("Group A", "Group B")
        val csvGroup = "Group A"

        val needsCreation = csvGroup !in existingGroups

        assertFalse(needsCreation)
    }

    @Test
    fun groupAutoCreation_nullGroupNoCreation() {
        val existingGroups = setOf("Group A", "Group B")
        val csvGroup: String? = null

        val needsCreation = csvGroup != null && csvGroup !in existingGroups

        assertFalse(needsCreation)
    }

    @Test
    fun groupAutoCreation_countNewGroups() {
        val existingGroups = setOf("Existing Group")
        val csvGroups = listOf("Existing Group", "New Group 1", "New Group 2", null, "New Group 1")

        val newGroupsToCreate = csvGroups
            .filterNotNull()
            .filter { it.isNotEmpty() }
            .toSet()
            .filter { it !in existingGroups }

        assertEquals(2, newGroupsToCreate.size)
        assertTrue(newGroupsToCreate.contains("New Group 1"))
        assertTrue(newGroupsToCreate.contains("New Group 2"))
    }

    // ========================================
    // Import Report Tests
    // ========================================

    @Test
    fun importReport_countsCorrectly() {
        data class ImportResult(
            val successCount: Int,
            val skippedCount: Int,
            val errorCount: Int,
            val groupsCreatedCount: Int
        )

        // Simulate import results
        var success = 0
        var skipped = 0
        var error = 0
        var groupsCreated = 0

        // Line 1: Success, new group
        success++
        groupsCreated++

        // Line 2: Success, existing group
        success++

        // Line 3: Skipped (duplicate)
        skipped++

        // Line 4: Error (invalid type)
        error++

        // Line 5: Success, new group
        success++
        groupsCreated++

        val report = ImportResult(success, skipped, error, groupsCreated)

        assertEquals(3, report.successCount)
        assertEquals(1, report.skippedCount)
        assertEquals(1, report.errorCount)
        assertEquals(2, report.groupsCreatedCount)
    }

    // ========================================
    // Edge Cases Tests
    // ========================================

    @Test
    fun edgeCase_emptyCSV() {
        val csv = ""
        val lines = csv.lines().filter { it.isNotBlank() && !it.startsWith("#") }

        assertTrue(lines.isEmpty())
    }

    @Test
    fun edgeCase_onlyHeader() {
        val csv = "name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite,displayOrder,restInterval,repDuration"
        val lines = csv.lines().filter { it.isNotBlank() && !it.startsWith("#") }
        val dataLines = lines.drop(1)

        assertTrue(dataLines.isEmpty())
    }

    @Test
    fun edgeCase_commaInComment() {
        // Comments should be ignored even if they contain commas
        val csv = """
            # This, is, a, comment
            name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite
            Test,Dynamic,Group,1,Bilateral,3,50,false
        """.trimIndent()

        val lines = csv.lines().filter { it.isNotBlank() && !it.startsWith("#") }

        assertEquals(2, lines.size)  // Header + 1 data line
    }

    @Test
    fun edgeCase_extraColumnsIgnored() {
        // If there are more than 11 columns, extras should be ignored
        val line = "Test,Dynamic,Group,1,Bilateral,3,50,false,0,120,5,extra1,extra2"
        val columns = line.split(",")

        assertTrue(columns.size > 11)

        // Parse only the first 11 columns
        val name = columns[0]
        val displayOrder = columns.getOrNull(8)?.toIntOrNull() ?: 0
        val repDuration = columns.getOrNull(10)?.toIntOrNull()

        assertEquals("Test", name)
        assertEquals(0, displayOrder)
        assertEquals(5, repDuration)
    }

    @Test
    fun edgeCase_unicodeCharacters() {
        val line = "壁腕立て伏せ,Dynamic,ステップ1,1,Bilateral,3,50,false,0,120,5"
        val columns = line.split(",")

        assertEquals("壁腕立て伏せ", columns[0])
        assertEquals("ステップ1", columns[2])
    }
}