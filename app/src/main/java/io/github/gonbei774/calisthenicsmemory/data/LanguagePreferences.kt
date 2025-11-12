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
enum class AppLanguage(val code: String, val displayNameJa: String, val displayNameEn: String, val displayNameEs: String, val displayNameDe: String, val displayNameZh: String, val displayNameFr: String) {
    SYSTEM("system", "システム設定に従う", "Follow system", "Seguir sistema", "Systemeinstellung folgen", "跟随系统", "Suivre le système"),
    JAPANESE("ja", "日本語", "Japanese", "Japonés", "Japanisch", "日语", "Japonais"),
    ENGLISH("en", "English", "English", "English", "English", "English", "English"),
    SPANISH("es", "スペイン語", "Spanish", "Español", "Spanisch", "西班牙语", "Espagnol"),
    GERMAN("de", "ドイツ語", "German", "Alemán", "Deutsch", "德语", "Allemand"),
    CHINESE("zh", "中国語（簡体字）", "Chinese (Simplified)", "Chino (Simplificado)", "Chinesisch (vereinfacht)", "简体中文", "Chinois (Simplifié)"),
    FRENCH("fr", "フランス語", "French", "Francés", "Französisch", "法语", "Français");

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.find { it.code == code } ?: SYSTEM
        }
    }

    /**
     * 現在の言語に応じた表示名を取得
     * @param currentLanguageCode 現在の言語コード（"ja", "en", "es", "de", "zh", "fr"）
     */
    fun getDisplayName(currentLanguageCode: String): String {
        return when (currentLanguageCode) {
            "ja" -> displayNameJa
            "es" -> displayNameEs
            "de" -> displayNameDe
            "zh" -> displayNameZh
            "fr" -> displayNameFr
            else -> displayNameEn
        }
    }
}