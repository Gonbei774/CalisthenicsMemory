package io.github.gonbei774.calisthenicsmemory.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "training_records",
    foreignKeys = [
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("exerciseId")]
)
data class TrainingRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val exerciseId: Long,          // 種目ID
    val valueRight: Int,           // ← リネーム: value → valueRight（右側 or 両側の値）
    val valueLeft: Int? = null,    // ← 追加: 左側の値（Unilateral種目用、nullはBilateral）
    val setNumber: Int,            // セット番号
    val date: String,              // 日付 (YYYY-MM-DD)
    val time: String,              // 時刻 (HH:mm)
    val comment: String = ""       // コメント
)