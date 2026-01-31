package io.github.gonbei774.calisthenicsmemory.ui.screens.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// 一覧表示コンポーネント
@Composable
fun RecordListView(
    sessions: List<SessionInfo>,
    exercises: List<Exercise>,
    selectedExerciseFilter: Exercise?, // フィルター状態の確認用（空メッセージ表示に使用）
    onExerciseClick: (Exercise) -> Unit,
    onRecordClick: (TrainingRecord) -> Unit,
    onSessionLongPress: (SessionInfo) -> Unit,
    onDeleteClick: (SessionInfo) -> Unit
) {
    val appColors = LocalAppColors.current
    if (sessions.isEmpty() && selectedExerciseFilter == null) {
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
            if (sessions.isEmpty()) {
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
                    items = sessions,
                    key = { session -> "${session.exerciseId}-${session.date}-${session.time}" }
                ) { session ->
                    SessionCard(
                        session = session,
                        exercise = exercises.find { it.id == session.exerciseId },
                        isSelected = selectedExerciseFilter?.id == session.exerciseId,
                        onExerciseClick = { exercise ->
                            onExerciseClick(exercise)
                        },
                        onRecordClick = onRecordClick,
                        onSessionLongPress = { onSessionLongPress(session) },
                        onDeleteClick = { onDeleteClick(session) }
                    )
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

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onSessionLongPress) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit),
                            tint = Blue600
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = Red600
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

            // Distance, Weight, and Assistance (from first record, as they're the same for all sets in a session)
            val firstRecord = session.records.firstOrNull()
            val hasDistance = firstRecord?.distanceCm != null
            val hasWeight = firstRecord?.weightG != null
            val hasAssistance = firstRecord?.assistanceG != null
            if (hasDistance || hasWeight || hasAssistance) {
                Row(
                    modifier = Modifier.padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (hasDistance) {
                        Text(
                            text = stringResource(R.string.distance_display_format, firstRecord!!.distanceCm!!),
                            fontSize = 14.sp,
                            color = Blue600,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (hasWeight) {
                        Text(
                            text = stringResource(R.string.weight_display_format, firstRecord!!.weightG!! / 1000.0f),
                            fontSize = 14.sp,
                            color = Orange600,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (hasAssistance) {
                        Text(
                            text = stringResource(R.string.assistance_display_format, firstRecord!!.assistanceG!! / 1000.0f),
                            fontSize = 14.sp,
                            color = Amber500,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Sets
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
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
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Int?, Int?, Int?) -> Unit  // date, time, comment, distanceCm, weightG, assistanceG
) {
    val firstRecord = session.records.firstOrNull()

    var editDate by remember { mutableStateOf(session.date) }
    var editTime by remember { mutableStateOf(session.time) }
    var editComment by remember { mutableStateOf(session.comment) }
    var editDistance by remember { mutableStateOf(firstRecord?.distanceCm?.toString() ?: "") }
    var editWeight by remember {
        mutableStateOf(
            firstRecord?.weightG?.let { "%.1f".format(it / 1000.0f) } ?: ""
        )
    }
    var editAssistance by remember {
        mutableStateOf(
            firstRecord?.assistanceG?.let { "%.1f".format(it / 1000.0f) } ?: ""
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_session_info)) },
        text = {
            Column(
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

                // 距離入力（cm単位）
                OutlinedTextField(
                    value = editDistance,
                    onValueChange = { value ->
                        // 全角→半角変換
                        val normalized = value
                            .replace(Regex("[０-９]")) { (it.value[0].code - '０'.code + '0'.code).toChar().toString() }
                            .replace("．", ".").replace("－", "-")
                        if (normalized.isEmpty() || normalized == "-" || normalized.toIntOrNull() != null) {
                            editDistance = normalized
                        }
                    },
                    label = { Text(stringResource(R.string.distance_input_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )

                // 荷重入力（kg単位）
                OutlinedTextField(
                    value = editWeight,
                    onValueChange = { value ->
                        // 全角→半角変換
                        val normalized = value
                            .replace(Regex("[０-９]")) { (it.value[0].code - '０'.code + '0'.code).toChar().toString() }
                            .replace("．", ".")
                        val isValidDecimal = normalized.isEmpty() ||
                            normalized == "." ||
                            normalized.matches(Regex("^\\d*\\.?\\d?\$"))
                        if (isValidDecimal) {
                            editWeight = normalized
                        }
                    },
                    label = { Text(stringResource(R.string.weight_input_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    )
                )

                // アシスト入力（kg単位）
                OutlinedTextField(
                    value = editAssistance,
                    onValueChange = { value ->
                        // 全角→半角変換
                        val normalized = value
                            .replace(Regex("[０-９]")) { (it.value[0].code - '０'.code + '0'.code).toChar().toString() }
                            .replace("．", ".")
                        val isValidDecimal = normalized.isEmpty() ||
                            normalized == "." ||
                            normalized.matches(Regex("^\\d*\\.?\\d?\$"))
                        if (isValidDecimal) {
                            editAssistance = normalized
                        }
                    },
                    label = { Text(stringResource(R.string.assistance_input_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val distanceCm = editDistance.ifEmpty { null }?.toIntOrNull()
                    val weightG = editWeight.ifEmpty { null }?.toDoubleOrNull()?.let { (it * 1000).toInt() }
                    val assistanceG = editAssistance.ifEmpty { null }?.toDoubleOrNull()?.let { (it * 1000).toInt() }
                    onConfirm(editDate, editTime, editComment, distanceCm, weightG, assistanceG)
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