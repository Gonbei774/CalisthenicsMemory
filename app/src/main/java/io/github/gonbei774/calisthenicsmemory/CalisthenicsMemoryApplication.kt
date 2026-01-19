package io.github.gonbei774.calisthenicsmemory

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import io.github.gonbei774.calisthenicsmemory.data.AppLanguage
import io.github.gonbei774.calisthenicsmemory.data.LanguagePreferences
import java.util.Locale

/**
 * カスタムApplicationクラス
 * ViewModelなどがgetApplication()で取得するContextにも言語設定を適用する
 */
class CalisthenicsMemoryApplication : Application() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(updateBaseContextLocale(newBase))
    }

    /**
     * Context の言語設定を更新する（全Androidバージョン対応）
     */
    private fun updateBaseContextLocale(context: Context): Context {
        val languagePrefs = LanguagePreferences(context)
        val selectedLanguage = languagePrefs.getLanguage()

        android.util.Log.d("CalisthenicsMemoryApplication", "Selected language: ${selectedLanguage.code}")

        // システム設定に従う場合は何もしない
        if (selectedLanguage == AppLanguage.SYSTEM) {
            android.util.Log.d("CalisthenicsMemoryApplication", "Using system language")
            return context
        }

        val locale = when (selectedLanguage) {
            AppLanguage.JAPANESE -> Locale("ja")
            AppLanguage.ENGLISH -> Locale("en")
            AppLanguage.SPANISH -> Locale("es")
            AppLanguage.GERMAN -> Locale("de")
            AppLanguage.CHINESE -> Locale("zh", "CN")
            AppLanguage.FRENCH -> Locale("fr")
            AppLanguage.ITALIAN -> Locale("it")
            AppLanguage.UKRAINIAN -> Locale("uk")
            AppLanguage.SYSTEM -> return context
        }

        android.util.Log.d("CalisthenicsMemoryApplication", "Setting locale to: ${locale.language}")
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }
}