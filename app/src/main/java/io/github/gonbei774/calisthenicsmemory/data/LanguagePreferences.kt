package io.github.gonbei774.calisthenicsmemory.data

import android.content.Context
import android.content.SharedPreferences

/**
 * 言語設定の保存・読み込みを管理するクラス
 */
class LanguagePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * 言語設定を取得
     * @return AppLanguage (SYSTEM, JAPANESE, ENGLISH)
     */
    fun getLanguage(): AppLanguage {
        val languageCode = prefs.getString(KEY_LANGUAGE, AppLanguage.SYSTEM.code)
        return AppLanguage.fromCode(languageCode ?: AppLanguage.SYSTEM.code)
    }

    /**
     * 言語設定を保存
     * @param language 保存する言語
     */
    fun setLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
    }

    companion object {
        private const val PREFS_NAME = "language_preferences"
        private const val KEY_LANGUAGE = "app_language"
    }
}

/**
 * アプリで選択可能な言語
 */
enum class AppLanguage(val code: String, val displayNameJa: String, val displayNameEn: String) {
    SYSTEM("system", "システム設定に従う", "Follow system"),
    JAPANESE("ja", "日本語", "Japanese"),
    ENGLISH("en", "English", "English");

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.find { it.code == code } ?: SYSTEM
        }
    }

    /**
     * 現在の言語に応じた表示名を取得
     * @param currentLanguageCode 現在の言語コード（"ja" or "en"）
     */
    fun getDisplayName(currentLanguageCode: String): String {
        return when (currentLanguageCode) {
            "ja" -> displayNameJa
            else -> displayNameEn
        }
    }
}