package io.github.gonbei774.calisthenicsmemory.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseGroupDao {

    @Query("SELECT * FROM exercise_groups ORDER BY displayOrder ASC")
    fun getAllGroups(): Flow<List<ExerciseGroup>>

    @Query("SELECT * FROM exercise_groups ORDER BY displayOrder ASC")
    suspend fun getAllGroupsSync(): List<ExerciseGroup>

    @Query("SELECT * FROM exercise_groups WHERE name = :name")
    suspend fun getGroupByName(name: String): ExerciseGroup?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGroup(group: ExerciseGroup): Long

    @Update
    suspend fun updateGroup(group: ExerciseGroup)

    @Delete
    suspend fun deleteGroup(group: ExerciseGroup)

    @Query("DELETE FROM exercise_groups WHERE name = :name")
    suspend fun deleteGroupByName(name: String)

    @Query("DELETE FROM exercise_groups")
    suspend fun deleteAll()
}