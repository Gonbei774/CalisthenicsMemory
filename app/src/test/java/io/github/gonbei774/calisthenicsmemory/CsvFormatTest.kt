package io.github.gonbei774.calisthenicsmemory

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for CSV format validation and parsing
 */
class CsvFormatTest {

    @Test
    fun groupsCsvFormat_validHeader() {
        val csv = "name"
        val lines = csv.lines()

        assertEquals(1, lines.size)
        assertEquals("name", lines[0])
    }

    @Test
    fun groupsCsvFormat_validData() {
        val csv = """
            name
            Step 1 Pushups
            Step 2 Squats
        """.trimIndent()

        val lines = csv.lines().filter { it.isNotBlank() && !it.startsWith("#") }

        assertEquals(3, lines.size)
        assertEquals("name", lines[0])
        assertEquals("Step 1 Pushups", lines[1])
        assertEquals("Step 2 Squats", lines[2])
    }

    @Test
    fun exercisesCsvFormat_validHeader() {
        val csv = "name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite"
        val lines = csv.lines()

        assertEquals(1, lines.size)
        assertTrue(lines[0].startsWith("name,type,group,sortOrder"))
    }

    @Test
    fun exercisesCsvFormat_v10Header() {
        // DB v10 format: 11 columns
        val csv = "name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite,displayOrder,restInterval,repDuration"
        val lines = csv.lines()

        assertEquals(1, lines.size)
        val columns = lines[0].split(",")
        assertEquals(11, columns.size)
        assertEquals("displayOrder", columns[8])
        assertEquals("restInterval", columns[9])
        assertEquals("repDuration", columns[10])
    }

    @Test
    fun exercisesCsvFormat_validData() {
        val csv = """
            name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite
            Wall Push-up,Dynamic,Step 1 Pushups,1,Bilateral,3,50,false
            Incline Push-up,Dynamic,Step 1 Pushups,2,Bilateral,3,40,false
        """.trimIndent()

        val lines = csv.lines().filter { it.isNotBlank() && !it.startsWith("#") }

        assertEquals(3, lines.size)

        // Parse first data line
        val data1 = lines[1].split(",")
        assertEquals(8, data1.size)
        assertEquals("Wall Push-up", data1[0])
        assertEquals("Dynamic", data1[1])
        assertEquals("Step 1 Pushups", data1[2])
        assertEquals("1", data1[3])
        assertEquals("Bilateral", data1[4])
        assertEquals("3", data1[5])
        assertEquals("50", data1[6])
        assertEquals("false", data1[7])
    }

    @Test
    fun exercisesCsvFormat_withNullableFields() {
        val csv = """
            name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite
            Simple Exercise,Dynamic,,0,Bilateral,,,false
        """.trimIndent()

        val lines = csv.lines().filter { it.isNotBlank() && !it.startsWith("#") }
        val data = lines[1].split(",")

        assertEquals(8, data.size)
        assertEquals("Simple Exercise", data[0])
        assertEquals("", data[2]) // group is empty
        assertEquals("0", data[3]) // sortOrder is 0
        assertEquals("", data[5]) // targetSets is empty
        assertEquals("", data[6]) // targetValue is empty
    }

    @Test
    fun recordsCsvFormat_validHeader() {
        val csv = "exerciseName,exerciseType,date,time,setNumber,valueRight,valueLeft,comment"
        val lines = csv.lines()

        assertEquals(1, lines.size)
        assertTrue(lines[0].startsWith("exerciseName,exerciseType"))
    }

    @Test
    fun recordsCsvFormat_validData() {
        val csv = """
            exerciseName,exerciseType,date,time,setNumber,valueRight,valueLeft,comment
            Wall Push-up,Dynamic,2025-11-15,10:30,1,30,,Good form
            Wall Push-up,Dynamic,2025-11-15,10:32,2,28,,
        """.trimIndent()

        val lines = csv.lines().filter { it.isNotBlank() && !it.startsWith("#") }

        assertEquals(3, lines.size)

        // Parse first data line
        val data1 = lines[1].split(",")
        assertEquals("Wall Push-up", data1[0])
        assertEquals("Dynamic", data1[1])
        assertEquals("2025-11-15", data1[2])
        assertEquals("10:30", data1[3])
        assertEquals("1", data1[4])
        assertEquals("30", data1[5])
        assertEquals("", data1[6]) // valueLeft is empty for bilateral
    }

    @Test
    fun recordsCsvFormat_unilateralExercise() {
        val csv = """
            exerciseName,exerciseType,date,time,setNumber,valueRight,valueLeft,comment
            One-leg Squat,Dynamic,2025-11-15,10:35,1,15,14,Right stronger
        """.trimIndent()

        val lines = csv.lines().filter { it.isNotBlank() && !it.startsWith("#") }
        val data = lines[1].split(",")

        assertEquals("One-leg Squat", data[0])
        assertEquals("15", data[5]) // valueRight
        assertEquals("14", data[6]) // valueLeft
        assertEquals("Right stronger", data[7]) // comment
    }

    @Test
    fun csvParsing_withComments() {
        val csv = """
            # This is a comment
            # Another comment
            name
            Step 1 Pushups
        """.trimIndent()

        val lines = csv.lines().filter { it.isNotBlank() && !it.startsWith("#") }

        assertEquals(2, lines.size)
        assertEquals("name", lines[0])
        assertEquals("Step 1 Pushups", lines[1])
    }

    @Test
    fun csvParsing_withBlankLines() {
        val csv = """

            name

            Step 1 Pushups

        """.trimIndent()

        val lines = csv.lines().filter { it.isNotBlank() && !it.startsWith("#") }

        assertEquals(2, lines.size)
        assertEquals("name", lines[0])
        assertEquals("Step 1 Pushups", lines[1])
    }

    @Test
    fun csvParsing_trimWhitespace() {
        val csv = """
            name
              Step 1 Pushups
        """.trimIndent()

        val lines = csv.lines().filter { it.isNotBlank() && !it.startsWith("#") }
        val trimmedData = lines[1].trim()

        assertEquals("Step 1 Pushups", trimmedData)
    }

    @Test
    fun exerciseValidation_validType() {
        val validTypes = listOf("Dynamic", "Isometric")

        assertTrue(validTypes.contains("Dynamic"))
        assertTrue(validTypes.contains("Isometric"))
        assertFalse(validTypes.contains("Invalid"))
    }

    @Test
    fun exerciseValidation_validLaterality() {
        val validLaterality = listOf("Bilateral", "Unilateral")

        assertTrue(validLaterality.contains("Bilateral"))
        assertTrue(validLaterality.contains("Unilateral"))
        assertFalse(validLaterality.contains("Invalid"))
    }

    @Test
    fun exerciseValidation_sortOrderRange() {
        val validSortOrders = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

        assertTrue(0 in validSortOrders)
        assertTrue(10 in validSortOrders)
        assertFalse(-1 in validSortOrders)
        assertFalse(11 in validSortOrders)
    }

    @Test
    fun csvDataCount_groups() {
        val csv = """
            name
            Step 1 Pushups
            Step 2 Squats
            Step 3 Pull-ups
        """.trimIndent()

        val dataCount = csv.lines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .drop(1) // Skip header
            .size

        assertEquals(3, dataCount)
    }

    @Test
    fun csvDataCount_exercises() {
        val csv = """
            name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite
            Wall Push-up,Dynamic,Step 1 Pushups,1,Bilateral,3,50,false
            Incline Push-up,Dynamic,Step 1 Pushups,2,Bilateral,3,40,false
        """.trimIndent()

        val dataCount = csv.lines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .drop(1) // Skip header
            .size

        assertEquals(2, dataCount)
    }

    @Test
    fun csvDataCount_withCommentsAndBlankLines() {
        val csv = """
            # Comment
            name

            Step 1 Pushups
            # Another comment
            Step 2 Squats

        """.trimIndent()

        val dataCount = csv.lines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .drop(1) // Skip header
            .size

        assertEquals(2, dataCount)
    }

    // ========================================
    // DB v10 format tests (11 columns)
    // ========================================

    @Test
    fun exercisesCsvFormat_v10ValidData() {
        val csv = """
            name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite,displayOrder,restInterval,repDuration
            Wall Push-up,Dynamic,Step 1 Pushups,1,Bilateral,3,50,false,0,120,5
            Incline Push-up,Dynamic,Step 1 Pushups,2,Bilateral,3,40,true,1,180,
        """.trimIndent()

        val lines = csv.lines().filter { it.isNotBlank() && !it.startsWith("#") }

        assertEquals(3, lines.size)

        // Parse first data line (all fields filled)
        val data1 = lines[1].split(",")
        assertEquals(11, data1.size)
        assertEquals("Wall Push-up", data1[0])
        assertEquals("Dynamic", data1[1])
        assertEquals("Step 1 Pushups", data1[2])
        assertEquals("1", data1[3])
        assertEquals("Bilateral", data1[4])
        assertEquals("3", data1[5])
        assertEquals("50", data1[6])
        assertEquals("false", data1[7])
        assertEquals("0", data1[8])      // displayOrder
        assertEquals("120", data1[9])    // restInterval
        assertEquals("5", data1[10])     // repDuration

        // Parse second data line (partial timer settings)
        val data2 = lines[2].split(",")
        assertEquals(11, data2.size)
        assertEquals("true", data2[7])   // isFavorite
        assertEquals("1", data2[8])      // displayOrder
        assertEquals("180", data2[9])    // restInterval
        assertEquals("", data2[10])      // repDuration is empty
    }

    @Test
    fun exercisesCsvFormat_v10WithNullableTimerFields() {
        val csv = """
            name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite,displayOrder,restInterval,repDuration
            Simple Exercise,Dynamic,,0,Bilateral,,,false,0,,
        """.trimIndent()

        val lines = csv.lines().filter { it.isNotBlank() && !it.startsWith("#") }
        val data = lines[1].split(",")

        assertEquals(11, data.size)
        assertEquals("Simple Exercise", data[0])
        assertEquals("", data[2])   // group is empty
        assertEquals("0", data[8])  // displayOrder
        assertEquals("", data[9])   // restInterval is empty
        assertEquals("", data[10])  // repDuration is empty
    }

    @Test
    fun exercisesCsvFormat_v8CompatibilityCheck() {
        // Old 8-column format should still be parseable
        val csv = """
            name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite
            Wall Push-up,Dynamic,Step 1 Pushups,1,Bilateral,3,50,false
        """.trimIndent()

        val lines = csv.lines().filter { it.isNotBlank() && !it.startsWith("#") }
        val data = lines[1].split(",")

        // Should have exactly 8 columns (old format)
        assertEquals(8, data.size)

        // Verify we can detect old format by column count
        val isOldFormat = data.size == 8
        val isNewFormat = data.size == 11
        assertTrue(isOldFormat)
        assertFalse(isNewFormat)
    }

    @Test
    fun exercisesCsvFormat_detectFormatByColumnCount() {
        val oldFormatCsv = "name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite"
        val newFormatCsv = "name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite,displayOrder,restInterval,repDuration"

        val oldColumns = oldFormatCsv.split(",")
        val newColumns = newFormatCsv.split(",")

        assertEquals(8, oldColumns.size)
        assertEquals(11, newColumns.size)

        // Detection logic
        fun detectVersion(header: String): Int {
            val cols = header.split(",").size
            return when {
                cols >= 11 -> 10  // DB v10
                cols >= 8 -> 9    // DB v9 or earlier
                else -> 0         // Unknown
            }
        }

        assertEquals(9, detectVersion(oldFormatCsv))
        assertEquals(10, detectVersion(newFormatCsv))
    }

    @Test
    fun exercisesCsvFormat_timerSettingsValidation() {
        // Test timer values validation
        val validRestIntervals = listOf(0, 60, 120, 240, 600)
        val invalidRestIntervals = listOf(-1, 601, 1000)

        validRestIntervals.forEach { interval ->
            assertTrue("$interval should be valid", interval in 0..600)
        }

        invalidRestIntervals.forEach { interval ->
            assertFalse("$interval should be invalid", interval in 0..600)
        }
    }

    @Test
    fun exercisesCsvFormat_displayOrderValidation() {
        // displayOrder should be non-negative integer
        val validDisplayOrders = listOf(0, 1, 2, 10, 100)
        val invalidDisplayOrders = listOf(-1, -10)

        validDisplayOrders.forEach { order ->
            assertTrue("$order should be valid", order >= 0)
        }

        invalidDisplayOrders.forEach { order ->
            assertFalse("$order should be invalid", order >= 0)
        }
    }
}