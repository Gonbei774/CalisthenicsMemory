package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.IntervalProgram
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel

@Composable
fun IntervalListScreen(
    viewModel: TrainingViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long?) -> Unit,  // null = new program
    onNavigateToExecute: (Long) -> Unit
) {
    val appColors = LocalAppColors.current
    val programs by viewModel.intervalPrograms.collectAsState()

    // Load exercise counts for each program
    val exerciseCounts = remember { mutableStateMapOf<Long, Int>() }
    LaunchedEffect(programs) {
        programs.forEach { program ->
            if (program.id !in exerciseCounts) {
                val exercises = viewModel.getIntervalProgramExercisesSync(program.id)
                exerciseCounts[program.id] = exercises.size
            }
        }
    }

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
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White
                        )
                    }
                    Text(
                        text = stringResource(R.string.interval_list_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToEdit(null) },
                containerColor = Orange600
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_interval_program), tint = appColors.textPrimary)
            }
        }
    ) { paddingValues ->
        if (programs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.interval_empty),
                    fontSize = 16.sp,
                    color = appColors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            val copySuffix = stringResource(R.string.program_copy_suffix)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = programs,
                    key = { it.id }
                ) { program ->
                    IntervalProgramListItem(
                        program = program,
                        exerciseCount = exerciseCounts[program.id] ?: 0,
                        onEdit = { onNavigateToEdit(program.id) },
                        onExecute = { onNavigateToExecute(program.id) },
                        onDelete = { viewModel.deleteIntervalProgram(program.id) },
                        onDuplicate = { viewModel.duplicateIntervalProgram(program.id, copySuffix) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IntervalProgramListItem(
    program: IntervalProgram,
    exerciseCount: Int,
    onEdit: () -> Unit,
    onExecute: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit
) {
    val appColors = LocalAppColors.current
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                showDeleteConfirmDialog = true
                false
            } else {
                false
            }
        }
    )

    // Delete confirmation dialog
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
                    text = stringResource(R.string.interval_delete_program_warning, program.name),
                    color = appColors.textTertiary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete()
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
        Box {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { },
                        onLongClick = { showContextMenu = true }
                    ),
                colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Program info
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = program.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = appColors.textPrimary
                        )
                        Text(
                            text = stringResource(
                                R.string.interval_summary_format,
                                exerciseCount,
                                program.workSeconds,
                                program.restSeconds,
                                program.rounds
                            ),
                            fontSize = 12.sp,
                            color = appColors.textSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    // Edit button
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit_interval_program),
                            tint = appColors.textSecondary
                        )
                    }

                    // Execute button
                    Button(
                        onClick = onExecute,
                        colors = ButtonDefaults.buttonColors(containerColor = Orange600),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.interval_start),
                            fontSize = 12.sp,
                            color = appColors.textPrimary
                        )
                    }
                }
            }

            // Context menu (long press)
            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false },
                offset = DpOffset(16.dp, 0.dp),
                containerColor = appColors.cardBackgroundSecondary
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.interval_duplicate),
                            color = appColors.textPrimary
                        )
                    },
                    onClick = {
                        showContextMenu = false
                        onDuplicate()
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = appColors.textTertiary
                        )
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.interval_delete_program),
                            color = Red600
                        )
                    },
                    onClick = {
                        showContextMenu = false
                        showDeleteConfirmDialog = true
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = Red600
                        )
                    }
                )
            }
        }
    }
}
