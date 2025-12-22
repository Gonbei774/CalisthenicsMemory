package io.github.gonbei774.calisthenicsmemory

import io.github.gonbei774.calisthenicsmemory.viewmodel.BackupData
import io.github.gonbei774.calisthenicsmemory.viewmodel.ExportExercise
import io.github.gonbei774.calisthenicsmemory.viewmodel.ExportGroup
import io.github.gonbei774.calisthenicsmemory.viewmodel.ExportProgram
import io.github.gonbei774.calisthenicsmemory.viewmodel.ExportProgramExercise
import io.github.gonbei774.calisthenicsmemory.viewmodel.ExportRecord
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * BackupData JSON serialization/deserialization tests
 * Issue #8: JSON import deletes program data
 */
class BackupDataTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `v4 backup includes programs and programExercises`() {
        val backupData = BackupData(
            version = 4,
            exportDate = "2025-12-14T12:00:00",
            app = "CalisthenicsMemory",
            groups = listOf(ExportGroup(1, "Upper Body")),
            exercises = listOf(
                ExportExercise(
                    id = 1,
                    name = "Push-up",
                    type = "Dynamic",
                    group = "Upper Body",
                    sortOrder = 0,
                    laterality = "Bilateral"
                )
            ),
            records = emptyList(),
            programs = listOf(
                ExportProgram(
                    id = 1,
                    name = "Morning Routine"
                )
            ),
            programExercises = listOf(
                ExportProgramExercise(
                    id = 1,
                    programId = 1,
                    exerciseId = 1,
                    sortOrder = 0,
                    sets = 3,
                    targetValue = 10,
                    intervalSeconds = 60
                )
            )
        )

        // Serialize
        val jsonString = json.encodeToString(backupData)

        // Verify JSON contains program data
        assertTrue(jsonString.contains("\"programs\""))
        assertTrue(jsonString.contains("\"programExercises\""))
        assertTrue(jsonString.contains("Morning Routine"))

        // Deserialize
        val restored = json.decodeFromString<BackupData>(jsonString)

        // Verify programs restored
        assertEquals(1, restored.programs.size)
        assertEquals("Morning Routine", restored.programs[0].name)

        // Verify programExercises restored
        assertEquals(1, restored.programExercises.size)
        assertEquals(1L, restored.programExercises[0].programId)
        assertEquals(1L, restored.programExercises[0].exerciseId)
        assertEquals(3, restored.programExercises[0].sets)
        assertEquals(10, restored.programExercises[0].targetValue)
        assertEquals(60, restored.programExercises[0].intervalSeconds)
    }

    @Test
    fun `v3 backup without programs deserializes with empty defaults`() {
        // Simulate v3 backup JSON (no programs/programExercises fields)
        val v3Json = """
            {
                "version": 3,
                "exportDate": "2025-12-14T12:00:00",
                "app": "CalisthenicsMemory",
                "groups": [{"id": 1, "name": "Upper Body"}],
                "exercises": [{
                    "id": 1,
                    "name": "Push-up",
                    "type": "Dynamic",
                    "group": "Upper Body",
                    "sortOrder": 0,
                    "laterality": "Bilateral"
                }],
                "records": []
            }
        """.trimIndent()

        // Deserialize v3 backup
        val restored = json.decodeFromString<BackupData>(v3Json)

        // Verify basic data restored
        assertEquals(3, restored.version)
        assertEquals(1, restored.groups.size)
        assertEquals(1, restored.exercises.size)

        // Verify programs defaults to empty (backward compatibility)
        assertTrue(restored.programs.isEmpty())
        assertTrue(restored.programExercises.isEmpty())
    }

    @Test
    fun `program serializes correctly`() {
        val program = ExportProgram(
            id = 42,
            name = "Test Program"
        )

        val jsonString = json.encodeToString(program)
        val restored = json.decodeFromString<ExportProgram>(jsonString)

        assertEquals(42L, restored.id)
        assertEquals("Test Program", restored.name)
    }

    @Test
    fun `old JSON with timerMode and startInterval imports correctly`() {
        // Simulate old v4 backup JSON with timerMode/startInterval fields
        val oldJson = """
            {
                "version": 4,
                "exportDate": "2025-12-14T12:00:00",
                "app": "CalisthenicsMemory",
                "groups": [],
                "exercises": [],
                "records": [],
                "programs": [{
                    "id": 1,
                    "name": "Old Program",
                    "timerMode": true,
                    "startInterval": 5
                }],
                "programExercises": []
            }
        """.trimIndent()

        // Deserialize with ignoreUnknownKeys - should not fail
        val restored = json.decodeFromString<BackupData>(oldJson)

        // Verify program data restored (timerMode/startInterval are ignored)
        assertEquals(1, restored.programs.size)
        assertEquals("Old Program", restored.programs[0].name)
        assertEquals(1L, restored.programs[0].id)
    }

    @Test
    fun `programExercise with all fields serializes correctly`() {
        val pe = ExportProgramExercise(
            id = 100,
            programId = 1,
            exerciseId = 5,
            sortOrder = 2,
            sets = 4,
            targetValue = 15,
            intervalSeconds = 90
        )

        val jsonString = json.encodeToString(pe)
        val restored = json.decodeFromString<ExportProgramExercise>(jsonString)

        assertEquals(100L, restored.id)
        assertEquals(1L, restored.programId)
        assertEquals(5L, restored.exerciseId)
        assertEquals(2, restored.sortOrder)
        assertEquals(4, restored.sets)
        assertEquals(15, restored.targetValue)
        assertEquals(90, restored.intervalSeconds)
    }

    @Test
    fun `multiple programs and exercises serialize correctly`() {
        val backupData = BackupData(
            version = 4,
            exportDate = "2025-12-14T12:00:00",
            app = "CalisthenicsMemory",
            groups = emptyList(),
            exercises = emptyList(),
            records = emptyList(),
            programs = listOf(
                ExportProgram(1, "Program A"),
                ExportProgram(2, "Program B")
            ),
            programExercises = listOf(
                ExportProgramExercise(1, 1, 1, 0, 3, 10, 60),
                ExportProgramExercise(2, 1, 2, 1, 2, 20, 45),
                ExportProgramExercise(3, 2, 1, 0, 5, 8, 90)
            )
        )

        val jsonString = json.encodeToString(backupData)
        val restored = json.decodeFromString<BackupData>(jsonString)

        assertEquals(2, restored.programs.size)
        assertEquals(3, restored.programExercises.size)

        // Verify Program A
        assertEquals("Program A", restored.programs[0].name)

        // Verify Program B
        assertEquals("Program B", restored.programs[1].name)

        // Verify program exercises belong to correct programs
        assertEquals(2, restored.programExercises.count { it.programId == 1L })
        assertEquals(1, restored.programExercises.count { it.programId == 2L })
    }
}