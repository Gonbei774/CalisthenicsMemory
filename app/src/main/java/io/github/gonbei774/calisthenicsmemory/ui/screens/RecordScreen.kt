package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// 画面のステップを管理
private enum class RecordStep {
    SelectExercise,  // ステップ1: 種目選択
    InputWorkout     // ステップ2: ワークアウト入力
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    viewModel: TrainingViewModel,
    onNavigateBack: () -> Unit
) {
    val exercises by viewModel.exercises.collectAsState()

    var currentStep by remember { mutableStateOf(RecordStep.SelectExercise) }
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var numberOfSets by remember { mutableIntStateOf(1) }
    var setValues by remember { mutableStateOf(List(1) { "" }) }

    // Date and Time with pickers
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTime by remember { mutableStateOf(LocalTime.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    var comment by remember { mutableStateOf("") }

    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    // Update set values when number of sets changes
    LaunchedEffect(numberOfSets) {
        setValues = List(numberOfSets) { index ->
            setValues.getOrElse(index) { "" }
        }
    }

    // Validation
    val hasValues = setValues.any { it.isNotBlank() && it.toIntOrNull() != null && it.toInt() >= 0 }
    val canRecord = selectedExercise != null && hasValues

    when (currentStep) {
        RecordStep.SelectExercise -> {
            // ステップ1: 種目選択画面
            ExerciseSelectionScreen(
                exercises = exercises,
                onNavigateBack = onNavigateBack,
                onExerciseSelected = { exercise ->
                    selectedExercise = exercise
                    // 値をリセット
                    numberOfSets = 1
                    setValues = List(1) { "" }
                    comment = ""
                    selectedDate = LocalDate.now()
                    selectedTime = LocalTime.now()
                    currentStep = RecordStep.InputWorkout
                }
            )
        }

        RecordStep.InputWorkout -> {
            // ステップ2: ワークアウト入力画面
            WorkoutInputScreen(
                exercise = selectedExercise!!,
                numberOfSets = numberOfSets,
                setValues = setValues,
                selectedDate = selectedDate,
                selectedTime = selectedTime,
                comment = comment,
                showDatePicker = showDatePicker,
                showTimePicker = showTimePicker,
                canRecord = canRecord,
                dateFormatter = dateFormatter,
                timeFormatter = timeFormatter,
                onNavigateBack = {
                    currentStep = RecordStep.SelectExercise
                    selectedExercise = null
                },
                onNumberOfSetsChange = { newValue ->
                    numberOfSets = newValue
                    if (newValue < numberOfSets) {
                        setValues = setValues.take(newValue)
                    } else {
                        setValues = setValues + listOf("")
                    }
                },
                onSetValueChange = { index, value ->
                    if (value.isEmpty() || value.all { it.isDigit() }) {
                        val newList = setValues.toMutableList()
                        while (newList.size <= index) {
                            newList.add("")
                        }
                        newList[index] = value
                        setValues = newList
                    }
                },
                onCommentChange = { comment = it },
                onShowDatePicker = { showDatePicker = it },
                onShowTimePicker = { showTimePicker = it },
                onDateSelected = { selectedDate = it },
                onTimeSelected = { selectedTime = it },
                onRecord = {
                    val values = setValues
                        .filter { it.isNotBlank() }
                        .mapNotNull { it.toIntOrNull() }
                        .filter { it >= 0 }

                    if (values.isNotEmpty()) {
                        viewModel.addTrainingRecords(
                            exerciseId = selectedExercise!!.id,
                            values = values,
                            date = selectedDate.format(dateFormatter),
                            time = selectedTime.format(timeFormatter),
                            comment = comment
                        )

                        // Reset and go back to selection
                        selectedExercise = null
                        numberOfSets = 1
                        setValues = List(1) { "" }
                        selectedDate = LocalDate.now()
                        selectedTime = LocalTime.now()
                        comment = ""
                        currentStep = RecordStep.SelectExercise
                    }
                },
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun ExerciseSelectionScreen(
    exercises: List<Exercise>,
    onNavigateBack: () -> Unit,
    onExerciseSelected: (Exercise) -> Unit
) {
    // ViewModelを取得
    val viewModel: TrainingViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val hierarchicalData by viewModel.hierarchicalExercises.collectAsState()
    val expandedGroups by viewModel.expandedGroups.collectAsState()

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                color = Green600
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
                        text = stringResource(R.string.training_record),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    ) { paddingValues ->
        if (exercises.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.no_exercises_registered),
                        fontSize = 18.sp,
                        color = Slate400
                    )
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Blue600
                        )
                    ) {
                        Text(stringResource(R.string.go_to_settings))
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.select_exercise),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        count = hierarchicalData.size,
                        key = { index -> hierarchicalData[index].groupName ?: "ungrouped" }
                    ) { index ->
                        val group = hierarchicalData[index]
                        HierarchicalExerciseGroup(
                            group = group,
                            isExpanded = if (group.groupName != null) {
                                group.groupName in expandedGroups
                            } else {
                                "ungrouped" in expandedGroups
                            },
                            onExpandToggle = {
                                val key = group.groupName ?: "ungrouped"
                                viewModel.toggleGroupExpansion(key)
                            },
                            onExerciseSelected = onExerciseSelected
                        )
                    }
                }
            }
        }
    }
}

// 記録画面用の階層グループ
@Composable
fun HierarchicalExerciseGroup(
    group: TrainingViewModel.GroupWithExercises,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onExerciseSelected: (Exercise) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // グループヘッダー
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
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Text(
                            text = group.groupName ?: stringResource(R.string.no_group),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.exercises_count, group.exercises.size),
                            fontSize = 14.sp,
                            color = Slate400
                        )
                    }
                }
            }

            // 種目リスト
            androidx.compose.animation.AnimatedVisibility(
                visible = isExpanded,
                enter = androidx.compose.animation.expandVertically(),
                exit = androidx.compose.animation.shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(start = 40.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    group.exercises.forEach { exercise ->
                        ExerciseSelectionItem(
                            exercise = exercise,
                            onClick = { onExerciseSelected(exercise) }
                        )
                    }
                }
            }
        }
    }
}

// 記録画面用の種目選択アイテム
@Composable
fun ExerciseSelectionItem(
    exercise: Exercise,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate700),
        shape = RoundedCornerShape(8.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    // お気に入りバッジ
                    if (exercise.isFavorite) {
                        Surface(
                            color = Color(0xFFFFD700).copy(alpha = 0.3f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "★",
                                fontSize = 11.sp,
                                color = Color(0xFFFFD700),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // レベルバッジ（課題設定がある場合のみ）
                    if (exercise.targetSets != null && exercise.targetValue != null && exercise.sortOrder > 0) {
                        Surface(
                            color = Blue600.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Lv.${exercise.sortOrder}",
                                fontSize = 11.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // タイプバッジ（回数制/時間制）
                    Surface(
                        color = Slate600.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = stringResource(if (exercise.type == "Dynamic") R.string.dynamic_type else R.string.isometric_type),
                            fontSize = 11.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    // Unilateralバッジ
                    if (exercise.laterality == "Unilateral") {
                        Surface(
                            color = Purple600.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.one_sided),
                                fontSize = 11.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // 課題バッジ
                if (exercise.targetSets != null && exercise.targetValue != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(
                                if (exercise.laterality == "Unilateral") R.string.target_format_unilateral else R.string.target_format,
                                exercise.targetSets!!,
                                exercise.targetValue!!,
                                stringResource(if (exercise.type == "Dynamic") R.string.unit_reps else R.string.unit_seconds)
                            ),
                            fontSize = 12.sp,
                            color = Green400
                        )
                    }
                }
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.select),
                tint = Green400,
                modifier = Modifier.rotate(180f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutInputScreen(
    exercise: Exercise,
    numberOfSets: Int,
    setValues: List<String>,
    selectedDate: LocalDate,
    selectedTime: LocalTime,
    comment: String,
    showDatePicker: Boolean,
    showTimePicker: Boolean,
    canRecord: Boolean,
    dateFormatter: DateTimeFormatter,
    timeFormatter: DateTimeFormatter,
    onNavigateBack: () -> Unit,
    onNumberOfSetsChange: (Int) -> Unit,
    onSetValueChange: (Int, String) -> Unit,
    onCommentChange: (String) -> Unit,
    onShowDatePicker: (Boolean) -> Unit,
    onShowTimePicker: (Boolean) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
    onRecord: () -> Unit,
    viewModel: TrainingViewModel
) {
    // Unilateral判定
    val isUnilateral = exercise.laterality == "Unilateral"

    // Unilateral用の左右別の値管理
    var setValuesRight by remember { mutableStateOf(List(numberOfSets) { "" }) }
    var setValuesLeft by remember { mutableStateOf(List(numberOfSets) { "" }) }

    // セット数変更時の処理
    LaunchedEffect(numberOfSets) {
        if (isUnilateral) {
            setValuesRight = List(numberOfSets) { index ->
                setValuesRight.getOrElse(index) { "" }
            }
            setValuesLeft = List(numberOfSets) { index ->
                setValuesLeft.getOrElse(index) { "" }
            }
        }
    }

    // バリデーション
    val hasValidValues = if (isUnilateral) {
        // Unilateral: 右側に少なくとも1つ有効な値があればOK（0を含む）
        setValuesRight.any { it.isNotBlank() && it.toIntOrNull() != null && it.toInt() >= 0 }
    } else {
        // Bilateral: 従来通り（0を含む）
        setValues.any { it.isNotBlank() && it.toIntOrNull() != null && it.toInt() >= 0 }
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                color = Green600
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
                        text = exercise.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 目標表示
            if (exercise.targetSets != null && exercise.targetValue != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Slate800.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(
                                    if (exercise.laterality == "Unilateral") R.string.target_display_unilateral else R.string.target_display,
                                    exercise.targetSets!!,
                                    exercise.targetValue!!,
                                    stringResource(if (exercise.type == "Dynamic") R.string.unit_reps else R.string.unit_seconds)
                                ),
                                fontSize = 16.sp,
                                color = Green400,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // セット数選択
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Slate800
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.sets_count),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(
                                onClick = {
                                    if (numberOfSets > 1) {
                                        onNumberOfSetsChange(numberOfSets - 1)
                                    }
                                },
                                enabled = numberOfSets > 1,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Green600,
                                    disabledContainerColor = Slate700
                                )
                            ) {
                                Text(
                                    text = "−",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = numberOfSets.toString(),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = Green400
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Button(
                                onClick = {
                                    if (numberOfSets < 10) {
                                        onNumberOfSetsChange(numberOfSets + 1)
                                    }
                                },
                                enabled = numberOfSets < 10,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Green600,
                                    disabledContainerColor = Slate700
                                )
                            ) {
                                Text(
                                    text = "＋",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 入力フィールド
            if (isUnilateral) {
                // Unilateral: 左右2つの入力フィールド
                items(numberOfSets) { index ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Slate800
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.set_number_format, index + 1),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 右側入力
                                OutlinedTextField(
                                    value = setValuesRight.getOrElse(index) { "" },
                                    onValueChange = { value ->
                                        if (value.isEmpty() || value.all { it.isDigit() }) {
                                            val newList = setValuesRight.toMutableList()
                                            while (newList.size <= index) {
                                                newList.add("")
                                            }
                                            newList[index] = value
                                            setValuesRight = newList
                                        }
                                    },
                                    label = { Text(stringResource(R.string.right_value, if (exercise.type == "Dynamic") stringResource(R.string.reps_label) else stringResource(R.string.time_label))) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Green600,
                                        unfocusedBorderColor = Slate600,
                                        focusedLabelColor = Green600,
                                        unfocusedLabelColor = Slate400,
                                        cursorColor = Green600,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )

                                // 左側入力
                                OutlinedTextField(
                                    value = setValuesLeft.getOrElse(index) { "" },
                                    onValueChange = { value ->
                                        if (value.isEmpty() || value.all { it.isDigit() }) {
                                            val newList = setValuesLeft.toMutableList()
                                            while (newList.size <= index) {
                                                newList.add("")
                                            }
                                            newList[index] = value
                                            setValuesLeft = newList
                                        }
                                    },
                                    label = { Text(stringResource(R.string.left_value, if (exercise.type == "Dynamic") stringResource(R.string.reps_label) else stringResource(R.string.time_label))) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Purple600,
                                        unfocusedBorderColor = Slate600,
                                        focusedLabelColor = Purple600,
                                        unfocusedLabelColor = Slate400,
                                        cursorColor = Purple600,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            } else {
                // Bilateral: 従来通り1つの入力フィールド
                items(numberOfSets) { index ->
                    OutlinedTextField(
                        value = setValues.getOrElse(index) { "" },
                        onValueChange = { value ->
                            onSetValueChange(index, value)
                        },
                        label = {
                            Text(stringResource(R.string.set_number_with_unit, index + 1, if (exercise.type == "Dynamic") stringResource(R.string.reps_label) else stringResource(R.string.time_label)))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Green600,
                            unfocusedBorderColor = Slate600,
                            focusedLabelColor = Green600,
                            unfocusedLabelColor = Slate400,
                            cursorColor = Green600,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            }

            // 日付選択
            item {
                OutlinedButton(
                    onClick = { onShowDatePicker(true) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.date_format, selectedDate.format(dateFormatter)))
                }
            }

            // 時刻選択
            item {
                OutlinedButton(
                    onClick = { onShowTimePicker(true) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.time_format, selectedTime.format(timeFormatter)))
                }
            }

            // コメント
            item {
                OutlinedTextField(
                    value = comment,
                    onValueChange = onCommentChange,
                    label = { Text(stringResource(R.string.comment_optional)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Green600,
                        unfocusedBorderColor = Slate600,
                        focusedLabelColor = Green600,
                        unfocusedLabelColor = Slate400,
                        cursorColor = Green600,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            // 記録ボタン
            item {
                Button(
                    onClick = {
                        if (isUnilateral) {
                            // Unilateral: 左右の値を処理（0を含む）
                            val valuesRight = setValuesRight
                                .filter { it.isNotBlank() }
                                .mapNotNull { it.toIntOrNull() }
                                .filter { it >= 0 }

                            val valuesLeft = setValuesLeft
                                .filter { it.isNotBlank() }
                                .mapNotNull { it.toIntOrNull() }
                                .filter { it >= 0 }

                            if (valuesRight.isNotEmpty()) {
                                viewModel.addTrainingRecordsUnilateral(
                                    exerciseId = exercise.id,
                                    valuesRight = valuesRight,
                                    valuesLeft = valuesLeft,
                                    date = selectedDate.format(dateFormatter),
                                    time = selectedTime.format(timeFormatter),
                                    comment = comment
                                )
                                onNavigateBack()
                            }
                        } else {
                            // Bilateral: 従来通り
                            onRecord()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = hasValidValues,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Green600,
                        disabledContainerColor = Slate700
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = stringResource(R.string.record_button),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toEpochDay() * 86400000
        )
        DatePickerDialog(
            onDismissRequest = { onShowDatePicker(false) },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(LocalDate.ofEpochDay(millis / 86400000))
                    }
                    onShowDatePicker(false)
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { onShowDatePicker(false) }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime.hour,
            initialMinute = selectedTime.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { onShowTimePicker(false) },
            confirmButton = {
                TextButton(onClick = {
                    onTimeSelected(LocalTime.of(timePickerState.hour, timePickerState.minute))
                    onShowTimePicker(false)
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { onShowTimePicker(false) }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}