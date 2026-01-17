package io.github.gonbei774.calisthenicsmemory.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramLoopDao {

    @Query("SELECT * FROM program_loops WHERE programId = :programId ORDER BY sortOrder ASC")
    fun getLoopsForProgram(programId: Long): Flow<List<ProgramLoop>>

    @Query("SELECT * FROM program_loops WHERE programId = :programId ORDER BY sortOrder ASC")
    suspend fun getLoopsForProgramSync(programId: Long): List<ProgramLoop>

    @Query("SELECT * FROM program_loops WHERE id = :loopId")
    suspend fun getLoopById(loopId: Long): ProgramLoop?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(loop: ProgramLoop): Long

    @Update
    suspend fun update(loop: ProgramLoop)

    @Delete
    suspend fun delete(loop: ProgramLoop)

    @Query("DELETE FROM program_loops WHERE programId = :programId")
    suspend fun deleteAllForProgram(programId: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM program_loops WHERE programId = :programId")
    suspend fun getNextSortOrder(programId: Long): Int

    @Query("UPDATE program_loops SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)
}