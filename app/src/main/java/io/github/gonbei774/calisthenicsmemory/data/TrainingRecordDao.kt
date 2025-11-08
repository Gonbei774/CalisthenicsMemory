package io.github.gonbei774.calisthenicsmemory.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingRecordDao {

    @Query("SELECT * FROM training_records ORDER BY date DESC, time DESC, setNumber ASC")
    fun getAllRecords(): Flow<List<TrainingRecord>>

    @Query("SELECT * FROM training_records WHERE exerciseId = :exerciseId ORDER BY date DESC, time DESC")
    fun getRecordsByExercise(exerciseId: Long): Flow<List<TrainingRecord>>

    @Query("SELECT * FROM training_records WHERE id = :id")
    suspend fun getRecordById(id: Long): TrainingRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: TrainingRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<TrainingRecord>)

    @Update
    suspend fun updateRecord(record: TrainingRecord)

    @Delete
    suspend fun deleteRecord(record: TrainingRecord)

    @Query("DELETE FROM training_records WHERE exerciseId = :exerciseId AND date = :date AND time = :time")
    suspend fun deleteSession(exerciseId: Long, date: String, time: String)

    @Query("SELECT * FROM training_records WHERE exerciseId = :exerciseId AND date = :date AND time = :time")
    suspend fun getSessionRecords(exerciseId: Long, date: String, time: String): List<TrainingRecord>

    @Query("DELETE FROM training_records")
    suspend fun deleteAll()
}