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
}