package io.github.gonbei774.calisthenicsmemory

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for CSV Exercise import/export with DB v11 format compatibility
 * Tests V11 (13-column) format and backward compatibility with V10 (11-column) and V9 (8-column)
 */
class CsvExerciseV11Test {

    // ========================================
    // CSV Format Detection Tests
    // ========================================

    @Test
    fun detectCsvVersion_13columns_returnsV11() {
        val header = "name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite,displayOrder,restInterval,repDuration,distanceTrackingEnabled,weightTrackingEnabled"
        val columnCount = header.split(",").size

        val version = when {
            columnCount >= 13 -> 11
            columnCount >= 11 -> 10
            columnCount >= 8 -> 9
            else -> 0
        }

        assertEquals(11, version)
    }

    @Test
    fun detectCsvVersion_backwardCompatibility() {
        val v9Header = "name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite"
        val v10Header = "name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite,displayOrder,restInterval,repDuration"
        val v11Header = "name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite,displayOrder,restInterval,repDuration,distanceTrackingEnabled,weightTrackingEnabled"

        fun detectVersion(header: String): Int {
            val cols = header.split(",").size
            return when {
                cols >= 13 -> 11
                cols >= 11 -> 10
                cols >= 8 -> 9
                else -> 0
            }
        }

        assertEquals(9, detectVersion(v9Header))
        assertEquals(10, detectVersion(v10Header))
        assertEquals(11, detectVersion(v11Header))
    }

    // ========================================
    // V11 Format Parsing Tests
    // ========================================

    @Test
    fun parseV11Format_allFieldsFilled() {
        val line = "Wall Push-up,Dynamic,Step 1 Pushups,1,Bilateral,3,50,false,0,120,5,false,false"
        val columns = line.split(",")

        assertEquals(13, columns.size)
        assertEquals("Wall Push-up", columns[0])
        assertEquals("Dynamic", columns[1])
        assertEquals("Step 1 Pushups", columns[2])
        assertEquals("1", columns[3])
        assertEquals("Bilateral", columns[4])
        assertEquals("3", columns[5])
        assertEquals("50", columns[6])
        assertEquals("false", columns[7])
        assertEquals("0", columns[8])      // displayOrder
        assertEquals("120", columns[9])    // restInterval
        assertEquals("5", columns[10])     // repDuration
        assertEquals("false", columns[11]) // distanceTrackingEnabled
        assertEquals("false", columns[12]) // weightTrackingEnabled
    }

    @Test
    fun parseV11Format_withDistanceTrackingEnabled() {
        val line = "Running,Dynamic,Cardio,1,Bilateral,,,false,0,,,true,false"
        val columns = line.split(",")

        assertEquals(13, columns.size)
        assertEquals("Running", columns[0])
        assertEquals("true", columns[11])  // distanceTrackingEnabled
        assertEquals("false", columns[12]) // weightTrackingEnabled
    }

    @Test
    fun parseV11Format_withWeightTrackingEnabled() {
        val line = "Weighted Pull-up,Dynamic,Pull-ups,1,Bilateral,3,10,false,0,120,,false,true"
        val columns = line.split(",")

        assertEquals(13, columns.size)
        assertEquals("Weighted Pull-up", columns[0])
        assertEquals("false", columns[11]) // distanceTrackingEnabled
        assertEquals("true", columns[12])  // weightTrackingEnabled
    }

    @Test
    fun parseV11Format_withBothTrackingEnabled() {
        val line = "Weighted Run,Dynamic,Cardio,1,Bilateral,,,false,0,,,true,true"
        val columns = line.split(",")

        assertEquals(13, columns.size)
        assertEquals("Weighted Run", columns[0])
        assertEquals("true", columns[11])  // distanceTrackingEnabled
        assertEquals("true", columns[12])  // weightTrackingEnabled
    }

    @Test
    fun parseV11Format_withEmptyOptionalFields() {
        val line = "Simple Exercise,Dynamic,,0,Bilateral,,,false,0,,,false,false"
        val columns = line.split(",")

        assertEquals(13, columns.size)
        assertEquals("Simple Exercise", columns[0])
        assertEquals("", columns[2])       // group empty
        assertEquals("", columns[5])       // targetSets empty
        assertEquals("", columns[6])       // targetValue empty
        assertEquals("", columns[9])       // restInterval empty
        assertEquals("", columns[10])      // repDuration empty
        assertEquals("false", columns[11]) // distanceTrackingEnabled
        assertEquals("false", columns[12]) // weightTrackingEnabled
    }

    // ========================================
    // Backward Compatibility Parsing Tests
    // ========================================

    @Test
    fun parseV10Format_applyDefaultsForV11Fields() {
        val line = "Test,Dynamic,Group,1,Bilateral,3,50,false,0,120,5"
        val columns = line.split(",")

        assertEquals(11, columns.size)

        // Parse V10 format
        val name = columns[0]
        val distanceTrackingEnabled = columns.getOrNull(11)?.toBooleanStrictOrNull() ?: false
        val weightTrackingEnabled = columns.getOrNull(12)?.toBooleanStrictOrNull() ?: false

        assertEquals("Test", name)
        assertEquals(false, distanceTrackingEnabled)  // Default
        assertEquals(false, weightTrackingEnabled)    // Default
    }

    @Test
    fun parseV9Format_applyDefaultsForV10AndV11Fields() {
        val line = "Test,Dynamic,Group,1,Bilateral,3,50,false"
        val columns = line.split(",")

        assertEquals(8, columns.size)

        // Parse V9 format
        val name = columns[0]
        val displayOrder = columns.getOrNull(8)?.toIntOrNull() ?: 0
        val restInterval = columns.getOrNull(9)?.toIntOrNull()
        val repDuration = columns.getOrNull(10)?.toIntOrNull()
        val distanceTrackingEnabled = columns.getOrNull(11)?.toBooleanStrictOrNull() ?: false
        val weightTrackingEnabled = columns.getOrNull(12)?.toBooleanStrictOrNull() ?: false

        assertEquals("Test", name)
        assertEquals(0, displayOrder)                  // V10 default
        assertNull(restInterval)                       // V10 default
        assertNull(repDuration)                        // V10 default
        assertEquals(false, distanceTrackingEnabled)   // V11 default
        assertEquals(false, weightTrackingEnabled)     // V11 default
    }

    // ========================================
    // CSV Export Format Tests
    // ========================================

    @Test
    fun exportV11Format_header() {
        val header = "name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite,displayOrder,restInterval,repDuration,distanceTrackingEnabled,weightTrackingEnabled"
        val columns = header.split(",")

        assertEquals(13, columns.size)
        assertEquals("name", columns[0])
        assertEquals("displayOrder", columns[8])
        assertEquals("restInterval", columns[9])
        assertEquals("repDuration", columns[10])
        assertEquals("distanceTrackingEnabled", columns[11])
        assertEquals("weightTrackingEnabled", columns[12])
    }

    @Test
    fun exportV11Format_dataLine() {
        // Simulate export
        val name = "Weighted Pull-up"
        val type = "Dynamic"
        val group = "Pull-ups"
        val sortOrder = 1
        val laterality = "Bilateral"
        val targetSets: Int? = 3
        val targetValue: Int? = 10
        val isFavorite = false
        val displayOrder = 0
        val restInterval: Int? = 120
        val repDuration: Int? = null
        val distanceTrackingEnabled = false
        val weightTrackingEnabled = true

        val csvLine = "$name,$type,${group ?: ""},$sortOrder,$laterality," +
                "${targetSets ?: ""},${targetValue ?: ""},$isFavorite," +
                "$displayOrder,${restInterval ?: ""},${repDuration ?: ""}," +
                "$distanceTrackingEnabled,$weightTrackingEnabled"

        assertEquals(
            "Weighted Pull-up,Dynamic,Pull-ups,1,Bilateral,3,10,false,0,120,,false,true",
            csvLine
        )
    }

    @Test
    fun exportV11Format_distanceTrackingExercise() {
        val name = "Running"
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
        val distanceTrackingEnabled = true
        val weightTrackingEnabled = false

        val csvLine = "$name,$type,${group ?: ""},$sortOrder,$laterality," +
                "${targetSets ?: ""},${targetValue ?: ""},$isFavorite," +
                "$displayOrder,${restInterval ?: ""},${repDuration ?: ""}," +
                "$distanceTrackingEnabled,$weightTrackingEnabled"

        assertEquals(
            "Running,Dynamic,,0,Bilateral,,,false,0,,,true,false",
            csvLine
        )
    }

    // ========================================
    // Field Validation Tests
    // ========================================

    @Test
    fun validation_distanceTrackingEnabledField() {
        val validValues = listOf("true", "false")
        val invalidValues = listOf("1", "0", "yes", "no", "", "TRUE", "FALSE")

        validValues.forEach { value ->
            val parsed = value.toBooleanStrictOrNull()
            assertNotNull("$value should be parseable", parsed)
        }

        invalidValues.forEach { value ->
            val parsed = value.toBooleanStrictOrNull()
            assertNull("$value should not be parseable as strict boolean", parsed)
        }
    }

    @Test
    fun validation_weightTrackingEnabledField() {
        val validValues = listOf("true", "false")
        val invalidValues = listOf("1", "0", "yes", "no", "", "TRUE", "FALSE")

        validValues.forEach { value ->
            val parsed = value.toBooleanStrictOrNull()
            assertNotNull("$value should be parseable", parsed)
        }

        invalidValues.forEach { value ->
            val parsed = value.toBooleanStrictOrNull()
            assertNull("$value should not be parseable as strict boolean", parsed)
        }
    }

    @Test
    fun validation_defaultValuesForMissingTrackingFields() {
        // When tracking fields are missing or empty, should default to false
        val emptyValue = ""
        val nullValue: String? = null

        val parsedEmpty = emptyValue.toBooleanStrictOrNull() ?: false
        val parsedNull = nullValue?.toBooleanStrictOrNull() ?: false

        assertEquals(false, parsedEmpty)
        assertEquals(false, parsedNull)
    }

    // ========================================
    // Edge Cases Tests
    // ========================================

    @Test
    fun edgeCase_extraColumnsIgnored() {
        // If there are more than 13 columns, extras should be ignored
        val line = "Test,Dynamic,Group,1,Bilateral,3,50,false,0,120,5,true,true,extra1,extra2"
        val columns = line.split(",")

        assertTrue(columns.size > 13)

        // Parse only the first 13 columns
        val name = columns[0]
        val distanceTrackingEnabled = columns.getOrNull(11)?.toBooleanStrictOrNull() ?: false
        val weightTrackingEnabled = columns.getOrNull(12)?.toBooleanStrictOrNull() ?: false

        assertEquals("Test", name)
        assertEquals(true, distanceTrackingEnabled)
        assertEquals(true, weightTrackingEnabled)
    }

    @Test
    fun edgeCase_unicodeCharactersWithTracking() {
        val line = "ランニング,Dynamic,有酸素,1,Bilateral,,,false,0,,,true,false"
        val columns = line.split(",")

        assertEquals(13, columns.size)
        assertEquals("ランニング", columns[0])
        assertEquals("有酸素", columns[2])
        assertEquals("true", columns[11])  // distanceTrackingEnabled
        assertEquals("false", columns[12]) // weightTrackingEnabled
    }

    @Test
    fun edgeCase_onlyHeaderV11() {
        val csv = "name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite,displayOrder,restInterval,repDuration,distanceTrackingEnabled,weightTrackingEnabled"
        val lines = csv.lines().filter { it.isNotBlank() && !it.startsWith("#") }
        val dataLines = lines.drop(1)

        assertTrue(dataLines.isEmpty())
    }

    @Test
    fun edgeCase_csvWithCommentsAndBlankLines() {
        val csv = """
            # V11 format with distance and weight tracking
            name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite,displayOrder,restInterval,repDuration,distanceTrackingEnabled,weightTrackingEnabled

            Running,Dynamic,Cardio,1,Bilateral,,,false,0,,,true,false
            # This is a weighted exercise
            Weighted Squat,Dynamic,Legs,2,Bilateral,3,20,false,0,120,,false,true
        """.trimIndent()

        val lines = csv.lines().filter { it.isNotBlank() && !it.startsWith("#") }

        assertEquals(3, lines.size)  // Header + 2 data lines
        assertTrue(lines[0].startsWith("name,type,group"))
        assertTrue(lines[1].startsWith("Running"))
        assertTrue(lines[2].startsWith("Weighted Squat"))
    }

    // ========================================
    // Full Parsing Simulation Tests
    // ========================================

    @Test
    fun fullParsing_v11ExerciseWithAllFields() {
        val line = "Weighted Pull-up,Dynamic,Pull-ups,5,Bilateral,3,10,true,2,180,5,false,true"
        val columns = line.split(",")

        val name = columns[0]
        val type = columns[1]
        val group = columns[2].ifEmpty { null }
        val sortOrder = columns[3].toIntOrNull() ?: 0
        val laterality = columns[4]
        val targetSets = columns[5].toIntOrNull()
        val targetValue = columns[6].toIntOrNull()
        val isFavorite = columns[7].toBooleanStrictOrNull() ?: false
        val displayOrder = columns[8].toIntOrNull() ?: 0
        val restInterval = columns[9].ifEmpty { null }?.toIntOrNull()
        val repDuration = columns[10].ifEmpty { null }?.toIntOrNull()
        val distanceTrackingEnabled = columns[11].toBooleanStrictOrNull() ?: false
        val weightTrackingEnabled = columns[12].toBooleanStrictOrNull() ?: false

        assertEquals("Weighted Pull-up", name)
        assertEquals("Dynamic", type)
        assertEquals("Pull-ups", group)
        assertEquals(5, sortOrder)
        assertEquals("Bilateral", laterality)
        assertEquals(3, targetSets)
        assertEquals(10, targetValue)
        assertEquals(true, isFavorite)
        assertEquals(2, displayOrder)
        assertEquals(180, restInterval)
        assertEquals(5, repDuration)
        assertEquals(false, distanceTrackingEnabled)
        assertEquals(true, weightTrackingEnabled)
    }

    @Test
    fun fullParsing_v10ExerciseWithDefaults() {
        // V10 format (11 columns) - should apply defaults for V11 fields
        val line = "Wall Push-up,Dynamic,Push-ups,1,Bilateral,3,50,false,0,120,5"
        val columns = line.split(",")

        val name = columns[0]
        val distanceTrackingEnabled = columns.getOrNull(11)?.toBooleanStrictOrNull() ?: false
        val weightTrackingEnabled = columns.getOrNull(12)?.toBooleanStrictOrNull() ?: false

        assertEquals("Wall Push-up", name)
        assertEquals(false, distanceTrackingEnabled)  // Default
        assertEquals(false, weightTrackingEnabled)    // Default
    }
}