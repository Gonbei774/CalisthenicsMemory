package io.github.gonbei774.calisthenicsmemory.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ExerciseGroup
import io.github.gonbei774.calisthenicsmemory.data.Program
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.IntervalProgram
import io.github.gonbei774.calisthenicsmemory.data.IntervalProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.IntervalRecord
import io.github.gonbei774.calisthenicsmemory.data.ProgramLoop
import io.github.gonbei774.calisthenicsmemory.data.TodoTask
import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
import io.github.gonbei774.calisthenicsmemory.ui.UiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.database.sqlite.SQLiteConstraintException
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// バックアップ用データクラス
@Serializable
data class BackupData(
    val version: Int,
    val exportDate: String,
    val app: String,
    val groups: List<ExportGroup>,
    val exercises: List<ExportExercise>,
    val records: List<ExportRecord>,
    val programs: List<ExportProgram> = emptyList(),           // v4で追加
    val programExercises: List<ExportProgramExercise> = emptyList(),  // v4で追加
    val programLoops: List<ExportProgramLoop> = emptyList(),    // v5で追加（後方互換性のためデフォルト空）
    val intervalPrograms: List<ExportIntervalProgram> = emptyList(),              // v7で追加
    val intervalProgramExercises: List<ExportIntervalProgramExercise> = emptyList(), // v7で追加
    val intervalRecords: List<ExportIntervalRecord> = emptyList(),                 // v7で追加
    val todoTasks: List<ExportTodoTask> = emptyList()                             // v8で追加
)

@Serializable
data class ExportGroup(
    val id: Long,
    val name: String,
    val displayOrder: Int = 0
)

@Serializable
data class ExportExercise(
    val id: Long,
    val name: String,
    val type: String,
    val group: String?,
    val sortOrder: Int,
    val displayOrder: Int = 0,       // 表示順（デフォルト値で後方互換）
    val laterality: String,
    val targetSets: Int? = null,
    val targetValue: Int? = null,
    val isFavorite: Boolean = false, // お気に入り（デフォルト値で後方互換）
    val restInterval: Int? = null,   // 種目固有の休憩時間（デフォルト値で後方互換）
    val repDuration: Int? = null,    // 種目固有の1レップ時間（デフォルト値で後方互換）
    val distanceTrackingEnabled: Boolean = false,  // 距離入力を有効化（v3で追加）
    val weightTrackingEnabled: Boolean = false,    // 荷重入力を有効化（v3で追加）
    val assistanceTrackingEnabled: Boolean = false, // アシスト入力を有効化（v6で追加）
    val description: String? = null                // 種目の説明文（v6で追加）
)

@Serializable
data class ExportRecord(
    val id: Long,
    val exerciseId: Long,
    val valueRight: Int,
    val valueLeft: Int?,
    val setNumber: Int,
    val date: String,
    val time: String,
    val comment: String,
    val distanceCm: Int? = null,  // 距離（cm、v3で追加）
    val weightG: Int? = null,     // 追加ウエイト（g、v3で追加）
    val assistanceG: Int? = null  // アシスト量（g、v6で追加）
)

@Serializable
data class ExportProgram(
    val id: Long,
    val name: String
    // timerMode/startIntervalはSharedPreferencesへ移行（v14）
    // 旧JSONインポート時はignoreUnknownKeysで無視される
)

@Serializable
data class ExportProgramExercise(
    val id: Long,
    val programId: Long,
    val exerciseId: Long,
    val sortOrder: Int,
    val sets: Int,
    val targetValue: Int,
    val intervalSeconds: Int,
    val loopId: Long? = null  // v5で追加（後方互換性のためデフォルトnull）
)

@Serializable
data class ExportProgramLoop(
    val id: Long,
    val programId: Long,
    val sortOrder: Int,
    val rounds: Int,
    val restBetweenRounds: Int
)

@Serializable
data class ExportIntervalProgram(
    val id: Long,
    val name: String,
    val workSeconds: Int,
    val restSeconds: Int,
    val rounds: Int,
    val roundRestSeconds: Int
)

@Serializable
data class ExportIntervalProgramExercise(
    val id: Long,
    val programId: Long,
    val exerciseId: Long,
    val sortOrder: Int
)

@Serializable
data class ExportIntervalRecord(
    val id: Long,
    val programName: String,
    val date: String,
    val time: String,
    val workSeconds: Int,
    val restSeconds: Int,
    val rounds: Int,
    val roundRestSeconds: Int,
    val completedRounds: Int,
    val completedExercisesInLastRound: Int,
    val exercisesJson: String,
    val comment: String? = null
)

@Serializable
data class ExportTodoTask(
    val id: Long,
    val type: String,
    val referenceId: Long,
    val sortOrder: Int,
    val repeatDays: String = "",
    val lastCompletedDate: String? = null
)

class TrainingViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val exerciseDao = database.exerciseDao()
    private val recordDao = database.trainingRecordDao()
    private val groupDao = database.exerciseGroupDao()
    private val todoTaskDao = database.todoTaskDao()
    private val programDao = database.programDao()
    private val programExerciseDao = database.programExerciseDao()
    private val programLoopDao = database.programLoopDao()
    private val intervalProgramDao = database.intervalProgramDao()
    private val intervalProgramExerciseDao = database.intervalProgramExerciseDao()
    private val intervalRecordDao = database.intervalRecordDao()

    companion object {
        // お気に入りグループの固定キー（UI側で翻訳される）
        const val FAVORITE_GROUP_KEY = "★FAVORITES"
    }

    // Exercises
    val exercises: StateFlow<List<Exercise>> = exerciseDao.getAllExercises()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // Exercise Groups
    val groups: StateFlow<List<ExerciseGroup>> = groupDao.getAllGroups()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // Training Records
    val records: StateFlow<List<TrainingRecord>> = recordDao.getAllRecords()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // Snackbar message (UiMessage型で言語変更に追従)
    private val _snackbarMessage = MutableStateFlow<UiMessage?>(null)
    val snackbarMessage: StateFlow<UiMessage?> = _snackbarMessage.asStateFlow()

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    fun showSnackbar(message: UiMessage) {
        _snackbarMessage.value = message
    }

    fun showBackupResult(success: Boolean) {
        _snackbarMessage.value = if (success) {
            UiMessage.BackupSaved
        } else {
            UiMessage.BackupFailed
        }
    }

    // Exercise operations
    fun addExercise(
        name: String,
        type: String,
        group: String? = null,
        sortOrder: Int = 0,
        laterality: String = "Bilateral",
        targetSets: Int? = null,
        targetValue: Int? = null,
        isFavorite: Boolean = false,
        restInterval: Int? = null,       // 種目固有の休憩時間（秒）
        repDuration: Int? = null,        // 種目固有の1レップ時間（秒）
        distanceTrackingEnabled: Boolean = false,  // 距離トラッキング有効
        weightTrackingEnabled: Boolean = false,    // 荷重トラッキング有効
        assistanceTrackingEnabled: Boolean = false, // アシストトラッキング有効
        description: String? = null                // 種目の説明文
    ) {
        viewModelScope.launch {
            try {
                val existingExercises = exercises.value
                val isDuplicate = existingExercises.any {
                    it.name.equals(name, ignoreCase = true) && it.type == type
                }

                if (isDuplicate) {
                    _snackbarMessage.value = UiMessage.AlreadyRegistered(name, type)
                    return@launch
                }

                val exercise = Exercise(
                    name = name,
                    type = type,
                    group = group,
                    sortOrder = sortOrder,
                    laterality = laterality,
                    targetSets = targetSets,
                    targetValue = targetValue,
                    isFavorite = isFavorite,
                    restInterval = restInterval,
                    repDuration = repDuration,
                    distanceTrackingEnabled = distanceTrackingEnabled,
                    weightTrackingEnabled = weightTrackingEnabled,
                    assistanceTrackingEnabled = assistanceTrackingEnabled,
                    description = description
                )
                exerciseDao.insertExercise(exercise)
                _snackbarMessage.value = UiMessage.ExerciseAdded
            } catch (e: SQLiteConstraintException) {
                _snackbarMessage.value = UiMessage.ExerciseAlreadyExists
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    fun updateExercise(exercise: Exercise) {
        viewModelScope.launch {
            try {
                val existingExercises = exercises.value
                val isDuplicate = existingExercises.any {
                    it.id != exercise.id &&
                            it.name.equals(exercise.name, ignoreCase = true) &&
                            it.type == exercise.type
                }

                if (isDuplicate) {
                    _snackbarMessage.value = UiMessage.AlreadyInUse(exercise.name, exercise.type)
                    return@launch
                }

                exerciseDao.updateExercise(exercise)
                _snackbarMessage.value = UiMessage.ExerciseUpdated
            } catch (e: SQLiteConstraintException) {
                _snackbarMessage.value = UiMessage.ExerciseAlreadyExists
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    fun deleteExercise(exercise: Exercise) {
        viewModelScope.launch {
            try {
                exerciseDao.deleteExercise(exercise)
                todoTaskDao.deleteByReference(TodoTask.TYPE_EXERCISE, exercise.id)
                _snackbarMessage.value = UiMessage.ExerciseDeleted
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    fun toggleFavorite(exerciseId: Long) {
        viewModelScope.launch {
            try {
                val exercise = exercises.value.find { it.id == exerciseId }
                exercise?.let {
                    val updated = it.copy(isFavorite = !it.isFavorite)
                    exerciseDao.updateExercise(updated)
                }
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    // Training Record operations
    // Training Record operations (既存)
    fun addTrainingRecords(
        exerciseId: Long,
        values: List<Int>,
        date: String,
        time: String,
        comment: String,
        distanceCm: Int? = null,   // 距離（cm）
        weightG: Int? = null,      // 追加ウエイト（g）
        assistanceG: Int? = null   // アシスト量（g）
    ) {
        viewModelScope.launch {
            try {
                val records = values.mapIndexed { index, value ->
                    TrainingRecord(
                        exerciseId = exerciseId,
                        valueRight = value,
                        valueLeft = null,
                        setNumber = index + 1,
                        date = date,
                        time = time,
                        comment = comment,
                        distanceCm = distanceCm,
                        weightG = weightG,
                        assistanceG = assistanceG
                    )
                }
                recordDao.insertRecords(records)
                _snackbarMessage.value = UiMessage.SetsRecorded(values.size)
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    // ← ここから追加: Unilateral種目用
    fun addTrainingRecordsUnilateral(
        exerciseId: Long,
        valuesRight: List<Int>,
        valuesLeft: List<Int>,
        date: String,
        time: String,
        comment: String,
        distanceCm: Int? = null,   // 距離（cm）
        weightG: Int? = null,      // 追加ウエイト（g）
        assistanceG: Int? = null   // アシスト量（g）
    ) {
        viewModelScope.launch {
            try {
                // 右側の値を基準にレコードを作成
                val records = valuesRight.mapIndexed { index, valueRight ->
                    TrainingRecord(
                        exerciseId = exerciseId,
                        valueRight = valueRight,
                        valueLeft = valuesLeft.getOrNull(index),  // 左側の値（なければnull）
                        setNumber = index + 1,
                        date = date,
                        time = time,
                        comment = comment,
                        distanceCm = distanceCm,
                        weightG = weightG,
                        assistanceG = assistanceG
                    )
                }
                recordDao.insertRecords(records)
                _snackbarMessage.value = UiMessage.SetsRecorded(valuesRight.size)
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    fun updateRecord(record: TrainingRecord) {
        viewModelScope.launch {
            try {
                recordDao.updateRecord(record)
                _snackbarMessage.value = UiMessage.RecordUpdated
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    fun deleteSession(exerciseId: Long, date: String, time: String) {
        viewModelScope.launch {
            try {
                recordDao.deleteSession(exerciseId, date, time)
                _snackbarMessage.value = UiMessage.RecordDeleted
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    // ========================================
    // Group operations
    // ========================================

    fun createGroup(name: String) {
        viewModelScope.launch {
            flushGroupOrder()
            try {
                val existingGroups = groupDao.getAllGroupsSync()
                val group = ExerciseGroup(name = name, displayOrder = existingGroups.size)
                groupDao.insertGroup(group)
                _snackbarMessage.value = UiMessage.GroupCreated
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.GroupAlreadyExists
            }
        }
    }

    fun renameGroup(oldName: String, newName: String) {
        viewModelScope.launch {
            flushGroupOrder()
            try {
                // 1. グループテーブルを更新
                val group = groupDao.getGroupByName(oldName)
                if (group != null) {
                    groupDao.updateGroup(group.copy(name = newName))
                }

                // 2. 種目のgroupフィールドも更新
                val affectedExercises = exercises.value.filter { it.group == oldName }
                affectedExercises.forEach { exercise ->
                    exerciseDao.updateExercise(exercise.copy(group = newName))
                }

                _snackbarMessage.value = UiMessage.GroupRenamed
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    fun deleteGroup(groupName: String) {
        viewModelScope.launch {
            flushGroupOrder()
            try {
                // ToDoの連動削除（グループ削除前にIDを取得）
                val group = groupDao.getGroupByName(groupName)
                if (group != null) {
                    todoTaskDao.deleteByReference(TodoTask.TYPE_GROUP, group.id)
                }

                // 1. グループテーブルから削除
                groupDao.deleteGroupByName(groupName)

                // 2. 種目のgroupをnullに
                val affectedExercises = exercises.value.filter { it.group == groupName }
                affectedExercises.forEach { exercise ->
                    exerciseDao.updateExercise(exercise.copy(group = null, sortOrder = 0))
                }

                _snackbarMessage.value = UiMessage.GroupDeleted
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }
    // ========================================
    // 階層表示用データ構造
    // ========================================

    data class GroupWithExercises(
        val groupName: String?,  // nullは「グループなし」
        val exercises: List<Exercise>,
        val isExpanded: Boolean = true  // 展開状態
    )

    // 展開状態管理
    private val _expandedGroups = MutableStateFlow<Set<String>>(emptySet())
    val expandedGroups: StateFlow<Set<String>> = _expandedGroups.asStateFlow()

    fun toggleGroupExpansion(groupName: String) {
        _expandedGroups.value = if (groupName in _expandedGroups.value) {
            _expandedGroups.value - groupName
        } else {
            _expandedGroups.value + groupName
        }
    }

    // グループ並び替え用ローカルステート（画面離脱時にDBに保存）
    private val _localGroupOrder = MutableStateFlow<List<ExerciseGroup>?>(null)

    // 階層データ準備
    val hierarchicalExercises: StateFlow<List<GroupWithExercises>> =
        combine(groups, exercises, expandedGroups, _localGroupOrder) { dbGroups, exercises, expanded, localOrder ->
            prepareHierarchicalData(localOrder ?: dbGroups, exercises, expanded)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private fun prepareHierarchicalData(
        groups: List<ExerciseGroup>,
        exercises: List<Exercise>,
        expandedGroups: Set<String>
    ): List<GroupWithExercises> {
        // 0. お気に入りグループ（先頭に追加、0件でも表示）
        // 固定キーを使用（UI側で翻訳）
        val favoriteGroupKey = FAVORITE_GROUP_KEY
        val favoriteExercises = exercises.filter { it.isFavorite }.sortedBy { it.displayOrder }
        val favoriteGroup = listOf(
            GroupWithExercises(
                groupName = favoriteGroupKey,
                exercises = favoriteExercises,
                isExpanded = favoriteGroupKey in expandedGroups
            )
        )

        // 1. groupsテーブルを基準にグループを表示（0件でも表示）
        // groups は既に displayOrder 順で取得されている
        val groupedExercises = groups.map { group ->
            val groupExercises = exercises.filter { it.group == group.name }
            GroupWithExercises(
                groupName = group.name,
                exercises = groupExercises.sortedBy { it.displayOrder },
                isExpanded = group.name in expandedGroups  // 全て閉じた状態も可能
            )
        }

        // 2. グループなし種目
        val ungroupedExercises = exercises.filter { it.group == null }
        val ungroupedGroup = if (ungroupedExercises.isNotEmpty()) {
            listOf(
                GroupWithExercises(
                    groupName = null,
                    exercises = ungroupedExercises.sortedBy { it.displayOrder },
                    isExpanded = "ungrouped" in expandedGroups  // 全て閉じた状態も可能
                )
            )
        } else {
            emptyList()
        }

        // お気に入りグループを先頭に配置
        return favoriteGroup + groupedExercises + ungroupedGroup
    }

    // ========================================
    // 並び替え機能
    // ========================================

    /**
     * 種目の並び順を変更する
     * @param groupName グループ名（null = グループなし、FAVORITE_GROUP_KEY = お気に入り）
     * @param fromIndex 移動元のインデックス
     * @param toIndex 移動先のインデックス
     */
    fun reorderExercises(groupName: String?, fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            // お気に入りグループでは並び替え不可（元のグループでの順序に影響するため）
            if (groupName == FAVORITE_GROUP_KEY) {
                return@launch
            }

            // 対象グループの種目を取得
            val targetExercises = when (groupName) {
                null -> {
                    // グループなし
                    exerciseDao.getUngroupedExercises()
                }
                else -> {
                    // 通常グループ
                    exerciseDao.getExercisesByGroup(groupName)
                }
            }

            if (fromIndex < 0 || toIndex < 0 ||
                fromIndex >= targetExercises.size ||
                toIndex >= targetExercises.size) {
                return@launch
            }

            // 並び替え
            val reordered = targetExercises.toMutableList()
            val item = reordered.removeAt(fromIndex)
            reordered.add(toIndex, item)

            // displayOrderを更新
            reordered.forEachIndexed { index, exercise ->
                exerciseDao.updateExercise(exercise.copy(displayOrder = index))
            }
        }
    }

    /**
     * グループの並び順を変更する（ローカルステートのみ更新）
     * DBへの保存は saveGroupOrder() で行う
     */
    fun reorderGroups(fromIndex: Int, toIndex: Int) {
        val current = (_localGroupOrder.value ?: groups.value).toList()

        if (fromIndex < 0 || toIndex < 0 ||
            fromIndex >= current.size ||
            toIndex >= current.size) {
            return
        }

        val reordered = current.toMutableList()
        val item = reordered.removeAt(fromIndex)
        reordered.add(toIndex, item)
        _localGroupOrder.value = reordered
    }

    /**
     * 保留中のグループ並び順をDBに保存する
     */
    fun saveGroupOrder() {
        val order = _localGroupOrder.value ?: return
        _localGroupOrder.value = null
        viewModelScope.launch {
            order.forEachIndexed { index, group ->
                groupDao.updateGroup(group.copy(displayOrder = index))
            }
        }
    }

    /**
     * 保留中のグループ並び順をDBに保存する（suspend版・内部用）
     */
    private suspend fun flushGroupOrder() {
        val order = _localGroupOrder.value ?: return
        _localGroupOrder.value = null
        order.forEachIndexed { index, group ->
            groupDao.updateGroup(group.copy(displayOrder = index))
        }
    }

    // ========================================
    // エクスポート・インポート機能
    // ========================================

    /**
     * データをJSON形式でエクスポート
     */
    suspend fun exportData(): String = withContext(Dispatchers.IO) {
        try {
            val currentGroups = groups.value
            val currentExercises = exercises.value
            val currentRecords = records.value

            val exportGroups = currentGroups.map { group ->
                ExportGroup(
                    id = group.id,
                    name = group.name,
                    displayOrder = group.displayOrder
                )
            }

            val exportExercises = currentExercises.map { exercise ->
                ExportExercise(
                    id = exercise.id,
                    name = exercise.name,
                    type = exercise.type,
                    group = exercise.group,
                    sortOrder = exercise.sortOrder,
                    displayOrder = exercise.displayOrder,
                    laterality = exercise.laterality,
                    targetSets = exercise.targetSets,
                    targetValue = exercise.targetValue,
                    isFavorite = exercise.isFavorite,
                    restInterval = exercise.restInterval,
                    repDuration = exercise.repDuration,
                    distanceTrackingEnabled = exercise.distanceTrackingEnabled,
                    weightTrackingEnabled = exercise.weightTrackingEnabled,
                    assistanceTrackingEnabled = exercise.assistanceTrackingEnabled,
                    description = exercise.description
                )
            }

            val exportRecords = currentRecords.map { record ->
                ExportRecord(
                    id = record.id,
                    exerciseId = record.exerciseId,
                    valueRight = record.valueRight,
                    valueLeft = record.valueLeft,
                    setNumber = record.setNumber,
                    date = record.date,
                    time = record.time,
                    comment = record.comment,
                    distanceCm = record.distanceCm,
                    weightG = record.weightG,
                    assistanceG = record.assistanceG
                )
            }

            // プログラムをエクスポート（v4で追加）
            val currentPrograms = programs.value
            val exportPrograms = currentPrograms.map { program ->
                ExportProgram(
                    id = program.id,
                    name = program.name
                )
            }

            // プログラム内種目をエクスポート（v4で追加、v5でloopId追加）
            val allProgramExercises = mutableListOf<ExportProgramExercise>()
            currentPrograms.forEach { program ->
                val programExercises = programExerciseDao.getExercisesForProgramSync(program.id)
                programExercises.forEach { pe ->
                    allProgramExercises.add(
                        ExportProgramExercise(
                            id = pe.id,
                            programId = pe.programId,
                            exerciseId = pe.exerciseId,
                            sortOrder = pe.sortOrder,
                            sets = pe.sets,
                            targetValue = pe.targetValue,
                            intervalSeconds = pe.intervalSeconds,
                            loopId = pe.loopId  // v5で追加
                        )
                    )
                }
            }

            // プログラムループをエクスポート（v5で追加）
            val allProgramLoops = mutableListOf<ExportProgramLoop>()
            currentPrograms.forEach { program ->
                val loops = programLoopDao.getLoopsForProgramSync(program.id)
                loops.forEach { loop ->
                    allProgramLoops.add(
                        ExportProgramLoop(
                            id = loop.id,
                            programId = loop.programId,
                            sortOrder = loop.sortOrder,
                            rounds = loop.rounds,
                            restBetweenRounds = loop.restBetweenRounds
                        )
                    )
                }
            }

            // インターバルプログラムをエクスポート（v7で追加）
            val currentIntervalPrograms = intervalPrograms.value
            val exportIntervalPrograms = currentIntervalPrograms.map { program ->
                ExportIntervalProgram(
                    id = program.id,
                    name = program.name,
                    workSeconds = program.workSeconds,
                    restSeconds = program.restSeconds,
                    rounds = program.rounds,
                    roundRestSeconds = program.roundRestSeconds
                )
            }

            // インターバルプログラム内種目をエクスポート（v7で追加）
            val allIntervalProgramExercises = mutableListOf<ExportIntervalProgramExercise>()
            currentIntervalPrograms.forEach { program ->
                val exercises = intervalProgramExerciseDao.getExercisesForProgramSync(program.id)
                exercises.forEach { pe ->
                    allIntervalProgramExercises.add(
                        ExportIntervalProgramExercise(
                            id = pe.id,
                            programId = pe.programId,
                            exerciseId = pe.exerciseId,
                            sortOrder = pe.sortOrder
                        )
                    )
                }
            }

            // インターバル記録をエクスポート（v7で追加）
            val currentIntervalRecords = intervalRecords.value
            val exportIntervalRecords = currentIntervalRecords.map { record ->
                ExportIntervalRecord(
                    id = record.id,
                    programName = record.programName,
                    date = record.date,
                    time = record.time,
                    workSeconds = record.workSeconds,
                    restSeconds = record.restSeconds,
                    rounds = record.rounds,
                    roundRestSeconds = record.roundRestSeconds,
                    completedRounds = record.completedRounds,
                    completedExercisesInLastRound = record.completedExercisesInLastRound,
                    exercisesJson = record.exercisesJson,
                    comment = record.comment
                )
            }

            // ToDoタスクをエクスポート（v8で追加）
            val currentTodoTasks = todoTasks.value
            val exportTodoTasks = currentTodoTasks.map { task ->
                ExportTodoTask(
                    id = task.id,
                    type = task.type,
                    referenceId = task.referenceId,
                    sortOrder = task.sortOrder,
                    repeatDays = task.repeatDays,
                    lastCompletedDate = task.lastCompletedDate
                )
            }

            val backupData = BackupData(
                version = 8,  // v8: ToDoタスク追加
                exportDate = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                app = "CalisthenicsMemory",
                groups = exportGroups,
                exercises = exportExercises,
                records = exportRecords,
                programs = exportPrograms,
                programExercises = allProgramExercises,
                programLoops = allProgramLoops,
                intervalPrograms = exportIntervalPrograms,
                intervalProgramExercises = allIntervalProgramExercises,
                intervalRecords = exportIntervalRecords,
                todoTasks = exportTodoTasks
            )

            withContext(Dispatchers.Main) {
                _snackbarMessage.value = UiMessage.ExportComplete(exportGroups.size, exportExercises.size, exportRecords.size)
            }

            val json = Json { ignoreUnknownKeys = true }
            json.encodeToString(backupData)
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _snackbarMessage.value = UiMessage.ExportError(e.message ?: "")
            }
            throw e
        }
    }

    /**
     * JSONデータをインポート（完全上書き）
     */
    suspend fun importData(jsonString: String) {
        withContext(Dispatchers.IO) {
            try {
                val json = Json { ignoreUnknownKeys = true }
                val backupData = json.decodeFromString<BackupData>(jsonString)

                // 1. 既存データを全削除
                database.clearAllTables()

                // 2. グループをインポート
                backupData.groups.forEach { exportGroup ->
                    val group = ExerciseGroup(
                        id = exportGroup.id,
                        name = exportGroup.name,
                        displayOrder = exportGroup.displayOrder
                    )
                    groupDao.insertGroup(group)
                }

                // 3. 種目をインポート
                backupData.exercises.forEach { exportExercise ->
                    val exercise = Exercise(
                        id = exportExercise.id,
                        name = exportExercise.name,
                        type = exportExercise.type,
                        group = exportExercise.group,
                        sortOrder = exportExercise.sortOrder,
                        displayOrder = exportExercise.displayOrder,
                        laterality = exportExercise.laterality,
                        targetSets = exportExercise.targetSets,
                        targetValue = exportExercise.targetValue,
                        isFavorite = exportExercise.isFavorite,
                        restInterval = exportExercise.restInterval,
                        repDuration = exportExercise.repDuration,
                        distanceTrackingEnabled = exportExercise.distanceTrackingEnabled,
                        weightTrackingEnabled = exportExercise.weightTrackingEnabled,
                        assistanceTrackingEnabled = exportExercise.assistanceTrackingEnabled,
                        description = exportExercise.description
                    )
                    exerciseDao.insertExercise(exercise)
                }

                // 4. 記録をインポート
                backupData.records.forEach { exportRecord ->
                    val record = TrainingRecord(
                        id = exportRecord.id,
                        exerciseId = exportRecord.exerciseId,
                        valueRight = exportRecord.valueRight,
                        valueLeft = exportRecord.valueLeft,
                        setNumber = exportRecord.setNumber,
                        date = exportRecord.date,
                        time = exportRecord.time,
                        comment = exportRecord.comment,
                        distanceCm = exportRecord.distanceCm,
                        weightG = exportRecord.weightG,
                        assistanceG = exportRecord.assistanceG
                    )
                    recordDao.insertRecord(record)
                }

                // 5. プログラムをインポート（v4で追加）
                backupData.programs.forEach { exportProgram ->
                    val program = Program(
                        id = exportProgram.id,
                        name = exportProgram.name
                    )
                    programDao.insert(program)
                }

                // 6. プログラムループをインポート（v5で追加）
                // ループを先にインポートしてからProgramExerciseをインポートする
                // （ProgramExerciseがloopIdを参照するため）
                backupData.programLoops.forEach { exportLoop ->
                    val loop = ProgramLoop(
                        id = exportLoop.id,
                        programId = exportLoop.programId,
                        sortOrder = exportLoop.sortOrder,
                        rounds = exportLoop.rounds,
                        restBetweenRounds = exportLoop.restBetweenRounds
                    )
                    programLoopDao.insert(loop)
                }

                // 7. プログラム内種目をインポート（v4で追加、v5でloopId追加）
                backupData.programExercises.forEach { exportPe ->
                    val programExercise = ProgramExercise(
                        id = exportPe.id,
                        programId = exportPe.programId,
                        exerciseId = exportPe.exerciseId,
                        sortOrder = exportPe.sortOrder,
                        sets = exportPe.sets,
                        targetValue = exportPe.targetValue,
                        intervalSeconds = exportPe.intervalSeconds,
                        loopId = exportPe.loopId  // v5で追加（v4以前のバックアップではnull）
                    )
                    programExerciseDao.insert(programExercise)
                }

                // 8. インターバルプログラムをインポート（v7で追加）
                backupData.intervalPrograms.forEach { exportIp ->
                    val intervalProgram = IntervalProgram(
                        id = exportIp.id,
                        name = exportIp.name,
                        workSeconds = exportIp.workSeconds,
                        restSeconds = exportIp.restSeconds,
                        rounds = exportIp.rounds,
                        roundRestSeconds = exportIp.roundRestSeconds
                    )
                    intervalProgramDao.insert(intervalProgram)
                }

                // 9. インターバルプログラム内種目をインポート（v7で追加）
                backupData.intervalProgramExercises.forEach { exportIpe ->
                    val intervalProgramExercise = IntervalProgramExercise(
                        id = exportIpe.id,
                        programId = exportIpe.programId,
                        exerciseId = exportIpe.exerciseId,
                        sortOrder = exportIpe.sortOrder
                    )
                    intervalProgramExerciseDao.insert(intervalProgramExercise)
                }

                // 10. インターバル記録をインポート（v7で追加）
                backupData.intervalRecords.forEach { exportIr ->
                    val intervalRecord = IntervalRecord(
                        id = exportIr.id,
                        programName = exportIr.programName,
                        date = exportIr.date,
                        time = exportIr.time,
                        workSeconds = exportIr.workSeconds,
                        restSeconds = exportIr.restSeconds,
                        rounds = exportIr.rounds,
                        roundRestSeconds = exportIr.roundRestSeconds,
                        completedRounds = exportIr.completedRounds,
                        completedExercisesInLastRound = exportIr.completedExercisesInLastRound,
                        exercisesJson = exportIr.exercisesJson,
                        comment = exportIr.comment
                    )
                    intervalRecordDao.insert(intervalRecord)
                }

                // 11. ToDoタスクをインポート（v8で追加）
                backupData.todoTasks.forEach { exportTask ->
                    val task = TodoTask(
                        id = exportTask.id,
                        type = exportTask.type,
                        referenceId = exportTask.referenceId,
                        sortOrder = exportTask.sortOrder,
                        repeatDays = exportTask.repeatDays,
                        lastCompletedDate = exportTask.lastCompletedDate
                    )
                    todoTaskDao.insert(task)
                }

                withContext(Dispatchers.Main) {
                    _snackbarMessage.value = UiMessage.ImportComplete(backupData.groups.size, backupData.exercises.size, backupData.records.size)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _snackbarMessage.value = UiMessage.ImportError(e.message ?: "")
                }
            }
        }
    }

    // ========================================
    // CSV エクスポート・インポート機能
    // ========================================

    /**
     * グループCSVをエクスポート
     */
    suspend fun exportGroups(): String = withContext(Dispatchers.IO) {
        try {
            val currentGroups = groups.value

            val csvBuilder = StringBuilder()
            csvBuilder.appendLine("name")

            currentGroups.forEach { group ->
                csvBuilder.appendLine(group.name)
            }

            withContext(Dispatchers.Main) {
                _snackbarMessage.value = UiMessage.CsvExportSuccess("Groups", currentGroups.size)
            }

            csvBuilder.toString()
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _snackbarMessage.value = UiMessage.ExportError(e.message ?: "")
            }
            throw e
        }
    }

    /**
     * 種目CSVをエクスポート
     */
    suspend fun exportExercises(): String = withContext(Dispatchers.IO) {
        try {
            val currentExercises = exercises.value

            val csvBuilder = StringBuilder()
            csvBuilder.appendLine("name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite,displayOrder,restInterval,repDuration,distanceTrackingEnabled,weightTrackingEnabled,assistanceTrackingEnabled,description")

            currentExercises.forEach { exercise ->
                csvBuilder.appendLine(
                    "${exercise.name},${exercise.type},${exercise.group ?: ""}," +
                    "${exercise.sortOrder},${exercise.laterality}," +
                    "${exercise.targetSets ?: ""},${exercise.targetValue ?: ""},${exercise.isFavorite}," +
                    "${exercise.displayOrder},${exercise.restInterval ?: ""},${exercise.repDuration ?: ""}," +
                    "${exercise.distanceTrackingEnabled},${exercise.weightTrackingEnabled},${exercise.assistanceTrackingEnabled}," +
                    (exercise.description ?: "")
                )
            }

            withContext(Dispatchers.Main) {
                _snackbarMessage.value = UiMessage.CsvExportSuccess("Exercises", currentExercises.size)
            }

            csvBuilder.toString()
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _snackbarMessage.value = UiMessage.ExportError(e.message ?: "")
            }
            throw e
        }
    }

    /**
     * 記録入力用テンプレートCSVをエクスポート
     * 種目リスト + コメント例
     */
    suspend fun exportRecordTemplate(): String = withContext(Dispatchers.IO) {
        try {
            val currentExercises = exercises.value

            val csvBuilder = StringBuilder()

            // ヘッダー (v12: 11列)
            csvBuilder.appendLine("exerciseName,exerciseType,date,time,setNumber,valueRight,valueLeft,comment,distanceCm,weightG,assistanceG")

            // 入力例（コメント - 英語のみ）
            csvBuilder.appendLine("# Example: Multiple sets with same date/time (one session)")
            csvBuilder.appendLine("# Wall Push-up,Dynamic,2025-11-09,10:00,1,20,,Morning session,,,")
            csvBuilder.appendLine("# Wall Push-up,Dynamic,2025-11-09,10:00,2,19,,Morning session,,,")
            csvBuilder.appendLine("# Wall Push-up,Dynamic,2025-11-09,10:00,3,15,,Morning session,,,")
            csvBuilder.appendLine("# Unilateral exercise example (with valueLeft)")
            csvBuilder.appendLine("# One-leg Squat,Dynamic,2025-11-09,10:30,1,8,7,Right leg stronger,,,")
            csvBuilder.appendLine("# With distance (cm) and weight (g)")
            csvBuilder.appendLine("# Running,Dynamic,2025-11-09,08:00,1,1,,5km run,500000,,")
            csvBuilder.appendLine("# Weighted Push-up,Dynamic,2025-11-09,10:00,1,10,,With vest,,10000,")
            csvBuilder.appendLine("# With assistance (g) - band assisted")
            csvBuilder.appendLine("# Pull-up,Dynamic,2025-11-09,11:00,1,5,,With band,,,22000")
            csvBuilder.appendLine("#")

            // 種目リスト（空欄テンプレート）
            currentExercises.forEach { exercise ->
                csvBuilder.appendLine("${exercise.name},${exercise.type},,,,,,,,,")
            }

            withContext(Dispatchers.Main) {
                _snackbarMessage.value = UiMessage.CsvTemplateExported(currentExercises.size)
            }

            csvBuilder.toString()
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _snackbarMessage.value = UiMessage.ExportError(e.message ?: "")
            }
            throw e
        }
    }

    /**
     * 実際の記録データをCSV形式でエクスポート (v12: 11列)
     */
    suspend fun exportRecords(): String = withContext(Dispatchers.IO) {
        try {
            val currentRecords = records.value
            val currentExercises = exercises.value

            // 種目IDから種目情報へのマップを作成
            val exerciseMap = currentExercises.associateBy { it.id }

            val csvBuilder = StringBuilder()

            // ヘッダー (v12: 11列)
            csvBuilder.appendLine("exerciseName,exerciseType,date,time,setNumber,valueRight,valueLeft,comment,distanceCm,weightG,assistanceG")

            // 記録データを出力（日付・時刻・セット番号で並べ替え）
            currentRecords
                .sortedWith(compareBy({ it.date }, { it.time }, { it.setNumber }))
                .forEach { record ->
                    val exercise = exerciseMap[record.exerciseId]
                    if (exercise != null) {
                        val valueLeft = record.valueLeft?.toString() ?: ""
                        val distanceCm = record.distanceCm?.toString() ?: ""
                        val weightG = record.weightG?.toString() ?: ""
                        val assistanceG = record.assistanceG?.toString() ?: ""
                        csvBuilder.appendLine(
                            "${exercise.name},${exercise.type},${record.date},${record.time}," +
                            "${record.setNumber},${record.valueRight},$valueLeft,${record.comment}," +
                            "$distanceCm,$weightG,$assistanceG"
                        )
                    }
                }

            withContext(Dispatchers.Main) {
                _snackbarMessage.value = UiMessage.CsvExportSuccess("Records", currentRecords.size)
            }

            csvBuilder.toString()
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _snackbarMessage.value = UiMessage.ExportError(e.message ?: "")
            }
            throw e
        }
    }

    /**
     * グループCSVをインポート（マージモード）
     */
    suspend fun importGroups(csvString: String): CsvImportReport = withContext(Dispatchers.IO) {
        var successCount = 0
        var skippedCount = 0
        var errorCount = 0
        val skippedItems = mutableListOf<String>()
        val errors = mutableListOf<String>()

        try {
            val lines = csvString.lines()
                .filter { it.isNotBlank() && !it.startsWith("#") }

            if (lines.isEmpty()) {
                withContext(Dispatchers.Main) {
                    _snackbarMessage.value = UiMessage.CsvEmpty
                }
                return@withContext CsvImportReport(CsvType.GROUPS, 0, 0, 0, emptyList(), emptyList())
            }

            // ヘッダー行をスキップ
            val dataLines = lines.drop(1)

            dataLines.forEachIndexed { index, line ->
                try {
                    val name = line.trim()

                    if (name.isEmpty()) {
                        errors.add("Line ${index + 2}: Group name is empty")
                        errorCount++
                        return@forEachIndexed
                    }

                    // 重複チェック
                    val existing = groupDao.getGroupByName(name)
                    if (existing != null) {
                        skippedItems.add("\"$name\" (already exists)")
                        skippedCount++
                        return@forEachIndexed
                    }

                    val group = ExerciseGroup(name = name)
                    groupDao.insertGroup(group)
                    successCount++

                } catch (e: Exception) {
                    errors.add("Line ${index + 2}: ${e.message}")
                    errorCount++
                }
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _snackbarMessage.value = UiMessage.ImportError(e.message ?: "")
            }
        }

        return@withContext CsvImportReport(CsvType.GROUPS, successCount, skippedCount, errorCount, skippedItems, errors)
    }

    /**
     * 種目CSVをインポート（マージモード）
     * - 8列（旧フォーマット）と11列（新フォーマット）の両方に対応
     * - グループが存在しない場合は自動作成
     */
    suspend fun importExercises(csvString: String): CsvImportReport = withContext(Dispatchers.IO) {
        var successCount = 0
        var skippedCount = 0
        var errorCount = 0
        var groupsCreatedCount = 0
        val skippedItems = mutableListOf<String>()
        val errors = mutableListOf<String>()

        try {
            val lines = csvString.lines()
                .filter { it.isNotBlank() && !it.startsWith("#") }

            if (lines.isEmpty()) {
                withContext(Dispatchers.Main) {
                    _snackbarMessage.value = UiMessage.CsvEmpty
                }
                return@withContext CsvImportReport(CsvType.EXERCISES, 0, 0, 0, emptyList(), emptyList())
            }

            // ヘッダー行をスキップ
            val dataLines = lines.drop(1)

            dataLines.forEachIndexed { index, line ->
                try {
                    val columns = line.split(",")
                    // 8列（旧フォーマット）または11列（新フォーマット）を許容
                    if (columns.size < 8) {
                        errors.add("Line ${index + 2}: Invalid format (expected at least 8 columns, got ${columns.size})")
                        errorCount++
                        return@forEachIndexed
                    }

                    val name = columns[0].trim()
                    val type = columns[1].trim()
                    val group = columns[2].trim().ifEmpty { null }
                    val sortOrderStr = columns[3].trim()
                    val laterality = columns[4].trim()
                    val targetSetsStr = columns[5].trim()
                    val targetValueStr = columns[6].trim()
                    val isFavoriteStr = columns[7].trim()

                    // 新フィールド（9列目以降、オプション）
                    val displayOrderStr = columns.getOrNull(8)?.trim() ?: ""
                    val restIntervalStr = columns.getOrNull(9)?.trim() ?: ""
                    val repDurationStr = columns.getOrNull(10)?.trim() ?: ""
                    val distanceTrackingEnabledStr = columns.getOrNull(11)?.trim() ?: ""
                    val weightTrackingEnabledStr = columns.getOrNull(12)?.trim() ?: ""
                    val assistanceTrackingEnabledStr = columns.getOrNull(13)?.trim() ?: ""
                    val descriptionStr = columns.getOrNull(14)?.trim() ?: ""

                    // 必須フィールドチェック
                    if (name.isEmpty() || type.isEmpty() || laterality.isEmpty()) {
                        errors.add("Line ${index + 2}: Missing required fields (name, type, or laterality)")
                        errorCount++
                        return@forEachIndexed
                    }

                    // バリデーション
                    if (type !in listOf("Dynamic", "Isometric")) {
                        errors.add("Line ${index + 2}: Invalid type \"$type\" (must be Dynamic or Isometric)")
                        errorCount++
                        return@forEachIndexed
                    }

                    if (laterality !in listOf("Bilateral", "Unilateral")) {
                        errors.add("Line ${index + 2}: Invalid laterality \"$laterality\" (must be Bilateral or Unilateral)")
                        errorCount++
                        return@forEachIndexed
                    }

                    // グループの存在チェック（なければ自動作成）
                    if (group != null) {
                        val groupExists = groupDao.getGroupByName(group) != null
                        if (!groupExists) {
                            // グループを自動作成
                            val newGroup = ExerciseGroup(name = group)
                            groupDao.insertGroup(newGroup)
                            groupsCreatedCount++
                        }
                    }

                    // 数値変換
                    val sortOrder = sortOrderStr.toIntOrNull() ?: 0
                    val targetSets = targetSetsStr.toIntOrNull()
                    val targetValue = targetValueStr.toIntOrNull()
                    val isFavorite = isFavoriteStr.toBooleanStrictOrNull() ?: false
                    val displayOrder = displayOrderStr.toIntOrNull() ?: 0
                    val restInterval = restIntervalStr.toIntOrNull()
                    val repDuration = repDurationStr.toIntOrNull()
                    val distanceTrackingEnabled = distanceTrackingEnabledStr.toBooleanStrictOrNull() ?: false
                    val weightTrackingEnabled = weightTrackingEnabledStr.toBooleanStrictOrNull() ?: false
                    val assistanceTrackingEnabled = assistanceTrackingEnabledStr.toBooleanStrictOrNull() ?: false
                    val description = descriptionStr.ifEmpty { null }

                    // 重複チェック
                    val existing = exercises.value.find {
                        it.name == name && it.type == type
                    }
                    if (existing != null) {
                        // laterality不一致の場合はスキップ
                        if (existing.laterality != laterality) {
                            skippedItems.add("\"$name, $type\" (laterality mismatch: existing=${existing.laterality}, CSV=$laterality)")
                            skippedCount++
                            return@forEachIndexed
                        }
                        // 完全一致の場合もスキップ
                        skippedItems.add("\"$name, $type\" (already exists)")
                        skippedCount++
                        return@forEachIndexed
                    }

                    val exercise = Exercise(
                        name = name,
                        type = type,
                        group = group,
                        sortOrder = sortOrder,
                        displayOrder = displayOrder,
                        laterality = laterality,
                        targetSets = targetSets,
                        targetValue = targetValue,
                        isFavorite = isFavorite,
                        restInterval = restInterval,
                        repDuration = repDuration,
                        distanceTrackingEnabled = distanceTrackingEnabled,
                        weightTrackingEnabled = weightTrackingEnabled,
                        assistanceTrackingEnabled = assistanceTrackingEnabled,
                        description = description
                    )
                    exerciseDao.insertExercise(exercise)
                    successCount++

                } catch (e: Exception) {
                    errors.add("Line ${index + 2}: ${e.message}")
                    errorCount++
                }
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _snackbarMessage.value = UiMessage.ImportError(e.message ?: "")
            }
        }

        return@withContext CsvImportReport(
            type = CsvType.EXERCISES,
            successCount = successCount,
            skippedCount = skippedCount,
            errorCount = errorCount,
            skippedItems = skippedItems,
            errors = errors,
            groupsCreatedCount = groupsCreatedCount
        )
    }

    /**
     * CSVから記録をインポート（マージモード）
     */
    suspend fun importRecordsFromCsv(csvString: String): CsvImportReport = withContext(Dispatchers.IO) {
        var successCount = 0
        var skippedCount = 0
        var errorCount = 0
        val skippedItems = mutableListOf<String>()
        val errors = mutableListOf<String>()

        try {
            val lines = csvString.lines()
                .filter { it.isNotBlank() && !it.startsWith("#") }

            if (lines.isEmpty()) {
                withContext(Dispatchers.Main) {
                    _snackbarMessage.value = UiMessage.CsvEmpty
                }
                return@withContext CsvImportReport(CsvType.RECORDS, 0, 0, 1, emptyList(), listOf("CSV file is empty"))
            }

            // ヘッダー行をスキップ
            val dataLines = lines.drop(1)

            dataLines.forEachIndexed { index, line ->
                try {
                    val columns = line.split(",")
                    if (columns.size < 8) {
                        errors.add("Line ${index + 2}: Invalid format (not enough columns)")
                        errorCount++
                        return@forEachIndexed
                    }

                    val exerciseName = columns[0].trim()
                    val exerciseType = columns[1].trim()
                    val date = columns[2].trim()
                    val time = columns[3].trim()
                    val setNumberStr = columns[4].trim()
                    val valueRightStr = columns[5].trim()
                    val valueLeftStr = columns[6].trim()
                    val comment = columns.getOrNull(7)?.trim() ?: ""
                    // v11+ 新フィールド（オプション）
                    val distanceCmStr = columns.getOrNull(8)?.trim() ?: ""
                    val weightGStr = columns.getOrNull(9)?.trim() ?: ""
                    // v12 新フィールド（オプション）
                    val assistanceGStr = columns.getOrNull(10)?.trim() ?: ""

                    // 必須フィールドチェック
                    if (exerciseName.isEmpty() || exerciseType.isEmpty() ||
                        date.isEmpty() || time.isEmpty() ||
                        setNumberStr.isEmpty() || valueRightStr.isEmpty()) {
                        errors.add("Line ${index + 2}: Missing required fields")
                        errorCount++
                        return@forEachIndexed
                    }

                    // 種目を検索
                    val exercise = exercises.value.find {
                        it.name == exerciseName && it.type == exerciseType
                    }

                    if (exercise == null) {
                        errors.add("Line ${index + 2}: Exercise not found: $exerciseName ($exerciseType)")
                        errorCount++
                        return@forEachIndexed
                    }

                    // 数値変換
                    val setNumber = setNumberStr.toIntOrNull()
                    val valueRight = valueRightStr.toIntOrNull()
                    val valueLeft = if (valueLeftStr.isEmpty()) null else valueLeftStr.toIntOrNull()
                    val distanceCm = if (distanceCmStr.isEmpty()) null else distanceCmStr.toIntOrNull()
                    val weightG = if (weightGStr.isEmpty()) null else weightGStr.toIntOrNull()
                    val assistanceG = if (assistanceGStr.isEmpty()) null else assistanceGStr.toIntOrNull()

                    if (setNumber == null || valueRight == null) {
                        errors.add("Line ${index + 2}: Invalid number format")
                        errorCount++
                        return@forEachIndexed
                    }

                    // 重複チェック
                    val isDuplicate = records.value.any { existingRecord ->
                        existingRecord.exerciseId == exercise.id &&
                        existingRecord.date == date &&
                        existingRecord.time == time &&
                        existingRecord.setNumber == setNumber
                    }

                    if (isDuplicate) {
                        skippedItems.add("\"$exerciseName ($exerciseType)\" - $date $time Set $setNumber (already exists)")
                        skippedCount++
                        return@forEachIndexed
                    }

                    // レコード作成
                    val record = TrainingRecord(
                        exerciseId = exercise.id,
                        valueRight = valueRight,
                        valueLeft = valueLeft,
                        setNumber = setNumber,
                        date = date,
                        time = time,
                        comment = comment,
                        distanceCm = distanceCm,
                        weightG = weightG,
                        assistanceG = assistanceG
                    )

                    recordDao.insertRecord(record)
                    successCount++

                } catch (e: Exception) {
                    errors.add("Line ${index + 2}: ${e.message}")
                    errorCount++
                }
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _snackbarMessage.value = UiMessage.ImportError(e.message ?: "")
            }
        }

        return@withContext CsvImportReport(CsvType.RECORDS, successCount, skippedCount, errorCount, skippedItems, errors)
    }

    // ========================================
    // To Do Task 操作
    // ========================================

    val todoTasks: StateFlow<List<TodoTask>> = todoTaskDao.getAllTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    fun addTodoTask(exerciseId: Long) {
        viewModelScope.launch {
            try {
                val sortOrder = todoTaskDao.getNextSortOrder()
                val task = TodoTask(
                    type = TodoTask.TYPE_EXERCISE,
                    referenceId = exerciseId,
                    sortOrder = sortOrder
                )
                todoTaskDao.insert(task)
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    fun addTodoTasks(exerciseIds: List<Long>) {
        viewModelScope.launch {
            try {
                var sortOrder = todoTaskDao.getNextSortOrder()
                exerciseIds.forEach { exerciseId ->
                    val task = TodoTask(
                        type = TodoTask.TYPE_EXERCISE,
                        referenceId = exerciseId,
                        sortOrder = sortOrder++
                    )
                    todoTaskDao.insert(task)
                }
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    fun addTodoTaskProgram(programId: Long) {
        viewModelScope.launch {
            try {
                val sortOrder = todoTaskDao.getNextSortOrder()
                val task = TodoTask(
                    type = TodoTask.TYPE_PROGRAM,
                    referenceId = programId,
                    sortOrder = sortOrder
                )
                todoTaskDao.insert(task)
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    fun addTodoTaskPrograms(programIds: List<Long>) {
        viewModelScope.launch {
            try {
                var sortOrder = todoTaskDao.getNextSortOrder()
                programIds.forEach { programId ->
                    val task = TodoTask(
                        type = TodoTask.TYPE_PROGRAM,
                        referenceId = programId,
                        sortOrder = sortOrder++
                    )
                    todoTaskDao.insert(task)
                }
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    fun addTodoTaskGroups(groupIds: List<Long>) {
        viewModelScope.launch {
            try {
                var sortOrder = todoTaskDao.getNextSortOrder()
                groupIds.forEach { groupId ->
                    val task = TodoTask(
                        type = TodoTask.TYPE_GROUP,
                        referenceId = groupId,
                        sortOrder = sortOrder++
                    )
                    todoTaskDao.insert(task)
                }
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    fun addTodoTaskInterval(intervalProgramId: Long) {
        viewModelScope.launch {
            try {
                val sortOrder = todoTaskDao.getNextSortOrder()
                val task = TodoTask(
                    type = TodoTask.TYPE_INTERVAL,
                    referenceId = intervalProgramId,
                    sortOrder = sortOrder
                )
                todoTaskDao.insert(task)
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    fun addTodoTaskIntervals(intervalProgramIds: List<Long>) {
        viewModelScope.launch {
            try {
                var sortOrder = todoTaskDao.getNextSortOrder()
                intervalProgramIds.forEach { intervalProgramId ->
                    val task = TodoTask(
                        type = TodoTask.TYPE_INTERVAL,
                        referenceId = intervalProgramId,
                        sortOrder = sortOrder++
                    )
                    todoTaskDao.insert(task)
                }
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    fun deleteTodoTask(taskId: Long) {
        viewModelScope.launch {
            try {
                todoTaskDao.deleteById(taskId)
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    fun deleteTodoTaskByReference(type: String, referenceId: Long) {
        viewModelScope.launch {
            try {
                todoTaskDao.deleteByReference(type, referenceId)
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    fun completeTodoTaskByReference(type: String, referenceId: Long) {
        viewModelScope.launch {
            try {
                val task = todoTaskDao.getTaskByReference(type, referenceId)
                if (task != null && task.isRepeating()) {
                    val todayStr = java.time.LocalDate.now().toString()
                    todoTaskDao.updateLastCompletedDate(type, referenceId, todayStr)
                } else {
                    todoTaskDao.deleteByReference(type, referenceId)
                }

                // 種目完了時にグループToDoの完了もチェック
                if (type == TodoTask.TYPE_EXERCISE) {
                    checkGroupTodoCompletion(referenceId)
                }
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    private suspend fun checkGroupTodoCompletion(exerciseId: Long) {
        val exercise = exercises.value.find { it.id == exerciseId } ?: return
        val groupName = exercise.group ?: return
        val group = groupDao.getGroupByName(groupName) ?: return

        // このグループがToDoに登録されているか
        val groupTask = todoTaskDao.getTaskByReference(TodoTask.TYPE_GROUP, group.id) ?: return

        // 今日のアクティブなタスクかチェック
        val todayStr = java.time.LocalDate.now().toString()
        if (groupTask.isRepeating()) {
            val todayDayNumber = java.time.LocalDate.now().dayOfWeek.value
            if (todayDayNumber !in groupTask.getRepeatDayNumbers()) return
            if (groupTask.lastCompletedDate == todayStr) return
        }

        // グループ内の全種目に今日の記録があるか確認
        val groupExercises = exercises.value.filter { it.group == groupName }
        val allCompleted = groupExercises.all { ex ->
            recordDao.hasRecordOnDate(ex.id, todayStr)
        }

        if (allCompleted) {
            if (groupTask.isRepeating()) {
                todoTaskDao.updateLastCompletedDate(TodoTask.TYPE_GROUP, group.id, todayStr)
            } else {
                todoTaskDao.deleteByReference(TodoTask.TYPE_GROUP, group.id)
            }
        }
    }

    suspend fun hasRecordOnDate(exerciseId: Long, date: String): Boolean {
        return recordDao.hasRecordOnDate(exerciseId, date)
    }

    fun updateTodoRepeatDays(taskId: Long, repeatDays: String) {
        viewModelScope.launch {
            try {
                todoTaskDao.updateRepeatDays(taskId, repeatDays)
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    fun reorderTodoTasks(taskIds: List<Long>) {
        viewModelScope.launch {
            try {
                todoTaskDao.reorderTasks(taskIds)
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    fun reorderTodoTasks(fromIndex: Int, toIndex: Int) {
        val currentTasks = todoTasks.value.toMutableList()
        if (fromIndex < 0 || toIndex < 0 ||
            fromIndex >= currentTasks.size || toIndex >= currentTasks.size) {
            return
        }

        // Move the item in the list
        val item = currentTasks.removeAt(fromIndex)
        currentTasks.add(toIndex, item)

        // Update database with new order
        val reorderedIds = currentTasks.map { it.id }
        reorderTodoTasks(reorderedIds)
    }

    /**
     * 指定した種目の前回セッション記録を取得
     * オートフィル機能用
     */
    suspend fun getLatestSession(exerciseId: Long): List<TrainingRecord> {
        return recordDao.getLatestSessionByExercise(exerciseId)
    }

    // ========================================
    // Program 操作
    // ========================================

    val programs: StateFlow<List<Program>> = programDao.getAllPrograms()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    fun createProgram(name: String) {
        viewModelScope.launch {
            try {
                val program = Program(name = name)
                programDao.insert(program)
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    suspend fun createProgramAndGetId(name: String): Long? {
        return try {
            val program = Program(name = name)
            programDao.insert(program)
        } catch (e: Exception) {
            _snackbarMessage.value = UiMessage.ErrorOccurred
            null
        }
    }

    suspend fun updateProgram(program: Program) {
        try {
            programDao.update(program)
        } catch (e: Exception) {
            _snackbarMessage.value = UiMessage.ErrorOccurred
        }
    }

    fun deleteProgram(programId: Long) {
        viewModelScope.launch {
            try {
                programDao.deleteById(programId)
                todoTaskDao.deleteByReference(TodoTask.TYPE_PROGRAM, programId)
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    fun duplicateProgram(programId: Long, copySuffix: String) {
        viewModelScope.launch {
            try {
                val sourceProgram = programDao.getProgramById(programId) ?: return@launch
                val sourceExercises = programExerciseDao.getExercisesForProgramSync(programId)
                val sourceLoops = programLoopDao.getLoopsForProgramSync(programId)

                // Create new program with copy suffix
                val newProgram = Program(name = "${sourceProgram.name} $copySuffix")
                val newProgramId = programDao.insert(newProgram)

                // Copy all loops and create ID mapping
                val loopIdMapping = mutableMapOf<Long, Long>()
                sourceLoops.forEach { loop ->
                    val newLoop = ProgramLoop(
                        programId = newProgramId,
                        sortOrder = loop.sortOrder,
                        rounds = loop.rounds,
                        restBetweenRounds = loop.restBetweenRounds
                    )
                    val newLoopId = programLoopDao.insert(newLoop)
                    loopIdMapping[loop.id] = newLoopId
                }

                // Copy all exercises with updated loopId
                sourceExercises.forEach { pe ->
                    val newPe = ProgramExercise(
                        programId = newProgramId,
                        exerciseId = pe.exerciseId,
                        sortOrder = pe.sortOrder,
                        sets = pe.sets,
                        targetValue = pe.targetValue,
                        intervalSeconds = pe.intervalSeconds,
                        loopId = pe.loopId?.let { loopIdMapping[it] }
                    )
                    programExerciseDao.insert(newPe)
                }

                _snackbarMessage.value = UiMessage.ProgramDuplicated
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    suspend fun getProgramById(programId: Long): Program? {
        return programDao.getProgramById(programId)
    }

    // ========================================
    // ProgramExercise 操作
    // ========================================

    fun getProgramExercisesFlow(programId: Long) = programExerciseDao.getExercisesForProgram(programId)

    suspend fun getProgramExercisesSync(programId: Long): List<ProgramExercise> {
        return programExerciseDao.getExercisesForProgramSync(programId)
    }

    fun addProgramExercise(
        programId: Long,
        exerciseId: Long,
        sets: Int = 1,
        targetValue: Int,
        intervalSeconds: Int = 60
    ) {
        viewModelScope.launch {
            try {
                val sortOrder = programExerciseDao.getNextSortOrder(programId)
                val programExercise = ProgramExercise(
                    programId = programId,
                    exerciseId = exerciseId,
                    sortOrder = sortOrder,
                    sets = sets,
                    targetValue = targetValue,
                    intervalSeconds = intervalSeconds
                )
                programExerciseDao.insert(programExercise)
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    suspend fun addProgramExerciseSync(
        programId: Long,
        exerciseId: Long,
        sets: Int = 1,
        targetValue: Int,
        intervalSeconds: Int = 60,
        loopId: Long? = null,
        sortOrder: Int? = null
    ): Long? {
        return try {
            val finalSortOrder = sortOrder ?: programExerciseDao.getNextSortOrder(programId)
            val programExercise = ProgramExercise(
                programId = programId,
                exerciseId = exerciseId,
                sortOrder = finalSortOrder,
                sets = sets,
                targetValue = targetValue,
                intervalSeconds = intervalSeconds,
                loopId = loopId
            )
            programExerciseDao.insert(programExercise)
        } catch (e: Exception) {
            _snackbarMessage.value = UiMessage.ErrorOccurred
            null
        }
    }

    suspend fun updateProgramExercise(programExercise: ProgramExercise) {
        try {
            programExerciseDao.update(programExercise)
        } catch (e: Exception) {
            _snackbarMessage.value = UiMessage.ErrorOccurred
        }
    }

    suspend fun deleteProgramExercise(programExercise: ProgramExercise) {
        try {
            programExerciseDao.delete(programExercise)
        } catch (e: Exception) {
            _snackbarMessage.value = UiMessage.ErrorOccurred
        }
    }

    suspend fun reorderProgramExercises(exerciseIds: List<Long>) {
        try {
            programExerciseDao.reorderExercises(exerciseIds)
        } catch (e: Exception) {
            _snackbarMessage.value = UiMessage.ErrorOccurred
        }
    }

    suspend fun reorderProgramExercises(programId: Long, fromIndex: Int, toIndex: Int) {
        try {
            val currentExercises = programExerciseDao.getExercisesForProgramSync(programId).toMutableList()
            if (fromIndex < 0 || toIndex < 0 ||
                fromIndex >= currentExercises.size || toIndex >= currentExercises.size) {
                return
            }

            val item = currentExercises.removeAt(fromIndex)
            currentExercises.add(toIndex, item)

            val reorderedIds = currentExercises.map { it.id }
            programExerciseDao.reorderExercises(reorderedIds)
        } catch (e: Exception) {
            _snackbarMessage.value = UiMessage.ErrorOccurred
        }
    }

    // ========================================
    // ProgramLoop 操作
    // ========================================

    fun getProgramLoopsFlow(programId: Long) = programLoopDao.getLoopsForProgram(programId)

    suspend fun getProgramLoopsSync(programId: Long): List<ProgramLoop> {
        return programLoopDao.getLoopsForProgramSync(programId)
    }

    suspend fun getProgramLoopById(loopId: Long): ProgramLoop? {
        return programLoopDao.getLoopById(loopId)
    }

    suspend fun addProgramLoop(
        programId: Long,
        rounds: Int = 3,
        restBetweenRounds: Int = 60
    ): Long? {
        return try {
            val sortOrder = programLoopDao.getNextSortOrder(programId)
            val loop = ProgramLoop(
                programId = programId,
                sortOrder = sortOrder,
                rounds = rounds,
                restBetweenRounds = restBetweenRounds
            )
            programLoopDao.insert(loop)
        } catch (e: Exception) {
            _snackbarMessage.value = UiMessage.ErrorOccurred
            null
        }
    }

    suspend fun updateProgramLoop(loop: ProgramLoop) {
        try {
            programLoopDao.update(loop)
        } catch (e: Exception) {
            _snackbarMessage.value = UiMessage.ErrorOccurred
        }
    }

    suspend fun deleteProgramLoop(loop: ProgramLoop) {
        try {
            programLoopDao.delete(loop)
        } catch (e: Exception) {
            _snackbarMessage.value = UiMessage.ErrorOccurred
        }
    }

    suspend fun addProgramExerciseToLoop(
        programId: Long,
        exerciseId: Long,
        loopId: Long,
        sets: Int = 1,
        targetValue: Int,
        intervalSeconds: Int = 60
    ): Long? {
        return try {
            val sortOrder = programExerciseDao.getNextSortOrder(programId)
            val programExercise = ProgramExercise(
                programId = programId,
                exerciseId = exerciseId,
                sortOrder = sortOrder,
                sets = sets,
                targetValue = targetValue,
                intervalSeconds = intervalSeconds,
                loopId = loopId
            )
            programExerciseDao.insert(programExercise)
        } catch (e: Exception) {
            _snackbarMessage.value = UiMessage.ErrorOccurred
            null
        }
    }

    suspend fun moveExerciseToLoop(programExercise: ProgramExercise, loopId: Long?) {
        try {
            programExerciseDao.update(programExercise.copy(loopId = loopId))
        } catch (e: Exception) {
            _snackbarMessage.value = UiMessage.ErrorOccurred
        }
    }

    // ========================================
    // IntervalProgram 操作
    // ========================================

    val intervalPrograms: StateFlow<List<IntervalProgram>> = intervalProgramDao.getAllPrograms()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    suspend fun createIntervalProgramAndGetId(
        name: String,
        workSeconds: Int,
        restSeconds: Int,
        rounds: Int,
        roundRestSeconds: Int
    ): Long? {
        return try {
            val program = IntervalProgram(
                name = name,
                workSeconds = workSeconds,
                restSeconds = restSeconds,
                rounds = rounds,
                roundRestSeconds = roundRestSeconds
            )
            intervalProgramDao.insert(program)
        } catch (e: Exception) {
            _snackbarMessage.value = UiMessage.ErrorOccurred
            null
        }
    }

    suspend fun updateIntervalProgram(program: IntervalProgram) {
        try {
            intervalProgramDao.update(program)
        } catch (e: Exception) {
            _snackbarMessage.value = UiMessage.ErrorOccurred
        }
    }

    fun deleteIntervalProgram(programId: Long) {
        viewModelScope.launch {
            try {
                intervalProgramDao.deleteById(programId)
                todoTaskDao.deleteByReference(TodoTask.TYPE_INTERVAL, programId)
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    suspend fun getIntervalProgramById(programId: Long): IntervalProgram? {
        return intervalProgramDao.getProgramById(programId)
    }

    fun duplicateIntervalProgram(programId: Long, copySuffix: String) {
        viewModelScope.launch {
            try {
                val source = intervalProgramDao.getProgramById(programId) ?: return@launch
                val sourceExercises = intervalProgramExerciseDao.getExercisesForProgramSync(programId)

                val newId = intervalProgramDao.insert(
                    IntervalProgram(
                        name = "${source.name} $copySuffix",
                        workSeconds = source.workSeconds,
                        restSeconds = source.restSeconds,
                        rounds = source.rounds,
                        roundRestSeconds = source.roundRestSeconds
                    )
                )

                sourceExercises.forEach { exercise ->
                    intervalProgramExerciseDao.insert(
                        IntervalProgramExercise(
                            programId = newId,
                            exerciseId = exercise.exerciseId,
                            sortOrder = exercise.sortOrder
                        )
                    )
                }

                _snackbarMessage.value = UiMessage.ProgramDuplicated
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

    // ========================================
    // IntervalProgramExercise 操作
    // ========================================

    fun getIntervalProgramExercisesFlow(programId: Long) =
        intervalProgramExerciseDao.getExercisesForProgram(programId)

    suspend fun getIntervalProgramExercisesSync(programId: Long): List<IntervalProgramExercise> {
        return intervalProgramExerciseDao.getExercisesForProgramSync(programId)
    }

    suspend fun addIntervalProgramExerciseSync(
        programId: Long,
        exerciseId: Long,
        sortOrder: Int? = null
    ): Long? {
        return try {
            val finalSortOrder = sortOrder ?: intervalProgramExerciseDao.getNextSortOrder(programId)
            val exercise = IntervalProgramExercise(
                programId = programId,
                exerciseId = exerciseId,
                sortOrder = finalSortOrder
            )
            intervalProgramExerciseDao.insert(exercise)
        } catch (e: Exception) {
            _snackbarMessage.value = UiMessage.ErrorOccurred
            null
        }
    }

    suspend fun deleteIntervalProgramExercise(exercise: IntervalProgramExercise) {
        try {
            intervalProgramExerciseDao.delete(exercise)
        } catch (e: Exception) {
            _snackbarMessage.value = UiMessage.ErrorOccurred
        }
    }

    suspend fun updateIntervalProgramExercise(exercise: IntervalProgramExercise) {
        try {
            intervalProgramExerciseDao.update(exercise)
        } catch (e: Exception) {
            _snackbarMessage.value = UiMessage.ErrorOccurred
        }
    }

    suspend fun reorderIntervalProgramExercises(exerciseIds: List<Long>) {
        try {
            intervalProgramExerciseDao.reorderExercises(exerciseIds)
        } catch (e: Exception) {
            _snackbarMessage.value = UiMessage.ErrorOccurred
        }
    }

    // ========================================
    // IntervalRecord 操作
    // ========================================

    val intervalRecords: StateFlow<List<IntervalRecord>> = intervalRecordDao.getAllRecords()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    suspend fun saveIntervalRecord(record: IntervalRecord): Long? {
        return try {
            intervalRecordDao.insert(record)
        } catch (e: Exception) {
            _snackbarMessage.value = UiMessage.ErrorOccurred
            null
        }
    }

    suspend fun updateIntervalRecord(record: IntervalRecord) {
        try {
            intervalRecordDao.update(record)
        } catch (e: Exception) {
            _snackbarMessage.value = UiMessage.ErrorOccurred
        }
    }

    fun updateIntervalRecordAsync(record: IntervalRecord) {
        viewModelScope.launch {
            updateIntervalRecord(record)
        }
    }

    fun deleteIntervalRecord(recordId: Long) {
        viewModelScope.launch {
            try {
                intervalRecordDao.deleteById(recordId)
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
            }
        }
    }

}