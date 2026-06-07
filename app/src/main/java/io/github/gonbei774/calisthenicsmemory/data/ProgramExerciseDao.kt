package io.github.gonbei774.calisthenicsmemory.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramExerciseDao {

    @Query("SELECT * FROM program_exercises WHERE programId = :programId ORDER BY sortOrder ASC")
    fun getExercisesForProgram(programId: Long): Flow<List<ProgramExercise>>

    @Query("SELECT * FROM program_exercises WHERE programId = :programId ORDER BY sortOrder ASC")
    suspend fun getExercisesForProgramSync(programId: Long): List<ProgramExercise>

    @Query("""
        SELECT DISTINCT p.name FROM programs p
        INNER JOIN program_exercises pe ON p.id = pe.programId
        WHERE pe.exerciseId = :exerciseId
        ORDER BY p.name ASC
    """)
    suspend fun getProgramNamesUsingExercise(exerciseId: Long): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(programExercise: ProgramExercise): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ProgramExercise>)

    @Update
    suspend fun update(programExercise: ProgramExercise)

    @Delete
    suspend fun delete(programExercise: ProgramExercise)

    @Query("DELETE FROM program_exercises WHERE programId = :programId")
    suspend fun deleteAllForProgram(programId: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM program_exercises WHERE programId = :programId")
    suspend fun getNextSortOrder(programId: Long): Int

    @Query("UPDATE program_exercises SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    @Transaction
    suspend fun reorderExercises(exerciseIds: List<Long>) {
        exerciseIds.forEachIndexed { index, id ->
            updateSortOrder(id, index)
        }
    }
}