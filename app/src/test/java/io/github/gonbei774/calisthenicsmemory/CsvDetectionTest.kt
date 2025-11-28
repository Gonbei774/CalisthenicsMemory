package io.github.gonbei774.calisthenicsmemory

import io.github.gonbei774.calisthenicsmemory.ui.screens.detectCsvType
import io.github.gonbei774.calisthenicsmemory.viewmodel.CsvType
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for CSV type detection function
 */
class CsvDetectionTest {

    @Test
    fun detectCsvType_groups_returnsGroups() {
        val csv = """
            name
            Step 1 Pushups
            Step 2 Squats
        """.trimIndent()

        val result = detectCsvType(csv)

        assertEquals(CsvType.GROUPS, result)
    }

    @Test
    fun detectCsvType_groupsWithComments_returnsGroups() {
        val csv = """
            # This is a comment
            name
            Step 1 Pushups
        """.trimIndent()

        val result = detectCsvType(csv)

        assertEquals(CsvType.GROUPS, result)
    }

    @Test
    fun detectCsvType_groupsWithBlankLines_returnsGroups() {
        val csv = """

            name

            Step 1 Pushups
        """.trimIndent()

        val result = detectCsvType(csv)

        assertEquals(CsvType.GROUPS, result)
    }

    @Test
    fun detectCsvType_groupsWithWhitespace_returnsGroups() {
        val csv = """
              name
            Step 1 Pushups
        """.trimIndent()

        val result = detectCsvType(csv)

        assertEquals(CsvType.GROUPS, result)
    }

    @Test
    fun detectCsvType_exercises_returnsExercises() {
        val csv = """
            name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite
            Wall Push-up,Dynamic,Step 1 Pushups,1,Bilateral,3,50,false
        """.trimIndent()

        val result = detectCsvType(csv)

        assertEquals(CsvType.EXERCISES, result)
    }

    @Test
    fun detectCsvType_exercisesWithComments_returnsExercises() {
        val csv = """
            # Exercise data export
            name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite
            Wall Push-up,Dynamic,Step 1 Pushups,1,Bilateral,3,50,false
        """.trimIndent()

        val result = detectCsvType(csv)

        assertEquals(CsvType.EXERCISES, result)
    }

    @Test
    fun detectCsvType_records_returnsRecords() {
        val csv = """
            exerciseName,exerciseType,setNumber,valueRight,valueLeft,date,time,comment
            Wall Push-up,Dynamic,1,30,,2025-11-15,10:30,
        """.trimIndent()

        val result = detectCsvType(csv)

        assertEquals(CsvType.RECORDS, result)
    }

    @Test
    fun detectCsvType_recordsWithComments_returnsRecords() {
        val csv = """
            # Training records export
            exerciseName,exerciseType,setNumber,valueRight,valueLeft,date,time,comment
            Wall Push-up,Dynamic,1,30,,2025-11-15,10:30,Good form
        """.trimIndent()

        val result = detectCsvType(csv)

        assertEquals(CsvType.RECORDS, result)
    }

    @Test
    fun detectCsvType_emptyString_returnsNull() {
        val csv = ""

        val result = detectCsvType(csv)

        assertNull(result)
    }

    @Test
    fun detectCsvType_onlyBlankLines_returnsNull() {
        val csv = """



        """.trimIndent()

        val result = detectCsvType(csv)

        assertNull(result)
    }

    @Test
    fun detectCsvType_onlyComments_returnsNull() {
        val csv = """
            # Comment line 1
            # Comment line 2
        """.trimIndent()

        val result = detectCsvType(csv)

        assertNull(result)
    }

    @Test
    fun detectCsvType_invalidHeader_returnsNull() {
        val csv = """
            invalid,header,format
            some,data,here
        """.trimIndent()

        val result = detectCsvType(csv)

        assertNull(result)
    }

    @Test
    fun detectCsvType_partialMatch_returnsNull() {
        val csv = """
            name,type
            Some Data,Dynamic
        """.trimIndent()

        val result = detectCsvType(csv)

        // Should return null because it doesn't match the full "name,type,group,sortOrder..." pattern
        assertNull(result)
    }
}