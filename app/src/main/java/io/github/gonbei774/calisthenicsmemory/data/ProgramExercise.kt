package io.github.gonbei774.calisthenicsmemory.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "program_exercises",
    foreignKeys = [
        ForeignKey(
            entity = Program::class,
            parentColumns = ["id"],
            childColumns = ["programId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("programId"),
        Index("exerciseId")
    ]
)
data class ProgramExercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val programId: Long,
    val exerciseId: Long,
    val sortOrder: Int,
    val sets: Int = 1,
    val targetValue: Int,
    val intervalSeconds: Int = 60
)