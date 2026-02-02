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
import io.github.gonbei774.calisthenicsmemory.ui.components.program.AddExerciseToProgramDialog
import io.github.gonbei774.calisthenicsmemory.ui.components.program.ExerciseSettingsDialog
import io.github.gonbei774.calisthenicsmemory.ui.components.program.LoopBlock
import io.github.gonbei774.calisthenicsmemory.ui.components.program.LoopSettingsDialog
import io.github.gonbei774.calisthenicsmemory.ui.components.program.ProgramExerciseItem
import io.github.gonbei774.calisthenicsmemory.ui.components.program.ProgramListItem

@Composable
fun ProgramEditScreen(
    viewModel: TrainingViewModel,
    programId: Long?,  // null = new program
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit
) {
    val appColors = LocalAppColors.current
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
                // Note: sortOrder is already set correctly in programExercises, so we just
                // need to update exercises that have changed (including sortOrder changes)
                val existingExercises = programExercises.filter { it.id in originalIds }
                existingExercises.forEach { pe ->
                    val original = originalProgramExercises.find { it.id == pe.id }
                    val mappedLoopId = pe.loopId?.let { loopIdMapping[it] }
                    val updatedPe = pe.copy(loopId = mappedLoopId)
                    if (original != null && updatedPe != original) {
                        viewModel.updateProgramExercise(updatedPe)
                    }
                }
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
                            unfocusedTextColor = appColors.textPrimary,
                            focusedTextColor = appColors.textPrimary,
                            unfocusedLabelColor = appColors.textSecondary,
                            unfocusedBorderColor = appColors.border
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
                        color = appColors.textPrimary
                    )
                }

                // Combined list with exercises and loops
                if (programListItems.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.program_exercises_required),
                                modifier = Modifier.padding(16.dp),
                                color = appColors.textSecondary,
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
            containerColor = appColors.cardBackground,
            title = {
                Text(
                    text = stringResource(R.string.delete_program),
                    color = appColors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_program_warning, name),
                    color = appColors.textTertiary
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
                    Text(stringResource(R.string.cancel), color = appColors.textSecondary)
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