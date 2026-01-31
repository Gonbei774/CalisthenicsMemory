package io.github.gonbei774.calisthenicsmemory.data

import android.content.Context
import android.content.SharedPreferences

/**
 * テーマ設定の保存・読み込みを管理するクラス
 */
class ThemePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * テーマ設定を取得
     * @return AppTheme (SYSTEM, LIGHT, DARK)
     */
    fun getTheme(): AppTheme {
        val themeCode = prefs.getString(KEY_THEME, AppTheme.SYSTEM.code)
        return AppTheme.fromCode(themeCode ?: AppTheme.SYSTEM.code)
    }

    /**
     * テーマ設定を保存
     * @param theme 保存するテーマ
     */
    fun setTheme(theme: AppTheme) {
        prefs.edit().putString(KEY_THEME, theme.code).apply()
    }

    companion object {
        private const val PREFS_NAME = "theme_preferences"
        private const val KEY_THEME = "app_theme"
    }
}

/**
 * アプリで選択可能なテーマ
 */
enum class AppTheme(val code: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromCode(code: String): AppTheme {
            return entries.find { it.code == code } ?: SYSTEM
        }
    }
}