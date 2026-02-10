package io.github.gonbei774.calisthenicsmemory.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface IntervalRecordDao {

    @Query("SELECT * FROM interval_records ORDER BY date DESC, time DESC")
    fun getAllRecords(): Flow<List<IntervalRecord>>

    @Query("SELECT * FROM interval_records WHERE id = :id")
    suspend fun getRecordById(id: Long): IntervalRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: IntervalRecord): Long

    @Update
    suspend fun update(record: IntervalRecord)

    @Delete
    suspend fun delete(record: IntervalRecord)

    @Query("DELETE FROM interval_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM interval_records")
    suspend fun deleteAll()
}
