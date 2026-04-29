package io.github.gonbei774.calisthenicsmemory.ui.components.program

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramExecutionSession
import io.github.gonbei774.calisthenicsmemory.data.ProgramWorkoutSet
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.ui.theme.LocalAppColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ワークアウトナビゲーション全画面ダイアログ
 *
 * プログラム実行中に全体を俯瞰し、セット単位でスキップ・やり直しを可能にする
 */
@Composable
fun ProgramNavigationSheet(
    session: ProgramExecutionSession,
    currentSetIndex: Int,
    isFromResult: Boolean = false,
    onDismiss: () -> Unit,
    onJumpToSet: (Int) -> Unit,
    onRedoSet: (Int) -> Unit,
    onUpdateTargetValue: (Int, Int) -> Unit,
    onUpdateActualValue: (Int, Int) -> Unit,
    onUpdateSetWeightG: (Int, Int?) -> Unit,
    onUpdateSetDistanceCm: (Int, Int?) -> Unit,
    onUpdateSetAssistanceG: (Int, Int?) -> Unit,
    onToggleComplete: (Int) -> Unit,
    onFinish: () -> Unit,
    onSaveAndExit: () -> Unit,
    onDiscard: () -> Unit
) {
    val appColors = LocalAppColors.current

    // 編集中セットのインデックス + どのメトリクス（reps / weight / distance / assistance）。
    // 同じピル再タップで -1 に戻して閉じる。
    var editingSetIndex by remember { mutableIntStateOf(-1) }
    var editingMetric by remember { mutableStateOf(EditingMetric.REPS) }

    // 進捗計算
    val completedSets = session.sets.count { it.isCompleted }
    val totalSets = session.sets.size
    val progress = if (totalSets > 0) completedSets.toFloat() / totalSets else 0f

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = appColors.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
            ) {
                // ヘッダー
                NavigationSheetHeader(onDismiss = onDismiss)

                // 進捗セクション
                NavigationProgressSection(
                    completedSets = completedSets,
                    totalSets = totalSets,
                    progress = progress
                )

                // セットリスト + フッター（リスト末尾に配置してスクロールで到達）
                NavigationSetsList(
                    session = session,
                    currentSetIndex = currentSetIndex,
                    editingSetIndex = editingSetIndex,
                    editingMetric = editingMetric,
                    onToggleEditing = { setIdx, metric ->
                        if (editingSetIndex == setIdx && editingMetric == metric) {
                            editingSetIndex = -1
                        } else {
                            editingSetIndex = setIdx
                            editingMetric = metric
                        }
                    },
                    onUpdateTargetValue = { setIdx, newValue ->
                        onUpdateTargetValue(setIdx, newValue)
                    },
                    onUpdateActualValue = { setIdx, newValue ->
                        onUpdateActualValue(setIdx, newValue)
                    },
                    onUpdateSetWeightG = onUpdateSetWeightG,
                    onUpdateSetDistanceCm = onUpdateSetDistanceCm,
                    onUpdateSetAssistanceG = onUpdateSetAssistanceG,
                    onJumpToSet = onJumpToSet,
                    onRedoSet = onRedoSet,
                    onToggleComplete = onToggleComplete,
                    showFooter = !isFromResult,
                    onFinish = onFinish,
                    onSaveAndExit = onSaveAndExit,
                    onDiscard = onDiscard,
                    modifier = Modifier.weight(1f)
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
    val appColors = LocalAppColors.current
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
            color = appColors.textPrimary
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = appColors.textSecondary
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
    val appColors = LocalAppColors.current
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
                color = appColors.textPrimary
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
 * 表示用アイテム（ループ外種目 or ラウンド）
 */
private sealed class NavigationDisplayItem {
    data class StandaloneExercise(
        val exerciseIndex: Int,
        val exercise: io.github.gonbei774.calisthenicsmemory.data.Exercise,
        val programExercise: io.github.gonbei774.calisthenicsmemory.data.ProgramExercise,
        val sets: List<ProgramWorkoutSet>
    ) : NavigationDisplayItem()

    data class LoopRound(
        val loopId: Long,
        val roundNumber: Int,
        val totalRounds: Int,
        val sets: List<ProgramWorkoutSet>
    ) : NavigationDisplayItem()
}

/**
 * 表示アイテムを構築（表示番号マップも返す）
 */
private fun buildNavigationItems(session: ProgramExecutionSession): Pair<List<NavigationDisplayItem>, Map<Int, Int>> {
    val items = mutableListOf<NavigationDisplayItem>()
    val processedLoopRounds = mutableSetOf<Pair<Long, Int>>() // (loopId, roundNumber)
    val exerciseDisplayNumbers = mutableMapOf<Int, Int>()  // exerciseIndex → 表示番号
    var nextDisplayNumber = 1

    // セットを実行順にイテレート
    for (set in session.sets) {
        // 表示番号を割り当て（まだ割り当てられていない場合）
        if (set.exerciseIndex !in exerciseDisplayNumbers) {
            exerciseDisplayNumbers[set.exerciseIndex] = nextDisplayNumber
            nextDisplayNumber++
        }

        if (set.loopId == null) {
            // ループ外の種目 - 種目ごとにまとめる
            val existingItem = items.filterIsInstance<NavigationDisplayItem.StandaloneExercise>()
                .find { it.exerciseIndex == set.exerciseIndex }

            if (existingItem == null) {
                val (pe, exercise) = session.exercises[set.exerciseIndex]
                val allSetsForExercise = session.sets.filter {
                    it.exerciseIndex == set.exerciseIndex && it.loopId == null
                }
                items.add(
                    NavigationDisplayItem.StandaloneExercise(
                        exerciseIndex = set.exerciseIndex,
                        exercise = exercise,
                        programExercise = pe,
                        sets = allSetsForExercise
                    )
                )
            }
        } else {
            // ループ内の種目 - ラウンドごとにまとめる
            val key = Pair(set.loopId, set.roundNumber)
            if (key !in processedLoopRounds) {
                processedLoopRounds.add(key)
                val setsForRound = session.sets.filter {
                    it.loopId == set.loopId && it.roundNumber == set.roundNumber
                }
                items.add(
                    NavigationDisplayItem.LoopRound(
                        loopId = set.loopId,
                        roundNumber = set.roundNumber,
                        totalRounds = set.totalRounds,
                        sets = setsForRound
                    )
                )
            }
        }
    }

    return Pair(items, exerciseDisplayNumbers)
}

/**
 * セットリスト（ラウンドカード形式）
 */
@Composable
private fun NavigationSetsList(
    session: ProgramExecutionSession,
    currentSetIndex: Int,
    editingSetIndex: Int,
    editingMetric: EditingMetric,
    onToggleEditing: (Int, EditingMetric) -> Unit,
    onUpdateTargetValue: (Int, Int) -> Unit,
    onUpdateActualValue: (Int, Int) -> Unit,
    onUpdateSetWeightG: (Int, Int?) -> Unit,
    onUpdateSetDistanceCm: (Int, Int?) -> Unit,
    onUpdateSetAssistanceG: (Int, Int?) -> Unit,
    onJumpToSet: (Int) -> Unit,
    onRedoSet: (Int) -> Unit,
    onToggleComplete: (Int) -> Unit,
    showFooter: Boolean,
    onFinish: () -> Unit,
    onSaveAndExit: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    val (displayItems, exerciseDisplayNumbers) = buildNavigationItems(session)

    // 現在のセットが属するアイテムのインデックスを取得
    val currentItemIndex = if (currentSetIndex in session.sets.indices) {
        val currentSet = session.sets[currentSetIndex]
        displayItems.indexOfFirst { item ->
            when (item) {
                is NavigationDisplayItem.StandaloneExercise ->
                    item.exerciseIndex == currentSet.exerciseIndex && currentSet.loopId == null
                is NavigationDisplayItem.LoopRound ->
                    item.loopId == currentSet.loopId && item.roundNumber == currentSet.roundNumber
            }
        }
    } else 0

    // シートが開いたときに現在のアイテムへスクロール
    LaunchedEffect(currentItemIndex) {
        if (currentItemIndex > 0) {
            lazyListState.animateScrollToItem(currentItemIndex)
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier.padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(displayItems.size) { index ->
            when (val item = displayItems[index]) {
                is NavigationDisplayItem.StandaloneExercise -> {
                    val exerciseStatus = getExerciseStatus(item.sets, session.sets, currentSetIndex)
                    val actualTotalSets = item.sets.maxOfOrNull { it.setNumber } ?: item.programExercise.sets
                    val displayNumber = exerciseDisplayNumbers[item.exerciseIndex] ?: (item.exerciseIndex + 1)

                    NavigationExerciseCard(
                        displayNumber = displayNumber,
                        exercise = item.exercise,
                        exerciseStatus = exerciseStatus,
                        sets = item.sets,
                        allSets = session.sets,
                        currentSetIndex = currentSetIndex,
                        totalSets = actualTotalSets,
                        editingSetIndex = editingSetIndex,
                        editingMetric = editingMetric,
                        onToggleEditing = onToggleEditing,
                        onUpdateTargetValue = onUpdateTargetValue,
                        onUpdateActualValue = onUpdateActualValue,
                        onUpdateSetWeightG = onUpdateSetWeightG,
                        onUpdateSetDistanceCm = onUpdateSetDistanceCm,
                        onUpdateSetAssistanceG = onUpdateSetAssistanceG,
                        onJumpToSet = onJumpToSet,
                        onRedoSet = onRedoSet,
                        onToggleComplete = onToggleComplete
                    )
                }
                is NavigationDisplayItem.LoopRound -> {
                    NavigationRoundCard(
                        roundNumber = item.roundNumber,
                        totalRounds = item.totalRounds,
                        sets = item.sets,
                        allSets = session.sets,
                        exercises = session.exercises,
                        currentSetIndex = currentSetIndex,
                        editingSetIndex = editingSetIndex,
                        editingMetric = editingMetric,
                        onToggleEditing = onToggleEditing,
                        onUpdateTargetValue = onUpdateTargetValue,
                        onUpdateActualValue = onUpdateActualValue,
                        onUpdateSetWeightG = onUpdateSetWeightG,
                        onUpdateSetDistanceCm = onUpdateSetDistanceCm,
                        onUpdateSetAssistanceG = onUpdateSetAssistanceG,
                        onJumpToSet = onJumpToSet,
                        onRedoSet = onRedoSet,
                        onToggleComplete = onToggleComplete
                    )
                }
            }
        }
        if (showFooter) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                NavigationSheetFooter(
                    onFinish = onFinish,
                    onSaveAndExit = onSaveAndExit,
                    onDiscard = onDiscard,
                    showTopDivider = false
                )
            }
        }
    }
}

/**
 * ラウンドカード（ループ内の1ラウンド分）
 */
@Composable
private fun NavigationRoundCard(
    roundNumber: Int,
    totalRounds: Int,
    sets: List<ProgramWorkoutSet>,
    allSets: List<ProgramWorkoutSet>,
    exercises: List<Pair<io.github.gonbei774.calisthenicsmemory.data.ProgramExercise, Exercise>>,
    currentSetIndex: Int,
    editingSetIndex: Int,
    editingMetric: EditingMetric,
    onToggleEditing: (Int, EditingMetric) -> Unit,
    onUpdateTargetValue: (Int, Int) -> Unit,
    onUpdateActualValue: (Int, Int) -> Unit,
    onUpdateSetWeightG: (Int, Int?) -> Unit,
    onUpdateSetDistanceCm: (Int, Int?) -> Unit,
    onUpdateSetAssistanceG: (Int, Int?) -> Unit,
    onJumpToSet: (Int) -> Unit,
    onRedoSet: (Int) -> Unit,
    onToggleComplete: (Int) -> Unit
) {
    val appColors = LocalAppColors.current
    val roundStatus = when {
        sets.all { it.isCompleted } -> ExerciseStatus.DONE
        sets.any { allSets.indexOf(it) == currentSetIndex } -> ExerciseStatus.CURRENT
        else -> ExerciseStatus.PENDING
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.border(
            width = 2.dp,
            color = Purple600.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp)
        )
    ) {
        Column {
            // ラウンドヘッダー
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
                // ラウンドバッジ
                Box(
                    modifier = Modifier
                        .background(Purple600, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.loop_current_round, roundNumber, totalRounds),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // ステータスバッジ
                when (roundStatus) {
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

            // ラウンド内の種目を実行順に表示
            sets.forEachIndexed { index, set ->
                val setIndex = allSets.indexOf(set)
                val isCurrent = setIndex == currentSetIndex
                val exercise = exercises.getOrNull(set.exerciseIndex)?.second
                val isIsometric = exercise?.type == "Isometric"
                val isEditing = editingSetIndex == setIndex
                val isEditorOpen = isEditing && !set.isSkipped

                NavigationRoundSetRow(
                    set = set,
                    setIndex = setIndex,
                    exercise = exercise,
                    isCurrent = isCurrent,
                    isIsometric = isIsometric,
                    isEditing = isEditing,
                    editingMetric = editingMetric,
                    onTogglePillEditing = onToggleEditing,
                    onJumpToSet = onJumpToSet,
                    onRedoSet = onRedoSet,
                    onToggleComplete = onToggleComplete,
                    isLast = index == sets.lastIndex && !isEditorOpen
                )
                if (isEditorOpen) {
                    InlineBilateralEditor(
                        set = set,
                        setIndex = setIndex,
                        isIsometric = isIsometric,
                        editingMetric = editingMetric,
                        onUpdateTargetValue = onUpdateTargetValue,
                        onUpdateActualValue = onUpdateActualValue,
                        onUpdateSetWeightG = onUpdateSetWeightG,
                        onUpdateSetDistanceCm = onUpdateSetDistanceCm,
                        onUpdateSetAssistanceG = onUpdateSetAssistanceG,
                        isLast = index == sets.lastIndex
                    )
                }
            }
        }
    }
}

/**
 * ラウンド内のセット行
 */
@Composable
private fun NavigationRoundSetRow(
    set: ProgramWorkoutSet,
    setIndex: Int,
    exercise: Exercise?,
    isCurrent: Boolean,
    isIsometric: Boolean,
    isEditing: Boolean,
    editingMetric: EditingMetric,
    onTogglePillEditing: (Int, EditingMetric) -> Unit,
    onJumpToSet: (Int) -> Unit,
    onRedoSet: (Int) -> Unit,
    onToggleComplete: (Int) -> Unit,
    isLast: Boolean
) {
    val appColors = LocalAppColors.current
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
                top = 10.dp,
                bottom = 10.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ステータスアイコン
        SetStatusIcon(
            status = setStatus,
            onClick = { onToggleComplete(setIndex) }
        )

        Spacer(modifier = Modifier.width(12.dp))

        // 種目名 + セット情報
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise?.name ?: "",
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                color = when (setStatus) {
                    SetStatus.COMPLETED -> Color.White
                    SetStatus.CURRENT -> Orange600
                    SetStatus.PENDING, SetStatus.SKIPPED -> Slate400
                }
            )
            Spacer(modifier = Modifier.height(2.dp))
            SetValueText(
                set = set,
                setIndex = setIndex,
                exercise = exercise,
                status = setStatus,
                isIsometric = isIsometric,
                isEditing = isEditing,
                editingMetric = editingMetric,
                onTogglePillEditing = if (setStatus != SetStatus.SKIPPED) onTogglePillEditing else null
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
 * 種目カード
 */
@Composable
private fun NavigationExerciseCard(
    displayNumber: Int,  // 表示用番号（ソート順）
    exercise: Exercise,
    exerciseStatus: ExerciseStatus,
    sets: List<ProgramWorkoutSet>,
    allSets: List<ProgramWorkoutSet>,
    currentSetIndex: Int,
    totalSets: Int,
    editingSetIndex: Int,
    editingMetric: EditingMetric,
    onToggleEditing: (Int, EditingMetric) -> Unit,
    onUpdateTargetValue: (Int, Int) -> Unit,
    onUpdateActualValue: (Int, Int) -> Unit,
    onUpdateSetWeightG: (Int, Int?) -> Unit,
    onUpdateSetDistanceCm: (Int, Int?) -> Unit,
    onUpdateSetAssistanceG: (Int, Int?) -> Unit,
    onJumpToSet: (Int) -> Unit,
    onRedoSet: (Int) -> Unit,
    onToggleComplete: (Int) -> Unit
) {
    val isUnilateral = exercise.laterality == "Unilateral"
    val exerciseType = exercise.type
    val appColors = LocalAppColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // 種目ヘッダー
            NavigationExerciseHeader(
                displayNumber = displayNumber,
                exerciseName = exercise.name,
                exerciseType = exerciseType,
                exerciseStatus = exerciseStatus
            )

            // セット行
            if (isUnilateral) {
                // Unilateral: セット番号でグループ化
                val setsByNumber = sets.groupBy { it.setNumber }
                val groupedEntries = setsByNumber.entries.toList()
                groupedEntries.forEachIndexed { groupIndex, (setNumber, sideSets) ->
                    val rightSet = sideSets.find { it.side == "Right" }
                    val leftSet = sideSets.find { it.side == "Left" }
                    val rightSetIndex = rightSet?.let { allSets.indexOf(it) } ?: -1
                    val leftSetIndex = leftSet?.let { allSets.indexOf(it) } ?: -1

                    // どちらかが現在実行中かを判定
                    val isCurrent = rightSetIndex == currentSetIndex || leftSetIndex == currentSetIndex
                    val isEditingRight = editingSetIndex == rightSetIndex && rightSetIndex >= 0
                    val isEditingLeft = editingSetIndex == leftSetIndex && leftSetIndex >= 0
                    val isEitherEditing = isEditingRight || isEditingLeft
                    val pairSkipped = (rightSet?.isSkipped == true) || (leftSet?.isSkipped == true)
                    val isEditorOpen = isEitherEditing && !pairSkipped
                    val isLastGroup = groupIndex == groupedEntries.lastIndex

                    NavigationUnilateralSetRow(
                        setNumber = setNumber,
                        totalSets = totalSets,
                        rightSet = rightSet,
                        leftSet = leftSet,
                        exercise = exercise,
                        isCurrent = isCurrent,
                        rightSetIndex = rightSetIndex,
                        leftSetIndex = leftSetIndex,
                        currentSetIndex = currentSetIndex,
                        isIsometric = exerciseType == "Isometric",
                        editingRight = isEditingRight,
                        editingLeft = isEditingLeft,
                        editingMetric = editingMetric,
                        onTogglePillEditing = onToggleEditing,
                        onJumpToSet = onJumpToSet,
                        onRedoSet = onRedoSet,
                        onToggleComplete = onToggleComplete,
                        isLast = isLastGroup && !isEditorOpen
                    )
                    if (isEditorOpen) {
                        InlineUnilateralEditor(
                            rightSet = rightSet,
                            leftSet = leftSet,
                            rightSetIndex = rightSetIndex,
                            leftSetIndex = leftSetIndex,
                            editingRight = isEditingRight,
                            editingLeft = isEditingLeft,
                            editingMetric = editingMetric,
                            isIsometric = exerciseType == "Isometric",
                            onUpdateTargetValue = onUpdateTargetValue,
                            onUpdateActualValue = onUpdateActualValue,
                            onUpdateSetWeightG = onUpdateSetWeightG,
                            onUpdateSetDistanceCm = onUpdateSetDistanceCm,
                            onUpdateSetAssistanceG = onUpdateSetAssistanceG,
                            isLast = isLastGroup
                        )
                    }
                }
            } else {
                // Bilateral: 各セットを個別に表示
                sets.forEachIndexed { index, set ->
                    val setIndex = allSets.indexOf(set)
                    val isCurrent = setIndex == currentSetIndex
                    val isEditing = editingSetIndex == setIndex
                    val isEditorOpen = isEditing && !set.isSkipped

                    NavigationBilateralSetRow(
                        set = set,
                        setIndex = setIndex,
                        exercise = exercise,
                        totalSets = totalSets,
                        isCurrent = isCurrent,
                        isIsometric = exerciseType == "Isometric",
                        isEditing = isEditing,
                        editingMetric = editingMetric,
                        onTogglePillEditing = onToggleEditing,
                        onJumpToSet = onJumpToSet,
                        onRedoSet = onRedoSet,
                        onToggleComplete = onToggleComplete,
                        isLast = index == sets.lastIndex && !isEditorOpen
                    )
                    if (isEditorOpen) {
                        InlineBilateralEditor(
                            set = set,
                            setIndex = setIndex,
                            isIsometric = exerciseType == "Isometric",
                            editingMetric = editingMetric,
                            onUpdateTargetValue = onUpdateTargetValue,
                            onUpdateActualValue = onUpdateActualValue,
                            onUpdateSetWeightG = onUpdateSetWeightG,
                            onUpdateSetDistanceCm = onUpdateSetDistanceCm,
                            onUpdateSetAssistanceG = onUpdateSetAssistanceG,
                            isLast = index == sets.lastIndex
                        )
                    }
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
    displayNumber: Int,  // 表示用番号（ソート順）
    exerciseName: String,
    exerciseType: String,
    exerciseStatus: ExerciseStatus
) {
    val appColors = LocalAppColors.current
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
                text = displayNumber.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary
            )
        }

        // 種目名
        Text(
            text = exerciseName,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = appColors.textPrimary,
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
    exercise: Exercise,
    totalSets: Int,
    isCurrent: Boolean,
    isIsometric: Boolean,
    isEditing: Boolean,
    editingMetric: EditingMetric,
    onTogglePillEditing: (Int, EditingMetric) -> Unit,
    onJumpToSet: (Int) -> Unit,
    onRedoSet: (Int) -> Unit,
    onToggleComplete: (Int) -> Unit,
    isLast: Boolean
) {
    val appColors = LocalAppColors.current
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
        SetStatusIcon(
            status = setStatus,
            onClick = { onToggleComplete(setIndex) }
        )

        Spacer(modifier = Modifier.width(12.dp))

        // セット情報
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
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
                // ループ内のセットならラウンドバッジを表示
                if (set.loopId != null && set.totalRounds > 1) {
                    Box(
                        modifier = Modifier
                            .background(Purple600.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "R${set.roundNumber}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Purple400
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            SetValueText(
                set = set,
                setIndex = setIndex,
                exercise = exercise,
                status = setStatus,
                isIsometric = isIsometric,
                isEditing = isEditing,
                editingMetric = editingMetric,
                onTogglePillEditing = if (setStatus != SetStatus.SKIPPED) onTogglePillEditing else null
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
    exercise: Exercise,
    isCurrent: Boolean,
    rightSetIndex: Int,
    leftSetIndex: Int,
    currentSetIndex: Int,
    isIsometric: Boolean,
    editingRight: Boolean,
    editingLeft: Boolean,
    editingMetric: EditingMetric,
    onTogglePillEditing: (Int, EditingMetric) -> Unit,
    onJumpToSet: (Int) -> Unit,
    onRedoSet: (Int) -> Unit,
    onToggleComplete: (Int) -> Unit,
    isLast: Boolean
) {
    val appColors = LocalAppColors.current
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
        SetStatusIcon(
            status = setStatus,
            onClick = {
                // currentSetIndex に一致する側を優先（CURRENT 判定を呼び出し側で正しく行うため）
                val targetIdx = when {
                    rightSetIndex == currentSetIndex -> rightSetIndex
                    leftSetIndex == currentSetIndex -> leftSetIndex
                    rightSetIndex >= 0 -> rightSetIndex
                    else -> leftSetIndex
                }
                onToggleComplete(targetIdx)
            }
        )

        Spacer(modifier = Modifier.width(12.dp))

        // セット情報
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
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
                // ループ内のセットならラウンドバッジを表示（rightSetから取得）
                val loopSet = rightSet ?: leftSet
                if (loopSet != null && loopSet.loopId != null && loopSet.totalRounds > 1) {
                    Box(
                        modifier = Modifier
                            .background(Purple600.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "R${loopSet.roundNumber}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Purple400
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            UnilateralValueText(
                rightSet = rightSet,
                leftSet = leftSet,
                rightSetIndex = rightSetIndex,
                leftSetIndex = leftSetIndex,
                exercise = exercise,
                status = setStatus,
                isIsometric = isIsometric,
                rightIsCurrent = rightSetIndex == currentSetIndex,
                leftIsCurrent = leftSetIndex == currentSetIndex,
                editingRight = editingRight,
                editingLeft = editingLeft,
                editingMetric = editingMetric,
                onTogglePillEditing = onTogglePillEditing
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
 *
 * onClick が指定されれば、32dp のタッチターゲット内でタップ可能。
 * COMPLETED の場合は「未実行に戻す」、それ以外は「完了にする」として a11y 描述。
 */
@Composable
private fun SetStatusIcon(
    status: SetStatus,
    onClick: (() -> Unit)? = null
) {
    val appColors = LocalAppColors.current
    val description = stringResource(
        if (status == SetStatus.COMPLETED) R.string.nav_uncheck_set
        else R.string.nav_check_set
    )
    Box(
        modifier = Modifier
            .size(32.dp)
            .then(if (onClick != null) Modifier.clickable(onClickLabel = description) { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
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
                        tint = appColors.textPrimary,
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
}

/**
 * セット値テキスト（Bilateral）
 *
 * Reps/Sec ピル + tracking ピル（重量/距離/アシスト、各 *TrackingEnabled が true の場合のみ）
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetValueText(
    set: ProgramWorkoutSet,
    setIndex: Int,
    exercise: Exercise?,
    status: SetStatus,
    isIsometric: Boolean,
    isEditing: Boolean = false,
    editingMetric: EditingMetric = EditingMetric.REPS,
    onTogglePillEditing: ((Int, EditingMetric) -> Unit)? = null
) {
    val unit = stringResource(if (isIsometric) R.string.unit_seconds else R.string.unit_reps)

    if (status == SetStatus.SKIPPED) {
        Text(
            text = stringResource(R.string.nav_skipped),
            fontSize = 13.sp,
            fontStyle = FontStyle.Italic,
            color = Slate500
        )
        return
    }

    val (valueText, valueColor) = when (status) {
        SetStatus.COMPLETED -> "${set.actualValue}" to Green400
        SetStatus.CURRENT -> "${set.actualValue}/${set.targetValue}" to Slate400
        SetStatus.PENDING -> "${set.targetValue}" to Slate500
        SetStatus.SKIPPED -> "" to Slate500
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ValuePill(
            valueText = valueText,
            valueColor = valueColor,
            unit = unit,
            isEditing = isEditing && editingMetric == EditingMetric.REPS,
            onClick = onTogglePillEditing?.let { cb -> { cb(setIndex, EditingMetric.REPS) } }
        )
        TrackingPills(
            set = set,
            setIndex = setIndex,
            exercise = exercise,
            isEditing = isEditing,
            editingMetric = editingMetric,
            onTogglePillEditing = onTogglePillEditing
        )
    }
}

/**
 * 重量/距離/アシスト ピル（exercise の各 *TrackingEnabled が true のもののみ表示）
 */
@Composable
private fun TrackingPills(
    set: ProgramWorkoutSet,
    setIndex: Int,
    exercise: Exercise?,
    isEditing: Boolean,
    editingMetric: EditingMetric,
    onTogglePillEditing: ((Int, EditingMetric) -> Unit)?
) {
    if (exercise == null) return
    if (exercise.weightTrackingEnabled) {
        TrackingPill(
            prefix = "W:",
            valueText = set.weightG?.let { "%.1f".format(it / 1000.0) } ?: "—",
            unit = "kg",
            accentColor = Orange600,
            isEditing = isEditing && editingMetric == EditingMetric.WEIGHT,
            onClick = onTogglePillEditing?.let { cb -> { cb(setIndex, EditingMetric.WEIGHT) } }
        )
    }
    if (exercise.distanceTrackingEnabled) {
        TrackingPill(
            prefix = "D:",
            valueText = set.distanceCm?.toString() ?: "—",
            unit = "cm",
            accentColor = Blue600,
            isEditing = isEditing && editingMetric == EditingMetric.DISTANCE,
            onClick = onTogglePillEditing?.let { cb -> { cb(setIndex, EditingMetric.DISTANCE) } }
        )
    }
    if (exercise.assistanceTrackingEnabled) {
        TrackingPill(
            prefix = "A:",
            valueText = set.assistanceG?.let { "%.1f".format(it / 1000.0) } ?: "—",
            unit = "kg",
            accentColor = Amber500,
            isEditing = isEditing && editingMetric == EditingMetric.ASSISTANCE,
            onClick = onTogglePillEditing?.let { cb -> { cb(setIndex, EditingMetric.ASSISTANCE) } }
        )
    }
}

/**
 * tracking 値ピル：枠の色でメトリクスを区別
 */
@Composable
private fun TrackingPill(
    prefix: String,
    valueText: String,
    unit: String,
    accentColor: Color,
    isEditing: Boolean,
    onClick: (() -> Unit)?
) {
    val borderColor = if (isEditing) Green400 else accentColor.copy(alpha = 0.6f)
    val pillModifier = Modifier
        .border(1.dp, borderColor, RoundedCornerShape(6.dp))
        .then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        )
        .padding(horizontal = 8.dp, vertical = 3.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = pillModifier
    ) {
        Text(
            text = prefix,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = accentColor
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(text = valueText, fontSize = 13.sp, color = Color.White)
        Spacer(modifier = Modifier.width(3.dp))
        Text(text = unit, fontSize = 11.sp, color = Slate500)
        if (onClick != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "✎",
                fontSize = 11.sp,
                color = if (isEditing) Green400 else Slate500
            )
        }
    }
}

/**
 * タップ可能な値ピル（破線風の細枠 + 編集アイコン）
 */
@Composable
private fun ValuePill(
    valueText: String,
    valueColor: Color,
    unit: String,
    isEditing: Boolean,
    onClick: (() -> Unit)?
) {
    val borderColor = if (isEditing) Green400 else Slate600
    val pillModifier = Modifier
        .border(1.dp, borderColor, RoundedCornerShape(6.dp))
        .then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        )
        .padding(horizontal = 8.dp, vertical = 3.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = pillModifier
    ) {
        Text(text = valueText, fontSize = 13.sp, color = valueColor)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = unit, fontSize = 11.sp, color = Slate500)
        if (onClick != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "✎",
                fontSize = 11.sp,
                color = if (isEditing) Green400 else Slate500
            )
        }
    }
}

/**
 * Unilateral用の値表示
 *
 * R/L 各々の reps ピル + 共通の tracking ピル（重量/距離/アシスト）。
 * tracking 値は R/L 共通なので、編集ピルは「Right側」のインデックス経由で発火する。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UnilateralValueText(
    rightSet: ProgramWorkoutSet?,
    leftSet: ProgramWorkoutSet?,
    rightSetIndex: Int,
    leftSetIndex: Int,
    exercise: Exercise,
    status: SetStatus,
    isIsometric: Boolean,
    rightIsCurrent: Boolean,
    leftIsCurrent: Boolean,
    editingRight: Boolean,
    editingLeft: Boolean,
    editingMetric: EditingMetric,
    onTogglePillEditing: (Int, EditingMetric) -> Unit
) {
    val unit = stringResource(if (isIsometric) R.string.unit_seconds else R.string.unit_reps)

    if (status == SetStatus.SKIPPED) {
        Text(
            text = stringResource(R.string.nav_skipped),
            fontSize = 13.sp,
            fontStyle = FontStyle.Italic,
            color = Slate500
        )
        return
    }

    val onClickRightReps: (() -> Unit)? = if (rightSet != null && !rightSet.isSkipped && rightSetIndex >= 0) {
        { onTogglePillEditing(rightSetIndex, EditingMetric.REPS) }
    } else null
    val onClickLeftReps: (() -> Unit)? = if (leftSet != null && !leftSet.isSkipped && leftSetIndex >= 0) {
        { onTogglePillEditing(leftSetIndex, EditingMetric.REPS) }
    } else null

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            UnilateralSidePill(
                sideLabel = "R:",
                set = rightSet,
                isCurrent = rightIsCurrent,
                unit = unit,
                isEditing = editingRight && editingMetric == EditingMetric.REPS,
                onClick = onClickRightReps
            )
            UnilateralSidePill(
                sideLabel = "L:",
                set = leftSet,
                isCurrent = leftIsCurrent,
                unit = unit,
                isEditing = editingLeft && editingMetric == EditingMetric.REPS,
                onClick = onClickLeftReps
            )
        }
        // tracking 値は R/L 共通。Right を「代表」として編集起点にする
        // （対応する onUpdateSet*G 側で R/L 両方に同期書き込みする）
        val representativeSet = rightSet ?: leftSet
        val representativeIndex = if (rightSetIndex >= 0) rightSetIndex else leftSetIndex
        val isPairEditing = (editingRight || editingLeft)
        if (representativeSet != null && representativeIndex >= 0) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TrackingPills(
                    set = representativeSet,
                    setIndex = representativeIndex,
                    exercise = exercise,
                    isEditing = isPairEditing,
                    editingMetric = editingMetric,
                    onTogglePillEditing = onTogglePillEditing
                )
            }
        }
    }
}

@Composable
private fun UnilateralSidePill(
    sideLabel: String,
    set: ProgramWorkoutSet?,
    isCurrent: Boolean,
    unit: String,
    isEditing: Boolean,
    onClick: (() -> Unit)?
) {
    val sideStatus = when {
        set == null -> SetStatus.PENDING
        set.isCompleted -> SetStatus.COMPLETED
        set.isSkipped -> SetStatus.SKIPPED
        isCurrent -> SetStatus.CURRENT
        else -> SetStatus.PENDING
    }
    val (valueText, valueColor) = when (sideStatus) {
        SetStatus.COMPLETED -> "${set?.actualValue ?: 0}" to Green400
        SetStatus.CURRENT -> "${set?.actualValue ?: 0}/${set?.targetValue ?: 0}" to Slate400
        SetStatus.PENDING -> "${set?.targetValue ?: 0}" to Slate500
        SetStatus.SKIPPED -> "-" to Slate500
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = sideLabel,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Slate500
        )
        Spacer(modifier = Modifier.width(4.dp))
        ValuePill(
            valueText = valueText,
            valueColor = valueColor,
            unit = unit,
            isEditing = isEditing,
            onClick = onClick
        )
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
    val appColors = LocalAppColors.current
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
        SetStatus.CURRENT -> {
            OutlinedButton(
                onClick = { onRedoSet(setIndex) },
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate500)
            ) {
                Text(
                    text = stringResource(R.string.nav_redo),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate400
                )
            }
        }
    }
}

/**
 * フッター: Finish / Save & Exit / Discard (3ボタン構成)
 */
@Composable
private fun NavigationSheetFooter(
    onFinish: () -> Unit,
    onSaveAndExit: () -> Unit,
    onDiscard: () -> Unit,
    showTopDivider: Boolean = true
) {
    val appColors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (showTopDivider) {
                    Modifier.drawBehind {
                        drawLine(
                            color = Slate700,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                } else Modifier
            )
            .padding(vertical = 16.dp),
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
                    color = appColors.textTertiary
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
 * 編集対象メトリクス。ピルタップで切り替わる。
 * REPS は targetValue/actualValue（種目状態に応じて）、
 * WEIGHT/DISTANCE/ASSISTANCE はそれぞれの tracking 値を編集する。
 */
private enum class EditingMetric { REPS, WEIGHT, DISTANCE, ASSISTANCE }

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

/**
 * インライン値エディタ（Bilateral）: `−` 値 `+` の単一行
 *
 * 完了済み (COMPLETED) は actualValue、それ以外は targetValue を編集する。
 */
@Composable
private fun InlineBilateralEditor(
    set: ProgramWorkoutSet,
    setIndex: Int,
    isIsometric: Boolean,
    editingMetric: EditingMetric,
    onUpdateTargetValue: (Int, Int) -> Unit,
    onUpdateActualValue: (Int, Int) -> Unit,
    onUpdateSetWeightG: (Int, Int?) -> Unit,
    onUpdateSetDistanceCm: (Int, Int?) -> Unit,
    onUpdateSetAssistanceG: (Int, Int?) -> Unit,
    isLast: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Slate800.copy(alpha = 0.5f))
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        when (editingMetric) {
            EditingMetric.REPS -> {
                val unit = stringResource(if (isIsometric) R.string.unit_seconds else R.string.unit_reps)
                val isEditingActual = set.isCompleted
                val currentValue = if (isEditingActual) set.actualValue else set.targetValue
                InlineValueEditorCore(
                    value = currentValue,
                    unit = unit,
                    onChange = { newValue ->
                        if (isEditingActual) onUpdateActualValue(setIndex, newValue)
                        else onUpdateTargetValue(setIndex, newValue)
                    },
                    compact = false
                )
            }
            EditingMetric.WEIGHT -> InlineWeightEditor(
                valueG = set.weightG,
                onChange = { onUpdateSetWeightG(setIndex, it) }
            )
            EditingMetric.DISTANCE -> InlineDistanceEditor(
                valueCm = set.distanceCm,
                onChange = { onUpdateSetDistanceCm(setIndex, it) }
            )
            EditingMetric.ASSISTANCE -> InlineAssistanceEditor(
                valueG = set.assistanceG,
                onChange = { onUpdateSetAssistanceG(setIndex, it) }
            )
        }
    }
}

/**
 * インライン値エディタ（Unilateral）: R / L を独立して編集可能な2ブロック
 */
@Composable
private fun InlineUnilateralEditor(
    rightSet: ProgramWorkoutSet?,
    leftSet: ProgramWorkoutSet?,
    rightSetIndex: Int,
    leftSetIndex: Int,
    editingRight: Boolean,
    editingLeft: Boolean,
    editingMetric: EditingMetric,
    isIsometric: Boolean,
    onUpdateTargetValue: (Int, Int) -> Unit,
    onUpdateActualValue: (Int, Int) -> Unit,
    onUpdateSetWeightG: (Int, Int?) -> Unit,
    onUpdateSetDistanceCm: (Int, Int?) -> Unit,
    onUpdateSetAssistanceG: (Int, Int?) -> Unit,
    isLast: Boolean
) {
    val unit = stringResource(if (isIsometric) R.string.unit_seconds else R.string.unit_reps)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Slate800.copy(alpha = 0.5f))
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (editingMetric) {
            EditingMetric.REPS -> {
                if (editingRight && rightSet != null && rightSetIndex >= 0 && !rightSet.isSkipped) {
                    UnilateralEditorBlock(
                        sideLabel = "R",
                        set = rightSet,
                        setIndex = rightSetIndex,
                        unit = unit,
                        onUpdateTargetValue = onUpdateTargetValue,
                        onUpdateActualValue = onUpdateActualValue,
                        compact = false
                    )
                } else if (editingLeft && leftSet != null && leftSetIndex >= 0 && !leftSet.isSkipped) {
                    UnilateralEditorBlock(
                        sideLabel = "L",
                        set = leftSet,
                        setIndex = leftSetIndex,
                        unit = unit,
                        onUpdateTargetValue = onUpdateTargetValue,
                        onUpdateActualValue = onUpdateActualValue,
                        compact = false
                    )
                }
            }
            EditingMetric.WEIGHT -> {
                // R/L 共通入力。Right 側のセットを代表とし、callback で R/L 両方に同期書き込み。
                val repSet = rightSet ?: leftSet
                val repIndex = if (rightSetIndex >= 0) rightSetIndex else leftSetIndex
                if (repSet != null && repIndex >= 0) {
                    InlineWeightEditor(
                        valueG = repSet.weightG,
                        onChange = { onUpdateSetWeightG(repIndex, it) }
                    )
                }
            }
            EditingMetric.DISTANCE -> {
                val repSet = rightSet ?: leftSet
                val repIndex = if (rightSetIndex >= 0) rightSetIndex else leftSetIndex
                if (repSet != null && repIndex >= 0) {
                    InlineDistanceEditor(
                        valueCm = repSet.distanceCm,
                        onChange = { onUpdateSetDistanceCm(repIndex, it) }
                    )
                }
            }
            EditingMetric.ASSISTANCE -> {
                val repSet = rightSet ?: leftSet
                val repIndex = if (rightSetIndex >= 0) rightSetIndex else leftSetIndex
                if (repSet != null && repIndex >= 0) {
                    InlineAssistanceEditor(
                        valueG = repSet.assistanceG,
                        onChange = { onUpdateSetAssistanceG(repIndex, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun UnilateralEditorBlock(
    sideLabel: String,
    set: ProgramWorkoutSet,
    setIndex: Int,
    unit: String,
    onUpdateTargetValue: (Int, Int) -> Unit,
    onUpdateActualValue: (Int, Int) -> Unit,
    compact: Boolean = true
) {
    val isEditingActual = set.isCompleted
    val currentValue = if (isEditingActual) set.actualValue else set.targetValue
    val onChange: (Int) -> Unit = { newValue ->
        if (isEditingActual) onUpdateActualValue(setIndex, newValue)
        else onUpdateTargetValue(setIndex, newValue)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$sideLabel:",
            fontSize = if (compact) 12.sp else 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Slate500
        )
        Spacer(modifier = Modifier.width(if (compact) 6.dp else 10.dp))
        InlineValueEditorCore(
            value = currentValue,
            unit = unit,
            onChange = onChange,
            compact = compact
        )
    }
}

/**
 * 共通レイアウト: `−` ボタン / 値 / `+` ボタン
 */
@Composable
private fun InlineValueEditorCore(
    value: Int,
    unit: String,
    onChange: (Int) -> Unit,
    compact: Boolean
) {
    val buttonSize = if (compact) 32.dp else 36.dp
    val valueMinWidth = if (compact) 56.dp else 72.dp
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)
    ) {
        RepeatableStepButton(
            label = "−",
            size = buttonSize,
            enabled = value > 0,
            contentDescription = stringResource(R.string.nav_step_decrement),
            onStep = {
                val next = (value - 1).coerceAtLeast(0)
                if (next != value) {
                    onChange(next); true
                } else false
            }
        )
        Box(
            modifier = Modifier.widthIn(min = valueMinWidth),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value.toString(),
                    fontSize = if (compact) 16.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = unit,
                    fontSize = 11.sp,
                    color = Slate500,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
        RepeatableStepButton(
            label = "+",
            size = buttonSize,
            enabled = true,
            contentDescription = stringResource(R.string.nav_step_increment),
            onStep = {
                onChange(value + 1); true
            }
        )
    }
}

/**
 * 重量エディタ（kg 表示、内部は g、±1000g ステップ）
 */
@Composable
private fun InlineWeightEditor(
    valueG: Int?,
    onChange: (Int?) -> Unit
) {
    InlineTrackingEditorCore(
        valueDisplay = valueG?.let { "%.1f".format(it / 1000.0) } ?: "—",
        unit = "kg",
        accentColor = Orange600,
        decrementEnabled = (valueG ?: 0) > 0,
        onDecrement = {
            val next = ((valueG ?: 0) - 1000).coerceAtLeast(0)
            if (next != (valueG ?: 0)) {
                onChange(next); true
            } else false
        },
        onIncrement = {
            onChange(((valueG ?: 0) + 1000).coerceAtLeast(0)); true
        }
    )
}

/**
 * 距離エディタ（cm 表示、±1cm ステップ）
 */
@Composable
private fun InlineDistanceEditor(
    valueCm: Int?,
    onChange: (Int?) -> Unit
) {
    InlineTrackingEditorCore(
        valueDisplay = valueCm?.toString() ?: "—",
        unit = "cm",
        accentColor = Blue600,
        decrementEnabled = (valueCm ?: 0) > 0,
        onDecrement = {
            val next = ((valueCm ?: 0) - 1).coerceAtLeast(0)
            if (next != (valueCm ?: 0)) {
                onChange(next); true
            } else false
        },
        onIncrement = {
            onChange(((valueCm ?: 0) + 1).coerceAtLeast(0)); true
        }
    )
}

/**
 * アシストエディタ（kg 表示、内部は g、±1000g ステップ）
 */
@Composable
private fun InlineAssistanceEditor(
    valueG: Int?,
    onChange: (Int?) -> Unit
) {
    InlineTrackingEditorCore(
        valueDisplay = valueG?.let { "%.1f".format(it / 1000.0) } ?: "—",
        unit = "kg",
        accentColor = Amber500,
        decrementEnabled = (valueG ?: 0) > 0,
        onDecrement = {
            val next = ((valueG ?: 0) - 1000).coerceAtLeast(0)
            if (next != (valueG ?: 0)) {
                onChange(next); true
            } else false
        },
        onIncrement = {
            onChange(((valueG ?: 0) + 1000).coerceAtLeast(0)); true
        }
    )
}

/**
 * tracking 値エディタの共通レイアウト：`−` ボタン / 値+単位 / `+` ボタン
 * accentColor は単位テキストに反映してメトリクスを区別する
 */
@Composable
private fun InlineTrackingEditorCore(
    valueDisplay: String,
    unit: String,
    accentColor: Color,
    decrementEnabled: Boolean,
    onDecrement: () -> Boolean,
    onIncrement: () -> Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RepeatableStepButton(
            label = "−",
            size = 36.dp,
            enabled = decrementEnabled,
            contentDescription = stringResource(R.string.nav_step_decrement),
            onStep = onDecrement
        )
        Box(
            modifier = Modifier.widthIn(min = 80.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = valueDisplay,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
        RepeatableStepButton(
            label = "+",
            size = 36.dp,
            enabled = true,
            contentDescription = stringResource(R.string.nav_step_increment),
            onStep = onIncrement
        )
    }
}

/**
 * 長押しで連続発火する円形ステップボタン。
 * - 単発タップは即座に1回発火（離してもOK）
 * - 350ms 押し続けると連続モード突入、80ms 間隔で発火
 * - onStep が false を返したら連続モード停止（これ以上動かせない場合に使用）
 */
@Composable
private fun RepeatableStepButton(
    label: String,
    size: androidx.compose.ui.unit.Dp,
    enabled: Boolean,
    contentDescription: String,
    onStep: () -> Boolean
) {
    val scope = rememberCoroutineScope()
    // 値が変わるたびに onStep ラムダが新しくなるが、pointerInput は再起動させたくないので
    // rememberUpdatedState で「常に最新」のラムダを参照する
    val currentOnStep by rememberUpdatedState(onStep)
    val containerColor = if (enabled) Orange600 else Slate700
    val textColor = if (enabled) Color.White else Slate500
    Box(
        modifier = Modifier
            .size(size)
            .background(containerColor, CircleShape)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    val firstResult = currentOnStep()
                    var repeatJob: Job? = null
                    try {
                        if (firstResult) {
                            repeatJob = scope.launch {
                                delay(350)
                                while (isActive) {
                                    if (!currentOnStep()) break
                                    delay(80)
                                }
                            }
                        }
                        waitForUpOrCancellation()
                    } finally {
                        repeatJob?.cancel()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = if (size <= 32.dp) 18.sp else 20.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}