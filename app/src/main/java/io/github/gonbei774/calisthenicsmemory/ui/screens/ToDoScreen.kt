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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
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
import io.github.gonbei774.calisthenicsmemory.util.SearchUtils
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import sh.calvin.reorderable.ReorderableColumn

@Composable
fun ToDoScreen(
    viewModel: TrainingViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToRecord: (Long) -> Unit,
    onNavigateToWorkout: (Long) -> Unit
) {
    val appColors = LocalAppColors.current
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
                                            tint = appColors.textPrimary
                                        )
                                    }
                                },
                                enableDismissFromStartToEnd = false,
                                enableDismissFromEndToStart = true
                            ) {
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
                                                .longPressDraggableHandle()
                                        )

                                        // Exercise info
                                        Column(modifier = Modifier.weight(1f)) {
                                            // Exercise name
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
                                                    color = appColors.textSecondary
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
                                                color = appColors.textPrimary
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
                                                color = appColors.textPrimary
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
    val appColors = LocalAppColors.current
    val viewModel: TrainingViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val hierarchicalData by viewModel.hierarchicalExercises.collectAsState()
    val expandedGroups by viewModel.expandedGroups.collectAsState()

    // Track selected exercises
    var selectedExercises by remember { mutableStateOf(setOf<Long>()) }

    // Search query state
    var searchQuery by remember { mutableStateOf("") }

    // Filter out already added exercises
    val availableExercises = remember(exercises, existingTaskExerciseIds) {
        exercises.filter { it.id !in existingTaskExerciseIds }
    }

    // Filter exercises by search query
    val searchResults = remember(availableExercises, searchQuery) {
        SearchUtils.searchExercises(availableExercises, searchQuery)
    }

    // List state for controlling scroll position
    val listState = rememberLazyListState()

    // Scroll to top when search results change
    LaunchedEffect(searchQuery, searchResults) {
        if (searchQuery.isNotBlank() && searchResults.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = appColors.cardBackground,
        title = {
            Text(
                text = stringResource(R.string.todo_add_exercises),
                color = appColors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (availableExercises.isEmpty()) {
                Text(
                    text = stringResource(R.string.todo_all_added),
                    color = appColors.textSecondary
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.7f)
                ) {
                    // Search field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search_placeholder),
                                color = appColors.textSecondary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = appColors.textSecondary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = stringResource(R.string.clear),
                                        tint = appColors.textSecondary
                                    )
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

                    // Exercise list
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (searchQuery.isNotBlank()) {
                            // Flat search results
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
                                        isSelected = exercise.id in selectedExercises,
                                        onToggle = { exerciseId ->
                                            selectedExercises = if (exerciseId in selectedExercises) {
                                                selectedExercises - exerciseId
                                            } else {
                                                selectedExercises + exerciseId
                                            }
                                        }
                                    )
                                }
                            }
                        } else {
                            // Hierarchical group view
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
                    color = if (selectedExercises.isNotEmpty()) Amber500 else appColors.textSecondary
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
                                        color = appColors.textSecondary
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
                        color = appColors.textSecondary
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