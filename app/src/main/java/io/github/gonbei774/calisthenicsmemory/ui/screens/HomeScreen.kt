package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.Screen
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
import io.github.gonbei774.calisthenicsmemory.ui.UiMessage
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import java.time.LocalDate

@Composable
fun HomeScreen(
    onNavigate: (Screen) -> Unit,
    viewModel: TrainingViewModel = viewModel()
) {
    val exercises by viewModel.exercises.collectAsState()
    val records by viewModel.records.collectAsState()

    // Filter today's records
    val todayDate = LocalDate.now().toString()
    val todayRecords = remember(records, todayDate) {
        records.filter { it.date == todayDate }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "Calisthenics Memory",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(64.dp))

            // To Do Button
            MainButton(
                text = stringResource(R.string.todo_title),
                color = Slate800,
                onClick = { onNavigate(Screen.ToDo) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Record Button (solid color)
            MainButton(
                text = stringResource(R.string.home_record),
                color = Slate800,
                onClick = { onNavigate(Screen.Record()) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Workout Button (solid color)
            MainButton(
                text = stringResource(R.string.home_workout),
                color = Slate800,
                onClick = { onNavigate(Screen.Workout()) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Create Button (solid color)
            MainButton(
                text = stringResource(R.string.home_create),
                color = Slate800,
                onClick = { onNavigate(Screen.Create) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Dashboard Card
            TodayDashboardCard(
                records = todayRecords,
                exercises = exercises,
                onNavigateToView = { onNavigate(Screen.View) }
            )
        }

        // Settings Icon Button (bottom-right)
        IconButton(
            onClick = { onNavigate(Screen.Settings) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.settings),
                tint = Slate400,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TodayDashboardCard(
    records: List<TrainingRecord>,
    exercises: List<Exercise>,
    onNavigateToView: () -> Unit,
    viewModel: TrainingViewModel = viewModel()
) {
    val formattedText = remember(records, exercises) {
        formatRecordsForClipboard(records, exercises)
    }

    val formattedAnnotatedText = remember(records, exercises) {
        formatRecordsForDisplay(records, exercises)
    }

    val clipboardManager = LocalClipboardManager.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    if (records.isNotEmpty()) {
                        clipboardManager.setText(AnnotatedString(formattedText))
                        viewModel.showSnackbar(UiMessage.CopiedToClipboard)
                    }
                }
            ),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.today),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val scrollState = rememberScrollState()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 150.dp)
            ) {
                if (records.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_records_today),
                        fontSize = 16.sp,
                        color = Slate300
                    )
                } else {
                    Text(
                        text = formattedAnnotatedText,
                        fontSize = 18.sp,
                        lineHeight = 26.sp,
                        modifier = Modifier.verticalScroll(scrollState)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onNavigateToView) {
                    Text(
                        text = stringResource(R.string.view_all_records),
                        color = Blue600,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

/**
 * Format training records for clipboard copy
 *
 * Example: "Push-up: 12/11/10, Bridge: 20/20/20/30, Plank: 35s/32s/30s"
 *
 * Rules:
 * - Group by exercise
 * - Within each exercise, sort by time + set number
 * - Merge multiple sessions into one
 * - Unilateral: R6 L5/R5 L4 (セット間をスラッシュ)
 * - Isometric: 35s/32s/30s
 */
fun formatRecordsForClipboard(
    records: List<TrainingRecord>,
    exercises: List<Exercise>
): String {
    if (records.isEmpty()) return ""

    val exerciseMap = exercises.associateBy { it.id }

    // Group by exercise and sort by time + set number
    val recordsByExercise = records
        .groupBy { it.exerciseId }
        .mapValues { (_, recs) ->
            recs.sortedWith(compareBy({ it.time }, { it.setNumber }))
        }

    return recordsByExercise.entries.joinToString(", ") { (exerciseId, sortedRecords) ->
        val exercise = exerciseMap[exerciseId] ?: return@joinToString ""
        val exerciseName = exercise.name

        val valuesText = when {
            // Unilateral exercise - format: R6 L5/R5 L4
            exercise.laterality == "Unilateral" -> {
                val pairs = sortedRecords.map { record ->
                    "R${record.valueRight}" + (record.valueLeft?.let { " L$it" } ?: "")
                }
                pairs.joinToString("/")
            }

            // Isometric exercise - format: 35s/32s/30s
            exercise.type == "Isometric" -> {
                val values = sortedRecords.map { "${it.valueRight}s" }
                values.joinToString("/")
            }

            // Dynamic Bilateral exercise - format: 12/11/10
            else -> {
                val values = sortedRecords.map { it.valueRight.toString() }
                values.joinToString("/")
            }
        }

        "$exerciseName: $valuesText"
    }
}

/**
 * Format training records for display with color coding
 * Display format: "Exercise name X sets" (one per line)
 *
 * Colors:
 * - Exercise name: White
 * - Set count: Green400
 */
fun formatRecordsForDisplay(
    records: List<TrainingRecord>,
    exercises: List<Exercise>
): AnnotatedString {
    if (records.isEmpty()) return AnnotatedString("")

    val exerciseMap = exercises.associateBy { it.id }

    // Group by exercise and count sets
    val recordsByExercise = records
        .groupBy { it.exerciseId }
        .mapValues { (_, recs) -> recs.size }

    return buildAnnotatedString {
        recordsByExercise.entries.forEachIndexed { index, (exerciseId, setCount) ->
            val exercise = exerciseMap[exerciseId] ?: return@forEachIndexed

            // Exercise name in White
            withStyle(SpanStyle(color = Color.White)) {
                append(exercise.name)
                append(" ")
            }

            // Set count in Green400
            withStyle(SpanStyle(color = Green400)) {
                val setsText = if (setCount == 1) "set" else "sets"
                append("$setCount $setsText")
            }

            // Add newline between exercises
            if (index < recordsByExercise.size - 1) {
                append("\n")
            }
        }
    }
}

@Composable
fun MainButton(
    text: String,
    gradient: Brush? = null,
    color: Color? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color ?: Color.Transparent
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (gradient != null) {
                        Modifier.background(gradient)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

