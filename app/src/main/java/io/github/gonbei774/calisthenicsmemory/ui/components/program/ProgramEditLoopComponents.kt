package io.github.gonbei774.calisthenicsmemory.ui.components.program

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramLoop
import io.github.gonbei774.calisthenicsmemory.ui.theme.*

@Composable
fun LoopBlock(
    loop: ProgramLoop,
    exercises: List<Pair<ProgramExercise, Exercise>>,
    isExpanded: Boolean,
    isDragging: Boolean,
    elevation: Dp,
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
                                color = Slate300,
                                modifier = Modifier
                                    .background(Slate700, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
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