package io.github.gonbei774.calisthenicsmemory.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "program_loops",
    foreignKeys = [
        ForeignKey(
            entity = Program::class,
            parentColumns = ["id"],
            childColumns = ["programId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("programId")]
)
data class ProgramLoop(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val programId: Long,
    val sortOrder: Int,
    val rounds: Int,
    val restBetweenRounds: Int
)