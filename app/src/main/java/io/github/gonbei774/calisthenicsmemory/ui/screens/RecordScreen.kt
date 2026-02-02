package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
import io.github.gonbei774.calisthenicsmemory.data.WorkoutPreferences
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.util.SearchUtils
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import kotlinx.coroutines.launch
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
    onNavigateBack: () -> Unit,
    initialExerciseId: Long? = null,
    fromToDo: Boolean = false
) {
    val context = LocalContext.current
    val workoutPreferences = remember { WorkoutPreferences(context) }
    val coroutineScope = rememberCoroutineScope()

    val exercises by viewModel.exercises.collectAsState()

    // Find initial exercise if provided
    val initialExercise = remember(initialExerciseId, exercises) {
        if (initialExerciseId != null) {
            exercises.find { it.id == initialExerciseId }
        } else {
            null
        }
    }

    var currentStep by remember(initialExercise) {
        mutableStateOf(
            if (initialExercise != null) RecordStep.InputWorkout else RecordStep.SelectExercise
        )
    }
    var selectedExercise by remember(initialExercise) { mutableStateOf<Exercise?>(initialExercise) }
    var numberOfSets by remember { mutableIntStateOf(1) }
    var setValues by remember { mutableStateOf(List(1) { "" }) }

    // 前回セッションのプリフィル用データ
    var prefillData by remember { mutableStateOf<List<TrainingRecord>?>(null) }

    // Date and Time with pickers
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTime by remember { mutableStateOf(LocalTime.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    var comment by remember { mutableStateOf("") }

    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    // initialExerciseから来た場合（ToDo経由）のプリフィル
    LaunchedEffect(initialExercise) {
        if (initialExercise != null && workoutPreferences.isPrefillPreviousRecordEnabled()) {
            val previousSession = viewModel.getLatestSession(initialExercise.id)
            if (previousSession.isNotEmpty()) {
                numberOfSets = previousSession.size
                setValues = previousSession.map { it.valueRight.toString() }
                prefillData = previousSession
            }
        }
    }

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
                    comment = ""
                    selectedDate = LocalDate.now()
                    selectedTime = LocalTime.now()

                    // プリフィル設定が有効な場合、前回セッションを取得
                    if (workoutPreferences.isPrefillPreviousRecordEnabled()) {
                        coroutineScope.launch {
                            val previousSession = viewModel.getLatestSession(exercise.id)
                            if (previousSession.isNotEmpty()) {
                                // セット数をプリフィル
                                numberOfSets = previousSession.size
                                // Bilateral用の値をプリフィル
                                setValues = previousSession.map { it.valueRight.toString() }
                                // Unilateral用のプリフィルデータを保存
                                prefillData = previousSession
                            } else {
                                // 前回記録がない場合はデフォルト
                                numberOfSets = 1
                                setValues = List(1) { "" }
                                prefillData = null
                            }
                            currentStep = RecordStep.InputWorkout
                        }
                    } else {
                        // プリフィル無効の場合は従来通り
                        numberOfSets = 1
                        setValues = List(1) { "" }
                        prefillData = null
                        currentStep = RecordStep.InputWorkout
                    }
                }
            )
        }

        RecordStep.InputWorkout -> {
            // ステップ2: ワークアウト入力画面
            WorkoutInputScreen(
                exercise = selectedExercise!!,
                numberOfSets = numberOfSets,
                setValues = setValues,
                prefillData = prefillData,
                selectedDate = selectedDate,
                selectedTime = selectedTime,
                comment = comment,
                showDatePicker = showDatePicker,
                showTimePicker = showTimePicker,
                canRecord = canRecord,
                dateFormatter = dateFormatter,
                timeFormatter = timeFormatter,
                fromToDo = fromToDo,
                onNavigateBack = {
                    // If from ToDo, go back to ToDo; otherwise go to exercise selection
                    if (fromToDo) {
                        onNavigateBack()
                    } else {
                        currentStep = RecordStep.SelectExercise
                        selectedExercise = null
                    }
                },
                onNumberOfSetsChange = { newValue ->
                    numberOfSets = newValue
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
    val appColors = LocalAppColors.current
    // ViewModelを取得
    val viewModel: TrainingViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val hierarchicalData by viewModel.hierarchicalExercises.collectAsState()
    val expandedGroups by viewModel.expandedGroups.collectAsState()

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
                        color = appColors.textSecondary
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
                    color = appColors.textPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
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
                        focusedContainerColor = appColors.cardBackground,
                        unfocusedContainerColor = appColors.cardBackground,
                        focusedBorderColor = Green600,
                        unfocusedBorderColor = appColors.border,
                        cursorColor = Green600
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (searchQuery.isNotBlank()) {
                        // Flat search results
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
                                ExerciseSelectionItem(
                                    exercise = exercise,
                                    onClick = { onExerciseSelected(exercise) }
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
}

// 記録画面用の階層グループ
@Composable
fun HierarchicalExerciseGroup(
    group: TrainingViewModel.GroupWithExercises,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onExerciseSelected: (Exercise) -> Unit
) {
    val appColors = LocalAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
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
                            tint = appColors.textPrimary
                        )
                        Text(
                            text = when (group.groupName) {
                                TrainingViewModel.FAVORITE_GROUP_KEY -> stringResource(R.string.favorite)
                                null -> stringResource(R.string.no_group)
                                else -> group.groupName
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.textPrimary
                        )
                        Text(
                            text = stringResource(R.string.exercises_count, group.exercises.size),
                            fontSize = 14.sp,
                            color = appColors.textSecondary
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
    val appColors = LocalAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = appColors.cardBackgroundSecondary),
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
                    color = appColors.textPrimary
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    // お気に入り
                    if (exercise.isFavorite) {
                        Text(
                            text = "★",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }

                    // レベル（課題設定がある場合のみ）
                    if (exercise.targetSets != null && exercise.targetValue != null && exercise.sortOrder > 0) {
                        Text(
                            text = "Lv.${exercise.sortOrder}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Blue600
                        )
                    }

                    // タイプ（回数制/時間制）
                    Text(
                        text = stringResource(if (exercise.type == "Dynamic") R.string.dynamic_type else R.string.isometric_type),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.textSecondary
                    )

                    // Unilateral
                    if (exercise.laterality == "Unilateral") {
                        Text(
                            text = stringResource(R.string.one_sided),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Purple600
                        )
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
                            fontWeight = FontWeight.Bold,
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
    prefillData: List<TrainingRecord>? = null,
    selectedDate: LocalDate,
    selectedTime: LocalTime,
    comment: String,
    showDatePicker: Boolean,
    showTimePicker: Boolean,
    canRecord: Boolean,
    dateFormatter: DateTimeFormatter,
    timeFormatter: DateTimeFormatter,
    fromToDo: Boolean = false,
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
    val appColors = LocalAppColors.current
    // Unilateral判定
    val isUnilateral = exercise.laterality == "Unilateral"

    // Unilateral用の左右別の値管理（プリフィルデータで初期化）
    var setValuesRight by remember(prefillData) {
        mutableStateOf(
            if (isUnilateral && prefillData != null) {
                prefillData.map { it.valueRight.toString() }
            } else {
                List(numberOfSets) { "" }
            }
        )
    }
    var setValuesLeft by remember(prefillData) {
        mutableStateOf(
            if (isUnilateral && prefillData != null) {
                prefillData.map { it.valueLeft?.toString() ?: "" }
            } else {
                List(numberOfSets) { "" }
            }
        )
    }

    // 距離・荷重入力（プリフィルデータで初期化）
    var distanceInput by remember(prefillData) {
        mutableStateOf(
            if (prefillData != null && exercise.distanceTrackingEnabled) {
                prefillData.firstOrNull()?.distanceCm?.toString() ?: ""
            } else {
                ""
            }
        )
    }
    var weightInput by remember(prefillData) {
        mutableStateOf(
            if (prefillData != null && exercise.weightTrackingEnabled) {
                // gからkgに変換（例: 1500g → 1.5）
                prefillData.firstOrNull()?.weightG?.let { (it / 1000.0).toString() } ?: ""
            } else {
                ""
            }
        )
    }
    var assistanceInput by remember(prefillData) {
        mutableStateOf(
            if (prefillData != null && exercise.assistanceTrackingEnabled) {
                // gからkgに変換（例: 1500g → 1.5）
                prefillData.firstOrNull()?.assistanceG?.let { (it / 1000.0).toString() } ?: ""
            } else {
                ""
            }
        )
    }

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
                            containerColor = appColors.cardBackground.copy(alpha = 0.6f)
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
                        containerColor = appColors.cardBackground
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
                            color = appColors.textPrimary,
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
                                    disabledContainerColor = appColors.cardBackgroundDisabled
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
                                    disabledContainerColor = appColors.cardBackgroundDisabled
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

            // 緑ボタン: 種目設定を適用
            if (exercise.targetSets != null || exercise.targetValue != null) {
                item {
                    Button(
                        onClick = {
                            // セット数を反映
                            val targetSetsValue = exercise.targetSets ?: numberOfSets
                            if (exercise.targetSets != null) {
                                onNumberOfSetsChange(targetSetsValue)
                            }
                            // 各セットの値を反映
                            if (exercise.targetValue != null) {
                                val targetStr = exercise.targetValue.toString()
                                if (isUnilateral) {
                                    // Unilateral: 左右両方に反映
                                    setValuesRight = List(targetSetsValue) { targetStr }
                                    setValuesLeft = List(targetSetsValue) { targetStr }
                                } else {
                                    // Bilateral: setValues経由で反映
                                    for (i in 0 until targetSetsValue) {
                                        onSetValueChange(i, targetStr)
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Green600,
                            disabledContainerColor = Slate600
                        ),
                        enabled = exercise.targetSets != null || exercise.targetValue != null,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.apply_exercise_settings),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
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
                            containerColor = appColors.cardBackground
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
                                color = appColors.textPrimary
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
                                        unfocusedBorderColor = appColors.border,
                                        focusedLabelColor = Green600,
                                        unfocusedLabelColor = appColors.textSecondary,
                                        cursorColor = Green600,
                                        focusedTextColor = appColors.textPrimary,
                                        unfocusedTextColor = appColors.textPrimary
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
                                        unfocusedBorderColor = appColors.border,
                                        focusedLabelColor = Purple600,
                                        unfocusedLabelColor = appColors.textSecondary,
                                        cursorColor = Purple600,
                                        focusedTextColor = appColors.textPrimary,
                                        unfocusedTextColor = appColors.textPrimary
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
                            unfocusedBorderColor = appColors.border,
                            focusedLabelColor = Green600,
                            unfocusedLabelColor = appColors.textSecondary,
                            cursorColor = Green600,
                            focusedTextColor = appColors.textPrimary,
                            unfocusedTextColor = appColors.textPrimary
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
                        contentColor = appColors.textPrimary
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
                        contentColor = appColors.textPrimary
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
                        unfocusedBorderColor = appColors.border,
                        focusedLabelColor = Green600,
                        unfocusedLabelColor = appColors.textSecondary,
                        cursorColor = Green600,
                        focusedTextColor = appColors.textPrimary,
                        unfocusedTextColor = appColors.textPrimary
                    )
                )
            }

            // 距離入力（有効な場合のみ表示）- 負の値・0・正の値を許可
            if (exercise.distanceTrackingEnabled) {
                item {
                    OutlinedTextField(
                        value = distanceInput,
                        onValueChange = { value ->
                            // 全角→半角変換
                            val normalized = value
                                .replace(Regex("[０-９]")) { (it.value[0].code - '０'.code + '0'.code).toChar().toString() }
                                .replace("．", ".").replace("－", "-")
                            // 空、"-"、または整数（負を含む）を許可
                            if (normalized.isEmpty() || normalized == "-" || normalized.toIntOrNull() != null) {
                                distanceInput = normalized
                            }
                        },
                        label = { Text(stringResource(R.string.distance_input_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Blue600,
                            unfocusedBorderColor = appColors.border,
                            focusedLabelColor = Blue600,
                            unfocusedLabelColor = appColors.textSecondary,
                            cursorColor = Blue600,
                            focusedTextColor = appColors.textPrimary,
                            unfocusedTextColor = appColors.textPrimary
                        )
                    )
                }
            }

            // 荷重入力（有効な場合のみ表示）- kg単位で小数第1位まで
            if (exercise.weightTrackingEnabled) {
                item {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { value ->
                            // 全角→半角変換
                            val normalized = value
                                .replace(Regex("[０-９]")) { (it.value[0].code - '０'.code + '0'.code).toChar().toString() }
                                .replace("．", ".")
                            // 空、または小数（小数点1つまで、小数第1位まで）を許可
                            val isValidDecimal = normalized.isEmpty() ||
                                normalized == "." ||
                                normalized.matches(Regex("^\\d*\\.?\\d?\$"))
                            if (isValidDecimal) {
                                weightInput = normalized
                            }
                        },
                        label = { Text(stringResource(R.string.weight_input_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Orange600,
                            unfocusedBorderColor = appColors.border,
                            focusedLabelColor = Orange600,
                            unfocusedLabelColor = appColors.textSecondary,
                            cursorColor = Orange600,
                            focusedTextColor = appColors.textPrimary,
                            unfocusedTextColor = appColors.textPrimary
                        )
                    )
                }
            }

            // アシスト入力（有効な場合のみ表示）- kg単位で小数第1位まで
            if (exercise.assistanceTrackingEnabled) {
                item {
                    OutlinedTextField(
                        value = assistanceInput,
                        onValueChange = { value ->
                            // 全角→半角変換
                            val normalized = value
                                .replace(Regex("[０-９]")) { (it.value[0].code - '０'.code + '0'.code).toChar().toString() }
                                .replace("．", ".")
                            // 空、または小数（小数点1つまで、小数第1位まで）を許可
                            val isValidDecimal = normalized.isEmpty() ||
                                normalized == "." ||
                                normalized.matches(Regex("^\\d*\\.?\\d?\$"))
                            if (isValidDecimal) {
                                assistanceInput = normalized
                            }
                        },
                        label = { Text(stringResource(R.string.assistance_input_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Amber500,
                            unfocusedBorderColor = appColors.border,
                            focusedLabelColor = Amber500,
                            unfocusedLabelColor = appColors.textSecondary,
                            cursorColor = Amber500,
                            focusedTextColor = appColors.textPrimary,
                            unfocusedTextColor = appColors.textPrimary
                        )
                    )
                }
            }

            // 記録ボタン
            item {
                Button(
                    onClick = {
                        // 距離・荷重・アシストの値を取得（空の場合はnull）
                        val distanceCm = distanceInput.ifEmpty { null }?.toIntOrNull()
                        // 荷重はkgで入力、gに変換して保存（例: 1.5kg → 1500g）
                        val weightG = weightInput.ifEmpty { null }?.toDoubleOrNull()?.let { (it * 1000).toInt() }
                        // アシストはkgで入力、gに変換して保存（例: 22.5kg → 22500g）
                        val assistanceG = assistanceInput.ifEmpty { null }?.toDoubleOrNull()?.let { (it * 1000).toInt() }

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
                                    comment = comment,
                                    distanceCm = distanceCm,
                                    weightG = weightG,
                                    assistanceG = assistanceG
                                )
                                // Delete todo task if from ToDo
                                if (fromToDo) {
                                    viewModel.deleteTodoTaskByExerciseId(exercise.id)
                                }
                                onNavigateBack()
                            }
                        } else {
                            // Bilateral: 距離・荷重を含めて記録
                            val values = setValues
                                .filter { it.isNotBlank() }
                                .mapNotNull { it.toIntOrNull() }
                                .filter { it >= 0 }

                            if (values.isNotEmpty()) {
                                viewModel.addTrainingRecords(
                                    exerciseId = exercise.id,
                                    values = values,
                                    date = selectedDate.format(dateFormatter),
                                    time = selectedTime.format(timeFormatter),
                                    comment = comment,
                                    distanceCm = distanceCm,
                                    weightG = weightG,
                                    assistanceG = assistanceG
                                )
                                // Delete todo task if from ToDo
                                if (fromToDo) {
                                    viewModel.deleteTodoTaskByExerciseId(exercise.id)
                                }
                                onNavigateBack()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = hasValidValues,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Green600,
                        disabledContainerColor = appColors.cardBackgroundDisabled
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