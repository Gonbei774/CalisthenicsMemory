package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.Program
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableColumn

@Composable
fun ProgramEditScreen(
    viewModel: TrainingViewModel,
    programId: Long?,  // null = new program
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val exercises by viewModel.exercises.collectAsState()

    // Load existing program if editing
    var program by remember { mutableStateOf<Program?>(null) }
    var programExercises by remember { mutableStateOf<List<ProgramExercise>>(emptyList()) }
    var isLoading by remember { mutableStateOf(programId != null) }

    // Form state
    var name by remember { mutableStateOf("") }
    var timerMode by remember { mutableStateOf(false) }
    var startInterval by remember { mutableStateOf(5) }

    // Dialog states
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showExerciseSettingsDialog by remember { mutableStateOf<ProgramExercise?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Load existing program data
    LaunchedEffect(programId) {
        if (programId != null) {
            program = viewModel.getProgramById(programId)
            program?.let {
                name = it.name
                timerMode = it.timerMode
                startInterval = it.startInterval
            }
            programExercises = viewModel.getProgramExercisesSync(programId)
            isLoading = false
        }
    }

    // Exercise map for display
    val exerciseMap = remember(exercises) {
        exercises.associateBy { it.id }
    }

    fun saveProgram() {
        coroutineScope.launch {
            if (programId != null) {
                // Update existing program
                program?.copy(
                    name = name,
                    timerMode = timerMode,
                    startInterval = startInterval
                )?.let { updatedProgram ->
                    viewModel.updateProgram(updatedProgram)
                }
            } else {
                // Create new program
                val newProgramId = viewModel.createProgramAndGetId(name, timerMode, startInterval)
                if (newProgramId != null) {
                    // Add exercises to new program
                    programExercises.forEachIndexed { index, pe ->
                        viewModel.addProgramExerciseSync(
                            programId = newProgramId,
                            exerciseId = pe.exerciseId,
                            sets = pe.sets,
                            targetValue = pe.targetValue,
                            intervalSeconds = pe.intervalSeconds
                        )
                    }
                }
            }
            onSaved()
        }
    }

    val isValid = name.isNotBlank() && programExercises.isNotEmpty()

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
                            text = stringResource(
                                if (programId == null) R.string.new_program else R.string.edit_program
                            ),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        // Save button
                        TextButton(
                            onClick = { saveProgram() },
                            enabled = isValid
                        ) {
                            Text(
                                text = stringResource(R.string.save),
                                color = if (isValid) Color.White else Color.White.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Amber500)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Program Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.program_name)) },
                    placeholder = { Text(stringResource(R.string.program_name_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Amber500,
                        focusedLabelColor = Amber500,
                        cursorColor = Amber500,
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedLabelColor = Slate400,
                        unfocusedBorderColor = Slate600
                    ),
                    singleLine = true
                )

                // Timer Mode Toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Slate800),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.timer_mode_label),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            Switch(
                                checked = timerMode,
                                onCheckedChange = { timerMode = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Green600,
                                    uncheckedThumbColor = Slate400,
                                    uncheckedTrackColor = Slate600
                                )
                            )
                        }
                        Text(
                            text = stringResource(
                                if (timerMode) R.string.timer_mode_on_description
                                else R.string.timer_mode_off_description
                            ),
                            fontSize = 12.sp,
                            color = Slate400
                        )
                    }
                }

                // Start Countdown
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Slate800),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.start_countdown),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        OutlinedTextField(
                            value = startInterval.toString(),
                            onValueChange = { newValue ->
                                newValue.toIntOrNull()?.let { startInterval = it.coerceIn(0, 30) }
                            },
                            modifier = Modifier.width(80.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Amber500,
                                cursorColor = Amber500,
                                unfocusedTextColor = Color.White,
                                focusedTextColor = Color.White,
                                unfocusedBorderColor = Slate600
                            ),
                            singleLine = true,
                            suffix = { Text("s", color = Slate400) }
                        )
                    }
                }

                // Exercises Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.program_exercises),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Button(
                        onClick = { showAddExerciseDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Amber500),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.add))
                    }
                }

                // Exercise list with drag-and-drop
                if (programExercises.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Slate800),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.program_exercises_required),
                            modifier = Modifier.padding(16.dp),
                            color = Slate400,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    ReorderableColumn(
                        list = programExercises,
                        onSettle = { fromIndex, toIndex ->
                            val reordered = programExercises.toMutableList()
                            val item = reordered.removeAt(fromIndex)
                            reordered.add(toIndex, item)
                            programExercises = reordered
                            // If editing existing program, update in database
                            if (programId != null) {
                                viewModel.reorderProgramExercises(reordered.map { it.id })
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) { index, pe, isDragging ->
                        val exercise = exerciseMap[pe.exerciseId]
                        if (exercise != null) {
                            key(pe.id) {
                                ReorderableItem {
                                    val elevation by animateDpAsState(
                                        targetValue = if (isDragging) 4.dp else 0.dp,
                                        label = "elevation"
                                    )
                                    ProgramExerciseItem(
                                        programExercise = pe,
                                        exercise = exercise,
                                        isDragging = isDragging,
                                        elevation = elevation,
                                        onEdit = { showExerciseSettingsDialog = pe },
                                        onDelete = {
                                            programExercises = programExercises.filter { it != pe }
                                            if (programId != null) {
                                                viewModel.deleteProgramExercise(pe)
                                            }
                                        },
                                        dragHandle = { Modifier.longPressDraggableHandle() }
                                    )
                                }
                            }
                        }
                    }
                }

                // Delete program button (only for existing programs)
                if (programId != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Red600
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(Red600, Red600))
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.delete_program))
                    }
                }
            }
        }
    }

    // Add Exercise Dialog
    if (showAddExerciseDialog) {
        AddExerciseToProgramDialog(
            viewModel = viewModel,
            exercises = exercises,
            existingExerciseIds = programExercises.map { it.exerciseId }.toSet(),
            onDismiss = { showAddExerciseDialog = false },
            onAdd = { selectedExercise, sets, targetValue, intervalSeconds ->
                val newPe = ProgramExercise(
                    id = System.currentTimeMillis(), // Temporary ID for new items
                    programId = programId ?: 0L,
                    exerciseId = selectedExercise.id,
                    sortOrder = programExercises.size,
                    sets = sets,
                    targetValue = targetValue,
                    intervalSeconds = intervalSeconds
                )
                programExercises = programExercises + newPe
                if (programId != null) {
                    viewModel.addProgramExercise(
                        programId = programId,
                        exerciseId = selectedExercise.id,
                        sets = sets,
                        targetValue = targetValue,
                        intervalSeconds = intervalSeconds
                    )
                }
                showAddExerciseDialog = false
            }
        )
    }

    // Exercise Settings Dialog
    showExerciseSettingsDialog?.let { pe ->
        val exercise = exerciseMap[pe.exerciseId]
        if (exercise != null) {
            ExerciseSettingsDialog(
                programExercise = pe,
                exercise = exercise,
                onDismiss = { showExerciseSettingsDialog = null },
                onSave = { updatedPe ->
                    programExercises = programExercises.map {
                        if (it.id == updatedPe.id) updatedPe else it
                    }
                    if (programId != null) {
                        viewModel.updateProgramExercise(updatedPe)
                    }
                    showExerciseSettingsDialog = null
                }
            )
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = Slate800,
            title = {
                Text(
                    text = stringResource(R.string.delete_program),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_program_warning, name),
                    color = Slate300
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        programId?.let { viewModel.deleteProgram(it) }
                        showDeleteConfirmDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text(stringResource(R.string.delete), color = Red600)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel), color = Slate400)
                }
            }
        )
    }
}

@Composable
private fun ProgramExerciseItem(
    programExercise: ProgramExercise,
    exercise: Exercise,
    isDragging: Boolean,
    elevation: androidx.compose.ui.unit.Dp,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    dragHandle: @Composable () -> Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
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
            onClick = onEdit,
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
                        .then(dragHandle())
                )

                // Exercise info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.program_sets_format, programExercise.sets),
                            fontSize = 12.sp,
                            color = Blue600
                        )
                        Text(
                            text = stringResource(
                                R.string.program_target_format,
                                programExercise.targetValue,
                                stringResource(if (exercise.type == "Dynamic") R.string.unit_reps else R.string.unit_seconds)
                            ),
                            fontSize = 12.sp,
                            color = Green400
                        )
                        Text(
                            text = stringResource(R.string.program_interval_format, programExercise.intervalSeconds),
                            fontSize = 12.sp,
                            color = Slate400
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddExerciseToProgramDialog(
    viewModel: TrainingViewModel,
    exercises: List<Exercise>,
    existingExerciseIds: Set<Long>,
    onDismiss: () -> Unit,
    onAdd: (Exercise, Int, Int, Int) -> Unit
) {
    val hierarchicalData by viewModel.hierarchicalExercises.collectAsState()
    val expandedGroups by viewModel.expandedGroups.collectAsState()

    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var sets by remember { mutableStateOf("3") }
    var targetValue by remember { mutableStateOf("10") }
    var intervalSeconds by remember { mutableStateOf("60") }

    val availableExercises = remember(exercises, existingExerciseIds) {
        exercises.filter { it.id !in existingExerciseIds }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate800,
        title = {
            Text(
                text = stringResource(R.string.add_exercise_to_program),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedExercise == null) {
                    // Exercise selection
                    if (availableExercises.isEmpty()) {
                        Text(
                            text = stringResource(R.string.todo_all_added),
                            color = Slate400
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                count = hierarchicalData.size,
                                key = { index -> hierarchicalData[index].groupName ?: "ungrouped" }
                            ) { index ->
                                val group = hierarchicalData[index]
                                val availableInGroup = group.exercises.filter { it.id !in existingExerciseIds }
                                if (availableInGroup.isNotEmpty()) {
                                    SelectExerciseGroup(
                                        groupName = group.groupName,
                                        exercises = availableInGroup,
                                        isExpanded = (group.groupName ?: "ungrouped") in expandedGroups,
                                        onExpandToggle = {
                                            viewModel.toggleGroupExpansion(group.groupName ?: "ungrouped")
                                        },
                                        onExerciseSelected = { exercise ->
                                            selectedExercise = exercise
                                            // Pre-fill with exercise defaults
                                            exercise.targetSets?.let { sets = it.toString() }
                                            exercise.targetValue?.let { targetValue = it.toString() }
                                            exercise.restInterval?.let { intervalSeconds = it.toString() }
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Settings for selected exercise
                    Text(
                        text = selectedExercise!!.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    OutlinedTextField(
                        value = sets,
                        onValueChange = { sets = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.sets_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Amber500,
                            focusedLabelColor = Amber500,
                            cursorColor = Amber500,
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White,
                            unfocusedLabelColor = Slate400,
                            unfocusedBorderColor = Slate600
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = targetValue,
                        onValueChange = { targetValue = it.filter { c -> c.isDigit() } },
                        label = {
                            Text(
                                stringResource(
                                    R.string.target_value_label
                                ) + " (" + stringResource(
                                    if (selectedExercise!!.type == "Dynamic") R.string.unit_reps else R.string.unit_seconds
                                ) + ")"
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Amber500,
                            focusedLabelColor = Amber500,
                            cursorColor = Amber500,
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White,
                            unfocusedLabelColor = Slate400,
                            unfocusedBorderColor = Slate600
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = intervalSeconds,
                        onValueChange = { intervalSeconds = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.interval_seconds_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Amber500,
                            focusedLabelColor = Amber500,
                            cursorColor = Amber500,
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White,
                            unfocusedLabelColor = Slate400,
                            unfocusedBorderColor = Slate600
                        ),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            if (selectedExercise != null) {
                TextButton(
                    onClick = {
                        selectedExercise?.let { exercise ->
                            onAdd(
                                exercise,
                                sets.toIntOrNull() ?: 3,
                                targetValue.toIntOrNull() ?: 10,
                                intervalSeconds.toIntOrNull() ?: 60
                            )
                        }
                    },
                    enabled = sets.isNotBlank() && targetValue.isNotBlank() && intervalSeconds.isNotBlank()
                ) {
                    Text(stringResource(R.string.add), color = Amber500)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (selectedExercise != null) {
                        selectedExercise = null
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text(
                    text = if (selectedExercise != null) stringResource(R.string.back) else stringResource(R.string.cancel),
                    color = Slate400
                )
            }
        }
    )
}

@Composable
private fun SelectExerciseGroup(
    groupName: String?,
    exercises: List<Exercise>,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onExerciseSelected: (Exercise) -> Unit
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
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    exercises.forEach { exercise ->
                        Surface(
                            onClick = { onExerciseSelected(exercise) },
                            modifier = Modifier.fillMaxWidth(),
                            color = Slate600,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = exercise.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Text(
                                            text = stringResource(
                                                if (exercise.type == "Dynamic") R.string.dynamic_type else R.string.isometric_type
                                            ),
                                            fontSize = 10.sp,
                                            color = Slate400
                                        )
                                        if (exercise.laterality == "Unilateral") {
                                            Text(
                                                text = stringResource(R.string.one_sided),
                                                fontSize = 10.sp,
                                                color = Purple600
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
}

@Composable
private fun ExerciseSettingsDialog(
    programExercise: ProgramExercise,
    exercise: Exercise,
    onDismiss: () -> Unit,
    onSave: (ProgramExercise) -> Unit
) {
    var sets by remember { mutableStateOf(programExercise.sets.toString()) }
    var targetValue by remember { mutableStateOf(programExercise.targetValue.toString()) }
    var intervalSeconds by remember { mutableStateOf(programExercise.intervalSeconds.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate800,
        title = {
            Text(
                text = stringResource(R.string.exercise_settings),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = exercise.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate300
                )

                OutlinedTextField(
                    value = sets,
                    onValueChange = { sets = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.sets_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Amber500,
                        focusedLabelColor = Amber500,
                        cursorColor = Amber500,
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedLabelColor = Slate400,
                        unfocusedBorderColor = Slate600
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = targetValue,
                    onValueChange = { targetValue = it.filter { c -> c.isDigit() } },
                    label = {
                        Text(
                            stringResource(R.string.target_value_label) + " (" + stringResource(
                                if (exercise.type == "Dynamic") R.string.unit_reps else R.string.unit_seconds
                            ) + ")"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Amber500,
                        focusedLabelColor = Amber500,
                        cursorColor = Amber500,
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedLabelColor = Slate400,
                        unfocusedBorderColor = Slate600
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = intervalSeconds,
                    onValueChange = { intervalSeconds = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.interval_seconds_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Amber500,
                        focusedLabelColor = Amber500,
                        cursorColor = Amber500,
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedLabelColor = Slate400,
                        unfocusedBorderColor = Slate600
                    ),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        programExercise.copy(
                            sets = sets.toIntOrNull() ?: programExercise.sets,
                            targetValue = targetValue.toIntOrNull() ?: programExercise.targetValue,
                            intervalSeconds = intervalSeconds.toIntOrNull() ?: programExercise.intervalSeconds
                        )
                    )
                },
                enabled = sets.isNotBlank() && targetValue.isNotBlank() && intervalSeconds.isNotBlank()
            ) {
                Text(stringResource(R.string.save), color = Amber500)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = Slate400)
            }
        }
    )
}