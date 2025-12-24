package io.github.gonbei774.calisthenicsmemory.util

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramExecutionSession
import io.github.gonbei774.calisthenicsmemory.data.ProgramWorkoutSet
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * プログラム実行結果を保存する
 */
fun saveProgramResults(
    viewModel: TrainingViewModel,
    session: ProgramExecutionSession
) {
    val date = LocalDate.now().toString()
    val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

    // 同じ種目がプログラム内で複数回出てくる場合、全セットをまとめて1回で保存
    // 種目IDでグループ化
    val groupedByExerciseId = session.exercises
        .mapIndexed { index, pair -> index to pair }
        .groupBy { (_, pair) -> pair.second.id }

    groupedByExerciseId.forEach { (exerciseId, exerciseInfoList) ->
        val exercise = exerciseInfoList.first().second.second

        // この種目の全セット（プログラム内で複数回出てきても全て収集）
        // isCompleted または isSkipped のセットを記録対象とする
        val allSetsForExercise = exerciseInfoList.flatMap { (exerciseIndex, _) ->
            session.sets.filter {
                it.exerciseIndex == exerciseIndex && (it.isCompleted || it.isSkipped)
            }
        }

        if (allSetsForExercise.isEmpty()) return@forEach

        if (exercise.laterality == "Unilateral") {
            // 片側種目: 右・左をまとめて記録
            val rightSets = allSetsForExercise.filter { it.side == "Right" }
            val leftSets = allSetsForExercise.filter { it.side == "Left" }

            val valuesRight = rightSets.map { it.actualValue }
            val valuesLeft = leftSets.map { it.actualValue }

            if (valuesRight.isNotEmpty()) {
                viewModel.addTrainingRecordsUnilateral(
                    exerciseId = exerciseId,
                    valuesRight = valuesRight,
                    valuesLeft = valuesLeft,
                    date = date,
                    time = time,
                    comment = session.comment
                )
            }
        } else {
            // 両側種目
            val values = allSetsForExercise.map { it.actualValue }

            if (values.isNotEmpty()) {
                viewModel.addTrainingRecords(
                    exerciseId = exerciseId,
                    values = values,
                    date = date,
                    time = time,
                    comment = session.comment
                )
            }
        }
    }
}

/**
 * セットリストを構築するファクトリ関数
 * プログラム設定の値を使用
 * @param originalSets 元のセットリスト（previousValueを引き継ぐため）
 */
fun buildProgramValueSets(
    exercises: List<Pair<ProgramExercise, Exercise>>,
    originalSets: List<ProgramWorkoutSet> = emptyList()
): MutableList<ProgramWorkoutSet> {
    val sets = mutableListOf<ProgramWorkoutSet>()
    exercises.forEachIndexed { index, (pe, exercise) ->
        for (setNum in 1..pe.sets) {
            if (exercise.laterality == "Unilateral") {
                val prevRight = originalSets.find { it.exerciseIndex == index && it.setNumber == setNum && it.side == "Right" }?.previousValue
                val prevLeft = originalSets.find { it.exerciseIndex == index && it.setNumber == setNum && it.side == "Left" }?.previousValue
                sets.add(ProgramWorkoutSet(
                    exerciseIndex = index,
                    setNumber = setNum,
                    side = "Right",
                    targetValue = pe.targetValue,
                    intervalSeconds = pe.intervalSeconds,
                    previousValue = prevRight
                ))
                sets.add(ProgramWorkoutSet(
                    exerciseIndex = index,
                    setNumber = setNum,
                    side = "Left",
                    targetValue = pe.targetValue,
                    intervalSeconds = pe.intervalSeconds,
                    previousValue = prevLeft
                ))
            } else {
                val prevValue = originalSets.find { it.exerciseIndex == index && it.setNumber == setNum && it.side == null }?.previousValue
                sets.add(ProgramWorkoutSet(
                    exerciseIndex = index,
                    setNumber = setNum,
                    side = null,
                    targetValue = pe.targetValue,
                    intervalSeconds = pe.intervalSeconds,
                    previousValue = prevValue
                ))
            }
        }
    }
    return sets
}

/**
 * セットリストを構築するファクトリ関数
 * チャレンジ設定（種目のデフォルト値）を使用、なければプログラム設定
 * @param originalSets 元のセットリスト（previousValueを引き継ぐため）
 */
fun buildChallengeValueSets(
    exercises: List<Pair<ProgramExercise, Exercise>>,
    originalSets: List<ProgramWorkoutSet> = emptyList()
): MutableList<ProgramWorkoutSet> {
    val sets = mutableListOf<ProgramWorkoutSet>()
    exercises.forEachIndexed { index, (pe, exercise) ->
        val setCount = exercise.targetSets ?: pe.sets
        val targetValue = exercise.targetValue ?: pe.targetValue
        val interval = exercise.restInterval ?: pe.intervalSeconds
        for (setNum in 1..setCount) {
            if (exercise.laterality == "Unilateral") {
                val prevRight = originalSets.find { it.exerciseIndex == index && it.setNumber == setNum && it.side == "Right" }?.previousValue
                val prevLeft = originalSets.find { it.exerciseIndex == index && it.setNumber == setNum && it.side == "Left" }?.previousValue
                sets.add(ProgramWorkoutSet(
                    exerciseIndex = index,
                    setNumber = setNum,
                    side = "Right",
                    targetValue = targetValue,
                    intervalSeconds = interval,
                    previousValue = prevRight
                ))
                sets.add(ProgramWorkoutSet(
                    exerciseIndex = index,
                    setNumber = setNum,
                    side = "Left",
                    targetValue = targetValue,
                    intervalSeconds = interval,
                    previousValue = prevLeft
                ))
            } else {
                val prevValue = originalSets.find { it.exerciseIndex == index && it.setNumber == setNum && it.side == null }?.previousValue
                sets.add(ProgramWorkoutSet(
                    exerciseIndex = index,
                    setNumber = setNum,
                    side = null,
                    targetValue = targetValue,
                    intervalSeconds = interval,
                    previousValue = prevValue
                ))
            }
        }
    }
    return sets
}