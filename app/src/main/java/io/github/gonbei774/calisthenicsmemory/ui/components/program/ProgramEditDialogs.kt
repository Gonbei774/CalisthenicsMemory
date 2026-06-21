package io.github.gonbei774.calisthenicsmemory.ui.components.program

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramLoop
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.ui.theme.LocalAppColors
import io.github.gonbei774.calisthenicsmemory.util.SearchUtils
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel

@Composable
fun AddExerciseToProgramDialog(
    viewModel: TrainingViewModel,
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onAdd: (List<Exercise>) -> Unit
) {
    val appColors = LocalAppColors.current
    val hierarchicalData by viewModel.hierarchicalExercises.collectAsState()
    val expandedGroups by viewModel.expandedGroups.collectAsState()

    var selectedExercises by remember { mutableStateOf(listOf<Long>()) }
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
                text = stringResource(R.string.add_exercise_to_program),
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
                                    ProgramExerciseSelectItem(
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
                                    ProgramSelectExerciseGroup(
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
                    val selected = selectedExercises.mapNotNull { id -> exercises.find { it.id == id } }
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
private fun ProgramExerciseSelectItem(
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
private fun ProgramSelectExerciseGroup(
    groupName: String?,
    exercises: List<Exercise>,
    selectedExercises: List<Long>,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSettingsDialog(
    programExercise: ProgramExercise,
    exercise: Exercise,
    availableLoops: List<ProgramLoop>,
    onDismiss: () -> Unit,
    onSave: (ProgramExercise) -> Unit
) {
    val appColors = LocalAppColors.current
    var sets by remember {
        mutableStateOf(if (programExercise.sets == 0) "" else programExercise.sets.toString())
    }
    var targetValue by remember {
        mutableStateOf(if (programExercise.targetValue == 0) "" else programExercise.targetValue.toString())
    }
    var intervalSeconds by remember { mutableStateOf(programExercise.intervalSeconds.toString()) }

    // Loop selection state (only for standalone exercises)
    var selectedLoopId by remember { mutableStateOf<Long?>(programExercise.loopId) }
    var loopDropdownExpanded by remember { mutableStateOf(false) }

    // Show loop selection whenever loops exist, so exercises can be moved into,
    // between, or out of (via "None") a loop
    val showLoopSelection = availableLoops.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = appColors.cardBackground,
        title = {
            Text(
                text = stringResource(R.string.exercise_settings),
                color = appColors.textPrimary,
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
                    color = appColors.textTertiary
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
                        unfocusedTextColor = appColors.textPrimary,
                        focusedTextColor = appColors.textPrimary,
                        unfocusedLabelColor = appColors.textSecondary,
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
                        unfocusedTextColor = appColors.textPrimary,
                        focusedTextColor = appColors.textPrimary,
                        unfocusedLabelColor = appColors.textSecondary,
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
                        unfocusedTextColor = appColors.textPrimary,
                        focusedTextColor = appColors.textPrimary,
                        unfocusedLabelColor = appColors.textSecondary,
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
                        color = appColors.textTertiary
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
                                unfocusedTextColor = appColors.textPrimary,
                                focusedTextColor = appColors.textPrimary,
                                unfocusedLabelColor = appColors.textSecondary,
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
                Text(stringResource(R.string.save), color = if (isValid) Orange600 else appColors.textSecondary)
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
fun LoopSettingsDialog(
    loop: ProgramLoop?,
    onDismiss: () -> Unit,
    onSave: (rounds: Int, restBetweenRounds: Int) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val appColors = LocalAppColors.current
    var rounds by remember(loop) { mutableStateOf(loop?.rounds?.toString() ?: "3") }
    var restBetweenRounds by remember(loop) { mutableStateOf(loop?.restBetweenRounds?.toString() ?: "60") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = appColors.cardBackground,
        title = {
            Text(
                text = stringResource(R.string.loop_settings),
                color = appColors.textPrimary,
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
                        unfocusedTextColor = appColors.textPrimary,
                        focusedTextColor = appColors.textPrimary,
                        unfocusedLabelColor = appColors.textSecondary,
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
                        unfocusedTextColor = appColors.textPrimary,
                        focusedTextColor = appColors.textPrimary,
                        unfocusedLabelColor = appColors.textSecondary,
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
                Text(stringResource(R.string.save), color = if (isValid) Orange600 else appColors.textSecondary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = appColors.textSecondary)
            }
        }
    )
}
