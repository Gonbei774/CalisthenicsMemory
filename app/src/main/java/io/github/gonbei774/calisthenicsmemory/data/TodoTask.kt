package io.github.gonbei774.calisthenicsmemory.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todo_tasks")
data class TodoTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val exerciseId: Long,
    val sortOrder: Int
)