package io.github.gonbei774.calisthenicsmemory.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "interval_records")
data class IntervalRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val programName: String,
    val date: String,
    val time: String,
    val workSeconds: Int,
    val restSeconds: Int,
    val rounds: Int,
    val roundRestSeconds: Int,
    val completedRounds: Int,
    val completedExercisesInLastRound: Int,
    val exercisesJson: String,
    val comment: String? = null
)
