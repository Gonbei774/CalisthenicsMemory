package io.github.gonbei774.calisthenicsmemory.ui.screens.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import io.github.gonbei774.calisthenicsmemory.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel

// ViewMode enum
enum class ViewMode {
    List,       // 一覧モード
    Graph,      // グラフモード
    Challenge   // 課題モード
}

// データクラス
data class SessionInfo(
    val exerciseId: Long,
    val date: String,
    val time: String,
    val comment: String,
    val records: List<TrainingRecord>
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ViewScreen(
    viewModel: TrainingViewModel,
    onNavigateBack: () -> Unit
) {
    val exercises by viewModel.exercises.collectAsState()
    val records by viewModel.records.collectAsState()
    val hierarchicalData by viewModel.hierarchicalExercises.collectAsState()

    // ViewModeの状態
    var currentMode by remember { mutableStateOf(ViewMode.List) }

    // フィルター関連の状態
    var selectedExerciseFilter by remember { mutableStateOf<Exercise?>(null) }
    var selectedPeriod by remember { mutableStateOf<Period?>(null) }
    var showFilterBottomSheet by remember { mutableStateOf(false) }

    // 既存の状態変数
    var showDeleteDialog by remember { mutableStateOf<SessionInfo?>(null) }
    var editingRecord by remember { mutableStateOf<TrainingRecord?>(null) }
    var editValue by remember { mutableStateOf("") }
    var editValueRight by remember { mutableStateOf("") }
    var editValueLeft by remember { mutableStateOf("") }
    var showSessionEditDialog by remember { mutableStateOf<SessionInfo?>(null) }
    var showContextMenu by remember { mutableStateOf<SessionInfo?>(null) }

    // 一覧モード用のセッションデータ
    val sessions = remember(records, exercises) {
        records
            .groupBy { "${it.exerciseId}-${it.date}-${it.time}" }
            .map { (_, sessionRecords) ->
                val first = sessionRecords.first()
                SessionInfo(
                    exerciseId = first.exerciseId,
                    date = first.date,
                    time = first.time,
                    comment = first.comment,
                    records = sessionRecords.sortedBy { it.setNumber }
                )
            }
            .sortedWith(
                compareByDescending<SessionInfo> { it.date }
                    .thenByDescending { it.time }
            )
    }

    // フィルター済みセッション（種目＋期間）
    val filteredSessions = remember(sessions, selectedExerciseFilter, selectedPeriod) {
        var filtered = sessions

        // 種目フィルター
        if (selectedExerciseFilter != null) {
            filtered = filtered.filter { it.exerciseId == selectedExerciseFilter!!.id }
        }

        // 期間フィルター
        if (selectedPeriod != null) {
            val today = java.time.LocalDate.now()
            val cutoffDate = today.minusDays(selectedPeriod!!.days.toLong() - 1)

            filtered = filtered.filter { session ->
                try {
                    val sessionDate = java.time.LocalDate.parse(session.date)
                    sessionDate >= cutoffDate && sessionDate <= today
                } catch (e: Exception) {
                    false
                }
            }
        }

        filtered
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                color = Purple600
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
                        text = stringResource(R.string.view_records),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        },

        ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // タブ
            TabRow(
                selectedTabIndex = when (currentMode) {
                    ViewMode.List -> 0
                    ViewMode.Graph -> 1
                    ViewMode.Challenge -> 2
                },
                containerColor = Slate800,
                contentColor = Color.White
            ) {
                Tab(
                    selected = currentMode == ViewMode.List,
                    onClick = { currentMode = ViewMode.List },
                    text = {
                        Text(
                            stringResource(R.string.tab_list),
                            fontSize = 16.sp,
                            fontWeight = if (currentMode == ViewMode.List) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = currentMode == ViewMode.Graph,
                    onClick = { currentMode = ViewMode.Graph },
                    text = {
                        Text(
                            stringResource(R.string.tab_graph),
                            fontSize = 16.sp,
                            fontWeight = if (currentMode == ViewMode.Graph) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = currentMode == ViewMode.Challenge,
                    onClick = { currentMode = ViewMode.Challenge },
                    text = {
                        Text(
                            stringResource(R.string.tab_challenge),
                            fontSize = 16.sp,
                            fontWeight = if (currentMode == ViewMode.Challenge) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            // フィルターチップ（全タブで表示、一行に並べる）
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Slate800
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 種目フィルター
                    if (selectedExerciseFilter != null) {
                        // 選択中の種目を表示
                        FilterChip(
                            selected = true,
                            onClick = { selectedExerciseFilter = null },
                            label = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(stringResource(R.string.close_x))
                                    Text(selectedExerciseFilter!!.name)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Purple600,
                                selectedLabelColor = Color.White
                            )
                        )
                    } else {
                        // 未選択時は選択画面を開くボタンを表示
                        FilterChip(
                            selected = false,
                            onClick = { showFilterBottomSheet = true },
                            label = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = stringResource(R.string.select_exercise_filter),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(stringResource(R.string.select_exercise_filter))
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Slate700,
                                labelColor = Color.White
                            )
                        )
                    }

                    // 期間フィルター（トグル式）
                    listOf(Period.OneWeek, Period.OneMonth, Period.ThreeMonths).forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = {
                                selectedPeriod = if (selectedPeriod == period) null else period
                            },
                            label = { Text(stringResource(period.displayNameResId)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Purple600,
                                selectedLabelColor = Color.White,
                                containerColor = Slate700,
                                labelColor = Slate300
                            )
                        )
                    }
                }
            }

            // モードに応じた表示
            when (currentMode) {
                ViewMode.List -> {
                    RecordListView(
                        sessions = filteredSessions,
                        exercises = exercises,
                        selectedExerciseFilter = null, // チップを上部に移動したのでnullに
                        onExerciseClick = { exercise ->
                            selectedExerciseFilter = exercise
                        },
                        onRecordClick = { record ->
                            editingRecord = record
                            if (record.valueLeft != null) {
                                // Unilateral
                                editValueRight = record.valueRight.toString()
                                editValueLeft = record.valueLeft.toString()
                            } else {
                                // Bilateral
                                editValue = record.valueRight.toString()
                            }
                        },
                        onSessionLongPress = { session ->
                            showContextMenu = session
                        },
                        onDeleteClick = { session ->
                            showDeleteDialog = session
                        }
                    )
                }
                ViewMode.Graph -> {
                    GraphView(
                        exercises = exercises,
                        records = records,
                        selectedExerciseFilter = selectedExerciseFilter,
                        selectedPeriod = selectedPeriod
                    )
                }
                ViewMode.Challenge -> {
                    ChallengeView(
                        exercises = exercises,
                        records = records,
                        selectedExerciseFilter = selectedExerciseFilter,
                        selectedPeriod = selectedPeriod,
                        onExerciseClick = { exercise ->
                            selectedExerciseFilter = exercise
                        }
                    )
                }
            }
        }
    }

    // Filter BottomSheet
    if (showFilterBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterBottomSheet = false }
        ) {
            FilterBottomSheetContent(
                exercises = exercises,
                hierarchicalData = hierarchicalData,
                selectedExercise = selectedExerciseFilter,
                onExerciseSelected = { exercise ->
                    selectedExerciseFilter = exercise
                    showFilterBottomSheet = false
                },
                onClearFilter = {
                    selectedExerciseFilter = null
                    showFilterBottomSheet = false
                }
            )
        }
    }

    // Edit Record Dialog
    editingRecord?.let { record ->
        val isUnilateral = record.valueLeft != null

        AlertDialog(
            onDismissRequest = {
                editingRecord = null
                editValue = ""
                editValueRight = ""
                editValueLeft = ""
            },
            title = { Text(stringResource(R.string.edit_set_value)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.set_number_format, record.setNumber),
                        color = Slate400,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (isUnilateral) {
                        // Unilateral: 左右2つの入力
                        OutlinedTextField(
                            value = editValueRight,
                            onValueChange = {
                                if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                    editValueRight = it
                                }
                            },
                            label = { Text(stringResource(R.string.right_value_label)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editValueLeft,
                            onValueChange = {
                                if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                    editValueLeft = it
                                }
                            },
                            label = { Text(stringResource(R.string.left_value_label)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // Bilateral: 1つの入力
                        OutlinedTextField(
                            value = editValue,
                            onValueChange = {
                                if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                    editValue = it
                                }
                            },
                            label = { Text(stringResource(R.string.value_label)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isUnilateral) {
                            // Unilateral: 左右両方更新
                            val newValueRight = editValueRight.toIntOrNull()
                            val newValueLeft = editValueLeft.toIntOrNull()

                            if (newValueRight != null && newValueRight >= 0) {
                                viewModel.updateRecord(
                                    record.copy(
                                        valueRight = newValueRight,
                                        valueLeft = newValueLeft
                                    )
                                )
                                editingRecord = null
                                editValueRight = ""
                                editValueLeft = ""
                            }
                        } else {
                            // Bilateral: 従来通り
                            editValue.toIntOrNull()?.let { newValue ->
                                if (newValue >= 0) {
                                    viewModel.updateRecord(record.copy(valueRight = newValue))
                                    editingRecord = null
                                    editValue = ""
                                }
                            }
                        }
                    },
                    enabled = if (isUnilateral) {
                        editValueRight.toIntOrNull()?.let { it >= 0 } == true
                    } else {
                        editValue.toIntOrNull()?.let { it >= 0 } == true
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    editingRecord = null
                    editValue = ""
                    editValueRight = ""
                    editValueLeft = ""
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Context Menu
    showContextMenu?.let { session ->
        DropdownMenu(
            expanded = true,
            onDismissRequest = { showContextMenu = null }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.edit_session_info)) },
                onClick = {
                    showSessionEditDialog = session
                    showContextMenu = null
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete_session), color = Red600) },
                onClick = {
                    showDeleteDialog = session
                    showContextMenu = null
                }
            )
        }
    }

    // Session Edit Dialog
    showSessionEditDialog?.let { session ->
        SessionEditDialog(
            session = session,
            onDismiss = { showSessionEditDialog = null },
            onConfirm = { newDate, newTime, newComment ->
                session.records.forEach { record ->
                    viewModel.updateRecord(
                        record.copy(
                            date = newDate,
                            time = newTime,
                            comment = newComment
                        )
                    )
                }
                showSessionEditDialog = null
            }
        )
    }

    // Delete Dialog
    showDeleteDialog?.let { session ->
        val exercise = exercises.find { it.id == session.exerciseId }
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(R.string.delete_confirmation)) },
            text = {
                Text(stringResource(
                    R.string.delete_record_warning,
                    exercise?.name ?: stringResource(R.string.unknown_short),
                    session.date,
                    session.time,
                    session.records.size
                ))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSession(
                            session.exerciseId,
                            session.date,
                            session.time
                        )
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Red600
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

// フィルターBottomSheetのコンテンツ
@Composable
fun FilterBottomSheetContent(
    exercises: List<Exercise>,
    hierarchicalData: List<TrainingViewModel.GroupWithExercises>,
    selectedExercise: Exercise?,
    onExerciseSelected: (Exercise?) -> Unit,
    onClearFilter: () -> Unit
) {
    var expandedGroups by remember { mutableStateOf(setOf<String?>()) }
    var searchQuery by remember { mutableStateOf("") }

    // 検索フィルター
    val filteredHierarchicalData = remember(hierarchicalData, searchQuery) {
        if (searchQuery.isEmpty()) {
            hierarchicalData
        } else {
            hierarchicalData.map { group ->
                TrainingViewModel.GroupWithExercises(
                    groupName = group.groupName,
                    exercises = group.exercises.filter {
                        it.name.contains(searchQuery, ignoreCase = true)
                    }
                )
            }.filter { it.exercises.isNotEmpty() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.select_exercise),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 検索バー
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            placeholder = { Text(stringResource(R.string.search_exercise), color = Slate400) },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = stringResource(R.string.search),
                    tint = Slate400
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.clear),
                            tint = Slate400
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Purple600,
                unfocusedBorderColor = Slate600,
                cursorColor = Purple600
            ),
            singleLine = true
        )

        // スクロール可能な階層表示
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 全て表示
            if (searchQuery.isEmpty()) {
                item {
                    FilterTextItem(
                        text = stringResource(R.string.show_all),
                        isSelected = selectedExercise == null,
                        onClick = onClearFilter
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // 階層表示（アコーディオン式）
            filteredHierarchicalData.forEach { group ->
                // グループヘッダー
                if (group.groupName != null || group.exercises.isNotEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            onClick = {
                                expandedGroups = if (expandedGroups.contains(group.groupName)) {
                                    expandedGroups - group.groupName
                                } else {
                                    expandedGroups + group.groupName
                                }
                            },
                            color = Slate800,
                            shape = RoundedCornerShape(8.dp)
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
                                    Text(
                                        text = group.groupName ?: stringResource(R.string.no_group_display),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "(${group.exercises.size})",
                                        fontSize = 14.sp,
                                        color = Slate400
                                    )
                                }
                                Icon(
                                    imageVector = if (expandedGroups.contains(group.groupName)) {
                                        Icons.Default.KeyboardArrowDown
                                    } else {
                                        Icons.AutoMirrored.Filled.KeyboardArrowRight
                                    },
                                    contentDescription = null,
                                    tint = Slate400
                                )
                            }
                        }
                    }

                    // グループ内の種目（展開時のみ表示）
                    if (expandedGroups.contains(group.groupName)) {
                        group.exercises.forEach { exercise ->
                            item {
                                FilterExerciseItem(
                                    exercise = exercise,
                                    isSelected = selectedExercise?.id == exercise.id,
                                    onClick = { onExerciseSelected(exercise) },
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// Exerciseオブジェクト版（バッジ付き）
@Composable
fun FilterExerciseItem(
    exercise: Exercise,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        color = if (isSelected) Purple600.copy(alpha = 0.2f) else Color.Transparent,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    fontSize = 16.sp,
                    color = if (isSelected) Purple600 else Color.White,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )

                // バッジ行
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
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // レベルバッジ
                    if (exercise.targetSets != null && exercise.targetValue != null && exercise.sortOrder > 0) {
                        Surface(
                            color = Blue600.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Lv.${exercise.sortOrder}",
                                fontSize = 11.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // タイプバッジ
                    Surface(
                        color = Slate600.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = stringResource(if (exercise.type == "Dynamic") R.string.dynamic_type else R.string.isometric_type),
                            fontSize = 11.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Purple600
                )
            }
        }
    }
}

// テキストのみ版（"全て表示"など用）
@Composable
fun FilterTextItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        color = if (isSelected) Purple600.copy(alpha = 0.2f) else Color.Transparent,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                fontSize = 16.sp,
                color = if (isSelected) Purple600 else Color.White,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Purple600
                )
            }
        }
    }
}

// ========================================
// 課題画面
// ========================================

@Composable
fun ChallengeView(
    exercises: List<Exercise>,
    records: List<TrainingRecord>,
    selectedExerciseFilter: Exercise?,
    selectedPeriod: Period?,
    onExerciseClick: (Exercise) -> Unit
) {
    // ViewModelを取得（階層データ用）
    val viewModel: TrainingViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val hierarchicalData by viewModel.hierarchicalExercises.collectAsState()

    // フィルターを適用（種目 + 期間）
    val filteredExercises = remember(exercises, records, selectedExerciseFilter, selectedPeriod) {
        var filtered = exercises

        // 種目フィルター
        if (selectedExerciseFilter != null) {
            filtered = listOf(selectedExerciseFilter!!)
        }

        // 期間フィルター（nullの場合は全期間）
        if (selectedPeriod != null) {
            val today = java.time.LocalDate.now()
            val cutoffDate = today.minusDays(selectedPeriod!!.days.toLong() - 1)

            // 期間内に記録がある種目のみを抽出
            val exerciseIdsWithRecords = records.filter { record ->
                try {
                    val recordDate = java.time.LocalDate.parse(record.date)
                    recordDate >= cutoffDate && recordDate <= today
                } catch (e: Exception) {
                    false
                }
            }.map { it.exerciseId }.toSet()

            filtered = filtered.filter { it.id in exerciseIdsWithRecords }
        }

        filtered
    }

    if (filteredExercises.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (selectedPeriod == null) {
                        stringResource(R.string.no_exercises_available)
                    } else {
                        stringResource(R.string.no_exercises_in_period, stringResource(selectedPeriod.displayNameResId))
                    },
                    fontSize = 18.sp,
                    color = Slate400
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            hierarchicalData.forEach { group ->
                // グループ内の種目で期間フィルターを通過したものを抽出
                val groupFilteredExercises = group.exercises.filter { it in filteredExercises }

                if (groupFilteredExercises.isNotEmpty()) {
                    // グループヘッダー
                    item {
                        Text(
                            text = group.groupName ?: stringResource(R.string.no_group_display),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    // グループ内の種目
                    groupFilteredExercises.forEach { exercise ->
                        item {
                            ChallengeExerciseCard(
                                exercise = exercise,
                                records = records,
                                isSelected = selectedExerciseFilter?.id == exercise.id,
                                onClick = { onExerciseClick(exercise) }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// 課題評価データクラス
data class ChallengeStatus(
    val level: Int,
    val status: ChallengeResult,
    val achievementRate: Int,
    val lastAchievedDate: String?
)

enum class ChallengeResult {
    Perfect,      // 🟢 100%以上
    Good,         // 🟦 75-99%
    NearlyThere,  // 🟡 50-74%
    NeedWork,     // 🔴 50%未満
    NoRecord      // - 未記録
}

// 種目カード（課題タブ用）
@Composable
fun ChallengeExerciseCard(
    exercise: Exercise,
    records: List<TrainingRecord>,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val hasChallenge = exercise.targetSets != null && exercise.targetValue != null

    // 課題ありの場合、ステータスを計算
    val status = if (hasChallenge) {
        remember(exercise, records) {
            calculateChallengeStatus(exercise, records)
        }
    } else null

    // 最終記録日を取得
    val lastRecordDate = remember(exercise, records) {
        records.filter { it.exerciseId == exercise.id }
            .maxByOrNull { "${it.date} ${it.time}" }
            ?.date
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Slate600 else Slate700
        ),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 種目名とレベル
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // レベルバッジ（左側）
                if (exercise.sortOrder > 0) {
                    Surface(
                        color = Blue600.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Lv.${exercise.sortOrder}",
                            fontSize = 12.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 種目名
                Text(
                    text = exercise.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // 達成マーク
                if (hasChallenge && status != null && status.achievementRate >= 100 && status.status == ChallengeResult.Perfect) {
                    Text(
                        text = "✅",
                        fontSize = 18.sp
                    )
                }
            }

            // 課題ありの場合
            if (hasChallenge && status != null) {
                // プログレスバー（色を統一）
                val progress = (status.achievementRate / 100f).coerceIn(0f, 1f)

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = Purple600,  // 統一された色
                    trackColor = Slate800,
                )

                // 達成率と実績/目標
                val targetTotal = exercise.targetSets!! * exercise.targetValue!!
                val actualTotal = calculateActualTotal(exercise, records, status)
                val unit = stringResource(if (exercise.type == "Dynamic") R.string.unit_reps else R.string.unit_seconds)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${status.achievementRate}% ($actualTotal/$targetTotal$unit)",
                        fontSize = 14.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    if (lastRecordDate != null) {
                        Text(
                            text = stringResource(R.string.last_workout_date, lastRecordDate),
                            fontSize = 12.sp,
                            color = Slate400
                        )
                    }
                }
            } else {
                // 課題なしの場合
                Text(
                    text = stringResource(R.string.no_challenge_set),
                    fontSize = 14.sp,
                    color = Slate400
                )

                if (lastRecordDate != null) {
                    Text(
                        text = stringResource(R.string.last_workout_date, lastRecordDate),
                        fontSize = 12.sp,
                        color = Slate400
                    )
                } else {
                    Text(
                        text = stringResource(R.string.last_workout_none),
                        fontSize = 12.sp,
                        color = Slate400
                    )
                }
            }
        }
    }
}

// 実績の合計値を計算（上位N個の合計）
fun calculateActualTotal(
    exercise: Exercise,
    records: List<TrainingRecord>,
    status: ChallengeStatus
): Int {
    val targetSets = exercise.targetSets ?: return 0
    val exerciseRecords = records.filter { it.exerciseId == exercise.id }

    if (exerciseRecords.isEmpty()) return 0

    // セッションごとにグループ化して最良セッションの上位N個の合計を計算
    val sessions = exerciseRecords
        .groupBy { "${it.date}-${it.time}" }
        .map { (_, sessionRecords) ->
            if (exercise.laterality == "Unilateral") {
                // Unilateral: 右・左それぞれの上位N個を計算して平均
                val topRight = sessionRecords
                    .map { it.valueRight }
                    .sortedDescending()
                    .take(targetSets)
                    .sum()
                val topLeft = sessionRecords
                    .mapNotNull { it.valueLeft }
                    .sortedDescending()
                    .take(targetSets)
                    .sum()
                (topRight + topLeft) / 2
            } else {
                // Bilateral: 右側の上位N個の合計
                sessionRecords
                    .map { it.valueRight }
                    .sortedDescending()
                    .take(targetSets)
                    .sum()
            }
        }

    return sessions.maxOrNull() ?: 0
}

// 課題ステータス計算関数
fun calculateChallengeStatus(
    exercise: Exercise,
    records: List<TrainingRecord>
): ChallengeStatus {
    val targetSets = exercise.targetSets ?: return ChallengeStatus(
        level = exercise.sortOrder,
        status = ChallengeResult.NoRecord,
        achievementRate = 0,
        lastAchievedDate = null
    )
    val targetValue = exercise.targetValue ?: return ChallengeStatus(
        level = exercise.sortOrder,
        status = ChallengeResult.NoRecord,
        achievementRate = 0,
        lastAchievedDate = null
    )

    // この種目の全記録を取得
    val exerciseRecords = records.filter { it.exerciseId == exercise.id }
    if (exerciseRecords.isEmpty()) {
        return ChallengeStatus(
            level = exercise.sortOrder,
            status = ChallengeResult.NoRecord,
            achievementRate = 0,
            lastAchievedDate = null
        )
    }

    // セッションごとにグループ化
    val targetTotal = targetSets * targetValue

    val sessions = exerciseRecords
        .groupBy { "${it.date}-${it.time}" }
        .map { (dateTime, sessionRecords) ->
            val rate = if (exercise.laterality == "Unilateral") {
                // Unilateral: 右・左それぞれの上位N個を計算
                val topRight = sessionRecords
                    .map { it.valueRight }
                    .sortedDescending()
                    .take(targetSets)
                    .sum()
                val topLeft = sessionRecords
                    .mapNotNull { it.valueLeft }
                    .sortedDescending()
                    .take(targetSets)
                    .sum()

                // 右・左それぞれの達成率を計算して平均
                val rateRight = (topRight * 100) / targetTotal
                val rateLeft = (topLeft * 100) / targetTotal
                (rateRight + rateLeft) / 2
            } else {
                // Bilateral: 右側の上位N個の合計
                val topValues = sessionRecords
                    .map { it.valueRight }
                    .sortedDescending()
                    .take(targetSets)
                    .sum()
                (topValues * 100) / targetTotal
            }

            Pair(dateTime, rate)
        }
        .sortedBy { it.first }  // 古い順にソート

    // 優先順位1: クリア条件を満たすセッション（達成率≥100%）
    val clearSessions = sessions.filter { (_, rate) ->
        rate >= 100
    }

    if (clearSessions.isNotEmpty()) {
        // 最も古いクリアセッションを採用
        val (dateTime, rate) = clearSessions.first()
        val parts = dateTime.split("-")
        val achievedDate = if (parts.size >= 3) "${parts[0]}-${parts[1]}-${parts[2]}" else null

        return ChallengeStatus(
            level = exercise.sortOrder,
            status = ChallengeResult.Perfect,
            achievementRate = rate,
            lastAchievedDate = achievedDate
        )
    }

    // 優先順位2: 未達成だが最善を尽くしているセッション
    val bestSession = sessions.maxByOrNull { it.second }!!
    val (_, bestRate) = bestSession

    val status = when {
        bestRate >= 75 -> ChallengeResult.Good
        bestRate >= 50 -> ChallengeResult.NearlyThere
        else -> ChallengeResult.NeedWork
    }

    return ChallengeStatus(
        level = exercise.sortOrder,
        status = status,
        achievementRate = bestRate,
        lastAchievedDate = null
    )
}