package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.stringResource
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.TodoTask
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import sh.calvin.reorderable.ReorderableColumn

@Composable
fun ToDoScreen(
    viewModel: TrainingViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToRecord: (Long) -> Unit,
    onNavigateToWorkout: (Long) -> Unit
) {
    val todoTasks by viewModel.todoTasks.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    // Map exerciseId to Exercise for display
    val exerciseMap = remember(exercises) {
        exercises.associateBy { it.id }
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
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add), tint = Color.White)
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
                    color = Slate400,
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
                val exercise = exerciseMap[task.exerciseId]
                if (exercise != null) {
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
                                            tint = Color.White
                                        )
                                    }
                                },
                                enableDismissFromStartToEnd = false,
                                enableDismissFromEndToStart = true
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDragging) Slate700.copy(alpha = 0.9f) else Slate800
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
                                            tint = if (isDragging) Color.White else Slate400,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .longPressDraggableHandle()
                                        )

                                        // Exercise info
                                        Column(modifier = Modifier.weight(1f)) {
                                            // Exercise name
                                            Text(
                                                text = exercise.name,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.White
                                            )

                                            // Badges row
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                // Favorite
                                                if (exercise.isFavorite) {
                                                    Text(
                                                        text = "★",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFFFD700)
                                                    )
                                                }

                                                // Level
                                                if (exercise.targetSets != null && exercise.targetValue != null && exercise.sortOrder > 0) {
                                                    Text(
                                                        text = "Lv.${exercise.sortOrder}",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Blue600
                                                    )
                                                }

                                                // Type
                                                Text(
                                                    text = stringResource(if (exercise.type == "Dynamic") R.string.dynamic_type else R.string.isometric_type),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Slate400
                                                )

                                                // Unilateral
                                                if (exercise.laterality == "Unilateral") {
                                                    Text(
                                                        text = stringResource(R.string.one_sided),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Purple600
                                                    )
                                                }
                                            }

                                            // Target (separate row)
                                            if (exercise.targetSets != null && exercise.targetValue != null) {
                                                Text(
                                                    text = stringResource(
                                                        if (exercise.laterality == "Unilateral") R.string.target_format_unilateral else R.string.target_format,
                                                        exercise.targetSets!!,
                                                        exercise.targetValue!!,
                                                        stringResource(if (exercise.type == "Dynamic") R.string.unit_reps else R.string.unit_seconds)
                                                    ),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Green400,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                            }
                                        }

                                        // Record button
                                        Button(
                                            onClick = {
                                                onNavigateToRecord(task.exerciseId)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Green600),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.todo_rec_button),
                                                fontSize = 12.sp,
                                                color = Color.White
                                            )
                                        }

                                        // Workout button
                                        Button(
                                            onClick = {
                                                onNavigateToWorkout(task.exerciseId)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Orange600),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.todo_wo_button),
                                                fontSize = 12.sp,
                                                color = Color.White
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
    }

    // Add exercises dialog
    if (showAddDialog) {
        AddExercisesDialog(
            exercises = exercises,
            existingTaskExerciseIds = todoTasks.map { it.exerciseId }.toSet(),
            onDismiss = { showAddDialog = false },
            onAdd = { selectedExercises ->
                viewModel.addTodoTasks(selectedExercises.map { it.id })
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddExercisesDialog(
    exercises: List<Exercise>,
    existingTaskExerciseIds: Set<Long>,
    onDismiss: () -> Unit,
    onAdd: (List<Exercise>) -> Unit
) {
    val viewModel: TrainingViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val hierarchicalData by viewModel.hierarchicalExercises.collectAsState()
    val expandedGroups by viewModel.expandedGroups.collectAsState()

    // Track selected exercises
    var selectedExercises by remember { mutableStateOf(setOf<Long>()) }

    // Filter out already added exercises
    val availableExercises = remember(exercises, existingTaskExerciseIds) {
        exercises.filter { it.id !in existingTaskExerciseIds }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate800,
        title = {
            Text(
                text = stringResource(R.string.todo_add_exercises),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (availableExercises.isEmpty()) {
                Text(
                    text = stringResource(R.string.todo_all_added),
                    color = Slate400
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.7f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        count = hierarchicalData.size,
                        key = { index -> hierarchicalData[index].groupName ?: "ungrouped" }
                    ) { index ->
                        val group = hierarchicalData[index]
                        // Filter exercises in this group
                        val availableInGroup = group.exercises.filter { it.id !in existingTaskExerciseIds }
                        if (availableInGroup.isNotEmpty()) {
                            AddExerciseGroup(
                                groupName = group.groupName,
                                exercises = availableInGroup,
                                selectedExercises = selectedExercises,
                                isExpanded = (group.groupName ?: "ungrouped") in expandedGroups,
                                onExpandToggle = {
                                    viewModel.toggleGroupExpansion(group.groupName ?: "ungrouped")
                                },
                                onExerciseToggle = { exerciseId ->
                                    selectedExercises = if (exerciseId in selectedExercises) {
                                        selectedExercises - exerciseId
                                    } else {
                                        selectedExercises + exerciseId
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val selected = availableExercises.filter { it.id in selectedExercises }
                    onAdd(selected)
                },
                enabled = selectedExercises.isNotEmpty()
            ) {
                Text(
                    text = stringResource(R.string.add),
                    color = if (selectedExercises.isNotEmpty()) Amber500 else Slate400
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel), color = Slate400)
            }
        }
    )
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate700),
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
                            tint = Color.White
                        )
                        Text(
                            text = groupName ?: stringResource(R.string.no_group),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "(${exercises.size})",
                            fontSize = 14.sp,
                            color = Slate400
                        )
                    }
                }
            }

            // Exercise list
            androidx.compose.animation.AnimatedVisibility(
                visible = isExpanded,
                enter = androidx.compose.animation.expandVertically(),
                exit = androidx.compose.animation.shrinkVertically()
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
                                    uncheckedColor = Slate400
                                )
                            )
                            Column(modifier = Modifier.padding(start = 4.dp, top = 10.dp)) {
                                Text(
                                    text = exercise.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                // Badges row
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    // Favorite
                                    if (exercise.isFavorite) {
                                        Text(
                                            text = "★",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFD700)
                                        )
                                    }

                                    // Level
                                    if (exercise.targetSets != null && exercise.targetValue != null && exercise.sortOrder > 0) {
                                        Text(
                                            text = "Lv.${exercise.sortOrder}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Blue600
                                        )
                                    }

                                    // Type
                                    Text(
                                        text = stringResource(if (exercise.type == "Dynamic") R.string.dynamic_type else R.string.isometric_type),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate400
                                    )

                                    // Unilateral
                                    if (exercise.laterality == "Unilateral") {
                                        Text(
                                            text = stringResource(R.string.one_sided),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Purple600
                                        )
                                    }
                                }
                                // Target
                                if (exercise.targetSets != null && exercise.targetValue != null) {
                                    Text(
                                        text = stringResource(
                                            if (exercise.laterality == "Unilateral") R.string.target_format_unilateral else R.string.target_format,
                                            exercise.targetSets!!,
                                            exercise.targetValue!!,
                                            stringResource(if (exercise.type == "Dynamic") R.string.unit_reps else R.string.unit_seconds)
                                        ),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Green400,
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