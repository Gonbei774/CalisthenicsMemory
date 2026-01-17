package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import io.github.gonbei774.calisthenicsmemory.data.ProgramLoop
import io.github.gonbei774.calisthenicsmemory.data.WorkoutPreferences
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.util.SearchUtils
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

// Sealed class to represent items in the program list
private sealed class ProgramListItem {
    abstract val sortOrder: Int

    data class ExerciseItem(val programExercise: ProgramExercise) : ProgramListItem() {
        override val sortOrder: Int get() = programExercise.sortOrder
    }

    data class LoopItem(val loop: ProgramLoop) : ProgramListItem() {
        override val sortOrder: Int get() = loop.sortOrder
    }
}

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
    var programLoops by remember { mutableStateOf<List<ProgramLoop>>(emptyList()) }
    var isLoading by remember { mutableStateOf(programId != null) }

    // Form state
    var name by remember { mutableStateOf("") }

    // Loop expanded states (for collapse/expand)
    var expandedLoopIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    // Dialog states
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showExerciseSettingsDialog by remember { mutableStateOf<ProgramExercise?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showDiscardConfirmDialog by remember { mutableStateOf(false) }
    var showLoopSettingsDialog by remember { mutableStateOf<ProgramLoop?>(null) }
    var showAddLoopDialog by remember { mutableStateOf(false) }
    var showDeleteLoopConfirmDialog by remember { mutableStateOf<ProgramLoop?>(null) }
    var showAddExerciseToLoopDialog by remember { mutableStateOf<ProgramLoop?>(null) }

    // Track original values for existing programs (to detect changes)
    var originalName by remember { mutableStateOf("") }
    var originalProgramExercises by remember { mutableStateOf<List<ProgramExercise>>(emptyList()) }
    var originalProgramLoops by remember { mutableStateOf<List<ProgramLoop>>(emptyList()) }

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

            // Load loops
            val loadedLoops = viewModel.getProgramLoopsSync(programId)
            programLoops = loadedLoops
            originalProgramLoops = loadedLoops
            // Expand all loops by default
            expandedLoopIds = loadedLoops.map { it.id }.toSet()

            isLoading = false
        }
    }

    // Check if there are unsaved changes
    val hasUnsavedChanges = if (programId == null) {
        // New program: any input counts as unsaved
        name.isNotBlank() || programExercises.isNotEmpty() || programLoops.isNotEmpty()
    } else {
        // Existing program: check if settings, exercises, or loops differ from original
        val settingsChanged = name != originalName
        val exercisesChanged = programExercises != originalProgramExercises
        val loopsChanged = programLoops != originalProgramLoops
        settingsChanged || exercisesChanged || loopsChanged
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

    // Build combined list of standalone exercises and loops
    val programListItems = remember(programExercises, programLoops) {
        val standaloneExercises = programExercises.filter { it.loopId == null }
            .map { ProgramListItem.ExerciseItem(it) }
        val loops = programLoops.map { ProgramListItem.LoopItem(it) }
        (standaloneExercises + loops).sortedBy { it.sortOrder }
    }

    fun saveProgram() {
        coroutineScope.launch {
            if (programId != null) {
                // Update existing program
                program?.copy(name = name)?.let { updatedProgram ->
                    viewModel.updateProgram(updatedProgram)
                }

                // ========================================
                // Sync loops first (exercises depend on loop IDs)
                // ========================================
                val originalLoopIds = originalProgramLoops.map { it.id }.toSet()
                val currentLoopIds = programLoops.map { it.id }.toSet()

                // Map from temporary loop IDs to new DB IDs
                val loopIdMapping = mutableMapOf<Long, Long>()

                // Delete removed loops (cascade deletes exercises inside)
                val deletedLoopIds = originalLoopIds - currentLoopIds
                deletedLoopIds.forEach { id ->
                    originalProgramLoops.find { it.id == id }?.let { loop ->
                        viewModel.deleteProgramLoop(loop)
                    }
                }

                // Add new loops (temporary IDs are timestamps)
                val addedLoops = programLoops.filter { it.id !in originalLoopIds }
                addedLoops.forEach { loop ->
                    val newLoopId = viewModel.addProgramLoop(
                        programId = programId,
                        rounds = loop.rounds,
                        restBetweenRounds = loop.restBetweenRounds
                    )
                    if (newLoopId != null) {
                        loopIdMapping[loop.id] = newLoopId
                    }
                }

                // Update modified loops
                val existingLoops = programLoops.filter { it.id in originalLoopIds }
                existingLoops.forEach { loop ->
                    val original = originalProgramLoops.find { it.id == loop.id }
                    if (original != null && loop != original) {
                        viewModel.updateProgramLoop(loop)
                    }
                    // Keep original IDs in mapping
                    loopIdMapping[loop.id] = loop.id
                }

                // ========================================
                // Sync exercises with updated loop IDs
                // ========================================
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
                addedExercises.forEach { pe ->
                    val mappedLoopId = pe.loopId?.let { loopIdMapping[it] }
                    viewModel.addProgramExerciseSync(
                        programId = programId,
                        exerciseId = pe.exerciseId,
                        sets = pe.sets,
                        targetValue = pe.targetValue,
                        intervalSeconds = pe.intervalSeconds,
                        loopId = mappedLoopId,
                        sortOrder = pe.sortOrder
                    )
                }

                // Update modified exercises (existing ones that changed)
                val existingExercises = programExercises.filter { it.id in originalIds }
                existingExercises.forEach { pe ->
                    val original = originalProgramExercises.find { it.id == pe.id }
                    val mappedLoopId = pe.loopId?.let { loopIdMapping[it] }
                    val updatedPe = pe.copy(loopId = mappedLoopId)
                    if (original != null && updatedPe != original) {
                        viewModel.updateProgramExercise(updatedPe)
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
                    // Map from temporary loop IDs to new DB IDs
                    val loopIdMapping = mutableMapOf<Long, Long>()

                    // Add loops first
                    programLoops.forEach { loop ->
                        val newLoopId = viewModel.addProgramLoop(
                            programId = newProgramId,
                            rounds = loop.rounds,
                            restBetweenRounds = loop.restBetweenRounds
                        )
                        if (newLoopId != null) {
                            loopIdMapping[loop.id] = newLoopId
                        }
                    }

                    // Add exercises to new program with mapped loop IDs
                    programExercises.forEach { pe ->
                        val mappedLoopId = pe.loopId?.let { loopIdMapping[it] }
                        viewModel.addProgramExerciseSync(
                            programId = newProgramId,
                            exerciseId = pe.exerciseId,
                            sets = pe.sets,
                            targetValue = pe.targetValue,
                            intervalSeconds = pe.intervalSeconds,
                            loopId = mappedLoopId,
                            sortOrder = pe.sortOrder
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
                if (fromIndex >= 0 && toIndex >= 0 && fromIndex < programListItems.size && toIndex < programListItems.size) {
                    // Reorder the combined list
                    val reorderedItems = programListItems.toMutableList()
                    val item = reorderedItems.removeAt(fromIndex)
                    reorderedItems.add(toIndex, item)

                    // Update sortOrder for all items based on new positions
                    val updatedExercises = mutableListOf<ProgramExercise>()
                    val updatedLoops = mutableListOf<ProgramLoop>()

                    reorderedItems.forEachIndexed { index, listItem ->
                        when (listItem) {
                            is ProgramListItem.ExerciseItem -> {
                                updatedExercises.add(listItem.programExercise.copy(sortOrder = index))
                            }
                            is ProgramListItem.LoopItem -> {
                                updatedLoops.add(listItem.loop.copy(sortOrder = index))
                            }
                        }
                    }

                    // Keep loop exercises unchanged, update standalone exercises
                    val loopExercises = programExercises.filter { it.loopId != null }
                    programExercises = updatedExercises + loopExercises
                    programLoops = updatedLoops
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

                // Combined list with exercises and loops
                if (programListItems.isEmpty()) {
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
                    items(
                        count = programListItems.size,
                        key = { index ->
                            when (val item = programListItems[index]) {
                                is ProgramListItem.ExerciseItem -> "ex_${item.programExercise.id}"
                                is ProgramListItem.LoopItem -> "loop_${item.loop.id}"
                            }
                        }
                    ) { index ->
                        when (val item = programListItems[index]) {
                            is ProgramListItem.ExerciseItem -> {
                                val pe = item.programExercise
                                val exercise = exerciseMap[pe.exerciseId]
                                if (exercise != null) {
                                    ReorderableItem(reorderableLazyListState, key = "ex_${pe.id}") { isDragging ->
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
                            is ProgramListItem.LoopItem -> {
                                val loop = item.loop
                                // Get exercises for this loop
                                val loopExercises = programExercises
                                    .filter { it.loopId == loop.id }
                                    .sortedBy { it.sortOrder }
                                    .mapNotNull { pe ->
                                        exerciseMap[pe.exerciseId]?.let { ex -> pe to ex }
                                    }

                                ReorderableItem(reorderableLazyListState, key = "loop_${loop.id}") { isDragging ->
                                    val elevation by animateDpAsState(
                                        targetValue = if (isDragging) 4.dp else 0.dp,
                                        label = "elevation"
                                    )
                                    LoopBlock(
                                        loop = loop,
                                        exercises = loopExercises,
                                        isExpanded = loop.id in expandedLoopIds,
                                        isDragging = isDragging,
                                        elevation = elevation,
                                        onExpandToggle = {
                                            expandedLoopIds = if (loop.id in expandedLoopIds) {
                                                expandedLoopIds - loop.id
                                            } else {
                                                expandedLoopIds + loop.id
                                            }
                                        },
                                        onEdit = { showLoopSettingsDialog = loop },
                                        onDelete = {
                                            // Delete loop and its exercises
                                            programExercises = programExercises.filter { it.loopId != loop.id }
                                            programLoops = programLoops.filter { it.id != loop.id }
                                        },
                                        onAddExercise = { showAddExerciseToLoopDialog = loop },
                                        onExerciseEdit = { pe -> showExerciseSettingsDialog = pe },
                                        onExerciseDelete = { pe ->
                                            programExercises = programExercises.filter { it != pe }
                                        },
                                        onLoopExercisesReordered = { reorderedExercises ->
                                            // Update loop exercises with new order
                                            val otherExercises = programExercises.filter { it.loopId != loop.id }
                                            programExercises = otherExercises + reorderedExercises
                                        },
                                        dragHandle = { Modifier.longPressDraggableHandle() }
                                    )
                                }
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
                        Text(stringResource(R.string.add_exercise_to_program))
                    }
                }

                // Add loop button
                item {
                    OutlinedButton(
                        onClick = { showAddLoopDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Orange600
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(Orange600, Orange600))
                        )
                    ) {
                        Text("🔁", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.add_loop))
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
                availableLoops = programLoops,
                onDismiss = { showExerciseSettingsDialog = null },
                onSave = { updatedPe ->
                    // If moving to a loop, update sortOrder within that loop
                    val finalPe = if (updatedPe.loopId != null && updatedPe.loopId != pe.loopId) {
                        val loopExerciseCount = programExercises.count {
                            it.loopId == updatedPe.loopId && it.id != updatedPe.id
                        }
                        updatedPe.copy(sortOrder = loopExerciseCount)
                    } else {
                        updatedPe
                    }
                    programExercises = programExercises.map {
                        if (it.id == finalPe.id) finalPe else it
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

    // Add Loop Dialog
    if (showAddLoopDialog) {
        LoopSettingsDialog(
            loop = null,
            onDismiss = { showAddLoopDialog = false },
            onSave = { rounds, restBetweenRounds ->
                val newLoop = ProgramLoop(
                    id = System.currentTimeMillis(), // Temporary ID
                    programId = programId ?: 0L,
                    sortOrder = programLoops.size,
                    rounds = rounds,
                    restBetweenRounds = restBetweenRounds
                )
                programLoops = programLoops + newLoop
                expandedLoopIds = expandedLoopIds + newLoop.id
                showAddLoopDialog = false
            }
        )
    }

    // Edit Loop Settings Dialog
    showLoopSettingsDialog?.let { loop ->
        LoopSettingsDialog(
            loop = loop,
            onDismiss = { showLoopSettingsDialog = null },
            onSave = { rounds, restBetweenRounds ->
                programLoops = programLoops.map {
                    if (it.id == loop.id) it.copy(rounds = rounds, restBetweenRounds = restBetweenRounds)
                    else it
                }
                showLoopSettingsDialog = null
            },
            onDelete = {
                // Delete loop and its exercises
                programExercises = programExercises.filter { it.loopId != loop.id }
                programLoops = programLoops.filter { it.id != loop.id }
                showLoopSettingsDialog = null
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

    // Add Exercise to Loop Dialog
    showAddExerciseToLoopDialog?.let { targetLoop ->
        AddExerciseToProgramDialog(
            viewModel = viewModel,
            exercises = exercises,
            onDismiss = { showAddExerciseToLoopDialog = null },
            onAdd = { selectedExercise, sets, targetValue, intervalSeconds ->
                // Calculate sortOrder within the loop
                val loopExerciseCount = programExercises.count { it.loopId == targetLoop.id }
                val newPe = ProgramExercise(
                    id = System.currentTimeMillis(),
                    programId = programId ?: 0L,
                    exerciseId = selectedExercise.id,
                    sortOrder = loopExerciseCount,
                    sets = sets,
                    targetValue = targetValue,
                    intervalSeconds = intervalSeconds,
                    loopId = targetLoop.id
                )
                programExercises = programExercises + newPe
                showAddExerciseToLoopDialog = null
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
        SearchUtils.searchExercises(exercises, searchQuery)
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
                            state = listState,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseSettingsDialog(
    programExercise: ProgramExercise,
    exercise: Exercise,
    availableLoops: List<ProgramLoop>,
    onDismiss: () -> Unit,
    onSave: (ProgramExercise) -> Unit
) {
    var sets by remember { mutableStateOf(programExercise.sets.toString()) }
    var targetValue by remember { mutableStateOf(programExercise.targetValue.toString()) }
    var intervalSeconds by remember { mutableStateOf(programExercise.intervalSeconds.toString()) }

    // Loop selection state (only for standalone exercises)
    var selectedLoopId by remember { mutableStateOf<Long?>(programExercise.loopId) }
    var loopDropdownExpanded by remember { mutableStateOf(false) }

    // Only show loop selection for standalone exercises (not already in a loop)
    val showLoopSelection = programExercise.loopId == null && availableLoops.isNotEmpty()

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

                // Loop selection dropdown (only for standalone exercises)
                if (showLoopSelection) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.move_to_loop),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate300
                    )
                    ExposedDropdownMenuBox(
                        expanded = loopDropdownExpanded,
                        onExpandedChange = { loopDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedLoopId?.let { id ->
                                val loopIndex = availableLoops.indexOfFirst { it.id == id }
                                if (loopIndex >= 0) {
                                    stringResource(R.string.loop_number_format, loopIndex + 1)
                                } else {
                                    stringResource(R.string.none)
                                }
                            } ?: stringResource(R.string.none),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = loopDropdownExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Orange600,
                                focusedLabelColor = Orange600,
                                unfocusedTextColor = Color.White,
                                focusedTextColor = Color.White,
                                unfocusedLabelColor = Slate400,
                                unfocusedBorderColor = Slate600
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = loopDropdownExpanded,
                            onDismissRequest = { loopDropdownExpanded = false }
                        ) {
                            // None option
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.none)) },
                                onClick = {
                                    selectedLoopId = null
                                    loopDropdownExpanded = false
                                }
                            )
                            // Loop options
                            availableLoops.forEachIndexed { index, loop ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(R.string.loop_number_format, index + 1) +
                                                " (${loop.rounds}x)"
                                        )
                                    },
                                    onClick = {
                                        selectedLoopId = loop.id
                                        loopDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
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
                            intervalSeconds = intervalSeconds.toIntOrNull() ?: 0,
                            loopId = selectedLoopId
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

@Composable
private fun LoopBlock(
    loop: ProgramLoop,
    exercises: List<Pair<ProgramExercise, Exercise>>,
    isExpanded: Boolean,
    isDragging: Boolean,
    elevation: androidx.compose.ui.unit.Dp,
    onExpandToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddExercise: () -> Unit,
    onExerciseEdit: (ProgramExercise) -> Unit,
    onExerciseDelete: (ProgramExercise) -> Unit,
    onLoopExercisesReordered: (List<ProgramExercise>) -> Unit,
    dragHandle: @Composable () -> Modifier
) {
    // State for loop exercise reordering
    var loopExerciseList by remember(exercises) {
        mutableStateOf(exercises.map { it.first })
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = if (isDragging) Orange600.copy(alpha = 0.7f) else Orange600,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) Slate700.copy(alpha = 0.9f) else Slate800
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column {
            // Loop header
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
                        // Drag handle
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = stringResource(R.string.todo_drag_to_reorder),
                            tint = if (isDragging) Color.White else Slate400,
                            modifier = Modifier
                                .size(24.dp)
                                .then(dragHandle())
                        )
                        // Loop icon
                        Text(
                            text = "🔁",
                            fontSize = 18.sp
                        )
                        // Rounds badge
                        Text(
                            text = stringResource(R.string.loop_round_format, loop.rounds),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Amber500,
                            modifier = Modifier
                                .background(
                                    color = Amber500.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                        // Rest between rounds
                        if (loop.restBetweenRounds > 0) {
                            Text(
                                text = stringResource(R.string.loop_rest_format, loop.restBetweenRounds),
                                fontSize = 12.sp,
                                color = Slate400
                            )
                        }
                        // Exercise count
                        Text(
                            text = "(${exercises.size})",
                            fontSize = 12.sp,
                            color = Slate400
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Edit button
                        TextButton(
                            onClick = onEdit,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.edit),
                                fontSize = 12.sp,
                                color = Orange600
                            )
                        }
                        // Expand/collapse icon
                        Icon(
                            imageVector = if (isExpanded)
                                Icons.Default.KeyboardArrowDown
                            else
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Slate400
                        )
                    }
                }
            }

            // Loop exercises
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (exercises.isEmpty()) {
                        // Empty state
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Slate700,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = Amber500.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.loop_empty),
                                color = Slate400,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        // Exercise list with reordering
                        ReorderableLoopExerciseList(
                            exercises = exercises,
                            exerciseList = loopExerciseList,
                            onReorder = { fromIndex, toIndex ->
                                val reordered = loopExerciseList.toMutableList()
                                val item = reordered.removeAt(fromIndex)
                                reordered.add(toIndex, item)
                                // Update sortOrder
                                val updated = reordered.mapIndexed { index, pe ->
                                    pe.copy(sortOrder = index)
                                }
                                loopExerciseList = updated
                                onLoopExercisesReordered(updated)
                            },
                            onExerciseEdit = onExerciseEdit,
                            onExerciseDelete = onExerciseDelete
                        )
                    }

                    // Add exercise to loop button
                    OutlinedButton(
                        onClick = onAddExercise,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Amber500
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(Amber500, Amber500))
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.add_exercise_to_loop),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReorderableLoopExerciseList(
    exercises: List<Pair<ProgramExercise, Exercise>>,
    exerciseList: List<ProgramExercise>,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    onExerciseEdit: (ProgramExercise) -> Unit,
    onExerciseDelete: (ProgramExercise) -> Unit
) {
    val exerciseMap = remember(exercises) {
        exercises.associate { it.first.id to it.second }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        exerciseList.forEachIndexed { index, pe ->
            val exercise = exerciseMap[pe.id]
            if (exercise != null) {
                LoopExerciseItemWithReorder(
                    programExercise = pe,
                    exercise = exercise,
                    index = index,
                    totalCount = exerciseList.size,
                    onMoveUp = {
                        if (index > 0) onReorder(index, index - 1)
                    },
                    onMoveDown = {
                        if (index < exerciseList.size - 1) onReorder(index, index + 1)
                    },
                    onEdit = { onExerciseEdit(pe) },
                    onDelete = { onExerciseDelete(pe) }
                )
            }
        }
    }
}

@Composable
private fun LoopExerciseItemWithReorder(
    programExercise: ProgramExercise,
    exercise: Exercise,
    index: Int,
    totalCount: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
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
                        shape = RoundedCornerShape(8.dp)
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
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    color = Amber500,
                    shape = RoundedCornerShape(8.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = Slate700),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Reorder buttons
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = index > 0,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            tint = if (index > 0) Slate400 else Slate600,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = index < totalCount - 1,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = if (index < totalCount - 1) Slate400 else Slate600,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Exercise info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.program_sets_format, programExercise.sets),
                            fontSize = 11.sp,
                            color = Blue600
                        )
                        Text(
                            text = stringResource(
                                R.string.program_target_format,
                                programExercise.targetValue,
                                stringResource(if (exercise.type == "Dynamic") R.string.unit_reps else R.string.unit_seconds)
                            ),
                            fontSize = 11.sp,
                            color = Green400
                        )
                        Text(
                            text = stringResource(R.string.program_interval_format, programExercise.intervalSeconds),
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoopSettingsDialog(
    loop: ProgramLoop?,
    onDismiss: () -> Unit,
    onSave: (rounds: Int, restBetweenRounds: Int) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var rounds by remember(loop) { mutableStateOf(loop?.rounds?.toString() ?: "3") }
    var restBetweenRounds by remember(loop) { mutableStateOf(loop?.restBetweenRounds?.toString() ?: "60") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate800,
        title = {
            Text(
                text = stringResource(R.string.loop_settings),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = rounds,
                    onValueChange = { rounds = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.loop_rounds)) },
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
                    value = restBetweenRounds,
                    onValueChange = { restBetweenRounds = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.loop_rest_between_rounds)) },
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

                // Delete button (only for existing loops)
                if (onDelete != null) {
                    OutlinedButton(
                        onClick = onDelete,
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
                        Text(stringResource(R.string.delete))
                    }
                }
            }
        },
        confirmButton = {
            val roundsValue = rounds.toIntOrNull()
            val isValid = roundsValue != null && roundsValue > 0
            TextButton(
                onClick = {
                    onSave(
                        roundsValue!!,
                        restBetweenRounds.toIntOrNull() ?: 0
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