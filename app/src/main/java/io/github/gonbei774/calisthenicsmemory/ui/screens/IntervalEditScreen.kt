package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.IntervalProgram
import io.github.gonbei774.calisthenicsmemory.data.IntervalProgramExercise
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.util.SearchUtils
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun IntervalEditScreen(
    viewModel: TrainingViewModel,
    programId: Long?,  // null = new program
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit
) {
    val appColors = LocalAppColors.current
    val coroutineScope = rememberCoroutineScope()
    val exercises by viewModel.exercises.collectAsState()
    val lazyListState = rememberLazyListState()

    // Load existing program if editing
    var program by remember { mutableStateOf<IntervalProgram?>(null) }
    var programExercises by remember { mutableStateOf<List<IntervalProgramExercise>>(emptyList()) }
    var isLoading by remember { mutableStateOf(programId != null) }

    // Form state
    var name by remember { mutableStateOf("") }
    var workSeconds by remember { mutableStateOf("40") }
    var restSeconds by remember { mutableStateOf("20") }
    var rounds by remember { mutableStateOf("3") }
    var roundRestSeconds by remember { mutableStateOf("60") }

    // Dialog states
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showDiscardConfirmDialog by remember { mutableStateOf(false) }

    // Track original values for existing programs
    var originalName by remember { mutableStateOf("") }
    var originalWorkSeconds by remember { mutableStateOf("40") }
    var originalRestSeconds by remember { mutableStateOf("20") }
    var originalRounds by remember { mutableStateOf("3") }
    var originalRoundRestSeconds by remember { mutableStateOf("60") }
    var originalProgramExercises by remember { mutableStateOf<List<IntervalProgramExercise>>(emptyList()) }

    // Load existing program data
    LaunchedEffect(programId) {
        if (programId != null) {
            program = viewModel.getIntervalProgramById(programId)
            program?.let {
                name = it.name
                workSeconds = it.workSeconds.toString()
                restSeconds = it.restSeconds.toString()
                rounds = it.rounds.toString()
                roundRestSeconds = it.roundRestSeconds.toString()
                originalName = it.name
                originalWorkSeconds = it.workSeconds.toString()
                originalRestSeconds = it.restSeconds.toString()
                originalRounds = it.rounds.toString()
                originalRoundRestSeconds = it.roundRestSeconds.toString()
            }
            val loadedExercises = viewModel.getIntervalProgramExercisesSync(programId)
            programExercises = loadedExercises
            originalProgramExercises = loadedExercises
            isLoading = false
        }
    }

    // Check for unsaved changes
    val hasUnsavedChanges = if (programId == null) {
        name.isNotBlank() || programExercises.isNotEmpty()
    } else {
        name != originalName ||
            workSeconds != originalWorkSeconds ||
            restSeconds != originalRestSeconds ||
            rounds != originalRounds ||
            roundRestSeconds != originalRoundRestSeconds ||
            programExercises != originalProgramExercises
    }

    fun handleBackPress() {
        if (hasUnsavedChanges) {
            showDiscardConfirmDialog = true
        } else {
            onNavigateBack()
        }
    }

    BackHandler { handleBackPress() }

    // Exercise map for display
    val exerciseMap = remember(exercises) {
        exercises.associateBy { it.id }
    }

    fun saveProgram() {
        coroutineScope.launch {
            val workSecondsVal = workSeconds.toIntOrNull() ?: 40
            val restSecondsVal = restSeconds.toIntOrNull() ?: 20
            val roundsVal = rounds.toIntOrNull() ?: 3
            val roundRestSecondsVal = roundRestSeconds.toIntOrNull() ?: 60

            if (programId != null) {
                // Update existing program
                program?.copy(
                    name = name,
                    workSeconds = workSecondsVal,
                    restSeconds = restSecondsVal,
                    rounds = roundsVal,
                    roundRestSeconds = roundRestSecondsVal
                )?.let { updatedProgram ->
                    viewModel.updateIntervalProgram(updatedProgram)
                }

                // Sync exercises
                val originalIds = originalProgramExercises.map { it.id }.toSet()
                val currentIds = programExercises.map { it.id }.toSet()

                // Delete removed exercises
                val deletedIds = originalIds - currentIds
                deletedIds.forEach { id ->
                    originalProgramExercises.find { it.id == id }?.let { pe ->
                        viewModel.deleteIntervalProgramExercise(pe)
                    }
                }

                // Add new exercises
                val addedExercises = programExercises.filter { it.id !in originalIds }
                addedExercises.forEach { pe ->
                    viewModel.addIntervalProgramExerciseSync(
                        programId = programId,
                        exerciseId = pe.exerciseId,
                        sortOrder = pe.sortOrder
                    )
                }

                // Update modified exercises (sortOrder changes)
                val existingExercises = programExercises.filter { it.id in originalIds }
                existingExercises.forEach { pe ->
                    val original = originalProgramExercises.find { it.id == pe.id }
                    if (original != null && pe != original) {
                        viewModel.updateIntervalProgramExercise(pe)
                    }
                }
            } else {
                // Create new program
                val newProgramId = viewModel.createIntervalProgramAndGetId(
                    name = name,
                    workSeconds = workSecondsVal,
                    restSeconds = restSecondsVal,
                    rounds = roundsVal,
                    roundRestSeconds = roundRestSecondsVal
                )
                if (newProgramId != null) {
                    programExercises.forEach { pe ->
                        viewModel.addIntervalProgramExerciseSync(
                            programId = newProgramId,
                            exerciseId = pe.exerciseId,
                            sortOrder = pe.sortOrder
                        )
                    }
                }
            }
            onSaved()
        }
    }

    val isValid = name.isNotBlank() && programExercises.isNotEmpty() &&
        (workSeconds.toIntOrNull() ?: 0) > 0 &&
        (restSeconds.toIntOrNull() ?: 0) >= 0 &&
        (rounds.toIntOrNull() ?: 0) > 0 &&
        (roundRestSeconds.toIntOrNull() ?: 0) >= 0

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
                            if (programId == null) R.string.new_interval_program else R.string.edit_interval_program
                        ),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
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
            // Number of header items before the exercise list
            // (Name + Timer Settings title + 4 timer fields + Exercises title = 7)
            val headerItemCount = 7

            val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
                val fromIndex = from.index - headerItemCount
                val toIndex = to.index - headerItemCount
                if (fromIndex >= 0 && toIndex >= 0 && fromIndex < programExercises.size && toIndex < programExercises.size) {
                    val reorderedList = programExercises.toMutableList()
                    val item = reorderedList.removeAt(fromIndex)
                    reorderedList.add(toIndex, item)
                    programExercises = reorderedList.mapIndexed { index, pe ->
                        pe.copy(sortOrder = index)
                    }
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
                        label = { Text(stringResource(R.string.interval_program_name)) },
                        placeholder = { Text(stringResource(R.string.interval_program_name_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Orange600,
                            focusedLabelColor = Orange600,
                            cursorColor = Orange600,
                            unfocusedTextColor = appColors.textPrimary,
                            focusedTextColor = appColors.textPrimary,
                            unfocusedLabelColor = appColors.textSecondary,
                            unfocusedBorderColor = appColors.border
                        ),
                        singleLine = true
                    )
                }

                // Timer Settings Section
                item {
                    Text(
                        text = stringResource(R.string.interval_timer_settings),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.textPrimary
                    )
                }

                // Work Time
                item {
                    TimerSettingRow(
                        label = stringResource(R.string.interval_work_seconds),
                        value = workSeconds,
                        onValueChange = { workSeconds = it.filter { c -> c.isDigit() } },
                        suffix = stringResource(R.string.interval_seconds_suffix)
                    )
                }

                // Rest Time
                item {
                    TimerSettingRow(
                        label = stringResource(R.string.interval_rest_seconds),
                        value = restSeconds,
                        onValueChange = { restSeconds = it.filter { c -> c.isDigit() } },
                        suffix = stringResource(R.string.interval_seconds_suffix)
                    )
                }

                // Rounds
                item {
                    TimerSettingRow(
                        label = stringResource(R.string.interval_rounds),
                        value = rounds,
                        onValueChange = { rounds = it.filter { c -> c.isDigit() } },
                        suffix = stringResource(R.string.interval_rounds_suffix)
                    )
                }

                // Round Rest
                item {
                    TimerSettingRow(
                        label = stringResource(R.string.interval_round_rest_seconds),
                        value = roundRestSeconds,
                        onValueChange = { roundRestSeconds = it.filter { c -> c.isDigit() } },
                        suffix = stringResource(R.string.interval_seconds_suffix)
                    )
                }

                // Exercises Section
                item {
                    Text(
                        text = stringResource(R.string.interval_exercises),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.textPrimary
                    )
                }

                // Exercise list
                if (programExercises.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.interval_exercises_required),
                                modifier = Modifier.padding(16.dp),
                                color = appColors.textSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(
                        count = programExercises.size,
                        key = { index -> "ex_${programExercises[index].id}" }
                    ) { index ->
                        val pe = programExercises[index]
                        val exercise = exerciseMap[pe.exerciseId]
                        if (exercise != null) {
                            ReorderableItem(reorderableLazyListState, key = "ex_${pe.id}") { isDragging ->
                                val elevation by animateDpAsState(
                                    targetValue = if (isDragging) 4.dp else 0.dp,
                                    label = "elevation"
                                )
                                IntervalExerciseItem(
                                    index = index + 1,
                                    exercise = exercise,
                                    isDragging = isDragging,
                                    elevation = elevation,
                                    onDelete = {
                                        programExercises = programExercises.filter { it != pe }
                                            .mapIndexed { i, e -> e.copy(sortOrder = i) }
                                    },
                                    dragHandle = { Modifier.longPressDraggableHandle() }
                                )
                            }
                        }
                    }
                }

                // Add exercise button
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
                        Text(stringResource(R.string.interval_add_exercise))
                    }
                }

                // Note about no recording
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = appColors.cardBackground.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.interval_no_record_note),
                            modifier = Modifier.padding(12.dp),
                            color = appColors.textTertiary,
                            fontSize = 12.sp
                        )
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
                            Text(stringResource(R.string.interval_delete_program))
                        }
                    }
                }
            }
        }
    }

    // Add Exercise Dialog (batch selection with checkboxes)
    if (showAddExerciseDialog) {
        AddExerciseToIntervalDialog(
            viewModel = viewModel,
            exercises = exercises,
            onDismiss = { showAddExerciseDialog = false },
            onAdd = { selectedExercisesList ->
                val newEntries = selectedExercisesList.mapIndexed { index, exercise ->
                    IntervalProgramExercise(
                        id = System.currentTimeMillis() + index,
                        programId = programId ?: 0L,
                        exerciseId = exercise.id,
                        sortOrder = programExercises.size + index
                    )
                }
                programExercises = programExercises + newEntries
                showAddExerciseDialog = false
                coroutineScope.launch {
                    lazyListState.animateScrollToItem(lazyListState.layoutInfo.totalItemsCount - 1)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = appColors.cardBackground,
            title = {
                Text(
                    text = stringResource(R.string.interval_delete_program),
                    color = appColors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.interval_delete_program_warning, name),
                    color = appColors.textTertiary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        programId?.let { viewModel.deleteIntervalProgram(it) }
                        showDeleteConfirmDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text(stringResource(R.string.delete), color = Red600)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel), color = appColors.textSecondary)
                }
            }
        )
    }

    // Discard Changes Confirmation Dialog
    if (showDiscardConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmDialog = false },
            containerColor = appColors.cardBackground,
            title = {
                Text(
                    text = stringResource(R.string.discard_changes_title),
                    color = appColors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.discard_changes_message),
                    color = appColors.textTertiary
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
                    Text(stringResource(R.string.cancel), color = appColors.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun TimerSettingRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    suffix: String
) {
    val appColors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = appColors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.width(80.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Orange600,
                    cursorColor = Orange600,
                    unfocusedTextColor = appColors.textPrimary,
                    focusedTextColor = appColors.textPrimary,
                    unfocusedBorderColor = appColors.border
                ),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontSize = 14.sp
                )
            )
            Text(
                text = suffix,
                fontSize = 14.sp,
                color = appColors.textSecondary
            )
        }
    }
}

@Composable
private fun IntervalExerciseItem(
    index: Int,
    exercise: Exercise,
    isDragging: Boolean,
    elevation: androidx.compose.ui.unit.Dp,
    onDelete: () -> Unit,
    dragHandle: @Composable () -> Modifier
) {
    val appColors = LocalAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle
            Icon(
                Icons.Default.Menu,
                contentDescription = null,
                tint = appColors.textTertiary,
                modifier = Modifier
                    .size(24.dp)
                    .then(dragHandle())
            )

            // Number
            Text(
                text = "$index.",
                fontSize = 12.sp,
                color = appColors.textTertiary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Exercise info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    fontSize = 14.sp,
                    color = appColors.textPrimary
                )
                if (!exercise.description.isNullOrBlank()) {
                    Text(
                        text = exercise.description,
                        fontSize = 11.sp,
                        color = appColors.textTertiary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = stringResource(R.string.delete),
                    tint = Red600,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// Batch exercise selection dialog for interval mode (checkboxes + badges like ToDo)
@Composable
private fun AddExerciseToIntervalDialog(
    viewModel: TrainingViewModel,
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onAdd: (List<Exercise>) -> Unit
) {
    val appColors = LocalAppColors.current
    val hierarchicalData by viewModel.hierarchicalExercises.collectAsState()
    val expandedGroups by viewModel.expandedGroups.collectAsState()

    var selectedExercises by remember { mutableStateOf(setOf<Long>()) }
    var searchQuery by remember { mutableStateOf("") }
    val searchResults = remember(exercises, searchQuery) {
        SearchUtils.searchExercises(exercises, searchQuery)
    }
    val listState = rememberLazyListState()

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
                text = stringResource(R.string.interval_add_exercise),
                color = appColors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (exercises.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_exercises_available),
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
                            focusedBorderColor = Orange600,
                            unfocusedBorderColor = Slate600,
                            cursorColor = Orange600
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
                                    IntervalExerciseSelectItem(
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
                            items(
                                count = hierarchicalData.size,
                                key = { index -> hierarchicalData[index].groupName ?: "ungrouped" }
                            ) { index ->
                                val group = hierarchicalData[index]
                                if (group.exercises.isNotEmpty()) {
                                    IntervalSelectExerciseGroup(
                                        groupName = group.groupName,
                                        exercises = group.exercises,
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
                    val selected = exercises.filter { it.id in selectedExercises }
                    onAdd(selected)
                },
                enabled = selectedExercises.isNotEmpty()
            ) {
                Text(
                    text = stringResource(R.string.add),
                    color = if (selectedExercises.isNotEmpty()) Orange600 else appColors.textSecondary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = appColors.textSecondary)
            }
        }
    )
}

@Composable
private fun IntervalExerciseSelectItem(
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
                    checkedColor = Orange600,
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
                        Text(
                            text = "★",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }
                    if (exercise.targetSets != null && exercise.targetValue != null && exercise.sortOrder > 0) {
                        Text(
                            text = "Lv.${exercise.sortOrder}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Blue600
                        )
                    }
                    Text(
                        text = stringResource(if (exercise.type == "Dynamic") R.string.dynamic_type else R.string.isometric_type),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.textSecondary
                    )
                    if (exercise.laterality == "Unilateral") {
                        Text(
                            text = stringResource(R.string.one_sided),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Purple600
                        )
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
private fun IntervalSelectExerciseGroup(
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

            // Exercise list with animation
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
                                    checkedColor = Orange600,
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
                                        Text(
                                            text = "★",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFD700)
                                        )
                                    }
                                    if (exercise.targetSets != null && exercise.targetValue != null && exercise.sortOrder > 0) {
                                        Text(
                                            text = "Lv.${exercise.sortOrder}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Blue600
                                        )
                                    }
                                    Text(
                                        text = stringResource(if (exercise.type == "Dynamic") R.string.dynamic_type else R.string.isometric_type),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = appColors.textSecondary
                                    )
                                    if (exercise.laterality == "Unilateral") {
                                        Text(
                                            text = stringResource(R.string.one_sided),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Purple600
                                        )
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
