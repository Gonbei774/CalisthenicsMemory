package io.github.gonbei774.calisthenicsmemory.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercise_groups",
    indices = [Index(value = ["name"], unique = true)]
)
data class ExerciseGroup(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String  // グループ名（ユニーク）
)