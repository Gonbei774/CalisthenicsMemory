package io.github.gonbei774.calisthenicsmemory

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for CSV Record import/export with DB v11 format compatibility
 * Tests V11 (10-column) format and backward compatibility with V10 (8-column)
 */
class CsvRecordV11Test {

    // ========================================
    // CSV Format Detection Tests
    // ========================================

    @Test
    fun detectCsvVersion_10columns_returnsV11() {
        val header = "exerciseName,exerciseType,date,time,setNumber,valueRight,valueLeft,comment,distanceCm,weightG"
        val columnCount = header.split(",").size

        val version = when {
            columnCount >= 10 -> 11
            columnCount >= 8 -> 10
            else -> 0
        }

        assertEquals(11, version)
    }

    @Test
    fun detectCsvVersion_8columns_returnsV10() {
        val header = "exerciseName,exerciseType,date,time,setNumber,valueRight,valueLeft,comment"
        val columnCount = header.split(",").size

        val version = when {
            columnCount >= 10 -> 11
            columnCount >= 8 -> 10
            else -> 0
        }

        assertEquals(10, version)
    }

    @Test
    fun detectCsvVersion_lessThan8columns_returnsUnknown() {
        val header = "exerciseName,exerciseType,date"
        val columnCount = header.split(",").size

        val version = when {
            columnCount >= 10 -> 11
            columnCount >= 8 -> 10
            else -> 0
        }

        assertEquals(0, version)
    }

    // ========================================
    // V11 Format Parsing Tests
    // ========================================

    @Test
    fun parseV11Format_allFieldsFilled() {
        val line = "Weighted Pull-up,Dynamic,2025-12-04,10:30,1,10,,Heavy workout,50,5000"
        val columns = line.split(",")

        assertEquals(10, columns.size)
        assertEquals("Weighted Pull-up", columns[0])
        assertEquals("Dynamic", columns[1])
        assertEquals("2025-12-04", columns[2])
        assertEquals("10:30", columns[3])
        assertEquals("1", columns[4])
        assertEquals("10", columns[5])     // valueRight
        assertEquals("", columns[6])       // valueLeft (bilateral)
        assertEquals("Heavy workout", columns[7])
        assertEquals("50", columns[8])     // distanceCm
        assertEquals("5000", columns[9])   // weightG (5kg = 5000g)
    }

    @Test
    fun parseV11Format_withDistanceOnly() {
        val line = "Running,Dynamic,2025-12-04,10:30,1,30,,Good run,500,"
        val columns = line.split(",")

        assertEquals(10, columns.size)
        assertEquals("Running", columns[0])
        assertEquals("500", columns[8])    // distanceCm (5 meters)
        assertEquals("", columns[9])       // weightG is empty
    }

    @Test
    fun parseV11Format_withWeightOnly() {
        val line = "Weighted Squat,Dynamic,2025-12-04,10:30,1,20,,Felt heavy,,2500"
        val columns = line.split(",")

        assertEquals(10, columns.size)
        assertEquals("Weighted Squat", columns[0])
        assertEquals("", columns[8])       // distanceCm is empty
        assertEquals("2500", columns[9])   // weightG (2.5kg = 2500g)
    }

    @Test
    fun parseV11Format_withBothDistanceAndWeight() {
        val line = "Weighted Lunges,Dynamic,2025-12-04,10:30,1,15,14,Long steps with weight,100,3000"
        val columns = line.split(",")

        assertEquals(10, columns.size)
        assertEquals("Weighted Lunges", columns[0])
        assertEquals("15", columns[5])     // valueRight
        assertEquals("14", columns[6])     // valueLeft (unilateral)
        assertEquals("100", columns[8])    // distanceCm (1 meter)
        assertEquals("3000", columns[9])   // weightG (3kg)
    }

    @Test
    fun parseV11Format_withNullDistanceAndWeight() {
        val line = "Wall Push-up,Dynamic,2025-12-04,10:30,1,30,,Good form,,"
        val columns = line.split(",")

        assertEquals(10, columns.size)
        assertEquals("Wall Push-up", columns[0])
        assertEquals("", columns[8])       // distanceCm is empty
        assertEquals("", columns[9])       // weightG is empty
    }

    @Test
    fun parseV11Format_unilateralExercise() {
        val line = "Pistol Squat,Dynamic,2025-12-04,10:30,1,5,4,Right stronger,,1500"
        val columns = line.split(",")

        assertEquals(10, columns.size)
        assertEquals("Pistol Squat", columns[0])
        assertEquals("5", columns[5])      // valueRight
        assertEquals("4", columns[6])      // valueLeft
        assertEquals("", columns[8])       // distanceCm is empty
        assertEquals("1500", columns[9])   // weightG (1.5kg)
    }

    // ========================================
    // Backward Compatibility Parsing Tests
    // ========================================

    @Test
    fun parseV10Format_applyDefaultsForV11Fields() {
        val line = "Wall Push-up,Dynamic,2025-12-04,10:30,1,30,,Good form"
        val columns = line.split(",")

        assertEquals(8, columns.size)

        // Parse V10 format
        val exerciseName = columns[0]
        val distanceCm = columns.getOrNull(8)?.ifEmpty { null }?.toIntOrNull()
        val weightG = columns.getOrNull(9)?.ifEmpty { null }?.toIntOrNull()

        assertEquals("Wall Push-up", exerciseName)
        assertNull(distanceCm)   // Default (not present)
        assertNull(weightG)      // Default (not present)
    }

    // ========================================
    // CSV Export Format Tests
    // ========================================

    @Test
    fun exportV11Format_header() {
        val header = "exerciseName,exerciseType,date,time,setNumber,valueRight,valueLeft,comment,distanceCm,weightG"
        val columns = header.split(",")

        assertEquals(10, columns.size)
        assertEquals("exerciseName", columns[0])
        assertEquals("comment", columns[7])
        assertEquals("distanceCm", columns[8])
        assertEquals("weightG", columns[9])
    }

    @Test
    fun exportV11Format_dataLineWithDistanceAndWeight() {
        // Simulate export
        val exerciseName = "Weighted Lunges"
        val exerciseType = "Dynamic"
        val date = "2025-12-04"
        val time = "10:30"
        val setNumber = 1
        val valueRight: Int? = 15
        val valueLeft: Int? = 14
        val comment = "Long steps"
        val distanceCm: Int? = 100
        val weightG: Int? = 3000

        val csvLine = "$exerciseName,$exerciseType,$date,$time,$setNumber," +
                "${valueRight ?: ""},${valueLeft ?: ""},$comment," +
                "${distanceCm ?: ""},${weightG ?: ""}"

        assertEquals(
            "Weighted Lunges,Dynamic,2025-12-04,10:30,1,15,14,Long steps,100,3000",
            csvLine
        )
    }

    @Test
    fun exportV11Format_dataLineWithNullDistanceAndWeight() {
        val exerciseName = "Wall Push-up"
        val exerciseType = "Dynamic"
        val date = "2025-12-04"
        val time = "10:30"
        val setNumber = 1
        val valueRight: Int? = 30
        val valueLeft: Int? = null
        val comment = ""
        val distanceCm: Int? = null
        val weightG: Int? = null

        val csvLine = "$exerciseName,$exerciseType,$date,$time,$setNumber," +
                "${valueRight ?: ""},${valueLeft ?: ""},$comment," +
                "${distanceCm ?: ""},${weightG ?: ""}"

        assertEquals(
            "Wall Push-up,Dynamic,2025-12-04,10:30,1,30,,,,",
            csvLine
        )
    }

    // ========================================
    // Field Validation Tests
    // ========================================

    @Test
    fun validation_distanceCmField() {
        // distanceCm should be non-negative integer (or null)
        val validValues = listOf(0, 1, 50, 100, 500, 10000)
        val invalidValues = listOf(-1, -100)

        validValues.forEach { value ->
            assertTrue("$value should be valid", value >= 0)
        }

        invalidValues.forEach { value ->
            assertFalse("$value should be invalid", value >= 0)
        }
    }

    @Test
    fun validation_weightGField() {
        // weightG should be non-negative integer in grams (or null)
        val validValues = listOf(0, 100, 500, 1000, 5000, 20000)
        val invalidValues = listOf(-1, -500)

        validValues.forEach { value ->
            assertTrue("$value should be valid", value >= 0)
        }

        invalidValues.forEach { value ->
            assertFalse("$value should be invalid", value >= 0)
        }
    }

    @Test
    fun validation_distanceCmParsing() {
        val validStrings = listOf("0", "50", "100", "500")
        val emptyStrings = listOf("", " ")
        val invalidStrings = listOf("abc", "12.5", "-10")

        validStrings.forEach { str ->
            val parsed = str.toIntOrNull()
            assertNotNull("$str should be parseable", parsed)
            assertTrue("$str should be non-negative", parsed!! >= 0)
        }

        emptyStrings.forEach { str ->
            val parsed = str.ifEmpty { null }?.toIntOrNull()
            assertNull("'$str' should result in null", parsed)
        }

        invalidStrings.forEach { str ->
            val parsed = str.toIntOrNull()
            // "abc" and "12.5" should be null, "-10" parses to -10
            if (str == "-10") {
                assertEquals(-10, parsed)
            } else {
                assertNull("'$str' should not be parseable", parsed)
            }
        }
    }

    @Test
    fun validation_weightGParsing() {
        val validStrings = listOf("0", "500", "1000", "5000")
        val emptyStrings = listOf("", " ")

        validStrings.forEach { str ->
            val parsed = str.toIntOrNull()
            assertNotNull("$str should be parseable", parsed)
            assertTrue("$str should be non-negative", parsed!! >= 0)
        }

        emptyStrings.forEach { str ->
            val parsed = str.ifEmpty { null }?.toIntOrNull()
            assertNull("'$str' should result in null", parsed)
        }
    }

    // ========================================
    // Edge Cases Tests
    // ========================================

    @Test
    fun edgeCase_extraColumnsIgnored() {
        // If there are more than 10 columns, extras should be ignored
        val line = "Test,Dynamic,2025-12-04,10:30,1,20,,,100,500,extra1,extra2"
        val columns = line.split(",")

        assertTrue(columns.size > 10)

        // Parse only the first 10 columns
        val exerciseName = columns[0]
        val distanceCm = columns.getOrNull(8)?.ifEmpty { null }?.toIntOrNull()
        val weightG = columns.getOrNull(9)?.ifEmpty { null }?.toIntOrNull()

        assertEquals("Test", exerciseName)
        assertEquals(100, distanceCm)
        assertEquals(500, weightG)
    }

    @Test
    fun edgeCase_unicodeCharactersWithDistanceAndWeight() {
        val line = "ランニング,Dynamic,2025-12-04,10:30,1,30,,良い走り,1000,0"
        val columns = line.split(",")

        assertEquals(10, columns.size)
        assertEquals("ランニング", columns[0])
        assertEquals("良い走り", columns[7])
        assertEquals("1000", columns[8])   // distanceCm (10 meters)
        assertEquals("0", columns[9])      // weightG (no weight)
    }

    @Test
    fun edgeCase_onlyHeaderV11() {
        val csv = "exerciseName,exerciseType,date,time,setNumber,valueRight,valueLeft,comment,distanceCm,weightG"
        val lines = csv.lines().filter { it.isNotBlank() && !it.startsWith("#") }
        val dataLines = lines.drop(1)

        assertTrue(dataLines.isEmpty())
    }

    @Test
    fun edgeCase_csvWithCommentsAndBlankLines() {
        val csv = """
            # V11 format with distance and weight tracking
            exerciseName,exerciseType,date,time,setNumber,valueRight,valueLeft,comment,distanceCm,weightG

            Running,Dynamic,2025-12-04,10:30,1,30,,Morning run,1000,
            # This is a weighted exercise
            Weighted Squat,Dynamic,2025-12-04,10:35,1,20,,Heavy,,5000
        """.trimIndent()

        val lines = csv.lines().filter { it.isNotBlank() && !it.startsWith("#") }

        assertEquals(3, lines.size)  // Header + 2 data lines
        assertTrue(lines[0].startsWith("exerciseName"))
        assertTrue(lines[1].startsWith("Running"))
        assertTrue(lines[2].startsWith("Weighted Squat"))
    }

    @Test
    fun edgeCase_largeDistanceAndWeight() {
        // Test with large values (e.g., marathon distance, heavy weight)
        val line = "Marathon,Dynamic,2025-12-04,08:00,1,1,,,4219500,0"  // 42.195km = 4219500cm
        val columns = line.split(",")

        val distanceCm = columns[8].toIntOrNull()
        val weightG = columns[9].toIntOrNull()

        assertEquals(4219500, distanceCm)  // 42.195km in cm
        assertEquals(0, weightG)
    }

    @Test
    fun edgeCase_zeroDistanceAndWeight() {
        // Zero values should be valid (distinct from null/empty)
        val line = "Test Exercise,Dynamic,2025-12-04,10:30,1,20,,,0,0"
        val columns = line.split(",")

        val distanceCm = columns[8].toIntOrNull()
        val weightG = columns[9].toIntOrNull()

        assertEquals(0, distanceCm)
        assertEquals(0, weightG)
    }

    // ========================================
    // Full Parsing Simulation Tests
    // ========================================

    @Test
    fun fullParsing_v11RecordWithAllFields() {
        val line = "Weighted Run,Dynamic,2025-12-04,06:30,1,1,,Morning workout,500,2000"
        val columns = line.split(",")

        val exerciseName = columns[0]
        val exerciseType = columns[1]
        val date = columns[2]
        val time = columns[3]
        val setNumber = columns[4].toIntOrNull() ?: 0
        val valueRight = columns[5].toIntOrNull()
        val valueLeft = columns[6].ifEmpty { null }?.toIntOrNull()
        val comment = columns[7]
        val distanceCm = columns[8].ifEmpty { null }?.toIntOrNull()
        val weightG = columns[9].ifEmpty { null }?.toIntOrNull()

        assertEquals("Weighted Run", exerciseName)
        assertEquals("Dynamic", exerciseType)
        assertEquals("2025-12-04", date)
        assertEquals("06:30", time)
        assertEquals(1, setNumber)
        assertEquals(1, valueRight)
        assertNull(valueLeft)
        assertEquals("Morning workout", comment)
        assertEquals(500, distanceCm)      // 5 meters
        assertEquals(2000, weightG)        // 2kg
    }

    @Test
    fun fullParsing_v10RecordWithDefaults() {
        // V10 format (8 columns) - should apply null for V11 fields
        val line = "Wall Push-up,Dynamic,2025-12-04,10:30,1,30,,Good form"
        val columns = line.split(",")

        val exerciseName = columns[0]
        val distanceCm = columns.getOrNull(8)?.ifEmpty { null }?.toIntOrNull()
        val weightG = columns.getOrNull(9)?.ifEmpty { null }?.toIntOrNull()

        assertEquals("Wall Push-up", exerciseName)
        assertNull(distanceCm)   // Not present in V10 format
        assertNull(weightG)      // Not present in V10 format
    }

    // ========================================
    // Unit Conversion Reference Tests
    // ========================================

    @Test
    fun unitConversion_distanceCmToMeters() {
        // Reference tests for unit conversion (cm to meters)
        val testCases = listOf(
            100 to 1.0,      // 100cm = 1m
            500 to 5.0,      // 500cm = 5m
            1000 to 10.0,    // 1000cm = 10m
            50 to 0.5        // 50cm = 0.5m
        )

        testCases.forEach { (cm, expectedM) ->
            val meters = cm / 100.0
            assertEquals("$cm cm should equal $expectedM m", expectedM, meters, 0.001)
        }
    }

    @Test
    fun unitConversion_weightGToKg() {
        // Reference tests for unit conversion (grams to kg)
        val testCases = listOf(
            1000 to 1.0,     // 1000g = 1kg
            5000 to 5.0,     // 5000g = 5kg
            500 to 0.5,      // 500g = 0.5kg
            2500 to 2.5      // 2500g = 2.5kg
        )

        testCases.forEach { (g, expectedKg) ->
            val kg = g / 1000.0
            assertEquals("$g g should equal $expectedKg kg", expectedKg, kg, 0.001)
        }
    }
}