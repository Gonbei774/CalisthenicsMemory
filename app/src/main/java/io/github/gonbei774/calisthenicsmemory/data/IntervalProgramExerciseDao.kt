package io.github.gonbei774.calisthenicsmemory.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface IntervalProgramExerciseDao {

    @Query("SELECT * FROM interval_program_exercises WHERE programId = :programId ORDER BY sortOrder ASC")
    fun getExercisesForProgram(programId: Long): Flow<List<IntervalProgramExercise>>

    @Query("SELECT * FROM interval_program_exercises WHERE programId = :programId ORDER BY sortOrder ASC")
    suspend fun getExercisesForProgramSync(programId: Long): List<IntervalProgramExercise>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: IntervalProgramExercise): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<IntervalProgramExercise>)

    @Update
    suspend fun update(exercise: IntervalProgramExercise)

    @Delete
    suspend fun delete(exercise: IntervalProgramExercise)

    @Query("DELETE FROM interval_program_exercises WHERE programId = :programId")
    suspend fun deleteAllForProgram(programId: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM interval_program_exercises WHERE programId = :programId")
    suspend fun getNextSortOrder(programId: Long): Int

    @Query("UPDATE interval_program_exercises SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    @Transaction
    suspend fun reorderExercises(exerciseIds: List<Long>) {
        exerciseIds.forEachIndexed { index, id ->
            updateSortOrder(id, index)
        }
    }
}
