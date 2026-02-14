package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ExerciseGroup
import io.github.gonbei774.calisthenicsmemory.data.IntervalProgram
import io.github.gonbei774.calisthenicsmemory.data.Program
import io.github.gonbei774.calisthenicsmemory.ui.theme.Amber500
import io.github.gonbei774.calisthenicsmemory.ui.theme.Amber600
import io.github.gonbei774.calisthenicsmemory.ui.theme.LocalAppColors
import io.github.gonbei774.calisthenicsmemory.ui.theme.Slate600
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun CommunityShareExportScreen(
    viewModel: TrainingViewModel,
    onNavigateBack: () -> Unit
) {
    val appColors = LocalAppColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val programs by viewModel.programs.collectAsState()
    val intervalPrograms by viewModel.intervalPrograms.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val groups by viewModel.groups.collectAsState()

    var selectedProgramIds by remember { mutableStateOf(setOf<Long>()) }
    var selectedIntervalProgramIds by remember { mutableStateOf(setOf<Long>()) }
    var selectedExerciseIds by remember { mutableStateOf(setOf<Long>()) }

    var showPreviewDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    // Program exercise counts
    var programExerciseCounts by remember { mutableStateOf(mapOf<Long, Int>()) }
    var programLoopCounts by remember { mutableStateOf(mapOf<Long, Int>()) }
    // Interval exercise counts
    var intervalExerciseCounts by remember { mutableStateOf(mapOf<Long, Int>()) }
    // Exercise IDs already included in selected programs/intervals
    var includedExerciseIds by remember { mutableStateOf(setOf<Long>()) }

    // Load exercise counts for programs
    LaunchedEffect(programs) {
        withContext(Dispatchers.IO) {
            val exCounts = mutableMapOf<Long, Int>()
            val loopCounts = mutableMapOf<Long, Int>()
            programs.forEach { program ->
                val programExercises = viewModel.getProgramExercisesSync(program.id)
                exCounts[program.id] = programExercises.size
                val loops = viewModel.getProgramLoopsSync(program.id)
                loopCounts[program.id] = loops.size
            }
            programExerciseCounts = exCounts
            programLoopCounts = loopCounts
        }
    }

    // Load exercise counts for intervals
    LaunchedEffect(intervalPrograms) {
        withContext(Dispatchers.IO) {
            val counts = mutableMapOf<Long, Int>()
            intervalPrograms.forEach { interval ->
                val intervalExercises = viewModel.getIntervalProgramExercisesSync(interval.id)
                counts[interval.id] = intervalExercises.size
            }
            intervalExerciseCounts = counts
        }
    }

    // Compute included exercise IDs from selected programs/intervals
    LaunchedEffect(selectedProgramIds, selectedIntervalProgramIds) {
        withContext(Dispatchers.IO) {
            val ids = mutableSetOf<Long>()
            selectedProgramIds.forEach { programId ->
                val programExercises = viewModel.getProgramExercisesSync(programId)
                programExercises.forEach { pe -> ids.add(pe.exerciseId) }
            }
            selectedIntervalProgramIds.forEach { intervalId ->
                val intervalExercises = viewModel.getIntervalProgramExercisesSync(intervalId)
                intervalExercises.forEach { ie -> ids.add(ie.exerciseId) }
            }
            includedExerciseIds = ids
        }
    }

    val totalSelected = selectedProgramIds.size + selectedIntervalProgramIds.size + selectedExerciseIds.size

    // SAF file save launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isExporting = true
                try {
                    withContext(Dispatchers.IO) {
                        val jsonData = viewModel.exportCommunityShare(
                            selectedProgramIds = selectedProgramIds,
                            selectedIntervalProgramIds = selectedIntervalProgramIds,
                            selectedExerciseIds = selectedExerciseIds
                        )
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(jsonData.toByteArray())
                        }
                    }
                } finally {
                    isExporting = false
                    showPreviewDialog = false
                }
            }
        }
    }

    val tabTitles = listOf(
        stringResource(R.string.share_tab_programs),
        stringResource(R.string.share_tab_intervals),
        stringResource(R.string.share_tab_exercises)
    )
    val pagerState = rememberPagerState(pageCount = { tabTitles.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                color = Slate600
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
                        text = stringResource(R.string.share_export_screen_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        },
        bottomBar = {
            BottomAppBar(
                containerColor = appColors.cardBackground
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { showPreviewDialog = true },
                        enabled = totalSelected > 0 && !isExporting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Amber500,
                            disabledContainerColor = appColors.cardBackgroundSecondary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (totalSelected > 0) stringResource(R.string.share_export_button_with_count, totalSelected) else stringResource(R.string.share_export_button),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (totalSelected > 0) Color.White else appColors.textSecondary
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            BoxWithConstraints {
                val minTabWidth = maxWidth / tabTitles.size
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = appColors.cardBackground,
                    contentColor = Amber500,
                    edgePadding = 0.dp
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                            modifier = Modifier.widthIn(min = minTabWidth),
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    color = if (pagerState.currentPage == index) Amber500 else appColors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> ProgramsExportTab(
                        programs = programs,
                        selectedIds = selectedProgramIds,
                        exerciseCounts = programExerciseCounts,
                        loopCounts = programLoopCounts,
                        onToggle = { id ->
                            selectedProgramIds = if (id in selectedProgramIds)
                                selectedProgramIds - id else selectedProgramIds + id
                        }
                    )
                    1 -> IntervalsExportTab(
                        intervalPrograms = intervalPrograms,
                        selectedIds = selectedIntervalProgramIds,
                        exerciseCounts = intervalExerciseCounts,
                        onToggle = { id ->
                            selectedIntervalProgramIds = if (id in selectedIntervalProgramIds)
                                selectedIntervalProgramIds - id else selectedIntervalProgramIds + id
                        }
                    )
                    2 -> ExercisesExportTab(
                        exercises = exercises,
                        groups = groups,
                        selectedIds = selectedExerciseIds,
                        includedExerciseIds = includedExerciseIds,
                        onToggle = { id ->
                            selectedExerciseIds = if (id in selectedExerciseIds)
                                selectedExerciseIds - id else selectedExerciseIds + id
                        }
                    )
                }
            }
        }
    }

    // Preview Dialog
    if (showPreviewDialog) {
        val selectedPrograms = programs.filter { it.id in selectedProgramIds }
        val selectedIntervals = intervalPrograms.filter { it.id in selectedIntervalProgramIds }
        val autoIncludedExerciseCount = includedExerciseIds.size
        val additionalExerciseCount = selectedExerciseIds.count { it !in includedExerciseIds }
        val totalExerciseCount = autoIncludedExerciseCount + additionalExerciseCount

        // Collect group names from all included exercises
        val allIncludedIds = includedExerciseIds + selectedExerciseIds
        val groupCount = exercises
            .filter { it.id in allIncludedIds }
            .mapNotNull { it.group }
            .distinct()
            .size

        AlertDialog(
            onDismissRequest = { if (!isExporting) showPreviewDialog = false },
            containerColor = appColors.cardBackground,
            title = {
                Text(
                    text = stringResource(R.string.share_export_preview_title),
                    fontWeight = FontWeight.Bold,
                    color = appColors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (selectedPrograms.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.share_preview_programs, selectedPrograms.size),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = appColors.textPrimary
                        )
                        selectedPrograms.forEach { program ->
                            Text(
                                text = "  • ${program.name}",
                                fontSize = 13.sp,
                                color = appColors.textSecondary
                            )
                        }
                    }
                    if (selectedIntervals.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.share_preview_intervals, selectedIntervals.size),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = appColors.textPrimary
                        )
                        selectedIntervals.forEach { interval ->
                            Text(
                                text = "  • ${interval.name}",
                                fontSize = 13.sp,
                                color = appColors.textSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.share_preview_total_exercises, totalExerciseCount) +
                                if (autoIncludedExerciseCount > 0) stringResource(R.string.share_preview_auto_included, autoIncludedExerciseCount) else "",
                        fontSize = 13.sp,
                        color = appColors.textSecondary
                    )
                    Text(
                        text = stringResource(R.string.share_preview_groups, groupCount),
                        fontSize = 13.sp,
                        color = appColors.textSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val dateTime = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
                        val fileName = "share_${dateTime.format(formatter)}.json"
                        exportLauncher.launch(fileName)
                    },
                    enabled = !isExporting,
                    colors = ButtonDefaults.buttonColors(containerColor = Amber500)
                ) {
                    Text(
                        text = if (isExporting) stringResource(R.string.share_exporting) else stringResource(R.string.share_export_button),
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPreviewDialog = false },
                    enabled = !isExporting
                ) {
                    Text(text = stringResource(R.string.cancel), color = appColors.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun ProgramsExportTab(
    programs: List<Program>,
    selectedIds: Set<Long>,
    exerciseCounts: Map<Long, Int>,
    loopCounts: Map<Long, Int>,
    onToggle: (Long) -> Unit
) {
    val appColors = LocalAppColors.current

    if (programs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
            Text(
                text = stringResource(R.string.share_no_programs),
                color = appColors.textSecondary,
                modifier = Modifier.padding(16.dp)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            items(
                items = programs,
                key = { it.id }
            ) { program ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = appColors.cardBackgroundSecondary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = program.id in selectedIds,
                            onCheckedChange = { onToggle(program.id) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Amber500,
                                uncheckedColor = appColors.textSecondary
                            )
                        )
                        Column(modifier = Modifier.padding(start = 4.dp)) {
                            Text(
                                text = program.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.textPrimary
                            )
                            val exCount = exerciseCounts[program.id] ?: 0
                            val lpCount = loopCounts[program.id] ?: 0
                            val exercisesText = stringResource(R.string.share_program_exercises_format, exCount)
                            val loopsText = if (lpCount > 0) stringResource(R.string.share_program_loops_format, lpCount) else ""
                            val subtitle = exercisesText + loopsText
                            Text(
                                text = subtitle,
                                fontSize = 11.sp,
                                color = appColors.textSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IntervalsExportTab(
    intervalPrograms: List<IntervalProgram>,
    selectedIds: Set<Long>,
    exerciseCounts: Map<Long, Int>,
    onToggle: (Long) -> Unit
) {
    val appColors = LocalAppColors.current

    if (intervalPrograms.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
            Text(
                text = stringResource(R.string.share_no_intervals),
                color = appColors.textSecondary,
                modifier = Modifier.padding(16.dp)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            items(
                items = intervalPrograms,
                key = { it.id }
            ) { interval ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = appColors.cardBackgroundSecondary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = interval.id in selectedIds,
                            onCheckedChange = { onToggle(interval.id) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Amber500,
                                uncheckedColor = appColors.textSecondary
                            )
                        )
                        Column(modifier = Modifier.padding(start = 4.dp)) {
                            Text(
                                text = interval.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.textPrimary
                            )
                            val exCount = exerciseCounts[interval.id] ?: 0
                            Text(
                                text = "$exCount exercises · ${interval.workSeconds}s/${interval.restSeconds}s · ${interval.rounds} rounds",
                                fontSize = 11.sp,
                                color = appColors.textSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExercisesExportTab(
    exercises: List<Exercise>,
    groups: List<ExerciseGroup>,
    selectedIds: Set<Long>,
    includedExerciseIds: Set<Long>,
    onToggle: (Long) -> Unit
) {
    val appColors = LocalAppColors.current

    // Group exercises by group name, with ungrouped at the end
    val groupedExercises = remember(exercises, groups) {
        val groupOrder = groups.associate { it.name to it.displayOrder }
        exercises
            .sortedWith(compareBy<Exercise> {
                if (it.group == null) Int.MAX_VALUE else (groupOrder[it.group] ?: Int.MAX_VALUE)
            }.thenBy { it.name })
            .groupBy { it.group ?: "" }
    }

    if (exercises.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
            Text(
                text = stringResource(R.string.share_no_exercises),
                color = appColors.textSecondary,
                modifier = Modifier.padding(16.dp)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            groupedExercises.forEach { (groupName, groupExercises) ->
                item(key = "group_header_$groupName") {
                    Text(
                        text = groupName.ifEmpty { stringResource(R.string.share_ungrouped) },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Amber500,
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
                    )
                }
                items(
                    items = groupExercises,
                    key = { it.id }
                ) { exercise ->
                    val isIncluded = exercise.id in includedExerciseIds
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = appColors.cardBackgroundSecondary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = exercise.id in selectedIds || isIncluded,
                                onCheckedChange = { if (!isIncluded) onToggle(exercise.id) },
                                enabled = !isIncluded,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = if (isIncluded) appColors.textSecondary else Amber500,
                                    uncheckedColor = appColors.textSecondary,
                                    disabledCheckedColor = appColors.textSecondary
                                )
                            )
                            Column(modifier = Modifier.padding(start = 4.dp)) {
                                Text(
                                    text = exercise.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isIncluded) appColors.textSecondary else appColors.textPrimary
                                )
                                Text(
                                    text = if (isIncluded) stringResource(R.string.share_exercise_auto_included)
                                           else exercise.type,
                                    fontSize = 11.sp,
                                    color = appColors.textSecondary,
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
