package io.github.gonbei774.calisthenicsmemory.util

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.IntervalProgram
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramExecutionSession
import io.github.gonbei774.calisthenicsmemory.data.ProgramLoop
import io.github.gonbei774.calisthenicsmemory.data.ProgramWorkoutSet
import kotlin.math.roundToInt

/**
 * プログラムの推定所要時間を算出する。
 *
 * 計算内訳:
 *   - 各セットの作業時間
 *       Isometric: targetValue 秒
 *       Dynamic: targetValue × (exercise.repDuration ?: DEFAULT_DYNAMIC_REP_SECONDS)
 *   - 各セット前の開始前カウントダウン（有効時のみ加算）
 *   - 各セット後の intervalSeconds と loopRestAfterSeconds
 *   - 最後のセット後のインターバル／ラウンド休憩は差し引く（実際には発生しないため）
 */
object ProgramTimeEstimator {
    private const val DEFAULT_DYNAMIC_REP_SECONDS = 3

    fun estimateSeconds(
        session: ProgramExecutionSession,
        startCountdownSeconds: Int
    ): Int {
        val exerciseByIndex = session.exercises.mapIndexed { index, pair -> index to pair.second }.toMap()
        return computeFromFlatSets(session.sets) { set ->
            exerciseByIndex[set.exerciseIndex]
        }.let { workTotal ->
            workTotal + startCountdownSeconds * session.sets.size
        }
    }

    fun estimateSeconds(
        programExercises: List<ProgramExercise>,
        loops: List<ProgramLoop>,
        exerciseMap: Map<Long, Exercise>,
        startCountdownSeconds: Int
    ): Int {
        val sets = expandToFlatSets(programExercises, loops, exerciseMap)
        if (sets.isEmpty()) return 0
        var total = 0
        val lastIndex = sets.lastIndex
        sets.forEachIndexed { index, flat ->
            total += startCountdownSeconds + flat.workSeconds
            if (index != lastIndex) {
                total += flat.intervalSeconds + flat.loopRestSeconds
            }
        }
        return total
    }

    fun formatMinutes(seconds: Int): Int =
        (seconds / 60.0).roundToInt().coerceAtLeast(1)

    fun estimateExerciseSeconds(exercise: Exercise): Int? {
        val sets = exercise.targetSets ?: return null
        val targetValue = exercise.targetValue ?: return null
        val rest = exercise.restInterval ?: 0
        val sides = if (exercise.laterality == "Unilateral") 2 else 1
        val work = workSecondsFor(exercise, targetValue)
        val totalUnits = sets * sides
        return totalUnits * work + (totalUnits - 1).coerceAtLeast(0) * rest
    }

    fun estimateIntervalSeconds(interval: IntervalProgram, exerciseCount: Int): Int {
        if (exerciseCount <= 0 || interval.rounds <= 0) return 0
        val perRoundWork = exerciseCount * interval.workSeconds
        val perRoundRest = (exerciseCount - 1).coerceAtLeast(0) * interval.restSeconds
        val rounds = interval.rounds
        return rounds * (perRoundWork + perRoundRest) +
            (rounds - 1).coerceAtLeast(0) * interval.roundRestSeconds
    }

    fun formatHmmss(seconds: Int): String {
        val s = seconds.coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, sec)
        else "%d:%02d".format(m, sec)
    }

    private data class FlatSet(
        val workSeconds: Int,
        val intervalSeconds: Int,
        val loopRestSeconds: Int
    )

    private fun computeFromFlatSets(
        sets: List<ProgramWorkoutSet>,
        exerciseLookup: (ProgramWorkoutSet) -> Exercise?
    ): Int {
        if (sets.isEmpty()) return 0
        var total = 0
        val lastIndex = sets.lastIndex
        sets.forEachIndexed { index, set ->
            val exercise = exerciseLookup(set) ?: return@forEachIndexed
            total += workSecondsFor(exercise, set.targetValue)
            if (index != lastIndex) {
                total += set.intervalSeconds + set.loopRestAfterSeconds
            }
        }
        return total
    }

    private fun expandToFlatSets(
        programExercises: List<ProgramExercise>,
        loops: List<ProgramLoop>,
        exerciseMap: Map<Long, Exercise>
    ): List<FlatSet> {
        val result = mutableListOf<FlatSet>()

        // 実行順に並べる: スタンドアロン種目とループを sortOrder で混在ソート
        val standalone = programExercises.filter { it.loopId == null }
        val items = mutableListOf<Pair<Int, ExpansionItem>>()
        standalone.forEach { pe ->
            items.add(pe.sortOrder to ExpansionItem.Standalone(pe))
        }
        loops.forEach { loop ->
            items.add(loop.sortOrder to ExpansionItem.LoopItem(loop))
        }
        val sorted = items.sortedBy { it.first }.map { it.second }

        sorted.forEach { item ->
            when (item) {
                is ExpansionItem.Standalone -> {
                    val ex = exerciseMap[item.pe.exerciseId] ?: return@forEach
                    appendExerciseSets(result, item.pe, ex, loopRestAfter = 0)
                }
                is ExpansionItem.LoopItem -> {
                    val loop = item.loop
                    val loopPes = programExercises
                        .filter { it.loopId == loop.id }
                        .sortedBy { it.sortOrder }
                    if (loopPes.isEmpty()) return@forEach
                    for (round in 1..loop.rounds) {
                        loopPes.forEachIndexed { idx, lpe ->
                            val ex = exerciseMap[lpe.exerciseId] ?: return@forEachIndexed
                            val isLastExInRound = idx == loopPes.lastIndex
                            val loopRestAfter = if (isLastExInRound) loop.restBetweenRounds else 0
                            appendExerciseSets(result, lpe, ex, loopRestAfter = loopRestAfter)
                        }
                    }
                }
            }
        }
        return result
    }

    private fun appendExerciseSets(
        out: MutableList<FlatSet>,
        pe: ProgramExercise,
        exercise: Exercise,
        loopRestAfter: Int
    ) {
        val work = workSecondsFor(exercise, pe.targetValue)
        val sides = if (exercise.laterality == "Unilateral") 2 else 1
        for (setIdx in 1..pe.sets) {
            val isLastSet = setIdx == pe.sets
            for (side in 1..sides) {
                val isLastSide = side == sides
                // ProgramExecutionScreen の addExerciseSets に合わせて R/L 両方 intervalSeconds を持つ
                val loopRest = if (isLastSet && isLastSide) loopRestAfter else 0
                out.add(FlatSet(
                    workSeconds = work,
                    intervalSeconds = pe.intervalSeconds,
                    loopRestSeconds = loopRest
                ))
            }
        }
    }

    private fun workSecondsFor(exercise: Exercise, targetValue: Int): Int =
        when (exercise.type) {
            "Isometric" -> targetValue
            else -> targetValue * (exercise.repDuration ?: DEFAULT_DYNAMIC_REP_SECONDS)
        }

    private sealed class ExpansionItem {
        data class Standalone(val pe: ProgramExercise) : ExpansionItem()
        data class LoopItem(val loop: ProgramLoop) : ExpansionItem()
    }
}