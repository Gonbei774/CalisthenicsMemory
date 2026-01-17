package io.github.gonbei774.calisthenicsmemory.data

import kotlinx.serialization.Serializable

/**
 * プログラム実行用のセットデータ
 */
@Serializable
data class ProgramWorkoutSet(
    val exerciseIndex: Int,       // プログラム内の種目インデックス
    val setNumber: Int,           // セット番号（1始まり）
    val side: String?,            // "Right" or "Left" or null
    var targetValue: Int,         // 目標値
    var actualValue: Int = 0,     // 実際の値
    var isCompleted: Boolean = false,
    var isSkipped: Boolean = false,
    var intervalSeconds: Int,     // このセット後のインターバル
    val previousValue: Int? = null, // 前回値（表示用）
    // ループ関連
    val loopId: Long? = null,     // 所属ループID（nullはループ外）
    val roundNumber: Int = 1,     // ラウンド番号（1始まり）
    val totalRounds: Int = 1,     // 総ラウンド数
    val loopRestAfterSeconds: Int = 0  // ラウンド間休憩（このセット後に追加、最終ラウンドは0）
)

/**
 * プログラム実行用のセッションデータ
 */
data class ProgramExecutionSession(
    val program: Program,
    val exercises: List<Pair<ProgramExercise, Exercise>>, // ProgramExercise + Exercise情報
    val sets: MutableList<ProgramWorkoutSet>,             // 実行順の全セット
    var comment: String = "",
    val loops: List<ProgramLoop> = emptyList()            // ループ情報
)

/**
 * プログラム実行画面のステップ（状態機械）
 */
sealed class ProgramExecutionStep {
    data class Confirm(val session: ProgramExecutionSession) : ProgramExecutionStep()
    data class StartInterval(val session: ProgramExecutionSession, val currentSetIndex: Int) : ProgramExecutionStep()
    data class Executing(val session: ProgramExecutionSession, val currentSetIndex: Int) : ProgramExecutionStep()
    data class Interval(val session: ProgramExecutionSession, val currentSetIndex: Int) : ProgramExecutionStep()
    data class Result(val session: ProgramExecutionSession) : ProgramExecutionStep()
}