package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ExerciseGroup
import io.github.gonbei774.calisthenicsmemory.data.IntervalProgram
import io.github.gonbei774.calisthenicsmemory.data.Program
import io.github.gonbei774.calisthenicsmemory.data.TodoTask
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.util.SearchUtils
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import sh.calvin.reorderable.ReorderableColumn

@Composable
fun ToDoScreen(
    viewModel: TrainingViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToRecord: (Long) -> Unit,
    onNavigateToWorkout: (Long) -> Unit,
    onNavigateToProgramPreview: (Long) -> Unit,
    onNavigateToIntervalPreview: (Long) -> Unit
) {
    val appColors = LocalAppColors.current
    val todoTasks by viewModel.todoTasks.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val programs by viewModel.programs.collectAsState()
    val intervalPrograms by viewModel.intervalPrograms.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var repeatDialogTask by remember { mutableStateOf<TodoTask?>(null) }
    var deleteConfirmTask by remember { mutableStateOf<TodoTask?>(null) }

    // Map IDs to objects for display
    val exerciseMap = remember(exercises) {
        exercises.associateBy { it.id }
    }
    val groupMap = remember(groups) {
        groups.associateBy { it.id }
    }
    val programMap = remember(programs) {
        programs.associateBy { it.id }
    }
    val intervalProgramMap = remember(intervalPrograms) {
        intervalPrograms.associateBy { it.id }
    }

    // グループごとの所属種目
    val groupExercisesMap = remember(groups, exercises) {
        groups.associate { group ->
            group.id to exercises.filter { it.group == group.name }.sortedBy { it.displayOrder }
        }
    }

    // グループ内種目の今日の記録状態
    val todayStr = remember { java.time.LocalDate.now().toString() }
    val groupExerciseCompletions = remember { mutableStateMapOf<Long, Set<Long>>() }
    LaunchedEffect(todoTasks, exercises, groups) {
        val groupTodoTasks = todoTasks.filter { it.type == TodoTask.TYPE_GROUP }
        groupTodoTasks.forEach { task ->
            val group = groupMap[task.referenceId] ?: return@forEach
            val groupExercises = exercises.filter { it.group == group.name }
            val completedIds = groupExercises.filter { ex ->
                viewModel.hasRecordOnDate(ex.id, todayStr)
            }.map { it.id }.toSet()
            groupExerciseCompletions[task.referenceId] = completedIds
        }
    }

    // Load exercise counts for interval programs
    val intervalExerciseCounts = remember { mutableStateMapOf<Long, Int>() }
    LaunchedEffect(intervalPrograms) {
        intervalPrograms.forEach { program ->
            if (program.id !in intervalExerciseCounts) {
                val exs = viewModel.getIntervalProgramExercisesSync(program.id)
                intervalExerciseCounts[program.id] = exs.size
            }
        }
    }

    Scaffold(
        topBar = {
            // Amber gradient header
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Amber500, Yellow500)
                            )
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                tint = Color.White
                            )
                        }
                        Text(
                            text = stringResource(R.string.todo_title),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Amber500
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add), tint = appColors.textPrimary)
            }
        }
    ) { paddingValues ->
        // Split tasks into active and inactive
        val today = remember { java.time.LocalDate.now() }
        val todayDayNumber = remember { today.dayOfWeek.value }
        val todayStr = remember { today.toString() }

        val activeTasks = remember(todoTasks, todayDayNumber, todayStr) {
            todoTasks.filter { task ->
                if (!task.isRepeating()) true
                else todayDayNumber in task.getRepeatDayNumbers() && task.lastCompletedDate != todayStr
            }
        }
        val inactiveTasks = remember(todoTasks, todayDayNumber, todayStr) {
            todoTasks.filter { task ->
                task.isRepeating() &&
                    (todayDayNumber !in task.getRepeatDayNumbers() || task.lastCompletedDate == todayStr)
            }
        }

        if (todoTasks.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.todo_empty),
                    fontSize = 16.sp,
                    color = appColors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Active tasks with drag-and-drop reordering
                if (activeTasks.isNotEmpty()) {
                    ReorderableColumn(
                        list = activeTasks,
                        onSettle = { fromIndex, toIndex ->
                            val reorderedActive = activeTasks.toMutableList()
                            val item = reorderedActive.removeAt(fromIndex)
                            reorderedActive.add(toIndex, item)
                            val allIds = reorderedActive.map { it.id } + inactiveTasks.map { it.id }
                            viewModel.reorderTodoTasks(allIds)
                        },
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) { index, task, isDragging ->
                        key(task.id) {
                            ReorderableItem {
                                val elevation by animateDpAsState(
                                    targetValue = if (isDragging) 4.dp else 0.dp,
                                    label = "elevation"
                                )
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        if (value == SwipeToDismissBoxValue.EndToStart) {
                                            deleteConfirmTask = task
                                        }
                                        false
                                    }
                                )
                                val dragHandleModifier = Modifier.longPressDraggableHandle()
                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    color = Red600,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .padding(horizontal = 16.dp),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = stringResource(R.string.delete),
                                                tint = appColors.textPrimary
                                            )
                                        }
                                    },
                                    enableDismissFromStartToEnd = false,
                                    enableDismissFromEndToStart = true
                                ) {
                                    ActiveTaskContent(
                                        task = task,
                                        exerciseMap = exerciseMap,
                                        groupMap = groupMap,
                                        groupExercisesMap = groupExercisesMap,
                                        groupExerciseCompletions = groupExerciseCompletions,
                                        programMap = programMap,
                                        intervalProgramMap = intervalProgramMap,
                                        intervalExerciseCounts = intervalExerciseCounts,
                                        isDragging = isDragging,
                                        elevation = elevation,
                                        dragHandleModifier = dragHandleModifier,
                                        onNavigateToRecord = onNavigateToRecord,
                                        onNavigateToWorkout = onNavigateToWorkout,
                                        onNavigateToProgramPreview = onNavigateToProgramPreview,
                                        onNavigateToIntervalPreview = onNavigateToIntervalPreview,
                                        onLongClick = { repeatDialogTask = task }
                                    )
                                }
                            }
                        }
                    }
                }

                // Inactive tasks (grayed out, no start button)
                if (inactiveTasks.isNotEmpty()) {
                    if (activeTasks.isNotEmpty()) {
                        HorizontalDivider(
                            color = appColors.border,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    inactiveTasks.forEach { task ->
                        key(task.id) {
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        deleteConfirmTask = task
                                    }
                                    false
                                }
                            )
                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                color = Red600,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.delete),
                                            tint = appColors.textPrimary
                                        )
                                    }
                                },
                                enableDismissFromStartToEnd = false,
                                enableDismissFromEndToStart = true
                            ) {
                                InactiveTaskContent(
                                    task = task,
                                    exerciseMap = exerciseMap,
                                    groupMap = groupMap,
                                    groupExercisesMap = groupExercisesMap,
                                    groupExerciseCompletions = groupExerciseCompletions,
                                    programMap = programMap,
                                    intervalProgramMap = intervalProgramMap,
                                    intervalExerciseCounts = intervalExerciseCounts,
                                    onLongClick = { repeatDialogTask = task }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    deleteConfirmTask?.let { task ->
        val taskName = when (task.type) {
            TodoTask.TYPE_EXERCISE -> exerciseMap[task.referenceId]?.name
            TodoTask.TYPE_GROUP -> groupMap[task.referenceId]?.name
            TodoTask.TYPE_PROGRAM -> programMap[task.referenceId]?.name
            TodoTask.TYPE_INTERVAL -> intervalProgramMap[task.referenceId]?.name
            else -> null
        } ?: "?"
        AlertDialog(
            onDismissRequest = { deleteConfirmTask = null },
            title = { Text(stringResource(R.string.delete_confirmation), color = appColors.textPrimary) },
            text = { Text(stringResource(R.string.todo_delete_confirm_message, taskName), color = appColors.textPrimary) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTodoTask(task.id)
                    deleteConfirmTask = null
                }) {
                    Text(stringResource(R.string.delete), color = Red600)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmTask = null }) {
                    Text(stringResource(R.string.cancel), color = appColors.textSecondary)
                }
            },
            containerColor = appColors.cardBackground
        )
    }

    // Repeat days dialog
    repeatDialogTask?.let { task ->
        RepeatDaysDialog(
            currentRepeatDays = task.repeatDays,
            onSave = { newRepeatDays ->
                viewModel.updateTodoRepeatDays(task.id, newRepeatDays)
                repeatDialogTask = null
            },
            onDismiss = { repeatDialogTask = null }
        )
    }

    // Add items dialog (tabbed)
    if (showAddDialog) {
        AddItemsDialog(
            viewModel = viewModel,
            todoTasks = todoTasks,
            exercises = exercises,
            groups = groups,
            programs = programs,
            intervalPrograms = intervalPrograms,
            intervalExerciseCounts = intervalExerciseCounts,
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun ActiveTaskContent(
    task: TodoTask,
    exerciseMap: Map<Long, Exercise>,
    groupMap: Map<Long, ExerciseGroup>,
    groupExercisesMap: Map<Long, List<Exercise>>,
    groupExerciseCompletions: Map<Long, Set<Long>>,
    programMap: Map<Long, Program>,
    intervalProgramMap: Map<Long, IntervalProgram>,
    intervalExerciseCounts: Map<Long, Int>,
    isDragging: Boolean,
    elevation: androidx.compose.ui.unit.Dp,
    dragHandleModifier: Modifier,
    onNavigateToRecord: (Long) -> Unit,
    onNavigateToWorkout: (Long) -> Unit,
    onNavigateToProgramPreview: (Long) -> Unit,
    onNavigateToIntervalPreview: (Long) -> Unit,
    onLongClick: () -> Unit
) {
    val appColors = LocalAppColors.current
    when (task.type) {
        TodoTask.TYPE_EXERCISE -> {
            val exercise = exerciseMap[task.referenceId]
            if (exercise != null) {
                var showModeDialog by remember { mutableStateOf(false) }
                ExerciseTaskCard(
                    exercise = exercise,
                    task = task,
                    isDragging = isDragging,
                    elevation = elevation,
                    dragHandleModifier = dragHandleModifier,
                    onStart = { showModeDialog = true },
                    onLongClick = onLongClick
                )
                if (showModeDialog) {
                    AlertDialog(
                        onDismissRequest = { showModeDialog = false },
                        containerColor = appColors.cardBackground,
                        title = {
                            Text(
                                text = stringResource(R.string.todo_choose_mode),
                                color = appColors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        showModeDialog = false
                                        onNavigateToRecord(task.referenceId)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Green600),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.todo_mode_record),
                                        fontSize = 16.sp,
                                        color = appColors.textPrimary
                                    )
                                }
                                Button(
                                    onClick = {
                                        showModeDialog = false
                                        onNavigateToWorkout(task.referenceId)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Orange600),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.todo_mode_workout),
                                        fontSize = 16.sp,
                                        color = appColors.textPrimary
                                    )
                                }
                            }
                        },
                        confirmButton = {},
                        dismissButton = {
                            TextButton(onClick = { showModeDialog = false }) {
                                Text(
                                    text = stringResource(R.string.cancel),
                                    color = appColors.textSecondary
                                )
                            }
                        }
                    )
                }
            }
        }
        TodoTask.TYPE_GROUP -> {
            val group = groupMap[task.referenceId]
            if (group != null) {
                GroupTaskCard(
                    group = group,
                    groupExercises = groupExercisesMap[group.id] ?: emptyList(),
                    completedExerciseIds = groupExerciseCompletions[group.id] ?: emptySet(),
                    repeatDays = task.repeatDays,
                    isDragging = isDragging,
                    elevation = elevation,
                    dragHandleModifier = dragHandleModifier,
                    onNavigateToRecord = onNavigateToRecord,
                    onNavigateToWorkout = onNavigateToWorkout,
                    onLongClick = onLongClick
                )
            }
        }
        TodoTask.TYPE_PROGRAM -> {
            val program = programMap[task.referenceId]
            if (program != null) {
                ProgramTaskCard(
                    program = program,
                    repeatDays = task.repeatDays,
                    isDragging = isDragging,
                    elevation = elevation,
                    dragHandleModifier = dragHandleModifier,
                    onNavigate = { onNavigateToProgramPreview(program.id) },
                    onLongClick = onLongClick
                )
            }
        }
        TodoTask.TYPE_INTERVAL -> {
            val intervalProgram = intervalProgramMap[task.referenceId]
            if (intervalProgram != null) {
                IntervalTaskCard(
                    intervalProgram = intervalProgram,
                    exerciseCount = intervalExerciseCounts[intervalProgram.id] ?: 0,
                    repeatDays = task.repeatDays,
                    isDragging = isDragging,
                    elevation = elevation,
                    dragHandleModifier = dragHandleModifier,
                    onNavigate = { onNavigateToIntervalPreview(intervalProgram.id) },
                    onLongClick = onLongClick
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InactiveTaskContent(
    task: TodoTask,
    exerciseMap: Map<Long, Exercise>,
    groupMap: Map<Long, ExerciseGroup>,
    groupExercisesMap: Map<Long, List<Exercise>>,
    groupExerciseCompletions: Map<Long, Set<Long>>,
    programMap: Map<Long, Program>,
    intervalProgramMap: Map<Long, IntervalProgram>,
    intervalExerciseCounts: Map<Long, Int>,
    onLongClick: () -> Unit
) {
    val appColors = LocalAppColors.current
    when (task.type) {
            TodoTask.TYPE_EXERCISE -> {
                val exercise = exerciseMap[task.referenceId]
                if (exercise != null) {
                    ExerciseTaskCard(
                        exercise = exercise,
                        task = task,
                        isDragging = false,
                        elevation = 0.dp,
                        dragHandleModifier = Modifier,
                        onStart = {},
                        onLongClick = onLongClick,
                        showStartButton = false
                    )
                }
            }
            TodoTask.TYPE_GROUP -> {
                val group = groupMap[task.referenceId]
                if (group != null) {
                    GroupTaskCard(
                        group = group,
                        groupExercises = groupExercisesMap[group.id] ?: emptyList(),
                        completedExerciseIds = groupExerciseCompletions[group.id] ?: emptySet(),
                        repeatDays = task.repeatDays,
                        isDragging = false,
                        elevation = 0.dp,
                        dragHandleModifier = Modifier,
                        onNavigateToRecord = {},
                        onNavigateToWorkout = {},
                        onLongClick = onLongClick,
                        showStartButton = false
                    )
                }
            }
            TodoTask.TYPE_PROGRAM -> {
                val program = programMap[task.referenceId]
                if (program != null) {
                    ProgramTaskCard(
                        program = program,
                        repeatDays = task.repeatDays,
                        isDragging = false,
                        elevation = 0.dp,
                        dragHandleModifier = Modifier,
                        onNavigate = {},
                        onLongClick = onLongClick,
                        showStartButton = false
                    )
                }
            }
            TodoTask.TYPE_INTERVAL -> {
                val intervalProgram = intervalProgramMap[task.referenceId]
                if (intervalProgram != null) {
                    IntervalTaskCard(
                        intervalProgram = intervalProgram,
                        exerciseCount = intervalExerciseCounts[intervalProgram.id] ?: 0,
                        repeatDays = task.repeatDays,
                        isDragging = false,
                        elevation = 0.dp,
                        dragHandleModifier = Modifier,
                        onNavigate = {},
                        onLongClick = onLongClick,
                        showStartButton = false
                    )
                }
            }
        }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupTaskCard(
    group: ExerciseGroup,
    groupExercises: List<Exercise>,
    completedExerciseIds: Set<Long>,
    repeatDays: String,
    isDragging: Boolean,
    elevation: androidx.compose.ui.unit.Dp,
    dragHandleModifier: Modifier,
    onNavigateToRecord: (Long) -> Unit,
    onNavigateToWorkout: (Long) -> Unit,
    onLongClick: () -> Unit,
    showStartButton: Boolean = true
) {
    val appColors = LocalAppColors.current
    var isExpanded by remember { mutableStateOf(true) }
    val completedCount = completedExerciseIds.size
    val totalCount = groupExercises.size

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) appColors.cardBackgroundSecondary.copy(alpha = 0.9f) else appColors.cardBackground
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(modifier = Modifier.alpha(if (showStartButton) 1f else 0.4f)) {
            // グループヘッダー（既存カードと同じレイアウト）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ドラッグハンドル
                Icon(
                    Icons.Default.Menu,
                    contentDescription = stringResource(R.string.todo_drag_to_reorder),
                    tint = if (isDragging) appColors.textPrimary else appColors.textSecondary,
                    modifier = Modifier
                        .size(24.dp)
                        .then(dragHandleModifier)
                )

                // グループ情報
                Column(modifier = Modifier.weight(1f).combinedClickable(onClick = { isExpanded = !isExpanded }, onLongClick = onLongClick)) {
                    Text(
                        text = group.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = appColors.textPrimary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.todo_tab_groups),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Blue600
                        )
                        Text(
                            text = "$completedCount/$totalCount",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (completedCount == totalCount && totalCount > 0) Green600 else appColors.textSecondary
                        )
                    }
                    RepeatDaysLabel(repeatDays = repeatDays)
                }

                // 展開ボタン
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp
                            else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = appColors.textSecondary
                    )
                }
            }

            // 展開時: 所属種目リスト
            if (isExpanded && groupExercises.isNotEmpty()) {
                HorizontalDivider(color = appColors.border)
                Column(
                    modifier = Modifier.padding(start = 48.dp, end = 12.dp, top = 4.dp, bottom = 8.dp)
                ) {
                    groupExercises.forEach { exercise ->
                        val isCompleted = exercise.id in completedExerciseIds
                        var showModeDialog by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .let { mod ->
                                    if (showStartButton && !isCompleted) {
                                        mod.clickable { showModeDialog = true }
                                    } else mod
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = exercise.name,
                                fontSize = 14.sp,
                                color = if (isCompleted) appColors.textSecondary else appColors.textPrimary,
                                modifier = Modifier
                                    .weight(1f)
                                    .alpha(if (isCompleted) 0.4f else 1f)
                            )
                            if (isCompleted) {
                                Text(
                                    text = "\u2713",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Green600.copy(alpha = 0.6f)
                                )
                            } else if (showStartButton) {
                                Button(
                                    onClick = { showModeDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Amber500),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(text = stringResource(R.string.todo_start_button), fontSize = 12.sp, color = appColors.textPrimary)
                                }
                            }
                        }

                        if (showModeDialog) {
                            AlertDialog(
                                onDismissRequest = { showModeDialog = false },
                                containerColor = appColors.cardBackground,
                                title = {
                                    Text(
                                        text = exercise.name,
                                        color = appColors.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                text = {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                showModeDialog = false
                                                onNavigateToRecord(exercise.id)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Green600),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().height(48.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.todo_mode_record),
                                                fontSize = 16.sp,
                                                color = appColors.textPrimary
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                showModeDialog = false
                                                onNavigateToWorkout(exercise.id)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Orange600),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().height(48.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.todo_mode_workout),
                                                fontSize = 16.sp,
                                                color = appColors.textPrimary
                                            )
                                        }
                                    }
                                },
                                confirmButton = {},
                                dismissButton = {
                                    TextButton(onClick = { showModeDialog = false }) {
                                        Text(
                                            text = stringResource(R.string.cancel),
                                            color = appColors.textSecondary
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExerciseTaskCard(
    exercise: Exercise,
    task: TodoTask,
    isDragging: Boolean,
    elevation: androidx.compose.ui.unit.Dp,
    dragHandleModifier: Modifier,
    onStart: () -> Unit,
    onLongClick: () -> Unit,
    showStartButton: Boolean = true
) {
    val appColors = LocalAppColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) appColors.cardBackgroundSecondary.copy(alpha = 0.9f) else appColors.cardBackground
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .alpha(if (showStartButton) 1f else 0.4f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Drag handle
            Icon(
                Icons.Default.Menu,
                contentDescription = stringResource(R.string.todo_drag_to_reorder),
                tint = if (isDragging) appColors.textPrimary else appColors.textSecondary,
                modifier = Modifier
                    .size(24.dp)
                    .then(dragHandleModifier)
            )

            // Exercise info
            Column(modifier = Modifier.weight(1f).combinedClickable(onClick = {}, onLongClick = onLongClick)) {
                Text(
                    text = exercise.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = appColors.textPrimary
                )
                // Badges row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    if (exercise.isFavorite) {
                        Text(text = "★", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                    }
                    if (exercise.targetSets != null && exercise.targetValue != null && exercise.sortOrder > 0) {
                        Text(text = "Lv.${exercise.sortOrder}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Blue600)
                    }
                    Text(
                        text = stringResource(if (exercise.type == "Dynamic") R.string.dynamic_type else R.string.isometric_type),
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, color = appColors.textSecondary
                    )
                    if (exercise.laterality == "Unilateral") {
                        Text(text = stringResource(R.string.one_sided), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Purple600)
                    }
                }
                if (exercise.targetSets != null && exercise.targetValue != null) {
                    Text(
                        text = stringResource(
                            if (exercise.laterality == "Unilateral") R.string.target_format_unilateral else R.string.target_format,
                            exercise.targetSets!!,
                            exercise.targetValue!!,
                            stringResource(if (exercise.type == "Dynamic") R.string.unit_reps else R.string.unit_seconds)
                        ),
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Green400,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                RepeatDaysLabel(repeatDays = task.repeatDays)
            }

            // Start button
            if (showStartButton) {
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(containerColor = Amber500),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(text = stringResource(R.string.todo_start_button), fontSize = 12.sp, color = appColors.textPrimary)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProgramTaskCard(
    program: Program,
    repeatDays: String,
    isDragging: Boolean,
    elevation: androidx.compose.ui.unit.Dp,
    dragHandleModifier: Modifier,
    onNavigate: () -> Unit,
    onLongClick: () -> Unit,
    showStartButton: Boolean = true
) {
    val appColors = LocalAppColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) appColors.cardBackgroundSecondary.copy(alpha = 0.9f) else appColors.cardBackground
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .alpha(if (showStartButton) 1f else 0.4f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Drag handle
            Icon(
                Icons.Default.Menu,
                contentDescription = stringResource(R.string.todo_drag_to_reorder),
                tint = if (isDragging) appColors.textPrimary else appColors.textSecondary,
                modifier = Modifier
                    .size(24.dp)
                    .then(dragHandleModifier)
            )

            // Program info
            Column(modifier = Modifier.weight(1f).combinedClickable(onClick = {}, onLongClick = onLongClick)) {
                Text(
                    text = program.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = appColors.textPrimary
                )
                Text(
                    text = stringResource(R.string.todo_tab_programs),
                    fontSize = 11.sp,
                    color = Orange600,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp)
                )
                RepeatDaysLabel(repeatDays = repeatDays)
            }

            // Start button
            if (showStartButton) {
                Button(
                    onClick = onNavigate,
                    colors = ButtonDefaults.buttonColors(containerColor = Amber500),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(text = stringResource(R.string.todo_start_button), fontSize = 12.sp, color = appColors.textPrimary)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IntervalTaskCard(
    intervalProgram: IntervalProgram,
    exerciseCount: Int,
    repeatDays: String,
    isDragging: Boolean,
    elevation: androidx.compose.ui.unit.Dp,
    dragHandleModifier: Modifier,
    onNavigate: () -> Unit,
    onLongClick: () -> Unit,
    showStartButton: Boolean = true
) {
    val appColors = LocalAppColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) appColors.cardBackgroundSecondary.copy(alpha = 0.9f) else appColors.cardBackground
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .alpha(if (showStartButton) 1f else 0.4f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Drag handle
            Icon(
                Icons.Default.Menu,
                contentDescription = stringResource(R.string.todo_drag_to_reorder),
                tint = if (isDragging) appColors.textPrimary else appColors.textSecondary,
                modifier = Modifier
                    .size(24.dp)
                    .then(dragHandleModifier)
            )

            // Interval program info
            Column(modifier = Modifier.weight(1f).combinedClickable(onClick = {}, onLongClick = onLongClick)) {
                Text(
                    text = intervalProgram.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = appColors.textPrimary
                )
                Text(
                    text = stringResource(
                        R.string.interval_summary_format,
                        exerciseCount,
                        intervalProgram.workSeconds,
                        intervalProgram.restSeconds,
                        intervalProgram.rounds
                    ),
                    fontSize = 12.sp,
                    color = appColors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
                RepeatDaysLabel(repeatDays = repeatDays)
            }

            // Start button
            if (showStartButton) {
                Button(
                    onClick = onNavigate,
                    colors = ButtonDefaults.buttonColors(containerColor = Amber500),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(text = stringResource(R.string.todo_start_button), fontSize = 12.sp, color = appColors.textPrimary)
                }
            }
        }
    }
}

@Composable
private fun AddItemsDialog(
    viewModel: TrainingViewModel,
    todoTasks: List<TodoTask>,
    exercises: List<Exercise>,
    groups: List<ExerciseGroup>,
    programs: List<Program>,
    intervalPrograms: List<IntervalProgram>,
    intervalExerciseCounts: Map<Long, Int>,
    onDismiss: () -> Unit
) {
    val appColors = LocalAppColors.current
    val tabTitles = listOf(
        stringResource(R.string.todo_tab_exercises),
        stringResource(R.string.todo_tab_groups),
        stringResource(R.string.todo_tab_programs),
        stringResource(R.string.todo_tab_intervals)
    )

    // Existing IDs by type
    val existingExerciseIds = remember(todoTasks) {
        todoTasks.filter { it.type == TodoTask.TYPE_EXERCISE }.map { it.referenceId }.toSet()
    }
    val existingGroupIds = remember(todoTasks) {
        todoTasks.filter { it.type == TodoTask.TYPE_GROUP }.map { it.referenceId }.toSet()
    }
    val existingProgramIds = remember(todoTasks) {
        todoTasks.filter { it.type == TodoTask.TYPE_PROGRAM }.map { it.referenceId }.toSet()
    }
    val existingIntervalIds = remember(todoTasks) {
        todoTasks.filter { it.type == TodoTask.TYPE_INTERVAL }.map { it.referenceId }.toSet()
    }

    // Selection state
    var selectedExercises by remember { mutableStateOf(setOf<Long>()) }
    var selectedGroups by remember { mutableStateOf(setOf<Long>()) }
    var selectedPrograms by remember { mutableStateOf(setOf<Long>()) }
    var selectedIntervals by remember { mutableStateOf(setOf<Long>()) }

    val hasSelection = selectedExercises.isNotEmpty() || selectedGroups.isNotEmpty() || selectedPrograms.isNotEmpty() || selectedIntervals.isNotEmpty()

    val pagerState = rememberPagerState(pageCount = { tabTitles.size })
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(appColors.cardBackground)
                .systemBarsPadding()
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.cancel), color = appColors.textSecondary)
                }
                Text(
                    text = stringResource(R.string.todo_add_items),
                    color = appColors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                TextButton(
                    onClick = {
                        if (selectedExercises.isNotEmpty()) {
                            viewModel.addTodoTasks(selectedExercises.toList())
                        }
                        if (selectedGroups.isNotEmpty()) {
                            viewModel.addTodoTaskGroups(selectedGroups.toList())
                        }
                        if (selectedPrograms.isNotEmpty()) {
                            viewModel.addTodoTaskPrograms(selectedPrograms.toList())
                        }
                        if (selectedIntervals.isNotEmpty()) {
                            viewModel.addTodoTaskIntervals(selectedIntervals.toList())
                        }
                        onDismiss()
                    },
                    enabled = hasSelection
                ) {
                    Text(
                        text = stringResource(R.string.add),
                        color = if (hasSelection) Amber500 else appColors.textSecondary
                    )
                }
            }

            // Tabs
            BoxWithConstraints {
                val minTabWidth = maxWidth / tabTitles.size
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = appColors.cardBackground,
                    contentColor = Amber500,
                    edgePadding = 0.dp
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                            modifier = Modifier.widthIn(min = minTabWidth),
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    color = if (pagerState.currentPage == index) Amber500 else appColors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tab content（スワイプ対応）
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> ExercisesTabContent(
                        viewModel = viewModel,
                        exercises = exercises,
                        existingIds = existingExerciseIds,
                        selectedIds = selectedExercises,
                        onToggle = { id ->
                            selectedExercises = if (id in selectedExercises) selectedExercises - id else selectedExercises + id
                        }
                    )
                    1 -> GroupsTabContent(
                        groups = groups,
                        exercises = exercises,
                        existingIds = existingGroupIds,
                        selectedIds = selectedGroups,
                        onToggle = { id ->
                            selectedGroups = if (id in selectedGroups) selectedGroups - id else selectedGroups + id
                        }
                    )
                    2 -> ProgramsTabContent(
                        programs = programs,
                        existingIds = existingProgramIds,
                        selectedIds = selectedPrograms,
                        onToggle = { id ->
                            selectedPrograms = if (id in selectedPrograms) selectedPrograms - id else selectedPrograms + id
                        }
                    )
                    3 -> IntervalsTabContent(
                        intervalPrograms = intervalPrograms,
                        existingIds = existingIntervalIds,
                        selectedIds = selectedIntervals,
                        exerciseCounts = intervalExerciseCounts,
                        onToggle = { id ->
                            selectedIntervals = if (id in selectedIntervals) selectedIntervals - id else selectedIntervals + id
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExercisesTabContent(
    viewModel: TrainingViewModel,
    exercises: List<Exercise>,
    existingIds: Set<Long>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit
) {
    val appColors = LocalAppColors.current
    val hierarchicalData by viewModel.hierarchicalExercises.collectAsState()
    val expandedGroups by viewModel.expandedGroups.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    val availableExercises = remember(exercises, existingIds) {
        exercises.filter { it.id !in existingIds }
    }

    val searchResults = remember(availableExercises, searchQuery) {
        SearchUtils.searchExercises(availableExercises, searchQuery)
    }

    val listState = rememberLazyListState()

    LaunchedEffect(searchQuery, searchResults) {
        if (searchQuery.isNotBlank() && searchResults.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    if (availableExercises.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
            Text(
                text = stringResource(R.string.todo_all_added),
                color = appColors.textSecondary,
                modifier = Modifier.padding(16.dp)
            )
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                placeholder = {
                    Text(text = stringResource(R.string.search_placeholder), color = appColors.textSecondary)
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = appColors.textSecondary)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear), tint = appColors.textSecondary)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = appColors.textPrimary,
                    unfocusedTextColor = appColors.textPrimary,
                    focusedContainerColor = appColors.cardBackgroundSecondary,
                    unfocusedContainerColor = appColors.cardBackgroundSecondary,
                    focusedBorderColor = Amber500,
                    unfocusedBorderColor = appColors.border,
                    cursorColor = Amber500
                ),
                shape = RoundedCornerShape(8.dp)
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                if (searchQuery.isNotBlank()) {
                    if (searchResults.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.no_results),
                                color = appColors.textSecondary,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        items(
                            count = searchResults.size,
                            key = { index -> searchResults[index].id }
                        ) { index ->
                            val exercise = searchResults[index]
                            SearchResultExerciseItem(
                                exercise = exercise,
                                isSelected = exercise.id in selectedIds,
                                onToggle = onToggle
                            )
                        }
                    }
                } else {
                    items(
                        count = hierarchicalData.size,
                        key = { index -> hierarchicalData[index].groupName ?: "ungrouped" }
                    ) { index ->
                        val group = hierarchicalData[index]
                        val availableInGroup = group.exercises.filter { it.id !in existingIds }
                        if (availableInGroup.isNotEmpty()) {
                            AddExerciseGroup(
                                groupName = group.groupName,
                                exercises = availableInGroup,
                                selectedExercises = selectedIds,
                                isExpanded = (group.groupName ?: "ungrouped") in expandedGroups,
                                onExpandToggle = {
                                    viewModel.toggleGroupExpansion(group.groupName ?: "ungrouped")
                                },
                                onExerciseToggle = onToggle,
                                onGroupToggle = { ids ->
                                    val allSelected = ids.all { it in selectedIds }
                                    ids.forEach { id ->
                                        if (allSelected || id !in selectedIds) {
                                            onToggle(id)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupsTabContent(
    groups: List<ExerciseGroup>,
    exercises: List<Exercise>,
    existingIds: Set<Long>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit
) {
    val appColors = LocalAppColors.current
    val availableGroups = remember(groups, existingIds) {
        groups.filter { it.id !in existingIds }
    }

    // グループごとの所属種目を事前計算
    val groupExercisesMap = remember(groups, exercises) {
        groups.associate { group ->
            group.id to exercises.filter { it.group == group.name }.sortedBy { it.displayOrder }
        }
    }

    var expandedGroupIds by remember { mutableStateOf(setOf<Long>()) }

    if (availableGroups.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
            Text(
                text = stringResource(R.string.todo_no_groups),
                color = appColors.textSecondary,
                modifier = Modifier.padding(16.dp)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(
                items = availableGroups,
                key = { it.id }
            ) { group ->
                val groupExercises = groupExercisesMap[group.id] ?: emptyList()
                val isExpanded = group.id in expandedGroupIds

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = appColors.cardBackgroundSecondary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedGroupIds = if (isExpanded) expandedGroupIds - group.id
                                        else expandedGroupIds + group.id
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = group.id in selectedIds,
                                onCheckedChange = { onToggle(group.id) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Amber500,
                                    uncheckedColor = appColors.textSecondary
                                )
                            )
                            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                                Text(
                                    text = group.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = appColors.textPrimary
                                )
                                Text(
                                    text = stringResource(R.string.todo_group_exercise_count, groupExercises.size),
                                    fontSize = 11.sp,
                                    color = appColors.textSecondary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp
                                    else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = appColors.textSecondary
                            )
                        }

                        // 展開時: 所属種目のプレビュー
                        if (isExpanded && groupExercises.isNotEmpty()) {
                            HorizontalDivider(color = appColors.border)
                            Column(
                                modifier = Modifier.padding(start = 48.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
                            ) {
                                groupExercises.forEach { exercise ->
                                    Text(
                                        text = exercise.name,
                                        fontSize = 12.sp,
                                        color = appColors.textSecondary,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgramsTabContent(
    programs: List<Program>,
    existingIds: Set<Long>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit
) {
    val appColors = LocalAppColors.current
    val availablePrograms = remember(programs, existingIds) {
        programs.filter { it.id !in existingIds }
    }

    if (availablePrograms.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
            Text(
                text = stringResource(R.string.todo_no_programs),
                color = appColors.textSecondary,
                modifier = Modifier.padding(16.dp)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(
                items = availablePrograms,
                key = { it.id }
            ) { program ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = appColors.cardBackgroundSecondary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = program.id in selectedIds,
                            onCheckedChange = { onToggle(program.id) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Amber500,
                                uncheckedColor = appColors.textSecondary
                            )
                        )
                        Text(
                            text = program.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.textPrimary,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IntervalsTabContent(
    intervalPrograms: List<IntervalProgram>,
    existingIds: Set<Long>,
    selectedIds: Set<Long>,
    exerciseCounts: Map<Long, Int>,
    onToggle: (Long) -> Unit
) {
    val appColors = LocalAppColors.current
    val availableIntervals = remember(intervalPrograms, existingIds) {
        intervalPrograms.filter { it.id !in existingIds }
    }

    if (availableIntervals.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
            Text(
                text = stringResource(R.string.todo_no_intervals),
                color = appColors.textSecondary,
                modifier = Modifier.padding(16.dp)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(
                items = availableIntervals,
                key = { it.id }
            ) { program ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = appColors.cardBackgroundSecondary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = program.id in selectedIds,
                            onCheckedChange = { onToggle(program.id) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Amber500,
                                uncheckedColor = appColors.textSecondary
                            )
                        )
                        Column(modifier = Modifier.padding(start = 4.dp)) {
                            Text(
                                text = program.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.textPrimary
                            )
                            Text(
                                text = stringResource(
                                    R.string.interval_summary_format,
                                    exerciseCounts[program.id] ?: 0,
                                    program.workSeconds,
                                    program.restSeconds,
                                    program.rounds
                                ),
                                fontSize = 11.sp,
                                color = appColors.textSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddExerciseGroup(
    groupName: String?,
    exercises: List<Exercise>,
    selectedExercises: Set<Long>,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onExerciseToggle: (Long) -> Unit,
    onGroupToggle: (List<Long>) -> Unit = {}
) {
    val appColors = LocalAppColors.current
    val exerciseIds = exercises.map { it.id }
    val allSelected = exerciseIds.isNotEmpty() && exerciseIds.all { it in selectedExercises }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = appColors.cardBackgroundSecondary),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            // Group header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent,
                onClick = onExpandToggle
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isExpanded)
                                Icons.Default.KeyboardArrowDown
                            else
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = appColors.textPrimary
                        )
                        Text(
                            text = groupName ?: stringResource(R.string.no_group),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.textPrimary
                        )
                        Text(
                            text = "(${exercises.size})",
                            fontSize = 14.sp,
                            color = appColors.textSecondary
                        )
                    }
                    Checkbox(
                        checked = allSelected,
                        onCheckedChange = { onGroupToggle(exerciseIds) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Amber500,
                            uncheckedColor = appColors.textSecondary
                        )
                    )
                }
            }

            // Exercise list
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(start = 8.dp, end = 16.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    exercises.forEach { exercise ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Checkbox(
                                checked = exercise.id in selectedExercises,
                                onCheckedChange = { onExerciseToggle(exercise.id) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Amber500,
                                    uncheckedColor = appColors.textSecondary
                                )
                            )
                            Column(modifier = Modifier.padding(start = 4.dp, top = 10.dp)) {
                                Text(
                                    text = exercise.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = appColors.textPrimary
                                )
                                // Badges row
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    if (exercise.isFavorite) {
                                        Text(text = "★", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                                    }
                                    if (exercise.targetSets != null && exercise.targetValue != null && exercise.sortOrder > 0) {
                                        Text(text = "Lv.${exercise.sortOrder}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Blue600)
                                    }
                                    Text(
                                        text = stringResource(if (exercise.type == "Dynamic") R.string.dynamic_type else R.string.isometric_type),
                                        fontSize = 10.sp, fontWeight = FontWeight.Bold, color = appColors.textSecondary
                                    )
                                    if (exercise.laterality == "Unilateral") {
                                        Text(text = stringResource(R.string.one_sided), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Purple600)
                                    }
                                }
                                if (exercise.targetSets != null && exercise.targetValue != null) {
                                    Text(
                                        text = stringResource(
                                            if (exercise.laterality == "Unilateral") R.string.target_format_unilateral else R.string.target_format,
                                            exercise.targetSets!!,
                                            exercise.targetValue!!,
                                            stringResource(if (exercise.type == "Dynamic") R.string.unit_reps else R.string.unit_seconds)
                                        ),
                                        fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Green400,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultExerciseItem(
    exercise: Exercise,
    isSelected: Boolean,
    onToggle: (Long) -> Unit
) {
    val appColors = LocalAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = appColors.cardBackgroundSecondary),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle(exercise.id) },
                colors = CheckboxDefaults.colors(
                    checkedColor = Amber500,
                    uncheckedColor = appColors.textSecondary
                )
            )
            Column(modifier = Modifier.padding(start = 4.dp, top = 10.dp)) {
                Text(
                    text = exercise.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = appColors.textPrimary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    if (exercise.isFavorite) {
                        Text(text = "★", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                    }
                    if (exercise.targetSets != null && exercise.targetValue != null && exercise.sortOrder > 0) {
                        Text(text = "Lv.${exercise.sortOrder}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Blue600)
                    }
                    Text(
                        text = stringResource(if (exercise.type == "Dynamic") R.string.dynamic_type else R.string.isometric_type),
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, color = appColors.textSecondary
                    )
                    if (exercise.laterality == "Unilateral") {
                        Text(text = stringResource(R.string.one_sided), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Purple600)
                    }
                }
                if (exercise.targetSets != null && exercise.targetValue != null) {
                    Text(
                        text = stringResource(
                            if (exercise.laterality == "Unilateral") R.string.target_format_unilateral else R.string.target_format,
                            exercise.targetSets!!,
                            exercise.targetValue!!,
                            stringResource(if (exercise.type == "Dynamic") R.string.unit_reps else R.string.unit_seconds)
                        ),
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Green400,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RepeatDaysLabel(repeatDays: String) {
    if (repeatDays.isEmpty()) return
    val dayNumbers = repeatDays.split(",").map { it.trim().toInt() }
    val locale = java.util.Locale.getDefault()
    val dayNames = dayNumbers.map { dayNum ->
        java.time.DayOfWeek.of(dayNum).getDisplayName(java.time.format.TextStyle.SHORT, locale)
    }
    Text(
        text = dayNames.joinToString(" "),
        fontSize = 10.sp,
        color = Amber500,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 2.dp)
    )
}

@Composable
private fun RepeatDaysDialog(
    currentRepeatDays: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val appColors = LocalAppColors.current
    val locale = java.util.Locale.getDefault()
    var selectedDays by remember(currentRepeatDays) {
        val initial = if (currentRepeatDays.isEmpty()) emptySet()
        else currentRepeatDays.split(",").map { it.trim().toInt() }.toSet()
        mutableStateOf(initial)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = appColors.cardBackground,
        title = {
            Text(
                text = stringResource(R.string.todo_repeat_title),
                color = appColors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                (1..7).forEach { dayNum ->
                    val dayOfWeek = java.time.DayOfWeek.of(dayNum)
                    val dayName = dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, locale)
                    val isSelected = dayNum in selectedDays

                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Amber500 else appColors.cardBackgroundSecondary,
                        onClick = {
                            selectedDays = if (isSelected) selectedDays - dayNum else selectedDays + dayNum
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = dayName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else appColors.textSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val result = selectedDays.sorted().joinToString(",")
                onSave(result)
            }) {
                Text(stringResource(R.string.todo_repeat_save), color = Amber500)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onSave("")
            }) {
                Text(stringResource(R.string.todo_repeat_clear), color = appColors.textSecondary)
            }
        }
    )
}
