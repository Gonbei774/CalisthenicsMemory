package io.github.gonbei774.calisthenicsmemory.ui.components.program

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.ProgramExecutionSession
import io.github.gonbei774.calisthenicsmemory.data.ProgramWorkoutSet
import io.github.gonbei774.calisthenicsmemory.ui.theme.*

@Composable
internal fun ProgramResultStep(
    session: ProgramExecutionSession,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    var comment by remember { mutableStateOf(session.comment) }

    // 0のセットがあるかチェック
    val hasZeroSets = remember(session.sets) {
        session.exercises.mapIndexed { exerciseIndex, (_, exercise) ->
            val setsForExercise = session.sets.filter { it.exerciseIndex == exerciseIndex && (it.isCompleted || it.isSkipped) }
            if (exercise.laterality == "Unilateral") {
                // 片側種目: 両方0のセットがあるか（ラウンドとセット番号でグループ化）
                setsForExercise.groupBy { it.roundNumber to it.setNumber }.any { (_, sets) ->
                    val rightValue = sets.firstOrNull { it.side == "Right" }?.actualValue ?: 0
                    val leftValue = sets.firstOrNull { it.side == "Left" }?.actualValue ?: 0
                    rightValue == 0 && leftValue == 0
                }
            } else {
                // 両側種目: 0のセットがあるか
                setsForExercise.any { it.actualValue == 0 }
            }
        }.any { it }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.workout_complete),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        // コメント入力
        OutlinedTextField(
            value = comment,
            onValueChange = {
                comment = it
                session.comment = it
            },
            label = { Text(stringResource(R.string.comment_label)) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Orange600,
                focusedLabelColor = Orange600
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 結果一覧
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            session.exercises.forEachIndexed { exerciseIndex, (_, exercise) ->
                val setsForExercise = session.sets.filter { it.exerciseIndex == exerciseIndex }
                val totalRounds = setsForExercise.firstOrNull()?.totalRounds ?: 1

                // 種目名ヘッダー
                item(key = "header-$exerciseIndex") {
                    Text(
                        text = exercise.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = if (exerciseIndex > 0) 8.dp else 0.dp)
                    )
                }

                // ラウンドごとにグループ化
                val groupedByRound = setsForExercise.groupBy { it.roundNumber }
                groupedByRound.toSortedMap().forEach { (roundNumber, setsInRound) ->
                    // ラウンドヘッダー（複数ラウンドの場合のみ表示）
                    if (totalRounds > 1) {
                        item(key = "exercise-$exerciseIndex-round-$roundNumber-header") {
                            Text(
                                text = stringResource(R.string.loop_round_current, roundNumber, totalRounds),
                                fontSize = 14.sp,
                                color = Purple400,
                                modifier = Modifier.padding(top = 4.dp, start = 8.dp)
                            )
                        }
                    }

                    if (exercise.laterality == "Unilateral") {
                        // 片側種目: セット番号でグループ化
                        val groupedSets = setsInRound.groupBy { it.setNumber }
                        groupedSets.toSortedMap().forEach { (setNumber, sets) ->
                            val rightSet = sets.firstOrNull { it.side == "Right" }
                            val leftSet = sets.firstOrNull { it.side == "Left" }

                            item(key = "exercise-$exerciseIndex-round-$roundNumber-set-$setNumber") {
                                ProgramUnilateralSetItem(
                                    setNumber = setNumber,
                                    rightSet = rightSet,
                                    leftSet = leftSet,
                                    exerciseType = exercise.type
                                )
                            }
                        }
                    } else {
                        // 両側種目
                        setsInRound.sortedBy { it.setNumber }.forEach { set ->
                            item(key = "exercise-$exerciseIndex-round-$roundNumber-set-${set.setNumber}") {
                                ProgramBilateralSetItem(
                                    set = set,
                                    exerciseType = exercise.type
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 0セット警告
        if (hasZeroSets) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Amber600.copy(alpha = 0.2f))
            ) {
                Text(
                    text = stringResource(R.string.program_result_zero_warning),
                    fontSize = 14.sp,
                    color = Amber500,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // 保存ボタン
        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Orange600)
        ) {
            Text(
                text = stringResource(R.string.record_workout),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.cancel))
        }
    }
}

// Program用 Unilateralセットアイテム（1行表示）
@Composable
internal fun ProgramUnilateralSetItem(
    setNumber: Int,
    rightSet: ProgramWorkoutSet?,
    leftSet: ProgramWorkoutSet?,
    exerciseType: String
) {
    var rightValue by remember(rightSet) { mutableStateOf(rightSet?.actualValue?.toString() ?: "0") }
    var leftValue by remember(leftSet) { mutableStateOf(leftSet?.actualValue?.toString() ?: "0") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (rightSet?.isSkipped == true && leftSet?.isSkipped == true) Slate700 else Slate800
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = stringResource(R.string.set_label, setNumber),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (rightSet?.isSkipped == true && leftSet?.isSkipped == true) {
                Text(
                    text = stringResource(R.string.skipped_label),
                    fontSize = 12.sp,
                    color = Slate400,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 右側
                Text(
                    text = stringResource(R.string.right_colon),
                    fontSize = 14.sp,
                    color = Slate400,
                    modifier = Modifier.width(30.dp)
                )
                OutlinedTextField(
                    value = rightValue,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            rightValue = newValue
                            newValue.toIntOrNull()?.let { rightSet?.actualValue = it }
                        }
                    },
                    label = {
                        Text(
                            stringResource(if (exerciseType == "Dynamic") R.string.reps_input else R.string.seconds_input),
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // 左側
                Text(
                    text = stringResource(R.string.left_colon),
                    fontSize = 14.sp,
                    color = Slate400,
                    modifier = Modifier.width(30.dp)
                )
                OutlinedTextField(
                    value = leftValue,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            leftValue = newValue
                            newValue.toIntOrNull()?.let { leftSet?.actualValue = it }
                        }
                    },
                    label = {
                        Text(
                            stringResource(if (exerciseType == "Dynamic") R.string.reps_input else R.string.seconds_input),
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }
    }
}

// Program用 Bilateralセットアイテム
@Composable
internal fun ProgramBilateralSetItem(
    set: ProgramWorkoutSet,
    exerciseType: String
) {
    var value by remember(set) { mutableStateOf(set.actualValue.toString()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (set.isSkipped) Slate700 else Slate800
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.set_label, set.setNumber),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (set.isSkipped) {
                    Text(
                        text = stringResource(R.string.skipped_label),
                        fontSize = 12.sp,
                        color = Slate400
                    )
                }
            }

            OutlinedTextField(
                value = value,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                        value = newValue
                        newValue.toIntOrNull()?.let { set.actualValue = it }
                    }
                },
                label = {
                    Text(
                        stringResource(if (exerciseType == "Dynamic") R.string.reps_input else R.string.seconds_input),
                        fontSize = 12.sp
                    )
                },
                modifier = Modifier.width(100.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}