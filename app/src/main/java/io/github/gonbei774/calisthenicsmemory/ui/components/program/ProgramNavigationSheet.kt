package io.github.gonbei774.calisthenicsmemory.ui.components.program

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.ProgramExecutionSession
import io.github.gonbei774.calisthenicsmemory.data.ProgramWorkoutSet
import io.github.gonbei774.calisthenicsmemory.ui.theme.*

/**
 * ワークアウトナビゲーションモーダルボトムシート
 *
 * プログラム実行中に全体を俯瞰し、セット単位でスキップ・やり直しを可能にする
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramNavigationSheet(
    session: ProgramExecutionSession,
    currentSetIndex: Int,
    isFromResult: Boolean = false,
    onDismiss: () -> Unit,
    onJumpToSet: (Int) -> Unit,
    onRedoSet: (Int) -> Unit,
    onFinish: () -> Unit,
    onSaveAndExit: () -> Unit,
    onDiscard: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 進捗計算
    val completedSets = session.sets.count { it.isCompleted }
    val totalSets = session.sets.size
    val progress = if (totalSets > 0) completedSets.toFloat() / totalSets else 0f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Slate900,
        dragHandle = {
            // ドラッグハンドル
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Slate600, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(0.85f)
        ) {
            // ヘッダー
            NavigationSheetHeader(onDismiss = onDismiss)

            // 進捗セクション
            NavigationProgressSection(
                completedSets = completedSets,
                totalSets = totalSets,
                progress = progress
            )

            // セットリスト
            NavigationSetsList(
                session = session,
                currentSetIndex = currentSetIndex,
                onJumpToSet = onJumpToSet,
                onRedoSet = onRedoSet,
                modifier = Modifier.weight(1f)
            )

            // フッター（Result画面では非表示）
            if (!isFromResult) {
                NavigationSheetFooter(
                    onFinish = onFinish,
                    onSaveAndExit = onSaveAndExit,
                    onDiscard = onDiscard
                )
            }
        }
    }
}

/**
 * ヘッダー: タイトル + 閉じるボタン
 */
@Composable
private fun NavigationSheetHeader(
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = Slate700,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.nav_program_overview),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = Slate400
            )
        }
    }
}

/**
 * 進捗セクション: X/Y sets + プログレスバー
 */
@Composable
private fun NavigationProgressSection(
    completedSets: Int,
    totalSets: Int,
    progress: Float
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = Slate700,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.nav_overall_progress),
            fontSize = 12.sp,
            color = Slate500,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.nav_sets_format, completedSets, totalSets),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp),
                color = Green600,
                trackColor = Slate700,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

/**
 * セットリスト（種目カード）
 */
@Composable
private fun NavigationSetsList(
    session: ProgramExecutionSession,
    currentSetIndex: Int,
    onJumpToSet: (Int) -> Unit,
    onRedoSet: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // 種目ごとにグループ化
    val exerciseGroups = session.sets.groupBy { it.exerciseIndex }

    // 現在のセットが属する種目インデックスを取得
    val currentExerciseIndex = if (currentSetIndex in session.sets.indices) {
        session.sets[currentSetIndex].exerciseIndex
    } else {
        0
    }

    val lazyListState = rememberLazyListState()

    // シートが開いたときに現在の種目へスクロール
    LaunchedEffect(currentExerciseIndex) {
        if (currentExerciseIndex > 0) {
            lazyListState.animateScrollToItem(currentExerciseIndex)
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(session.exercises.indices.toList()) { exerciseIndex ->
            val (pe, exercise) = session.exercises[exerciseIndex]
            val setsForExercise = exerciseGroups[exerciseIndex] ?: emptyList()

            // 種目のステータスを判定
            val exerciseStatus = getExerciseStatus(setsForExercise, session.sets, currentSetIndex)

            // 実際のセット数を計算（動的に変更される可能性があるため）
            val actualTotalSets = session.sets
                .filter { it.exerciseIndex == exerciseIndex }
                .maxOfOrNull { it.setNumber } ?: pe.sets

            NavigationExerciseCard(
                exerciseIndex = exerciseIndex,
                exerciseName = exercise.name,
                exerciseType = exercise.type,
                exerciseStatus = exerciseStatus,
                sets = setsForExercise,
                allSets = session.sets,
                currentSetIndex = currentSetIndex,
                totalSets = actualTotalSets,
                isUnilateral = exercise.laterality == "Unilateral",
                onJumpToSet = onJumpToSet,
                onRedoSet = onRedoSet
            )
        }
    }
}

/**
 * 種目カード
 */
@Composable
private fun NavigationExerciseCard(
    exerciseIndex: Int,
    exerciseName: String,
    exerciseType: String,
    exerciseStatus: ExerciseStatus,
    sets: List<ProgramWorkoutSet>,
    allSets: List<ProgramWorkoutSet>,
    currentSetIndex: Int,
    totalSets: Int,
    isUnilateral: Boolean,
    onJumpToSet: (Int) -> Unit,
    onRedoSet: (Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // 種目ヘッダー
            NavigationExerciseHeader(
                exerciseIndex = exerciseIndex,
                exerciseName = exerciseName,
                exerciseType = exerciseType,
                exerciseStatus = exerciseStatus
            )

            // セット行
            if (isUnilateral) {
                // Unilateral: セット番号でグループ化
                val setsByNumber = sets.groupBy { it.setNumber }
                setsByNumber.forEach { (setNumber, sideSets) ->
                    val rightSet = sideSets.find { it.side == "Right" }
                    val leftSet = sideSets.find { it.side == "Left" }
                    val rightSetIndex = rightSet?.let { allSets.indexOf(it) } ?: -1
                    val leftSetIndex = leftSet?.let { allSets.indexOf(it) } ?: -1

                    // どちらかが現在実行中かを判定
                    val isCurrent = rightSetIndex == currentSetIndex || leftSetIndex == currentSetIndex

                    NavigationUnilateralSetRow(
                        setNumber = setNumber,
                        totalSets = totalSets,
                        rightSet = rightSet,
                        leftSet = leftSet,
                        isCurrent = isCurrent,
                        rightSetIndex = rightSetIndex,
                        leftSetIndex = leftSetIndex,
                        currentSetIndex = currentSetIndex,
                        isIsometric = exerciseType == "Isometric",
                        onJumpToSet = onJumpToSet,
                        onRedoSet = onRedoSet,
                        isLast = setNumber == totalSets
                    )
                }
            } else {
                // Bilateral: 各セットを個別に表示
                sets.forEachIndexed { index, set ->
                    val setIndex = allSets.indexOf(set)
                    val isCurrent = setIndex == currentSetIndex

                    NavigationBilateralSetRow(
                        set = set,
                        setIndex = setIndex,
                        totalSets = totalSets,
                        isCurrent = isCurrent,
                        isIsometric = exerciseType == "Isometric",
                        onJumpToSet = onJumpToSet,
                        onRedoSet = onRedoSet,
                        isLast = index == sets.lastIndex
                    )
                }
            }
        }
    }
}

/**
 * 種目ヘッダー
 */
@Composable
private fun NavigationExerciseHeader(
    exerciseIndex: Int,
    exerciseName: String,
    exerciseType: String,
    exerciseStatus: ExerciseStatus
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = Slate700,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 番号バッジ
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(Amber600, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = (exerciseIndex + 1).toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // 種目名
        Text(
            text = exerciseName,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )

        // 種目タイプバッジ (Dyn / Iso)
        Box(
            modifier = Modifier
                .background(Slate700, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text(
                text = stringResource(
                    if (exerciseType == "Isometric") R.string.nav_type_isometric_short
                    else R.string.nav_type_dynamic_short
                ),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate400
            )
        }

        // ステータスバッジ
        when (exerciseStatus) {
            ExerciseStatus.DONE -> {
                Box(
                    modifier = Modifier
                        .background(Green600.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.nav_done),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Green400
                    )
                }
            }
            ExerciseStatus.CURRENT -> {
                Box(
                    modifier = Modifier
                        .background(Orange600.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.nav_current),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Orange600
                    )
                }
            }
            ExerciseStatus.PENDING -> { /* バッジなし */ }
        }
    }
}

/**
 * Bilateral（両側同時）セット行
 */
@Composable
private fun NavigationBilateralSetRow(
    set: ProgramWorkoutSet,
    setIndex: Int,
    totalSets: Int,
    isCurrent: Boolean,
    isIsometric: Boolean,
    onJumpToSet: (Int) -> Unit,
    onRedoSet: (Int) -> Unit,
    isLast: Boolean
) {
    val setStatus = when {
        set.isCompleted -> SetStatus.COMPLETED
        set.isSkipped -> SetStatus.SKIPPED
        isCurrent -> SetStatus.CURRENT
        else -> SetStatus.PENDING
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isCurrent) {
                    Modifier
                        .background(Orange600.copy(alpha = 0.1f))
                        .drawBehind {
                            drawLine(
                                color = Orange600,
                                start = Offset(0f, 0f),
                                end = Offset(0f, size.height),
                                strokeWidth = 3.dp.toPx()
                            )
                        }
                } else if (set.isSkipped) {
                    Modifier.background(Slate750)
                } else {
                    Modifier
                }
            )
            .then(
                if (!isLast) {
                    Modifier.drawBehind {
                        drawLine(
                            color = Slate700,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                } else Modifier
            )
            .padding(
                start = if (isCurrent) 13.dp else 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ステータスアイコン
        SetStatusIcon(status = setStatus)

        Spacer(modifier = Modifier.width(12.dp))

        // セット情報
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.set_format, set.setNumber, totalSets),
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                color = when (setStatus) {
                    SetStatus.COMPLETED -> Color.White
                    SetStatus.CURRENT -> Orange600
                    SetStatus.PENDING, SetStatus.SKIPPED -> Slate500
                }
            )
            Spacer(modifier = Modifier.height(2.dp))
            SetValueText(
                set = set,
                status = setStatus,
                isIsometric = isIsometric
            )
        }

        // アクションボタン
        SetActionButton(
            setStatus = setStatus,
            setIndex = setIndex,
            onJumpToSet = onJumpToSet,
            onRedoSet = onRedoSet
        )
    }
}

/**
 * Unilateral（片側）セット行
 */
@Composable
private fun NavigationUnilateralSetRow(
    setNumber: Int,
    totalSets: Int,
    rightSet: ProgramWorkoutSet?,
    leftSet: ProgramWorkoutSet?,
    isCurrent: Boolean,
    rightSetIndex: Int,
    leftSetIndex: Int,
    currentSetIndex: Int,
    isIsometric: Boolean,
    onJumpToSet: (Int) -> Unit,
    onRedoSet: (Int) -> Unit,
    isLast: Boolean
) {
    // 両方完了していれば COMPLETED、どちらかがスキップなら SKIPPED、現在実行中なら CURRENT
    val setStatus = when {
        rightSet?.isCompleted == true && leftSet?.isCompleted == true -> SetStatus.COMPLETED
        rightSet?.isSkipped == true || leftSet?.isSkipped == true -> SetStatus.SKIPPED
        isCurrent -> SetStatus.CURRENT
        else -> SetStatus.PENDING
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isCurrent) {
                    Modifier
                        .background(Orange600.copy(alpha = 0.1f))
                        .drawBehind {
                            drawLine(
                                color = Orange600,
                                start = Offset(0f, 0f),
                                end = Offset(0f, size.height),
                                strokeWidth = 3.dp.toPx()
                            )
                        }
                } else if (setStatus == SetStatus.SKIPPED) {
                    Modifier.background(Slate750)
                } else {
                    Modifier
                }
            )
            .then(
                if (!isLast) {
                    Modifier.drawBehind {
                        drawLine(
                            color = Slate700,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                } else Modifier
            )
            .padding(
                start = if (isCurrent) 13.dp else 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ステータスアイコン
        SetStatusIcon(status = setStatus)

        Spacer(modifier = Modifier.width(12.dp))

        // セット情報
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.set_format, setNumber, totalSets),
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                color = when (setStatus) {
                    SetStatus.COMPLETED -> Color.White
                    SetStatus.CURRENT -> Orange600
                    SetStatus.PENDING, SetStatus.SKIPPED -> Slate500
                }
            )
            Spacer(modifier = Modifier.height(2.dp))
            UnilateralValueText(
                rightSet = rightSet,
                leftSet = leftSet,
                status = setStatus,
                isIsometric = isIsometric,
                rightIsCurrent = rightSetIndex == currentSetIndex,
                leftIsCurrent = leftSetIndex == currentSetIndex
            )
        }

        // アクションボタン（右側のセットインデックスを使用）
        SetActionButton(
            setStatus = setStatus,
            setIndex = if (rightSetIndex >= 0) rightSetIndex else leftSetIndex,
            onJumpToSet = onJumpToSet,
            onRedoSet = onRedoSet
        )
    }
}

/**
 * セットステータスアイコン
 */
@Composable
private fun SetStatusIcon(status: SetStatus) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .then(
                when (status) {
                    SetStatus.COMPLETED -> Modifier.background(Green600, CircleShape)
                    SetStatus.CURRENT -> Modifier.background(Orange600, CircleShape)
                    SetStatus.PENDING -> Modifier.border(2.dp, Slate600, CircleShape)
                    SetStatus.SKIPPED -> Modifier.background(Slate600, CircleShape)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        when (status) {
            SetStatus.COMPLETED -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
            SetStatus.CURRENT -> {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.White, CircleShape)
                )
            }
            SetStatus.SKIPPED -> {
                Text(
                    text = "−",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400
                )
            }
            SetStatus.PENDING -> { /* 空 */ }
        }
    }
}

/**
 * セット値テキスト（Bilateral）
 */
@Composable
private fun SetValueText(
    set: ProgramWorkoutSet,
    status: SetStatus,
    isIsometric: Boolean
) {
    val unit = stringResource(if (isIsometric) R.string.unit_seconds else R.string.unit_reps)

    when (status) {
        SetStatus.COMPLETED -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${set.actualValue}",
                    fontSize = 13.sp,
                    color = Green400
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    fontSize = 11.sp,
                    color = Slate500
                )
            }
        }
        SetStatus.SKIPPED -> {
            Text(
                text = stringResource(R.string.nav_skipped),
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
                color = Slate500
            )
        }
        SetStatus.CURRENT -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${set.actualValue}/${set.targetValue}",
                    fontSize = 13.sp,
                    color = Slate400
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    fontSize = 11.sp,
                    color = Slate500
                )
            }
        }
        SetStatus.PENDING -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${set.targetValue}",
                    fontSize = 13.sp,
                    color = Slate500
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    fontSize = 11.sp,
                    color = Slate500
                )
            }
        }
    }
}

/**
 * Unilateral用の値表示
 */
@Composable
private fun UnilateralValueText(
    rightSet: ProgramWorkoutSet?,
    leftSet: ProgramWorkoutSet?,
    status: SetStatus,
    isIsometric: Boolean,
    rightIsCurrent: Boolean,
    leftIsCurrent: Boolean
) {
    val unit = stringResource(if (isIsometric) R.string.unit_seconds else R.string.unit_reps)

    when (status) {
        SetStatus.COMPLETED -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Right
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "R:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate500
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${rightSet?.actualValue ?: 0}",
                        fontSize = 13.sp,
                        color = Green400
                    )
                }
                // Left
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "L:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate500
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${leftSet?.actualValue ?: 0}",
                        fontSize = 13.sp,
                        color = Green400
                    )
                }
                Text(
                    text = unit,
                    fontSize = 11.sp,
                    color = Slate500
                )
            }
        }
        SetStatus.SKIPPED -> {
            Text(
                text = stringResource(R.string.nav_skipped),
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
                color = Slate500
            )
        }
        SetStatus.CURRENT -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Right
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "R:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate500
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    if (rightSet?.isCompleted == true) {
                        Text(
                            text = "${rightSet.actualValue}",
                            fontSize = 13.sp,
                            color = Green400
                        )
                    } else if (rightIsCurrent) {
                        Text(
                            text = "${rightSet?.actualValue ?: 0}/${rightSet?.targetValue ?: 0}",
                            fontSize = 13.sp,
                            color = Slate400
                        )
                    } else {
                        Text(
                            text = "-",
                            fontSize = 13.sp,
                            color = Slate500
                        )
                    }
                }
                // Left
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "L:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate500
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    if (leftSet?.isCompleted == true) {
                        Text(
                            text = "${leftSet.actualValue}",
                            fontSize = 13.sp,
                            color = Green400
                        )
                    } else if (leftIsCurrent) {
                        Text(
                            text = "${leftSet?.actualValue ?: 0}/${leftSet?.targetValue ?: 0}",
                            fontSize = 13.sp,
                            color = Slate400
                        )
                    } else {
                        Text(
                            text = "-",
                            fontSize = 13.sp,
                            color = Slate500
                        )
                    }
                }
                Text(
                    text = unit,
                    fontSize = 11.sp,
                    color = Slate500
                )
            }
        }
        SetStatus.PENDING -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Right
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "R:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate500
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${rightSet?.targetValue ?: 0}",
                        fontSize = 13.sp,
                        color = Slate500
                    )
                }
                // Left
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "L:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate500
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${leftSet?.targetValue ?: 0}",
                        fontSize = 13.sp,
                        color = Slate500
                    )
                }
                Text(
                    text = unit,
                    fontSize = 11.sp,
                    color = Slate500
                )
            }
        }
    }
}

/**
 * アクションボタン（Redo / Jump）
 */
@Composable
private fun SetActionButton(
    setStatus: SetStatus,
    setIndex: Int,
    onJumpToSet: (Int) -> Unit,
    onRedoSet: (Int) -> Unit
) {
    when (setStatus) {
        SetStatus.COMPLETED -> {
            OutlinedButton(
                onClick = { onRedoSet(setIndex) },
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Green600)
            ) {
                Text(
                    text = stringResource(R.string.nav_redo),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Green400
                )
            }
        }
        SetStatus.PENDING, SetStatus.SKIPPED -> {
            OutlinedButton(
                onClick = { onJumpToSet(setIndex) },
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate500)
            ) {
                Text(
                    text = stringResource(R.string.nav_jump),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate400
                )
            }
        }
        SetStatus.CURRENT -> { /* ボタンなし */ }
    }
}

/**
 * フッター: Finish / Save & Exit / Discard (3ボタン構成)
 */
@Composable
private fun NavigationSheetFooter(
    onFinish: () -> Unit,
    onSaveAndExit: () -> Unit,
    onDiscard: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = Slate700,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Finish (Result画面へ)
        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Green600),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = stringResource(R.string.nav_finish),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Save & Exit / Discard (横並び)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Save & Exit
            OutlinedButton(
                onClick = onSaveAndExit,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Orange600)
            ) {
                Text(
                    text = stringResource(R.string.nav_save_and_exit),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Orange600
                )
            }

            // Discard
            OutlinedButton(
                onClick = onDiscard,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate500)
            ) {
                Text(
                    text = stringResource(R.string.nav_discard),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate300
                )
            }
        }
    }
}

// --- Helper Types ---

private enum class ExerciseStatus {
    DONE, CURRENT, PENDING
}

private enum class SetStatus {
    COMPLETED, CURRENT, PENDING, SKIPPED
}

/**
 * 種目のステータスを判定
 */
private fun getExerciseStatus(
    setsForExercise: List<ProgramWorkoutSet>,
    allSets: List<ProgramWorkoutSet>,
    currentSetIndex: Int
): ExerciseStatus {
    val allCompleted = setsForExercise.all { it.isCompleted }
    val hasCurrent = setsForExercise.any { allSets.indexOf(it) == currentSetIndex }

    return when {
        allCompleted -> ExerciseStatus.DONE
        hasCurrent -> ExerciseStatus.CURRENT
        else -> ExerciseStatus.PENDING
    }
}