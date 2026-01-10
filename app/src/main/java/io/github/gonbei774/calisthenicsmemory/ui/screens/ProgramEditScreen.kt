package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.Program
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.WorkoutPreferences
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun ProgramEditScreen(
    viewModel: TrainingViewModel,
    programId: Long?,  // null = new program
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val exercises by viewModel.exercises.collectAsState()
    val workoutPreferences = remember { WorkoutPreferences(context) }
    val lazyListState = rememberLazyListState()

    // Load existing program if editing
    var program by remember { mutableStateOf<Program?>(null) }
    var programExercises by remember { mutableStateOf<List<ProgramExercise>>(emptyList()) }
    var isLoading by remember { mutableStateOf(programId != null) }

    // Form state
    var name by remember { mutableStateOf("") }

    // Dialog states
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showExerciseSettingsDialog by remember { mutableStateOf<ProgramExercise?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showDiscardConfirmDialog by remember { mutableStateOf(false) }

    // Track original values for existing programs (to detect changes)
    var originalName by remember { mutableStateOf("") }
    var originalProgramExercises by remember { mutableStateOf<List<ProgramExercise>>(emptyList()) }

    // Load existing program data
    LaunchedEffect(programId) {
        if (programId != null) {
            program = viewModel.getProgramById(programId)
            program?.let {
                name = it.name
                originalName = it.name
            }
            val loadedExercises = viewModel.getProgramExercisesSync(programId)
            programExercises = loadedExercises
            originalProgramExercises = loadedExercises
            isLoading = false
        }
    }

    // Check if there are unsaved changes
    val hasUnsavedChanges = if (programId == null) {
        // New program: any input counts as unsaved
        name.isNotBlank() || programExercises.isNotEmpty()
    } else {
        // Existing program: check if settings or exercises differ from original
        val settingsChanged = name != originalName
        val exercisesChanged = programExercises != originalProgramExercises
        settingsChanged || exercisesChanged
    }

    // Handle back button press
    fun handleBackPress() {
        if (hasUnsavedChanges) {
            showDiscardConfirmDialog = true
        } else {
            onNavigateBack()
        }
    }

    // BackHandler for system back button
    BackHandler {
        handleBackPress()
    }

    // Exercise map for display
    val exerciseMap = remember(exercises) {
        exercises.associateBy { it.id }
    }

    fun saveProgram() {
        coroutineScope.launch {
            if (programId != null) {
                // Update existing program
                program?.copy(name = name)?.let { updatedProgram ->
                    viewModel.updateProgram(updatedProgram)
                }

                // Sync exercises: calculate diff and apply changes
                val originalIds = originalProgramExercises.map { it.id }.toSet()
                val currentIds = programExercises.map { it.id }.toSet()

                // Delete removed exercises
                val deletedIds = originalIds - currentIds
                deletedIds.forEach { id ->
                    originalProgramExercises.find { it.id == id }?.let { pe ->
                        viewModel.deleteProgramExercise(pe)
                    }
                }

                // Add new exercises (temporary IDs are timestamps, not in original)
                val addedExercises = programExercises.filter { it.id !in originalIds }
                addedExercises.forEachIndexed { index, pe ->
                    viewModel.addProgramExerciseSync(
                        programId = programId,
                        exerciseId = pe.exerciseId,
                        sets = pe.sets,
                        targetValue = pe.targetValue,
                        intervalSeconds = pe.intervalSeconds
                    )
                }

                // Update modified exercises (existing ones that changed)
                val existingExercises = programExercises.filter { it.id in originalIds }
                existingExercises.forEach { pe ->
                    val original = originalProgramExercises.find { it.id == pe.id }
                    if (original != null && pe != original) {
                        viewModel.updateProgramExercise(pe)
                    }
                }

                // Update sort order for all existing exercises
                viewModel.reorderProgramExercises(
                    programExercises.filter { it.id in originalIds }.map { it.id }
                )
            } else {
                // Create new program
                val newProgramId = viewModel.createProgramAndGetId(name)
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
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                color = Orange600
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { handleBackPress() }) {
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
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Orange600)
            }
        } else {
            // Number of header items before the exercise list (Program Name + Exercises Section)
            val headerItemCount = 2

            val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
                val fromIndex = from.index - headerItemCount
                val toIndex = to.index - headerItemCount
                if (fromIndex >= 0 && toIndex >= 0 && fromIndex < programExercises.size && toIndex < programExercises.size) {
                    val reordered = programExercises.toMutableList()
                    val item = reordered.removeAt(fromIndex)
                    reordered.add(toIndex, item)
                    programExercises = reordered
                }
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Program Name
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.program_name)) },
                        placeholder = { Text(stringResource(R.string.program_name_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Orange600,
                            focusedLabelColor = Orange600,
                            cursorColor = Orange600,
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White,
                            unfocusedLabelColor = Slate400,
                            unfocusedBorderColor = Slate600
                        ),
                        singleLine = true
                    )
                }

                // Exercises Section
                item {
                    Text(
                        text = stringResource(R.string.program_exercises),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Exercise list with drag-and-drop
                if (programExercises.isEmpty()) {
                    item {
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
                    }
                } else {
                    itemsIndexed(
                        items = programExercises,
                        key = { _, pe -> pe.id }
                    ) { index, pe ->
                        val exercise = exerciseMap[pe.exerciseId]
                        if (exercise != null) {
                            ReorderableItem(reorderableLazyListState, key = pe.id) { isDragging ->
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
                                    },
                                    dragHandle = { Modifier.longPressDraggableHandle() }
                                )
                            }
                        }
                    }
                }

                // Add exercise button (at the bottom)
                item {
                    Button(
                        onClick = { showAddExerciseDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Orange600)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.add_exercise_to_program))
                    }
                }

                // Delete program button (only for existing programs)
                if (programId != null) {
                    item {
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
    }

    // Add Exercise Dialog
    if (showAddExerciseDialog) {
        AddExerciseToProgramDialog(
            viewModel = viewModel,
            exercises = exercises,
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
                showAddExerciseDialog = false
                // 追加後に最下部へスクロール
                coroutineScope.launch {
                    lazyListState.animateScrollToItem(lazyListState.layoutInfo.totalItemsCount - 1)
                }
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

    // Discard Changes Confirmation Dialog
    if (showDiscardConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmDialog = false },
            containerColor = Slate800,
            title = {
                Text(
                    text = stringResource(R.string.discard_changes_title),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.discard_changes_message),
                    color = Slate300
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirmDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text(stringResource(R.string.discard), color = Red600)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmDialog = false }) {
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
    var pendingDelete by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                pendingDelete = true
                true
            } else {
                false
            }
        }
    )

    // Wait for swipe animation to complete before deleting
    LaunchedEffect(pendingDelete) {
        if (pendingDelete) {
            kotlinx.coroutines.delay(300)
            onDelete()
        }
    }

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
    onDismiss: () -> Unit,
    onAdd: (Exercise, Int, Int, Int) -> Unit
) {
    val context = LocalContext.current
    val workoutPreferences = remember { WorkoutPreferences(context) }
    // スイッチONなら設定画面の秒数、OFFなら空欄
    val defaultInterval = remember {
        if (workoutPreferences.isSetIntervalEnabled()) {
            workoutPreferences.getSetInterval().toString()
        } else {
            ""
        }
    }

    val hierarchicalData by viewModel.hierarchicalExercises.collectAsState()
    val expandedGroups by viewModel.expandedGroups.collectAsState()

    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var sets by remember { mutableStateOf("") }
    var targetValue by remember { mutableStateOf("") }
    var intervalSeconds by remember { mutableStateOf(defaultInterval) }

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    val searchResults = remember(exercises, searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            exercises.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
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
                    if (exercises.isEmpty()) {
                        Text(
                            text = stringResource(R.string.todo_all_added),
                            color = Slate400
                        )
                    } else {
                        // Search field
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.search_placeholder),
                                    color = Slate400
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Slate400
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = stringResource(R.string.clear),
                                            tint = Slate400
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Slate700,
                                unfocusedContainerColor = Slate700,
                                focusedBorderColor = Orange600,
                                unfocusedBorderColor = Slate600,
                                cursorColor = Orange600
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (searchQuery.isNotBlank()) {
                                // Flat search results
                                if (searchResults.isEmpty()) {
                                    item {
                                        Text(
                                            text = stringResource(R.string.no_results),
                                            color = Slate400,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                } else {
                                    items(
                                        count = searchResults.size,
                                        key = { index -> searchResults[index].id }
                                    ) { index ->
                                        val exercise = searchResults[index]
                                        ProgramSearchResultItem(
                                            exercise = exercise,
                                            onSelected = {
                                                selectedExercise = exercise
                                                searchQuery = ""
                                                // Pre-fill with exercise defaults
                                                exercise.targetSets?.let { sets = it.toString() }
                                                exercise.targetValue?.let { targetValue = it.toString() }
                                                exercise.restInterval?.let { intervalSeconds = it.toString() }
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
                                    if (group.exercises.isNotEmpty()) {
                                        SelectExerciseGroup(
                                            groupName = group.groupName,
                                            exercises = group.exercises,
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
                            focusedBorderColor = Orange600,
                            focusedLabelColor = Orange600,
                            cursorColor = Orange600,
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
                            focusedBorderColor = Orange600,
                            focusedLabelColor = Orange600,
                            cursorColor = Orange600,
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
                            focusedBorderColor = Orange600,
                            focusedLabelColor = Orange600,
                            cursorColor = Orange600,
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
                val setsValue = sets.toIntOrNull()
                val targetValueValue = targetValue.toIntOrNull()
                val isValid = setsValue != null && targetValueValue != null
                TextButton(
                    onClick = {
                        selectedExercise?.let { exercise ->
                            onAdd(
                                exercise,
                                setsValue!!,
                                targetValueValue!!,
                                intervalSeconds.toIntOrNull() ?: 0  // 空欄=0秒
                            )
                        }
                    },
                    enabled = isValid
                ) {
                    Text(stringResource(R.string.add), color = if (isValid) Orange600 else Slate400)
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
                                            text = stringResource(
                                                if (exercise.type == "Dynamic") R.string.dynamic_type else R.string.isometric_type
                                            ),
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
}

@Composable
private fun ProgramSearchResultItem(
    exercise: Exercise,
    onSelected: () -> Unit
) {
    Surface(
        onClick = onSelected,
        modifier = Modifier.fillMaxWidth(),
        color = Slate700,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Exercise name with group info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = exercise.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    // Group name badge
                    exercise.group?.let { groupName ->
                        Text(
                            text = groupName,
                            fontSize = 10.sp,
                            color = Orange600,
                            modifier = Modifier
                                .background(
                                    color = Orange600.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
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
                        text = stringResource(
                            if (exercise.type == "Dynamic") R.string.dynamic_type else R.string.isometric_type
                        ),
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
                        focusedBorderColor = Orange600,
                        focusedLabelColor = Orange600,
                        cursorColor = Orange600,
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
                        focusedBorderColor = Orange600,
                        focusedLabelColor = Orange600,
                        cursorColor = Orange600,
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
                        focusedBorderColor = Orange600,
                        focusedLabelColor = Orange600,
                        cursorColor = Orange600,
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
            val setsValue = sets.toIntOrNull()
            val targetValueValue = targetValue.toIntOrNull()
            val isValid = setsValue != null && targetValueValue != null
            TextButton(
                onClick = {
                    onSave(
                        programExercise.copy(
                            sets = setsValue!!,
                            targetValue = targetValueValue!!,
                            intervalSeconds = intervalSeconds.toIntOrNull() ?: 0  // 空欄=0秒
                        )
                    )
                },
                enabled = isValid
            ) {
                Text(stringResource(R.string.save), color = if (isValid) Orange600 else Slate400)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = Slate400)
            }
        }
    )
}