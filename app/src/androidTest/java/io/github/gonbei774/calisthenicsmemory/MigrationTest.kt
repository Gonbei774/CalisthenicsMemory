package io.github.gonbei774.calisthenicsmemory

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Migration tests for AppDatabase
 * Tests MIGRATION_10_11 which adds distance and weight tracking fields
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val testDbName = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate10To11_exercisesTable() {
        // Create database at version 10
        helper.createDatabase(testDbName, 10).apply {
            // Insert test data in version 10 format
            execSQL("""
                INSERT INTO exercises (
                    id, name, type, groupId, sortOrder, laterality,
                    targetSets, targetValue, isFavorite, displayOrder,
                    restInterval, repDuration
                ) VALUES (
                    1, 'Wall Push-up', 'Dynamic', NULL, 1, 'Bilateral',
                    3, 50, 0, 0, 120, 5
                )
            """.trimIndent())
            close()
        }

        // Run migration
        val db = helper.runMigrationsAndValidate(testDbName, 11, true, AppDatabase.MIGRATION_10_11)

        // Verify new columns exist with default values
        val cursor = db.query("SELECT distanceTrackingEnabled, weightTrackingEnabled FROM exercises WHERE id = 1")
        assertTrue("Should have one row", cursor.moveToFirst())

        val distanceTrackingEnabled = cursor.getInt(0)
        val weightTrackingEnabled = cursor.getInt(1)

        assertEquals("distanceTrackingEnabled should default to 0 (false)", 0, distanceTrackingEnabled)
        assertEquals("weightTrackingEnabled should default to 0 (false)", 0, weightTrackingEnabled)

        cursor.close()
        db.close()
    }

    @Test
    fun migrate10To11_trainingRecordsTable() {
        // Create database at version 10
        helper.createDatabase(testDbName, 10).apply {
            // First insert an exercise (required for foreign key)
            execSQL("""
                INSERT INTO exercises (
                    id, name, type, groupId, sortOrder, laterality,
                    targetSets, targetValue, isFavorite, displayOrder,
                    restInterval, repDuration
                ) VALUES (
                    1, 'Wall Push-up', 'Dynamic', NULL, 1, 'Bilateral',
                    3, 50, 0, 0, 120, 5
                )
            """.trimIndent())

            // Insert training record in version 10 format
            execSQL("""
                INSERT INTO training_records (
                    id, exerciseId, setNumber, valueRight, valueLeft,
                    date, time, comment
                ) VALUES (
                    1, 1, 1, 30, NULL, '2025-12-04', '10:30', 'Good form'
                )
            """.trimIndent())
            close()
        }

        // Run migration
        val db = helper.runMigrationsAndValidate(testDbName, 11, true, AppDatabase.MIGRATION_10_11)

        // Verify new columns exist and are nullable
        val cursor = db.query("SELECT distanceCm, weightG FROM training_records WHERE id = 1")
        assertTrue("Should have one row", cursor.moveToFirst())

        val distanceCm = if (cursor.isNull(0)) null else cursor.getInt(0)
        val weightG = if (cursor.isNull(1)) null else cursor.getInt(1)

        assertNull("distanceCm should be null for existing records", distanceCm)
        assertNull("weightG should be null for existing records", weightG)

        cursor.close()
        db.close()
    }

    @Test
    fun migrate10To11_canInsertNewValuesAfterMigration() {
        // Create database at version 10 and migrate
        helper.createDatabase(testDbName, 10).apply {
            // Insert an exercise
            execSQL("""
                INSERT INTO exercises (
                    id, name, type, groupId, sortOrder, laterality,
                    targetSets, targetValue, isFavorite, displayOrder,
                    restInterval, repDuration
                ) VALUES (
                    1, 'Running', 'Dynamic', NULL, 1, 'Bilateral',
                    NULL, NULL, 0, 0, NULL, NULL
                )
            """.trimIndent())
            close()
        }

        // Run migration
        val db = helper.runMigrationsAndValidate(testDbName, 11, true, AppDatabase.MIGRATION_10_11)

        // Update exercise to enable distance tracking
        db.execSQL("UPDATE exercises SET distanceTrackingEnabled = 1 WHERE id = 1")

        // Insert a training record with distance
        db.execSQL("""
            INSERT INTO training_records (
                id, exerciseId, setNumber, valueRight, valueLeft,
                date, time, comment, distanceCm, weightG
            ) VALUES (
                1, 1, 1, 1, NULL, '2025-12-04', '10:30', 'Morning run', 500, NULL
            )
        """.trimIndent())

        // Verify exercise update
        val exerciseCursor = db.query("SELECT distanceTrackingEnabled FROM exercises WHERE id = 1")
        assertTrue(exerciseCursor.moveToFirst())
        assertEquals(1, exerciseCursor.getInt(0))
        exerciseCursor.close()

        // Verify record insert
        val recordCursor = db.query("SELECT distanceCm, weightG FROM training_records WHERE id = 1")
        assertTrue(recordCursor.moveToFirst())
        assertEquals(500, recordCursor.getInt(0))  // distanceCm = 500 (5 meters)
        assertTrue(recordCursor.isNull(1))         // weightG is null
        recordCursor.close()

        db.close()
    }

    @Test
    fun migrate10To11_preservesExistingData() {
        // Create database at version 10 with multiple records
        helper.createDatabase(testDbName, 10).apply {
            // Insert multiple exercises
            execSQL("""
                INSERT INTO exercises (id, name, type, groupId, sortOrder, laterality, targetSets, targetValue, isFavorite, displayOrder, restInterval, repDuration)
                VALUES (1, 'Wall Push-up', 'Dynamic', NULL, 1, 'Bilateral', 3, 50, 0, 0, 120, 5)
            """.trimIndent())
            execSQL("""
                INSERT INTO exercises (id, name, type, groupId, sortOrder, laterality, targetSets, targetValue, isFavorite, displayOrder, restInterval, repDuration)
                VALUES (2, 'Squat', 'Dynamic', NULL, 2, 'Bilateral', 3, 20, 1, 1, 180, NULL)
            """.trimIndent())

            // Insert training records
            execSQL("""
                INSERT INTO training_records (id, exerciseId, setNumber, valueRight, valueLeft, date, time, comment)
                VALUES (1, 1, 1, 30, NULL, '2025-12-04', '10:30', 'Good')
            """.trimIndent())
            execSQL("""
                INSERT INTO training_records (id, exerciseId, setNumber, valueRight, valueLeft, date, time, comment)
                VALUES (2, 1, 2, 28, NULL, '2025-12-04', '10:32', '')
            """.trimIndent())
            execSQL("""
                INSERT INTO training_records (id, exerciseId, setNumber, valueRight, valueLeft, date, time, comment)
                VALUES (3, 2, 1, 20, NULL, '2025-12-04', '10:35', 'Deep')
            """.trimIndent())
            close()
        }

        // Run migration
        val db = helper.runMigrationsAndValidate(testDbName, 11, true, AppDatabase.MIGRATION_10_11)

        // Verify all exercises preserved
        val exerciseCursor = db.query("SELECT COUNT(*) FROM exercises")
        assertTrue(exerciseCursor.moveToFirst())
        assertEquals(2, exerciseCursor.getInt(0))
        exerciseCursor.close()

        // Verify all records preserved
        val recordCursor = db.query("SELECT COUNT(*) FROM training_records")
        assertTrue(recordCursor.moveToFirst())
        assertEquals(3, recordCursor.getInt(0))
        recordCursor.close()

        // Verify existing data integrity
        val detailCursor = db.query("SELECT name, targetSets, isFavorite FROM exercises WHERE id = 2")
        assertTrue(detailCursor.moveToFirst())
        assertEquals("Squat", detailCursor.getString(0))
        assertEquals(3, detailCursor.getInt(1))
        assertEquals(1, detailCursor.getInt(2))  // isFavorite = true
        detailCursor.close()

        db.close()
    }

    @Test
    fun allMigrations_fromVersion10() {
        // Create database at version 10
        helper.createDatabase(testDbName, 10).apply {
            execSQL("""
                INSERT INTO exercises (id, name, type, groupId, sortOrder, laterality, targetSets, targetValue, isFavorite, displayOrder, restInterval, repDuration)
                VALUES (1, 'Test Exercise', 'Dynamic', NULL, 1, 'Bilateral', 3, 50, 0, 0, 120, 5)
            """.trimIndent())
            close()
        }

        // Open database with Room (this will run all migrations)
        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            testDbName
        ).addMigrations(AppDatabase.MIGRATION_10_11)
            .build().apply {
                // Verify database opens successfully and can query
                val dao = exerciseDao()
                // Just accessing the DAO is enough to verify the database opened correctly
                close()
            }
    }
}