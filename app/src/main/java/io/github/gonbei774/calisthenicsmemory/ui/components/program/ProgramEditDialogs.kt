package io.github.gonbei774.calisthenicsmemory.ui.components.program

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramLoop
import io.github.gonbei774.calisthenicsmemory.data.WorkoutPreferences
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.util.SearchUtils
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel

@Composable
fun AddExerciseToProgramDialog(
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
fun SelectExerciseGroup(
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
fun ProgramSearchResultItem(
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
fun ExerciseSettingsDialog(
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
fun LoopSettingsDialog(
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
