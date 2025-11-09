package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
    viewModel: TrainingViewModel,
    onNavigateBack: () -> Unit
) {
    val hierarchicalData by viewModel.hierarchicalExercises.collectAsState()
    val expandedGroups by viewModel.expandedGroups.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingExercise by remember { mutableStateOf<Exercise?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Exercise?>(null) }
    var showGroupMenu by remember { mutableStateOf<String?>(null) }
    var showGroupEditDialog by remember { mutableStateOf<String?>(null) }
    var showGroupDeleteDialog by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                color = Blue600
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
                        text = stringResource(R.string.exercise_creation),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Blue600
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.add),
                    tint = Color.White
                )
            }
        }
    ) { paddingValues ->
        if (hierarchicalData.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_exercises_add_with_plus),
                    color = Slate400,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(hierarchicalData) { group ->
                    ExpandableGroupCard(
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
                        onGroupMenuClick = {
                            showGroupMenu = group.groupName
                        },
                        onExerciseEdit = { exercise ->
                            editingExercise = exercise
                            showAddDialog = true
                        },
                        onExerciseDelete = { exercise ->
                            showDeleteDialog = exercise
                        }
                    )
                }
            }
        }
    }

    // 追加/編集ダイアログ
    if (showAddDialog) {
        UnifiedAddDialog(
            exercise = editingExercise,
            viewModel = viewModel,
            onDismiss = {
                showAddDialog = false
                editingExercise = null
            }
        )
    }

    // グループメニュー
    showGroupMenu?.let { groupName ->
        DropdownMenu(
            expanded = true,
            onDismissRequest = { showGroupMenu = null }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.rename_group)) },
                onClick = {
                    showGroupEditDialog = groupName
                    showGroupMenu = null
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete_group), color = Red600) },
                onClick = {
                    showGroupDeleteDialog = groupName
                    showGroupMenu = null
                }
            )
        }
    }

    // グループ編集ダイアログ
    showGroupEditDialog?.let { oldName ->
        GroupEditDialog(
            oldName = oldName,
            onDismiss = { showGroupEditDialog = null },
            onConfirm = { newName ->
                viewModel.renameGroup(oldName, newName)
                showGroupEditDialog = null
            }
        )
    }

    // グループ削除確認ダイアログ
    showGroupDeleteDialog?.let { groupName ->
        AlertDialog(
            onDismissRequest = { showGroupDeleteDialog = null },
            title = { Text(stringResource(R.string.delete_confirmation)) },
            text = { Text(stringResource(R.string.delete_group_confirm_message, groupName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteGroup(groupName)
                        showGroupDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Red600)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showGroupDeleteDialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // 種目削除確認ダイアログ
    showDeleteDialog?.let { exercise ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(R.string.delete_confirmation)) },
            text = { Text(stringResource(R.string.delete_exercise_confirm_message, exercise.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteExercise(exercise)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Red600)
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

// 階層表示カード
@Composable
fun ExpandableGroupCard(
    group: TrainingViewModel.GroupWithExercises,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onGroupMenuClick: () -> Unit,
    onExerciseEdit: (Exercise) -> Unit,
    onExerciseDelete: (Exercise) -> Unit
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
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
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

                    if (group.groupName != null) {
                        IconButton(
                            onClick = onGroupMenuClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.menu),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // 種目リスト
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(start = 40.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    group.exercises.forEach { exercise ->
                        ExerciseItemCompact(
                            exercise = exercise,
                            onEdit = { onExerciseEdit(exercise) },
                            onDelete = { onExerciseDelete(exercise) }
                        )
                    }
                }
            }
        }
    }
}

// コンパクトな種目アイテム
// コンパクトな種目アイテム
@Composable
fun ExerciseItemCompact(
    exercise: Exercise,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate700),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
                        color = Slate400
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

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit), tint = Blue600)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = Red600)
                }
            }
        }
    }
}

// 統一追加ダイアログ（種目とグループの両方に対応）
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedAddDialog(
    exercise: Exercise?,
    viewModel: TrainingViewModel,
    onDismiss: () -> Unit
) {
    val groups by viewModel.groups.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val existingGroupNames = remember(groups) { groups.map { it.name }.sorted() }

    // 作成種類（新規作成時のみ使用）
    var creationType by remember { mutableStateOf(if (exercise != null) "exercise" else "exercise") }

    // 種目用の状態
    var exerciseName by remember { mutableStateOf(exercise?.name ?: "") }
    var selectedType by remember { mutableStateOf(exercise?.type ?: "Dynamic") }
    var selectedLaterality by remember { mutableStateOf(exercise?.laterality ?: "Bilateral") }  // ← 追加
    var selectedGroup by remember { mutableStateOf(exercise?.group) }
    var selectedLevel by remember { mutableStateOf(exercise?.sortOrder?.coerceIn(1, 10) ?: 5) }
    var showGroupDropdown by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var isCreatingNewGroup by remember { mutableStateOf(false) }

    // お気に入り状態（ローカル管理でリアルタイム反映）
    var isFavorite by remember { mutableStateOf(exercise?.isFavorite ?: false) }

    // 課題設定用の状態
    var hasTarget by remember { mutableStateOf(exercise?.targetSets != null && exercise.targetValue != null) }
    var targetSets by remember { mutableStateOf(exercise?.targetSets?.toString() ?: "") }
    var targetValue by remember { mutableStateOf(exercise?.targetValue?.toString() ?: "") }

    // グループ用の状態
    var groupName by remember { mutableStateOf("") }

    val isDuplicate = remember(exerciseName, selectedType, exercises, exercise) {
        if (exerciseName.isBlank()) {
            false
        } else {
            exercises.any { ex ->
                ex.id != exercise?.id &&
                        ex.name.equals(exerciseName, ignoreCase = true) &&
                        ex.type == selectedType
            }
        }
    }

    val isGroupDuplicate = remember(groupName, groups) {
        if (groupName.isBlank()) {
            false
        } else {
            groups.any { it.name.equals(groupName, ignoreCase = true) }
        }
    }

    val isExerciseNameValid = exerciseName.isNotBlank() && exerciseName.length <= 30 && !isDuplicate
    val isGroupNameValid = groupName.isNotBlank() && groupName.length <= 20 && !isGroupDuplicate

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        when {
                            exercise != null -> R.string.edit_exercise_title
                            creationType == "group" -> R.string.create_group_title
                            else -> R.string.add_exercise_title
                        }
                    ),
                    modifier = Modifier.weight(1f)
                )

                // 星ボタン（種目作成・編集時に表示、グループ作成時は非表示）
                if (creationType == "exercise") {
                    IconButton(
                        onClick = {
                            isFavorite = !isFavorite
                        }
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = if (isFavorite) {
                                stringResource(R.string.remove_from_favorites)
                            } else {
                                stringResource(R.string.add_to_favorites)
                            },
                            tint = if (isFavorite) Color(0xFFFFD700) else Slate400
                        )
                    }
                }
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 作成種類選択（新規作成時のみ）
                if (exercise == null) {
                    item {
                        Text(stringResource(R.string.create_type), fontSize = 14.sp, color = Slate400, fontWeight = FontWeight.Bold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            FilterChip(
                                selected = creationType == "exercise",
                                onClick = { creationType = "exercise" },
                                label = { Text(stringResource(R.string.exercise)) }
                            )
                            FilterChip(
                                selected = creationType == "group",
                                onClick = { creationType = "group" },
                                label = { Text(stringResource(R.string.group)) }
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }

                // グループ作成フォーム
                if (creationType == "group" && exercise == null) {
                    item {
                        OutlinedTextField(
                            value = groupName,
                            onValueChange = { if (it.length <= 20) groupName = it },
                            label = { Text(stringResource(R.string.group_name)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = isGroupDuplicate,
                            supportingText = {
                                when {
                                    isGroupDuplicate -> Text(stringResource(R.string.duplicate_group_name), color = Red600)
                                    else -> Text(stringResource(R.string.character_count, groupName.length, 20), color = Slate400)
                                }
                            }
                        )
                    }
                }

                // 種目作成/編集フォーム
                if (creationType == "exercise" || exercise != null) {
                    item {
                        OutlinedTextField(
                            value = exerciseName,
                            onValueChange = { if (it.length <= 30) exerciseName = it },
                            label = { Text(stringResource(R.string.exercise_name)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = isDuplicate,
                            supportingText = {
                                when {
                                    isDuplicate -> Text(stringResource(R.string.duplicate_exercise_name), color = Red600)
                                    else -> Text(stringResource(R.string.character_count, exerciseName.length, 30), color = Slate400)
                                }
                            }
                        )
                    }

                    item {
                        Text(stringResource(R.string.type), fontSize = 14.sp, color = Slate400)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedType == "Dynamic",
                                onClick = { selectedType = "Dynamic" },
                                label = { Text("Dynamic") }
                            )
                            FilterChip(
                                selected = selectedType == "Isometric",
                                onClick = { selectedType = "Isometric" },
                                label = { Text("Isometric") }
                            )
                        }
                    }

                    // ← ここから追加: laterality 選択
                    item {
                        Text(stringResource(R.string.laterality), fontSize = 14.sp, color = Slate400, fontWeight = FontWeight.Bold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            FilterChip(
                                selected = selectedLaterality == "Bilateral",
                                onClick = { selectedLaterality = "Bilateral" },
                                label = { Text(stringResource(R.string.bilateral_with_parenthesis)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Blue600,
                                    selectedLabelColor = Color.White,
                                    containerColor = Slate700,
                                    labelColor = Slate300
                                )
                            )
                            FilterChip(
                                selected = selectedLaterality == "Unilateral",
                                onClick = { selectedLaterality = "Unilateral" },
                                label = { Text(stringResource(R.string.unilateral_with_parenthesis)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Purple600,
                                    selectedLabelColor = Color.White,
                                    containerColor = Slate700,
                                    labelColor = Slate300
                                )
                            )
                        }
                        Text(
                            text = stringResource(
                                if (selectedLaterality == "Bilateral") R.string.example_bilateral else R.string.example_unilateral
                            ),
                            fontSize = 12.sp,
                            color = Slate400,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    // ← ここまで追加

                    // 課題設定UI
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.set_challenge),
                                fontSize = 14.sp,
                                color = Slate400,
                                fontWeight = FontWeight.Bold
                            )
                            Checkbox(
                                checked = hasTarget,
                                onCheckedChange = { hasTarget = it }
                            )
                        }

                        if (hasTarget) {
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = targetSets,
                                    onValueChange = {
                                        if (it.isEmpty() || (it.all { char -> char.isDigit() } && it.toIntOrNull()?.let { num -> num in 1..20 } == true)) {
                                            targetSets = it
                                        }
                                    },
                                    label = { Text(stringResource(R.string.sets_count)) },
                                    placeholder = { Text(stringResource(R.string.example_3)) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                    )
                                )

                                OutlinedTextField(
                                    value = targetValue,
                                    onValueChange = {
                                        if (it.isEmpty() || (it.all { char -> char.isDigit() } && it.toIntOrNull()?.let { num -> num >= 1 } == true)) {
                                            targetValue = it
                                        }
                                    },
                                    label = { Text(stringResource(if (selectedType == "Dynamic") R.string.reps_label else R.string.time_label)) },
                                    placeholder = { Text(stringResource(if (selectedType == "Dynamic") R.string.example_10 else R.string.example_30)) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                    )
                                )
                            }

                            if (selectedLaterality == "Unilateral") {
                                Text(
                                    text = stringResource(R.string.per_side_parenthesis),
                                    fontSize = 12.sp,
                                    color = Slate400,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    item {
                        Text(stringResource(R.string.group_optional), fontSize = 14.sp, color = Slate400)

                        if (isCreatingNewGroup) {
                            OutlinedTextField(
                                value = newGroupName,
                                onValueChange = { if (it.length <= 20) newGroupName = it },
                                label = { Text(stringResource(R.string.new_group_name)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                supportingText = { Text("${newGroupName.length}/20", color = Slate400) },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        isCreatingNewGroup = false
                                        newGroupName = ""
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                                    }
                                }
                            )
                        } else {
                            ExposedDropdownMenuBox(
                                expanded = showGroupDropdown,
                                onExpandedChange = { showGroupDropdown = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedGroup ?: stringResource(R.string.no_group_display),
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
                                )

                                ExposedDropdownMenu(
                                    expanded = showGroupDropdown,
                                    onDismissRequest = { showGroupDropdown = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.no_group_display)) },
                                        onClick = {
                                            selectedGroup = null
                                            showGroupDropdown = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.new_group_plus), fontWeight = FontWeight.Bold, color = Blue600) },
                                        onClick = {
                                            isCreatingNewGroup = true
                                            showGroupDropdown = false
                                        }
                                    )
                                    existingGroupNames.forEach { group ->
                                        DropdownMenuItem(
                                            text = { Text(group) },
                                            onClick = {
                                                selectedGroup = group
                                                showGroupDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // レベル設定（課題設定がある場合のみ）
                    if (hasTarget && !isCreatingNewGroup) {
                        item {
                            Text(stringResource(R.string.level_display, selectedLevel), fontSize = 14.sp, color = Slate400)
                            Slider(
                                value = selectedLevel.toFloat(),
                                onValueChange = { selectedLevel = it.toInt() },
                                valueRange = 1f..10f,
                                steps = 8,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        // グループ作成
                        creationType == "group" && exercise == null && isGroupNameValid -> {
                            viewModel.createGroup(groupName)
                            onDismiss()
                        }
                        // 種目編集
                        exercise != null && isExerciseNameValid -> {
                            val finalGroup = if (isCreatingNewGroup && newGroupName.isNotBlank()) {
                                viewModel.createGroup(newGroupName)
                                newGroupName
                            } else {
                                selectedGroup
                            }

                            val finalTargetSets = if (hasTarget) targetSets.toIntOrNull() else null
                            val finalTargetValue = if (hasTarget) targetValue.toIntOrNull() else null
                            val finalSortOrder = if (hasTarget) selectedLevel else 0

                            viewModel.updateExercise(
                                exercise.copy(
                                    name = exerciseName,
                                    type = selectedType,
                                    laterality = selectedLaterality,
                                    group = finalGroup,
                                    sortOrder = finalSortOrder,
                                    targetSets = finalTargetSets,
                                    targetValue = finalTargetValue,
                                    isFavorite = isFavorite
                                )
                            )
                            onDismiss()
                        }
                        // 種目追加
                        creationType == "exercise" && exercise == null && isExerciseNameValid -> {
                            val finalGroup = if (isCreatingNewGroup && newGroupName.isNotBlank()) {
                                viewModel.createGroup(newGroupName)
                                newGroupName
                            } else {
                                selectedGroup
                            }

                            val finalTargetSets = if (hasTarget) targetSets.toIntOrNull() else null
                            val finalTargetValue = if (hasTarget) targetValue.toIntOrNull() else null
                            val finalSortOrder = if (hasTarget) selectedLevel else 0

                            viewModel.addExercise(
                                exerciseName,
                                selectedType,
                                finalGroup,
                                finalSortOrder,
                                selectedLaterality,
                                finalTargetSets,
                                finalTargetValue,
                                isFavorite
                            )
                            onDismiss()
                        }
                    }
                },
                enabled = when {
                    creationType == "group" && exercise == null -> isGroupNameValid
                    else -> isExerciseNameValid && (!isCreatingNewGroup || newGroupName.isNotBlank())
                }
            ) {
                Text(
                    stringResource(
                        when {
                            exercise != null -> R.string.save_button
                            creationType == "group" -> R.string.create_button
                            else -> R.string.add_button
                        }
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

// グループ編集ダイアログ
@Composable
fun GroupEditDialog(
    oldName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(oldName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_group)) },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { if (it.length <= 20) newName = it },
                label = { Text(stringResource(R.string.group_name)) },
                singleLine = true,
                supportingText = { Text("${newName.length}/20") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(newName) },
                enabled = newName.isNotBlank() && newName != oldName
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
}