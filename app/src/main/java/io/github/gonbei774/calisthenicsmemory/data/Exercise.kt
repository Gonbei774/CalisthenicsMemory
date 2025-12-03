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
    val displayOrder: Int = 0,           // 表示順（0, 1, 2...、並び替え機能用）
    val laterality: String = "Bilateral", // "Bilateral" or "Unilateral"（デフォルト: Bilateral）
    val targetSets: Int? = null,         // 目標セット数（任意）
    val targetValue: Int? = null,        // 目標値（回数or秒数、任意）
    val isFavorite: Boolean = false,     // お気に入り登録（デフォルト: false）
    val restInterval: Int? = null,       // 種目固有の休憩時間（秒、任意）
    val repDuration: Int? = null,        // 種目固有の1レップ時間（秒、任意）
    val distanceTrackingEnabled: Boolean = false,  // 距離入力を有効化
    val weightTrackingEnabled: Boolean = false     // 荷重入力を有効化
)