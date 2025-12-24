package io.github.gonbei774.calisthenicsmemory.data

/**
 * プログラム実行用のセットデータ
 */
data class ProgramWorkoutSet(
    val exerciseIndex: Int,       // プログラム内の種目インデックス
    val setNumber: Int,           // セット番号（1始まり）
    val side: String?,            // "Right" or "Left" or null
    var targetValue: Int,         // 目標値
    var actualValue: Int = 0,     // 実際の値
    var isCompleted: Boolean = false,
    var isSkipped: Boolean = false,
    var intervalSeconds: Int,     // このセット後のインターバル
    val previousValue: Int? = null // 前回値（表示用）
)

/**
 * プログラム実行用のセッションデータ
 */
data class ProgramExecutionSession(
    val program: Program,
    val exercises: List<Pair<ProgramExercise, Exercise>>, // ProgramExercise + Exercise情報
    val sets: MutableList<ProgramWorkoutSet>,             // 実行順の全セット
    var comment: String = ""
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