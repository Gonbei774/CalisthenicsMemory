package io.github.gonbei774.calisthenicsmemory.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * ワークアウトの途中状態を保存・読み込み・削除するクラス
 *
 * Save & Exit機能で使用。1つのプログラムの途中状態のみ保持。
 */
class SavedWorkoutState(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 途中状態を保存
     */
    fun save(
        programId: Long,
        currentSetIndex: Int,
        sets: List<ProgramWorkoutSet>,
        comment: String
    ) {
        val setsJson = json.encodeToString(sets)
        prefs.edit()
            .putLong(KEY_PROGRAM_ID, programId)
            .putInt(KEY_CURRENT_SET_INDEX, currentSetIndex)
            .putString(KEY_SETS, setsJson)
            .putString(KEY_COMMENT, comment)
            .putLong(KEY_SAVED_AT, System.currentTimeMillis())
            .apply()
    }

    /**
     * 保存された途中状態があるかどうか
     */
    fun hasSavedState(): Boolean {
        return prefs.contains(KEY_PROGRAM_ID)
    }

    /**
     * 保存されたプログラムIDを取得
     */
    fun getSavedProgramId(): Long? {
        return if (hasSavedState()) {
            prefs.getLong(KEY_PROGRAM_ID, -1L).takeIf { it >= 0 }
        } else {
            null
        }
    }

    /**
     * 保存された現在セットインデックスを取得
     */
    fun getCurrentSetIndex(): Int {
        return prefs.getInt(KEY_CURRENT_SET_INDEX, 0)
    }

    /**
     * 保存されたセットリストを取得
     */
    fun getSets(): List<ProgramWorkoutSet>? {
        val setsJson = prefs.getString(KEY_SETS, null) ?: return null
        return try {
            json.decodeFromString<List<ProgramWorkoutSet>>(setsJson)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 保存されたコメントを取得
     */
    fun getComment(): String {
        return prefs.getString(KEY_COMMENT, "") ?: ""
    }

    /**
     * 保存日時を取得
     */
    fun getSavedAt(): Long {
        return prefs.getLong(KEY_SAVED_AT, 0L)
    }

    /**
     * 途中状態をクリア
     */
    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "saved_workout_state"
        private const val KEY_PROGRAM_ID = "program_id"
        private const val KEY_CURRENT_SET_INDEX = "current_set_index"
        private const val KEY_SETS = "sets"
        private const val KEY_COMMENT = "comment"
        private const val KEY_SAVED_AT = "saved_at"
    }
}