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

    /**
     * 指定した種目の最新セッションの記録を取得
     * @return 最新セッション（日付+時刻）の全レコード、setNumber順
     */
    @Query("""
        SELECT * FROM training_records
        WHERE exerciseId = :exerciseId
        AND date || ' ' || time = (
            SELECT date || ' ' || time
            FROM training_records
            WHERE exerciseId = :exerciseId
            ORDER BY date DESC, time DESC
            LIMIT 1
        )
        ORDER BY setNumber ASC
    """)
    suspend fun getLatestSessionByExercise(exerciseId: Long): List<TrainingRecord>

    @Query("SELECT EXISTS(SELECT 1 FROM training_records WHERE exerciseId = :exerciseId AND date = :date LIMIT 1)")
    suspend fun hasRecordOnDate(exerciseId: Long, date: String): Boolean

    @Query("DELETE FROM training_records")
    suspend fun deleteAll()
}