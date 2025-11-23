package io.github.gonbei774.calisthenicsmemory.data

import android.content.Context
import android.content.SharedPreferences

/**
 * 記録入力設定の保存・読み込みを管理するクラス
 */
class RecordPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * 目標値を自動入力する機能の有効/無効を取得
     * @return 有効: true, 無効: false（デフォルト: false）
     */
    fun isAutoFillTargetEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_FILL_TARGET_ENABLED, false)
    }

    /**
     * 目標値を自動入力する機能の有効/無効を保存
     * @param enabled 有効: true, 無効: false
     */
    fun setAutoFillTargetEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_FILL_TARGET_ENABLED, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "record_preferences"
        private const val KEY_AUTO_FILL_TARGET_ENABLED = "auto_fill_target_enabled"
    }
}