package io.github.gonbei774.calisthenicsmemory.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercises",
    indices = [Index(value = ["name", "type"], unique = true)]
)
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String,
    val group: String? = null,           // グループ名（任意）
    val sortOrder: Int = 0,              // レベル（0〜10、0はグループなし時のデフォルト）
    val laterality: String = "Bilateral", // "Bilateral" or "Unilateral"（デフォルト: Bilateral）
    val targetSets: Int? = null,         // 目標セット数（任意）
    val targetValue: Int? = null,        // 目標値（回数or秒数、任意）
    val isFavorite: Boolean = false      // お気に入り登録（デフォルト: false）
)