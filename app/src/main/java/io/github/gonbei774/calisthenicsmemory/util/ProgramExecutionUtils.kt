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
            // 両方0のセットは除外、片方だけ0は保存
            val groupedBySetNumber = allSetsForExercise.groupBy { it.setNumber }
            val validSets = groupedBySetNumber.filter { (_, sets) ->
                val rightValue = sets.firstOrNull { it.side == "Right" }?.actualValue ?: 0
                val leftValue = sets.firstOrNull { it.side == "Left" }?.actualValue ?: 0
                rightValue > 0 || leftValue > 0  // 少なくとも片方が0より大きい
            }

            val valuesRight = validSets.flatMap { (_, sets) ->
                sets.filter { it.side == "Right" }.map { it.actualValue }
            }
            val valuesLeft = validSets.flatMap { (_, sets) ->
                sets.filter { it.side == "Left" }.map { it.actualValue }
            }
            // tracking値はR/L共通なのでRight行から取得（編集時にR/L同期書き込み済み）
            val rightSetsInOrder = validSets.flatMap { (_, sets) -> sets.filter { it.side == "Right" } }
            val distancesCm = rightSetsInOrder.map { it.distanceCm }
            val weightsG = rightSetsInOrder.map { it.weightG }
            val assistancesG = rightSetsInOrder.map { it.assistanceG }

            if (valuesRight.isNotEmpty()) {
                viewModel.addTrainingRecordsUnilateral(
                    exerciseId = exerciseId,
                    valuesRight = valuesRight,
                    valuesLeft = valuesLeft,
                    date = date,
                    time = time,
                    comment = session.comment,
                    distancesCm = distancesCm,
                    weightsG = weightsG,
                    assistancesG = assistancesG
                )
            }
        } else {
            // 両側種目: 0のセットは除外
            val validSets = allSetsForExercise.filter { it.actualValue > 0 }
            val values = validSets.map { it.actualValue }
            val distancesCm = validSets.map { it.distanceCm }
            val weightsG = validSets.map { it.weightG }
            val assistancesG = validSets.map { it.assistanceG }

            if (values.isNotEmpty()) {
                viewModel.addTrainingRecords(
                    exerciseId = exerciseId,
                    values = values,
                    date = date,
                    time = time,
                    comment = session.comment,
                    distancesCm = distancesCm,
                    weightsG = weightsG,
                    assistancesG = assistancesG
                )
            }
        }
    }
}

/**
 * セットリストを構築するファクトリ関数
 * プログラム設定の値を使用
 * @param originalSets 元のセットリスト（previousValueとループ情報を引き継ぐため）
 */
fun buildProgramValueSets(
    exercises: List<Pair<ProgramExercise, Exercise>>,
    originalSets: List<ProgramWorkoutSet> = emptyList()
): MutableList<ProgramWorkoutSet> {
    val sets = mutableListOf<ProgramWorkoutSet>()
    exercises.forEachIndexed { index, (pe, exercise) ->
        // ループ情報を元のセットから取得
        val exerciseSets = originalSets.filter { it.exerciseIndex == index }
        val firstSet = exerciseSets.firstOrNull()
        val loopId = firstSet?.loopId
        val totalRounds = firstSet?.totalRounds ?: 1

        // 各ラウンドのセットを生成
        for (round in 1..totalRounds) {
            // このラウンドの既存セットからloopRestAfterSecondsを取得
            val existingRoundSets = exerciseSets.filter { it.roundNumber == round }
            val loopRestAfter = existingRoundSets.lastOrNull()?.loopRestAfterSeconds ?: 0

            for (setNum in 1..pe.sets) {
                val isLastSetOfRound = setNum == pe.sets
                if (exercise.laterality == "Unilateral") {
                    val priorRight = originalSets.find { it.exerciseIndex == index && it.setNumber == setNum && it.side == "Right" && it.roundNumber == round }
                    val priorLeft = originalSets.find { it.exerciseIndex == index && it.setNumber == setNum && it.side == "Left" && it.roundNumber == round }
                    val priorTracking = priorRight ?: priorLeft  // tracking値はR/L共通
                    sets.add(ProgramWorkoutSet(
                        exerciseIndex = index,
                        setNumber = setNum,
                        side = "Right",
                        targetValue = pe.targetValue,
                        intervalSeconds = pe.intervalSeconds,
                        previousValue = priorRight?.previousValue,
                        loopId = loopId,
                        roundNumber = round,
                        totalRounds = totalRounds,
                        loopRestAfterSeconds = 0,  // Rightの後はLeftが来る
                        weightG = priorTracking?.weightG,
                        distanceCm = priorTracking?.distanceCm,
                        assistanceG = priorTracking?.assistanceG,
                        previousWeightG = priorTracking?.previousWeightG,
                        previousDistanceCm = priorTracking?.previousDistanceCm,
                        previousAssistanceG = priorTracking?.previousAssistanceG
                    ))
                    sets.add(ProgramWorkoutSet(
                        exerciseIndex = index,
                        setNumber = setNum,
                        side = "Left",
                        targetValue = pe.targetValue,
                        intervalSeconds = pe.intervalSeconds,
                        previousValue = priorLeft?.previousValue,
                        loopId = loopId,
                        roundNumber = round,
                        totalRounds = totalRounds,
                        loopRestAfterSeconds = if (isLastSetOfRound) loopRestAfter else 0,
                        weightG = priorTracking?.weightG,
                        distanceCm = priorTracking?.distanceCm,
                        assistanceG = priorTracking?.assistanceG,
                        previousWeightG = priorTracking?.previousWeightG,
                        previousDistanceCm = priorTracking?.previousDistanceCm,
                        previousAssistanceG = priorTracking?.previousAssistanceG
                    ))
                } else {
                    val priorSet = originalSets.find { it.exerciseIndex == index && it.setNumber == setNum && it.side == null && it.roundNumber == round }
                    sets.add(ProgramWorkoutSet(
                        exerciseIndex = index,
                        setNumber = setNum,
                        side = null,
                        targetValue = pe.targetValue,
                        intervalSeconds = pe.intervalSeconds,
                        previousValue = priorSet?.previousValue,
                        loopId = loopId,
                        roundNumber = round,
                        totalRounds = totalRounds,
                        loopRestAfterSeconds = if (isLastSetOfRound) loopRestAfter else 0,
                        weightG = priorSet?.weightG,
                        distanceCm = priorSet?.distanceCm,
                        assistanceG = priorSet?.assistanceG,
                        previousWeightG = priorSet?.previousWeightG,
                        previousDistanceCm = priorSet?.previousDistanceCm,
                        previousAssistanceG = priorSet?.previousAssistanceG
                    ))
                }
            }
        }
    }
    return sets
}

/**
 * セットリストを構築するファクトリ関数
 * チャレンジ設定（種目のデフォルト値）を使用、なければプログラム設定
 * @param originalSets 元のセットリスト（previousValueとループ情報を引き継ぐため）
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

        // ループ情報を元のセットから取得
        val exerciseSets = originalSets.filter { it.exerciseIndex == index }
        val firstSet = exerciseSets.firstOrNull()
        val loopId = firstSet?.loopId
        val totalRounds = firstSet?.totalRounds ?: 1

        // 各ラウンドのセットを生成
        for (round in 1..totalRounds) {
            // このラウンドの既存セットからloopRestAfterSecondsを取得
            val existingRoundSets = exerciseSets.filter { it.roundNumber == round }
            val loopRestAfter = existingRoundSets.lastOrNull()?.loopRestAfterSeconds ?: 0

            for (setNum in 1..setCount) {
                val isLastSetOfRound = setNum == setCount
                if (exercise.laterality == "Unilateral") {
                    val priorRight = originalSets.find { it.exerciseIndex == index && it.setNumber == setNum && it.side == "Right" && it.roundNumber == round }
                    val priorLeft = originalSets.find { it.exerciseIndex == index && it.setNumber == setNum && it.side == "Left" && it.roundNumber == round }
                    val priorTracking = priorRight ?: priorLeft  // tracking値はR/L共通
                    sets.add(ProgramWorkoutSet(
                        exerciseIndex = index,
                        setNumber = setNum,
                        side = "Right",
                        targetValue = targetValue,
                        intervalSeconds = interval,
                        previousValue = priorRight?.previousValue,
                        loopId = loopId,
                        roundNumber = round,
                        totalRounds = totalRounds,
                        loopRestAfterSeconds = 0,  // Rightの後はLeftが来る
                        weightG = priorTracking?.weightG,
                        distanceCm = priorTracking?.distanceCm,
                        assistanceG = priorTracking?.assistanceG,
                        previousWeightG = priorTracking?.previousWeightG,
                        previousDistanceCm = priorTracking?.previousDistanceCm,
                        previousAssistanceG = priorTracking?.previousAssistanceG
                    ))
                    sets.add(ProgramWorkoutSet(
                        exerciseIndex = index,
                        setNumber = setNum,
                        side = "Left",
                        targetValue = targetValue,
                        intervalSeconds = interval,
                        previousValue = priorLeft?.previousValue,
                        loopId = loopId,
                        roundNumber = round,
                        totalRounds = totalRounds,
                        loopRestAfterSeconds = if (isLastSetOfRound) loopRestAfter else 0,
                        weightG = priorTracking?.weightG,
                        distanceCm = priorTracking?.distanceCm,
                        assistanceG = priorTracking?.assistanceG,
                        previousWeightG = priorTracking?.previousWeightG,
                        previousDistanceCm = priorTracking?.previousDistanceCm,
                        previousAssistanceG = priorTracking?.previousAssistanceG
                    ))
                } else {
                    val priorSet = originalSets.find { it.exerciseIndex == index && it.setNumber == setNum && it.side == null && it.roundNumber == round }
                    sets.add(ProgramWorkoutSet(
                        exerciseIndex = index,
                        setNumber = setNum,
                        side = null,
                        targetValue = targetValue,
                        intervalSeconds = interval,
                        previousValue = priorSet?.previousValue,
                        loopId = loopId,
                        roundNumber = round,
                        totalRounds = totalRounds,
                        loopRestAfterSeconds = if (isLastSetOfRound) loopRestAfter else 0,
                        weightG = priorSet?.weightG,
                        distanceCm = priorSet?.distanceCm,
                        assistanceG = priorSet?.assistanceG,
                        previousWeightG = priorSet?.previousWeightG,
                        previousDistanceCm = priorSet?.previousDistanceCm,
                        previousAssistanceG = priorSet?.previousAssistanceG
                    ))
                }
            }
        }
    }
    return sets
}