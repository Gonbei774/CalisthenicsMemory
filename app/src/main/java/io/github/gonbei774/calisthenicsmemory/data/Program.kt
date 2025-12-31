package io.github.gonbei774.calisthenicsmemory.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "programs")
data class Program(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
)