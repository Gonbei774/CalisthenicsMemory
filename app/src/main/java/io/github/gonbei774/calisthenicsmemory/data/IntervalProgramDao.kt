package io.github.gonbei774.calisthenicsmemory.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface IntervalProgramDao {

    @Query("SELECT * FROM interval_programs ORDER BY id DESC")
    fun getAllPrograms(): Flow<List<IntervalProgram>>

    @Query("SELECT * FROM interval_programs WHERE id = :id")
    suspend fun getProgramById(id: Long): IntervalProgram?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(program: IntervalProgram): Long

    @Update
    suspend fun update(program: IntervalProgram)

    @Delete
    suspend fun delete(program: IntervalProgram)

    @Query("DELETE FROM interval_programs WHERE id = :id")
    suspend fun deleteById(id: Long)
}
