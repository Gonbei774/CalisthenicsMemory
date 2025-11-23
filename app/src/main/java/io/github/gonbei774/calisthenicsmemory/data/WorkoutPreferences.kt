package io.github.gonbei774.calisthenicsmemory.data

import android.content.Context
import android.content.SharedPreferences

/**
 * ワークアウト設定の保存・読み込みを管理するクラス
 */
class WorkoutPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * 開始カウントダウンの秒数を取得
     * @return 開始カウントダウン秒数（デフォルト: 5秒）
     */
    fun getStartCountdown(): Int {
        return prefs.getInt(KEY_START_COUNTDOWN, DEFAULT_START_COUNTDOWN)
    }

    /**
     * 開始カウントダウンの秒数を保存
     * @param seconds 保存する秒数
     */
    fun setStartCountdown(seconds: Int) {
        prefs.edit().putInt(KEY_START_COUNTDOWN, seconds).apply()
    }

    /**
     * セット間インターバルの秒数を取得
     * @return セット間インターバル秒数（デフォルト: 240秒）
     */
    fun getSetInterval(): Int {
        return prefs.getInt(KEY_SET_INTERVAL, DEFAULT_SET_INTERVAL)
    }

    /**
     * セット間インターバルの秒数を保存
     * @param seconds 保存する秒数
     */
    fun setSetInterval(seconds: Int) {
        prefs.edit().putInt(KEY_SET_INTERVAL, seconds).apply()
    }

    /**
     * 1レップの時間（秒数）を取得
     * @return 1レップの時間（デフォルト: 5秒）
     */
    fun getRepDuration(): Int {
        return prefs.getInt(KEY_REP_DURATION, DEFAULT_REP_DURATION)
    }

    /**
     * 1レップの時間（秒数）を保存
     * @param seconds 保存する秒数
     */
    fun setRepDuration(seconds: Int) {
        prefs.edit().putInt(KEY_REP_DURATION, seconds).apply()
    }

    /**
     * 開始カウントダウンの有効/無効を取得
     * @return 有効: true, 無効: false（デフォルト: true）
     */
    fun isStartCountdownEnabled(): Boolean {
        return prefs.getBoolean(KEY_START_COUNTDOWN_ENABLED, true)
    }

    /**
     * 開始カウントダウンの有効/無効を保存
     * @param enabled 有効: true, 無効: false
     */
    fun setStartCountdownEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_START_COUNTDOWN_ENABLED, enabled).apply()
    }

    /**
     * セット間インターバルの有効/無効を取得
     * @return 有効: true, 無効: false（デフォルト: true）
     */
    fun isSetIntervalEnabled(): Boolean {
        return prefs.getBoolean(KEY_SET_INTERVAL_ENABLED, true)
    }

    /**
     * セット間インターバルの有効/無効を保存
     * @param enabled 有効: true, 無効: false
     */
    fun setSetIntervalEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SET_INTERVAL_ENABLED, enabled).apply()
    }

    /**
     * 1レップの時間の有効/無効を取得
     * @return 有効: true, 無効: false（デフォルト: true）
     */
    fun isRepDurationEnabled(): Boolean {
        return prefs.getBoolean(KEY_REP_DURATION_ENABLED, true)
    }

    /**
     * 1レップの時間の有効/無効を保存
     * @param enabled 有効: true, 無効: false
     */
    fun setRepDurationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REP_DURATION_ENABLED, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "workout_preferences"
        private const val KEY_START_COUNTDOWN = "start_countdown"
        private const val KEY_SET_INTERVAL = "set_interval"
        private const val KEY_REP_DURATION = "rep_duration"
        private const val KEY_START_COUNTDOWN_ENABLED = "start_countdown_enabled"
        private const val KEY_SET_INTERVAL_ENABLED = "set_interval_enabled"
        private const val KEY_REP_DURATION_ENABLED = "rep_duration_enabled"

        const val DEFAULT_START_COUNTDOWN = 5
        const val DEFAULT_SET_INTERVAL = 240
        const val DEFAULT_REP_DURATION = 5
    }
}
