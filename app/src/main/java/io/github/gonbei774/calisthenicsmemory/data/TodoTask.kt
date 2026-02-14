package io.github.gonbei774.calisthenicsmemory.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todo_tasks")
data class TodoTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String = TYPE_EXERCISE,
    val referenceId: Long,
    val sortOrder: Int,
    val repeatDays: String = "",
    val lastCompletedDate: String? = null
) {
    companion object {
        const val TYPE_EXERCISE = "EXERCISE"
        const val TYPE_GROUP = "GROUP"
        const val TYPE_PROGRAM = "PROGRAM"
        const val TYPE_INTERVAL = "INTERVAL"
    }

    fun isRepeating(): Boolean = repeatDays.isNotEmpty()

    fun getRepeatDayNumbers(): List<Int> =
        if (repeatDays.isEmpty()) emptyList()
        else repeatDays.split(",").map { it.trim().toInt() }
}
