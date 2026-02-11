package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.Exercise
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
    val programs by viewModel.programs.collectAsState()
    val intervalPrograms by viewModel.intervalPrograms.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    // Map IDs to objects for display
    val exerciseMap = remember(exercises) {
        exercises.associateBy { it.id }
    }
    val programMap = remember(programs) {
        programs.associateBy { it.id }
    }
    val intervalProgramMap = remember(intervalPrograms) {
        intervalPrograms.associateBy { it.id }
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
            // Task list with drag-and-drop reordering
            val scrollState = rememberScrollState()
            ReorderableColumn(
                list = todoTasks,
                onSettle = { fromIndex, toIndex ->
                    viewModel.reorderTodoTasks(fromIndex, toIndex)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(scrollState),
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
                                    viewModel.deleteTodoTask(task.id)
                                    true
                                } else {
                                    false
                                }
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
                            when (task.type) {
                                TodoTask.TYPE_EXERCISE -> {
                                    val exercise = exerciseMap[task.referenceId]
                                    if (exercise != null) {
                                        ExerciseTaskCard(
                                            exercise = exercise,
                                            task = task,
                                            isDragging = isDragging,
                                            elevation = elevation,
                                            dragHandleModifier = dragHandleModifier,
                                            onNavigateToRecord = onNavigateToRecord,
                                            onNavigateToWorkout = onNavigateToWorkout
                                        )
                                    }
                                }
                                TodoTask.TYPE_PROGRAM -> {
                                    val program = programMap[task.referenceId]
                                    if (program != null) {
                                        ProgramTaskCard(
                                            program = program,
                                            isDragging = isDragging,
                                            elevation = elevation,
                                            dragHandleModifier = dragHandleModifier,
                                            onNavigate = { onNavigateToProgramPreview(program.id) }
                                        )
                                    }
                                }
                                TodoTask.TYPE_INTERVAL -> {
                                    val intervalProgram = intervalProgramMap[task.referenceId]
                                    if (intervalProgram != null) {
                                        IntervalTaskCard(
                                            intervalProgram = intervalProgram,
                                            exerciseCount = intervalExerciseCounts[intervalProgram.id] ?: 0,
                                            isDragging = isDragging,
                                            elevation = elevation,
                                            dragHandleModifier = dragHandleModifier,
                                            onNavigate = { onNavigateToIntervalPreview(intervalProgram.id) }
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

    // Add items dialog (tabbed)
    if (showAddDialog) {
        AddItemsDialog(
            viewModel = viewModel,
            todoTasks = todoTasks,
            exercises = exercises,
            programs = programs,
            intervalPrograms = intervalPrograms,
            intervalExerciseCounts = intervalExerciseCounts,
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun ExerciseTaskCard(
    exercise: Exercise,
    task: TodoTask,
    isDragging: Boolean,
    elevation: androidx.compose.ui.unit.Dp,
    dragHandleModifier: Modifier,
    onNavigateToRecord: (Long) -> Unit,
    onNavigateToWorkout: (Long) -> Unit
) {
    val appColors = LocalAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) appColors.cardBackgroundSecondary.copy(alpha = 0.9f) else appColors.cardBackground
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
            Column(modifier = Modifier.weight(1f)) {
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
            }

            // Record button
            Button(
                onClick = { onNavigateToRecord(task.referenceId) },
                colors = ButtonDefaults.buttonColors(containerColor = Green600),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(text = stringResource(R.string.todo_rec_button), fontSize = 12.sp, color = appColors.textPrimary)
            }

            // Workout button
            Button(
                onClick = { onNavigateToWorkout(task.referenceId) },
                colors = ButtonDefaults.buttonColors(containerColor = Orange600),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(text = stringResource(R.string.todo_wo_button), fontSize = 12.sp, color = appColors.textPrimary)
            }
        }
    }
}

@Composable
private fun ProgramTaskCard(
    program: Program,
    isDragging: Boolean,
    elevation: androidx.compose.ui.unit.Dp,
    dragHandleModifier: Modifier,
    onNavigate: () -> Unit
) {
    val appColors = LocalAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) appColors.cardBackgroundSecondary.copy(alpha = 0.9f) else appColors.cardBackground
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
            Column(modifier = Modifier.weight(1f)) {
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
            }

            // Navigate button
            Button(
                onClick = onNavigate,
                colors = ButtonDefaults.buttonColors(containerColor = Orange600),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun IntervalTaskCard(
    intervalProgram: IntervalProgram,
    exerciseCount: Int,
    isDragging: Boolean,
    elevation: androidx.compose.ui.unit.Dp,
    dragHandleModifier: Modifier,
    onNavigate: () -> Unit
) {
    val appColors = LocalAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) appColors.cardBackgroundSecondary.copy(alpha = 0.9f) else appColors.cardBackground
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
            Column(modifier = Modifier.weight(1f)) {
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
            }

            // Navigate button
            Button(
                onClick = onNavigate,
                colors = ButtonDefaults.buttonColors(containerColor = Orange600),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AddItemsDialog(
    viewModel: TrainingViewModel,
    todoTasks: List<TodoTask>,
    exercises: List<Exercise>,
    programs: List<Program>,
    intervalPrograms: List<IntervalProgram>,
    intervalExerciseCounts: Map<Long, Int>,
    onDismiss: () -> Unit
) {
    val appColors = LocalAppColors.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf(
        stringResource(R.string.todo_tab_exercises),
        stringResource(R.string.todo_tab_programs),
        stringResource(R.string.todo_tab_intervals)
    )

    // Existing IDs by type
    val existingExerciseIds = remember(todoTasks) {
        todoTasks.filter { it.type == TodoTask.TYPE_EXERCISE }.map { it.referenceId }.toSet()
    }
    val existingProgramIds = remember(todoTasks) {
        todoTasks.filter { it.type == TodoTask.TYPE_PROGRAM }.map { it.referenceId }.toSet()
    }
    val existingIntervalIds = remember(todoTasks) {
        todoTasks.filter { it.type == TodoTask.TYPE_INTERVAL }.map { it.referenceId }.toSet()
    }

    // Selection state
    var selectedExercises by remember { mutableStateOf(setOf<Long>()) }
    var selectedPrograms by remember { mutableStateOf(setOf<Long>()) }
    var selectedIntervals by remember { mutableStateOf(setOf<Long>()) }

    val hasSelection = selectedExercises.isNotEmpty() || selectedPrograms.isNotEmpty() || selectedIntervals.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = appColors.cardBackground,
        title = {
            Text(
                text = stringResource(R.string.todo_add_items),
                color = appColors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
            ) {
                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = appColors.cardBackground,
                    contentColor = Amber500
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    color = if (selectedTab == index) Amber500 else appColors.textSecondary
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tab content
                when (selectedTab) {
                    0 -> ExercisesTabContent(
                        viewModel = viewModel,
                        exercises = exercises,
                        existingIds = existingExerciseIds,
                        selectedIds = selectedExercises,
                        onToggle = { id ->
                            selectedExercises = if (id in selectedExercises) selectedExercises - id else selectedExercises + id
                        }
                    )
                    1 -> ProgramsTabContent(
                        programs = programs,
                        existingIds = existingProgramIds,
                        selectedIds = selectedPrograms,
                        onToggle = { id ->
                            selectedPrograms = if (id in selectedPrograms) selectedPrograms - id else selectedPrograms + id
                        }
                    )
                    2 -> IntervalsTabContent(
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
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedExercises.isNotEmpty()) {
                        viewModel.addTodoTasks(selectedExercises.toList())
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
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel), color = appColors.textSecondary)
            }
        }
    )
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
        Text(
            text = stringResource(R.string.todo_all_added),
            color = appColors.textSecondary,
            modifier = Modifier.padding(16.dp)
        )
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                onExerciseToggle = onToggle
                            )
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
        Text(
            text = stringResource(R.string.todo_no_programs),
            color = appColors.textSecondary,
            modifier = Modifier.padding(16.dp)
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
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
        Text(
            text = stringResource(R.string.todo_no_intervals),
            color = appColors.textSecondary,
            modifier = Modifier.padding(16.dp)
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
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
    onExerciseToggle: (Long) -> Unit
) {
    val appColors = LocalAppColors.current
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
                        verticalAlignment = Alignment.CenterVertically
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
