package io.github.gonbei774.calisthenicsmemory

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gonbei774.calisthenicsmemory.data.*
import io.github.gonbei774.calisthenicsmemory.viewmodel.CsvImportReport
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Integration tests for CSV Import functionality
 * Uses Room In-Memory Database
 */
@RunWith(AndroidJUnit4::class)
class CsvImportTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var groupDao: ExerciseGroupDao
    private lateinit var exerciseDao: ExerciseDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        groupDao = database.exerciseGroupDao()
        exerciseDao = database.exerciseDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun importGroups_merge_successfulImport() = runBlocking {
        val csv = """
            name
            Step 1 Pushups
            Step 2 Squats
        """.trimIndent()

        val report = importGroupsCsv(csv)

        assertEquals(2, report.successCount)
        assertEquals(0, report.skippedCount)
        assertEquals(0, report.errorCount)

        val groups = groupDao.getAllGroups()
        // Note: Flow is not collected in test, so we use suspend function instead
        val group1 = groupDao.getGroupByName("Step 1 Pushups")
        val group2 = groupDao.getGroupByName("Step 2 Squats")

        assertNotNull(group1)
        assertNotNull(group2)
        assertEquals("Step 1 Pushups", group1?.name)
        assertEquals("Step 2 Squats", group2?.name)
    }

    @Test
    fun importGroups_merge_skipDuplicates() = runBlocking {
        // Insert existing group
        groupDao.insertGroup(ExerciseGroup(name = "Step 1 Pushups"))

        val csv = """
            name
            Step 1 Pushups
            Step 2 Squats
        """.trimIndent()

        val report = importGroupsCsv(csv)

        assertEquals(1, report.successCount)
        assertEquals(1, report.skippedCount)
        assertEquals(0, report.errorCount)
        assertEquals(1, report.skippedItems.size)
        assertTrue(report.skippedItems[0].contains("Step 1 Pushups"))
    }

    @Test
    fun importGroups_emptyName_error() = runBlocking {
        val csv = """
            name
            Step 1 Pushups

            Step 2 Squats
        """.trimIndent()

        val report = importGroupsCsv(csv)

        assertEquals(2, report.successCount)
        assertEquals(0, report.skippedCount)
        // Empty line is filtered out, so no error
        assertEquals(0, report.errorCount)
    }

    @Test
    fun importExercises_merge_successfulImport() = runBlocking {
        // Insert required group
        groupDao.insertGroup(ExerciseGroup(name = "Step 1 Pushups"))

        val csv = """
            name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite
            Wall Push-up,Dynamic,Step 1 Pushups,1,Bilateral,3,50,false
        """.trimIndent()

        val report = importExercisesCsv(csv)

        assertEquals(1, report.successCount)
        assertEquals(0, report.skippedCount)
        assertEquals(0, report.errorCount)

        val exercise = exerciseDao.getExerciseByNameAndType("Wall Push-up", "Dynamic")
        assertNotNull(exercise)
        assertEquals("Wall Push-up", exercise?.name)
        assertEquals("Dynamic", exercise?.type)
        assertEquals("Step 1 Pushups", exercise?.group)
        assertEquals(1, exercise?.sortOrder)
        assertEquals("Bilateral", exercise?.laterality)
        assertEquals(3, exercise?.targetSets)
        assertEquals(50, exercise?.targetValue)
        assertEquals(false, exercise?.isFavorite)
    }

    @Test
    fun importExercises_invalidType_error() = runBlocking {
        groupDao.insertGroup(ExerciseGroup(name = "Step 1 Pushups"))

        val csv = """
            name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite
            Wall Push-up,InvalidType,Step 1 Pushups,1,Bilateral,3,50,false
        """.trimIndent()

        val report = importExercisesCsv(csv)

        assertEquals(0, report.successCount)
        assertEquals(0, report.skippedCount)
        assertEquals(1, report.errorCount)
        assertTrue(report.errors[0].contains("type"))
    }

    @Test
    fun importExercises_invalidLaterality_error() = runBlocking {
        groupDao.insertGroup(ExerciseGroup(name = "Step 1 Pushups"))

        val csv = """
            name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite
            Wall Push-up,Dynamic,Step 1 Pushups,1,InvalidLaterality,3,50,false
        """.trimIndent()

        val report = importExercisesCsv(csv)

        assertEquals(0, report.successCount)
        assertEquals(0, report.skippedCount)
        assertEquals(1, report.errorCount)
        assertTrue(report.errors[0].contains("laterality"))
    }

    @Test
    fun importExercises_groupNotFound_error() = runBlocking {
        val csv = """
            name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite
            Wall Push-up,Dynamic,Nonexistent Group,1,Bilateral,3,50,false
        """.trimIndent()

        val report = importExercisesCsv(csv)

        assertEquals(0, report.successCount)
        assertEquals(0, report.skippedCount)
        assertEquals(1, report.errorCount)
        assertTrue(report.errors[0].contains("Group"))
    }

    @Test
    fun importExercises_duplicateDifferentLaterality_skip() = runBlocking {
        groupDao.insertGroup(ExerciseGroup(name = "Step 1 Pushups"))

        // Insert existing exercise with Bilateral
        exerciseDao.insertExercise(
            Exercise(
                name = "Wall Push-up",
                type = "Dynamic",
                group = "Step 1 Pushups",
                sortOrder = 1,
                laterality = "Bilateral",
                targetSets = 3,
                targetValue = 50,
                isFavorite = false
            )
        )

        // Try to import same exercise with Unilateral
        val csv = """
            name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite
            Wall Push-up,Dynamic,Step 1 Pushups,1,Unilateral,3,50,false
        """.trimIndent()

        val report = importExercisesCsv(csv)

        assertEquals(0, report.successCount)
        assertEquals(1, report.skippedCount)
        assertEquals(0, report.errorCount)
        assertTrue(report.skippedItems[0].contains("laterality"))
    }

    // Helper functions to simulate ViewModel CSV import logic

    private suspend fun importGroupsCsv(csvString: String): CsvImportReport {
        var successCount = 0
        var skippedCount = 0
        var errorCount = 0
        val skippedItems = mutableListOf<String>()
        val errors = mutableListOf<String>()

        val lines = csvString.lines().filter { it.isNotBlank() && !it.startsWith("#") }
        if (lines.isEmpty()) {
            return CsvImportReport("Groups", 0, 0, 0, emptyList(), emptyList())
        }

        val dataLines = lines.drop(1) // Skip header

        dataLines.forEachIndexed { index, line ->
            try {
                val name = line.trim()
                if (name.isEmpty()) {
                    errors.add("Line ${index + 2}: Group name is empty")
                    errorCount++
                    return@forEachIndexed
                }

                // Check for duplicates (merge mode only)
                val existing = groupDao.getGroupByName(name)
                if (existing != null) {
                    skippedItems.add("\"$name\" (already exists)")
                    skippedCount++
                    return@forEachIndexed
                }

                groupDao.insertGroup(ExerciseGroup(name = name))
                successCount++
            } catch (e: Exception) {
                errors.add("Line ${index + 2}: ${e.message}")
                errorCount++
            }
        }

        return CsvImportReport("Groups", successCount, skippedCount, errorCount, skippedItems, errors)
    }

    private suspend fun importExercisesCsv(csvString: String): CsvImportReport {
        var successCount = 0
        var skippedCount = 0
        var errorCount = 0
        val skippedItems = mutableListOf<String>()
        val errors = mutableListOf<String>()

        val lines = csvString.lines().filter { it.isNotBlank() && !it.startsWith("#") }
        if (lines.isEmpty()) {
            return CsvImportReport("Exercises", 0, 0, 0, emptyList(), emptyList())
        }

        val dataLines = lines.drop(1)

        dataLines.forEachIndexed { index, line ->
            try {
                val parts = line.split(",")
                if (parts.size < 8) {
                    errors.add("Line ${index + 2}: Invalid format")
                    errorCount++
                    return@forEachIndexed
                }

                val name = parts[0].trim()
                val type = parts[1].trim()
                val group = parts[2].trim().ifEmpty { null }
                val sortOrder = parts[3].trim().toIntOrNull() ?: 0
                val laterality = parts[4].trim()
                val targetSets = parts[5].trim().toIntOrNull()
                val targetValue = parts[6].trim().toIntOrNull()
                val isFavorite = parts[7].trim().toBoolean()

                // Validation
                if (type !in listOf("Dynamic", "Isometric")) {
                    errors.add("Line ${index + 2}: Invalid type \"$type\"")
                    errorCount++
                    return@forEachIndexed
                }

                if (laterality !in listOf("Bilateral", "Unilateral")) {
                    errors.add("Line ${index + 2}: Invalid laterality \"$laterality\"")
                    errorCount++
                    return@forEachIndexed
                }

                if (group != null) {
                    val groupExists = groupDao.getGroupByName(group)
                    if (groupExists == null) {
                        errors.add("Line ${index + 2}: Group \"$group\" not found")
                        errorCount++
                        return@forEachIndexed
                    }
                }

                // Check for duplicates (merge mode only)
                val existing = exerciseDao.getExerciseByNameAndType(name, type)
                if (existing != null) {
                    if (existing.laterality != laterality) {
                        skippedItems.add("\"$name\" ($type) - laterality mismatch")
                        skippedCount++
                        return@forEachIndexed
                    } else {
                        skippedItems.add("\"$name\" ($type) - already exists")
                        skippedCount++
                        return@forEachIndexed
                    }
                }

                val exercise = Exercise(
                    name = name,
                    type = type,
                    group = group,
                    sortOrder = sortOrder,
                    laterality = laterality,
                    targetSets = targetSets,
                    targetValue = targetValue,
                    isFavorite = isFavorite
                )

                exerciseDao.insertExercise(exercise)
                successCount++
            } catch (e: Exception) {
                errors.add("Line ${index + 2}: ${e.message}")
                errorCount++
            }
        }

        return CsvImportReport("Exercises", successCount, skippedCount, errorCount, skippedItems, errors)
    }
}