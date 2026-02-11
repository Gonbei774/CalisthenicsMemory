package io.github.gonbei774.calisthenicsmemory.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoTaskDao {

    @Query("SELECT * FROM todo_tasks ORDER BY sortOrder ASC")
    fun getAllTasks(): Flow<List<TodoTask>>

    @Insert
    suspend fun insert(task: TodoTask): Long

    @Delete
    suspend fun delete(task: TodoTask)

    @Query("DELETE FROM todo_tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM todo_tasks WHERE type = :type AND referenceId = :referenceId")
    suspend fun deleteByReference(type: String, referenceId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM todo_tasks WHERE type = :type AND referenceId = :referenceId)")
    suspend fun existsTask(type: String, referenceId: Long): Boolean

    @Query("SELECT COALESCE(MAX(sortOrder), 0) + 1 FROM todo_tasks")
    suspend fun getNextSortOrder(): Int

    @Query("UPDATE todo_tasks SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    @Transaction
    suspend fun reorderTasks(taskIds: List<Long>) {
        taskIds.forEachIndexed { index, id ->
            updateSortOrder(id, index)
        }
    }

    @Query("UPDATE todo_tasks SET repeatDays = :repeatDays WHERE id = :id")
    suspend fun updateRepeatDays(id: Long, repeatDays: String)

    @Query("UPDATE todo_tasks SET lastCompletedDate = :date WHERE type = :type AND referenceId = :referenceId")
    suspend fun updateLastCompletedDate(type: String, referenceId: Long, date: String)

    @Query("SELECT * FROM todo_tasks WHERE type = :type AND referenceId = :referenceId LIMIT 1")
    suspend fun getTaskByReference(type: String, referenceId: Long): TodoTask?
}
