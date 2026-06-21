package io.github.gonbei774.calisthenicsmemory.ui.screens.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.IntervalRecord
import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import org.json.JSONArray
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// 一覧表示コンポーネント
@Composable
fun RecordListView(
    items: List<RecordItem>,
    exercises: List<Exercise>,
    selectedExerciseFilter: Exercise?,
    onExerciseClick: (Exercise) -> Unit,
    onRecordClick: (TrainingRecord) -> Unit,
    onSessionLongPress: (SessionInfo) -> Unit,
    onDeleteClick: (SessionInfo) -> Unit,
    onIntervalEditClick: (IntervalRecord) -> Unit,
    onIntervalDeleteClick: (IntervalRecord) -> Unit
) {
    val appColors = LocalAppColors.current
    if (items.isEmpty() && selectedExerciseFilter == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_records_yet),
                fontSize = 18.sp,
                color = appColors.textSecondary
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (items.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_records_for_exercise),
                            fontSize = 16.sp,
                            color = appColors.textSecondary
                        )
                    }
                }
            } else {
                items(
                    items = items,
                    key = { item ->
                        when (item) {
                            is RecordItem.Session -> "s-${item.session.exerciseId}-${item.session.date}-${item.session.time}"
                            is RecordItem.Interval -> "i-${item.record.id}"
                        }
                    }
                ) { item ->
                    when (item) {
                        is RecordItem.Session -> {
                            SessionCard(
                                session = item.session,
                                exercise = exercises.find { it.id == item.session.exerciseId },
                                isSelected = selectedExerciseFilter?.id == item.session.exerciseId,
                                onExerciseClick = { exercise ->
                                    onExerciseClick(exercise)
                                },
                                onRecordClick = onRecordClick,
                                onSessionLongPress = { onSessionLongPress(item.session) },
                                onDeleteClick = { onDeleteClick(item.session) }
                            )
                        }
                        is RecordItem.Interval -> {
                            IntervalRecordCard(
                                record = item.record,
                                onEditClick = { onIntervalEditClick(item.record) },
                                onDeleteClick = { onIntervalDeleteClick(item.record) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SessionCard(
    session: SessionInfo,
    exercise: Exercise?,
    isSelected: Boolean,
    onExerciseClick: (Exercise) -> Unit,
    onRecordClick: (TrainingRecord) -> Unit,
    onSessionLongPress: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val appColors = LocalAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) appColors.cardBackgroundSelected else appColors.cardBackground
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // 種目名をクリッカブルに
                    Text(
                        text = exercise?.name ?: stringResource(R.string.unknown_exercise),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (exercise != null) Purple600 else Color.White,
                        modifier = Modifier.clickable(enabled = exercise != null) {
                            exercise?.let { onExerciseClick(it) }
                        }
                    )
                    Text(
                        text = "${session.date} ${session.time}",
                        fontSize = 14.sp,
                        color = appColors.textSecondary
                    )
                }

                Box {
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.menu),
                            tint = appColors.textSecondary
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit)) },
                            onClick = {
                                menuExpanded = false
                                onSessionLongPress()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete), color = Red600) },
                            onClick = {
                                menuExpanded = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }

            // Comment
            if (session.comment.isNotBlank()) {
                Row(
                    modifier = Modifier.padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "💬",
                        fontSize = 14.sp
                    )
                    Text(
                        text = session.comment,
                        fontSize = 14.sp,
                        color = appColors.textTertiary,
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            // Sets（各セットにセット別の距離/荷重/アシストを表示）
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                session.records.forEach { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = appColors.cardBackgroundSecondary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        onClick = { onRecordClick(record) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.set_number, record.setNumber),
                                    fontSize = 14.sp,
                                    color = appColors.textTertiary
                                )

                                // Unilateral/Bilateral 対応
                                if (record.valueLeft != null) {
                                    // Unilateral: 左右表示
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.right_value_short, record.valueRight, if (exercise?.type == "Dynamic") stringResource(R.string.unit_reps) else stringResource(R.string.unit_seconds)),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Green400
                                        )
                                        Text(
                                            text = stringResource(R.string.left_value_short, record.valueLeft!!, if (exercise?.type == "Dynamic") stringResource(R.string.unit_reps) else stringResource(R.string.unit_seconds)),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Purple600
                                        )
                                    }
                                } else {
                                    // Bilateral: 従来通り
                                    Text(
                                        text = "${record.valueRight}${if (exercise?.type == "Dynamic") stringResource(R.string.unit_reps) else stringResource(R.string.unit_seconds)}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Green400
                                    )
                                }
                            }

                            // セット別 距離/荷重/アシスト（有効な値のみ表示）
                            val hasDistance = record.distanceCm != null
                            val hasWeight = record.weightG != null
                            val hasAssistance = record.assistanceG != null
                            if (hasDistance || hasWeight || hasAssistance) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (hasDistance) {
                                        Text(
                                            text = stringResource(R.string.distance_display_format, record.distanceCm!!),
                                            fontSize = 13.sp,
                                            color = Blue600,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (hasWeight) {
                                        Text(
                                            text = stringResource(R.string.weight_display_format, record.weightG!! / 1000.0f),
                                            fontSize = 13.sp,
                                            color = Orange600,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (hasAssistance) {
                                        Text(
                                            text = stringResource(R.string.assistance_display_format, record.assistanceG!! / 1000.0f),
                                            fontSize = 13.sp,
                                            color = Amber500,
                                            fontWeight = FontWeight.Bold
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionEditDialog(
    session: SessionInfo,
    exercise: Exercise?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, List<Int?>, List<Int?>, List<Int?>) -> Unit
    // date, time, comment, distancesCm, weightsG, assistancesG（セッションのrecordsと同じ順序・同じサイズ）
) {
    var editDate by remember { mutableStateOf(session.date) }
    var editTime by remember { mutableStateOf(session.time) }
    var editComment by remember { mutableStateOf(session.comment) }

    // セット別の編集値（recordsと同じ順序・サイズ）
    var editDistances by remember {
        mutableStateOf(
            session.records.map { it.distanceCm?.toString() ?: "" }
        )
    }
    var editWeights by remember {
        mutableStateOf(
            session.records.map { r ->
                r.weightG?.let { "%.1f".format(it / 1000.0f) } ?: ""
            }
        )
    }
    var editAssistances by remember {
        mutableStateOf(
            session.records.map { r ->
                r.assistanceG?.let { "%.1f".format(it / 1000.0f) } ?: ""
            }
        )
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val showDistance = exercise?.distanceTrackingEnabled == true
    val showWeight = exercise?.weightTrackingEnabled == true
    val showAssistance = exercise?.assistanceTrackingEnabled == true
    val hasAnyTracking = showDistance || showWeight || showAssistance

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_session_info)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.date_format, editDate))
                }

                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.time_format, editTime))
                }

                OutlinedTextField(
                    value = editComment,
                    onValueChange = { editComment = it },
                    label = { Text(stringResource(R.string.comment)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                // セット別 距離/荷重/アシスト 編集
                if (hasAnyTracking) {
                    session.records.forEachIndexed { index, record ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.set_number, record.setNumber),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                if (showDistance) {
                                    OutlinedTextField(
                                        value = editDistances.getOrElse(index) { "" },
                                        onValueChange = { value ->
                                            val normalized = value
                                                .replace(Regex("[０-９]")) { (it.value[0].code - '０'.code + '0'.code).toChar().toString() }
                                                .replace("．", ".").replace("－", "-")
                                            if (normalized.isEmpty() || normalized == "-" || normalized.toIntOrNull() != null) {
                                                editDistances = editDistances.toMutableList().also { it[index] = normalized }
                                            }
                                        },
                                        label = { Text(stringResource(R.string.distance_input_label), fontSize = 12.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                }

                                if (showWeight) {
                                    OutlinedTextField(
                                        value = editWeights.getOrElse(index) { "" },
                                        onValueChange = { value ->
                                            val normalized = value
                                                .replace(Regex("[０-９]")) { (it.value[0].code - '０'.code + '0'.code).toChar().toString() }
                                                .replace("．", ".")
                                            val isValid = normalized.isEmpty() || normalized == "." ||
                                                normalized.matches(Regex("^\\d*\\.?\\d?\$"))
                                            if (isValid) {
                                                editWeights = editWeights.toMutableList().also { it[index] = normalized }
                                            }
                                        },
                                        label = { Text(stringResource(R.string.weight_input_label), fontSize = 12.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                    )
                                }

                                if (showAssistance) {
                                    OutlinedTextField(
                                        value = editAssistances.getOrElse(index) { "" },
                                        onValueChange = { value ->
                                            val normalized = value
                                                .replace(Regex("[０-９]")) { (it.value[0].code - '０'.code + '0'.code).toChar().toString() }
                                                .replace("．", ".")
                                            val isValid = normalized.isEmpty() || normalized == "." ||
                                                normalized.matches(Regex("^\\d*\\.?\\d?\$"))
                                            if (isValid) {
                                                editAssistances = editAssistances.toMutableList().also { it[index] = normalized }
                                            }
                                        },
                                        label = { Text(stringResource(R.string.assistance_input_label), fontSize = 12.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
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
                    val distancesCm: List<Int?> = session.records.indices.map { i ->
                        editDistances.getOrElse(i) { "" }
                            .takeIf { it.isNotEmpty() && it != "-" }
                            ?.toIntOrNull()
                    }
                    val weightsG: List<Int?> = session.records.indices.map { i ->
                        editWeights.getOrElse(i) { "" }
                            .takeIf { it.isNotEmpty() && it != "." }
                            ?.toDoubleOrNull()
                            ?.let { (it * 1000).toInt() }
                    }
                    val assistancesG: List<Int?> = session.records.indices.map { i ->
                        editAssistances.getOrElse(i) { "" }
                            .takeIf { it.isNotEmpty() && it != "." }
                            ?.toDoubleOrNull()
                            ?.let { (it * 1000).toInt() }
                    }
                    onConfirm(editDate, editTime, editComment, distancesCm, weightsG, assistancesG)
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )

    // Date Picker
    if (showDatePicker) {
        val currentDate = try {
            LocalDate.parse(editDate, dateFormatter)
        } catch (e: Exception) {
            LocalDate.now()
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = currentDate.toEpochDay() * 86400000
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val newDate = LocalDate.ofEpochDay(millis / 86400000)
                        editDate = newDate.format(dateFormatter)
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time Picker
    if (showTimePicker) {
        val currentTime = try {
            LocalTime.parse(editTime, timeFormatter)
        } catch (e: Exception) {
            LocalTime.now()
        }

        val timePickerState = rememberTimePickerState(
            initialHour = currentTime.hour,
            initialMinute = currentTime.minute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val newTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    editTime = newTime.format(timeFormatter)
                    showTimePicker = false
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}

// ========================================
// Interval Record Card
// ========================================

@Composable
fun IntervalRecordCard(
    record: IntervalRecord,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val appColors = LocalAppColors.current
    var exercisesExpanded by remember { mutableStateOf(false) }

    val exercises = remember(record.exercisesJson) {
        try {
            val arr = JSONArray(record.exercisesJson)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) { emptyList() }
    }

    val isFullCompletion = record.completedRounds == record.rounds

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: program name + action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.programName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Orange600
                    )
                    Text(
                        text = "${record.date} ${record.time}",
                        fontSize = 14.sp,
                        color = appColors.textSecondary
                    )
                }
                Box {
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.menu),
                            tint = appColors.textSecondary
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit)) },
                            onClick = {
                                menuExpanded = false
                                onEditClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete), color = Red600) },
                            onClick = {
                                menuExpanded = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }

            // Comment
            if (!record.comment.isNullOrBlank()) {
                Row(
                    modifier = Modifier.padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "\uD83D\uDCAC", fontSize = 14.sp)
                    Text(
                        text = record.comment!!,
                        fontSize = 14.sp,
                        color = appColors.textTertiary,
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            // Settings & completion
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = appColors.cardBackgroundSecondary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Completion status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(
                                R.string.interval_record_rounds_format,
                                record.completedRounds,
                                record.rounds
                            ),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFullCompletion) Orange600 else appColors.textTertiary
                        )
                        if (isFullCompletion) {
                            Text(
                                text = stringResource(R.string.interval_record_complete),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Orange600
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.interval_record_partial),
                                fontSize = 12.sp,
                                color = appColors.textTertiary
                            )
                        }
                    }

                    // Settings summary
                    Text(
                        text = stringResource(
                            R.string.interval_record_settings_format,
                            record.workSeconds,
                            record.restSeconds,
                            record.roundRestSeconds
                        ),
                        fontSize = 13.sp,
                        color = appColors.textSecondary
                    )
                }
            }

            // Expandable exercise list
            if (exercises.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    onClick = { exercisesExpanded = !exercisesExpanded },
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.interval_record_exercises_count, exercises.size),
                            fontSize = 14.sp,
                            color = appColors.textSecondary
                        )
                        Icon(
                            if (exercisesExpanded) Icons.Default.KeyboardArrowDown
                            else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = appColors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (exercisesExpanded) {
                    Column(
                        modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        exercises.forEachIndexed { index, name ->
                            val doneRounds = if (isFullCompletion) {
                                record.rounds
                            } else if (index < record.completedExercisesInLastRound) {
                                record.completedRounds + 1
                            } else {
                                record.completedRounds
                            }
                            val isExerciseComplete = doneRounds == record.rounds

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = name,
                                    fontSize = 13.sp,
                                    color = if (isExerciseComplete) appColors.textPrimary
                                    else appColors.textTertiary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "$doneRounds/${record.rounds}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isExerciseComplete) Orange600
                                    else appColors.textTertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ========================================
// Interval Record Edit Dialog
// ========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntervalRecordEditDialog(
    record: IntervalRecord,
    appColors: AppColors,
    onDismiss: () -> Unit,
    onConfirm: (IntervalRecord) -> Unit
) {
    var editDate by remember { mutableStateOf(record.date) }
    var editTime by remember { mutableStateOf(record.time) }
    var editComment by remember { mutableStateOf(record.comment ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = appColors.cardBackground,
        title = {
            Text(
                stringResource(R.string.edit_session_info),
                color = appColors.textPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.date_format, editDate))
                }

                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.time_format, editTime))
                }

                OutlinedTextField(
                    value = editComment,
                    onValueChange = { editComment = it },
                    label = { Text(stringResource(R.string.interval_comment_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = appColors.textPrimary,
                        unfocusedTextColor = appColors.textPrimary,
                        focusedBorderColor = Orange600,
                        unfocusedBorderColor = appColors.textTertiary,
                        focusedLabelColor = Orange600,
                        unfocusedLabelColor = appColors.textTertiary,
                        cursorColor = Orange600
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    record.copy(
                        date = editDate,
                        time = editTime,
                        comment = editComment.ifBlank { null }
                    )
                )
            }) {
                Text(stringResource(R.string.save), color = Orange600)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = appColors.textSecondary)
            }
        }
    )

    // Date Picker
    if (showDatePicker) {
        val currentDate = try {
            LocalDate.parse(editDate, dateFormatter)
        } catch (e: Exception) {
            LocalDate.now()
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = currentDate.toEpochDay() * 86400000
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val newDate = LocalDate.ofEpochDay(millis / 86400000)
                        editDate = newDate.format(dateFormatter)
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time Picker
    if (showTimePicker) {
        val currentTime = try {
            LocalTime.parse(editTime, timeFormatter)
        } catch (e: Exception) {
            LocalTime.now()
        }

        val timePickerState = rememberTimePickerState(
            initialHour = currentTime.hour,
            initialMinute = currentTime.minute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val newTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    editTime = newTime.format(timeFormatter)
                    showTimePicker = false
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}