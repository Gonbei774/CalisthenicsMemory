package io.github.gonbei774.calisthenicsmemory.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ExerciseGroup
import io.github.gonbei774.calisthenicsmemory.data.TrainingRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.database.sqlite.SQLiteConstraintException
import kotlinx.coroutines.flow.combine
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
    val records: List<ExportRecord>
)

@Serializable
data class ExportGroup(
    val id: Long,
    val name: String
)

@Serializable
data class ExportExercise(
    val id: Long,
    val name: String,
    val type: String,
    val group: String?,
    val sortOrder: Int,
    val laterality: String,
    val targetSets: Int? = null,
    val targetValue: Int? = null
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
    val comment: String
)

class TrainingViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val exerciseDao = database.exerciseDao()
    private val recordDao = database.trainingRecordDao()
    private val groupDao = database.exerciseGroupDao()

    // Helper function to get string resources
    private fun getString(resId: Int, vararg formatArgs: Any): String {
        return getApplication<Application>().getString(resId, *formatArgs)
    }

    // Exercises
    val exercises: StateFlow<List<Exercise>> = exerciseDao.getAllExercises()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Exercise Groups
    val groups: StateFlow<List<ExerciseGroup>> = groupDao.getAllGroups()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Training Records
    val records: StateFlow<List<TrainingRecord>> = recordDao.getAllRecords()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Snackbar message
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    // Exercise operations
    fun addExercise(
        name: String,
        type: String,
        group: String? = null,
        sortOrder: Int = 0,
        laterality: String = "Bilateral",
        targetSets: Int? = null,
        targetValue: Int? = null
    ) {
        viewModelScope.launch {
            try {
                val existingExercises = exercises.value
                val isDuplicate = existingExercises.any {
                    it.name.equals(name, ignoreCase = true) && it.type == type
                }

                if (isDuplicate) {
                    val typeLabel = getString(if (type == "Dynamic") R.string.dynamic_label else R.string.isometric_label)
                    _snackbarMessage.value = getString(R.string.already_registered_format, name, typeLabel)
                    return@launch
                }

                val exercise = Exercise(
                    name = name,
                    type = type,
                    group = group,
                    sortOrder = sortOrder,
                    laterality = laterality,
                    targetSets = targetSets,
                    targetValue = targetValue
                )
                exerciseDao.insertExercise(exercise)
                _snackbarMessage.value = getString(R.string.exercise_added)
            } catch (e: SQLiteConstraintException) {
                _snackbarMessage.value = getString(R.string.exercise_already_exists)
            } catch (e: Exception) {
                _snackbarMessage.value = getString(R.string.error_occurred)
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
                    val typeLabel = getString(if (exercise.type == "Dynamic") R.string.dynamic_label else R.string.isometric_label)
                    _snackbarMessage.value = getString(R.string.already_in_use_format, exercise.name, typeLabel)
                    return@launch
                }

                exerciseDao.updateExercise(exercise)
                _snackbarMessage.value = getString(R.string.exercise_updated)
            } catch (e: SQLiteConstraintException) {
                _snackbarMessage.value = getString(R.string.exercise_already_exists)
            } catch (e: Exception) {
                _snackbarMessage.value = getString(R.string.error_occurred)
            }
        }
    }

    fun deleteExercise(exercise: Exercise) {
        viewModelScope.launch {
            try {
                exerciseDao.deleteExercise(exercise)
                _snackbarMessage.value = getString(R.string.exercise_deleted)
            } catch (e: Exception) {
                _snackbarMessage.value = getString(R.string.error_occurred)
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
        comment: String
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
                        comment = comment
                    )
                }
                recordDao.insertRecords(records)
                _snackbarMessage.value = getString(R.string.sets_recorded, values.size)
            } catch (e: Exception) {
                _snackbarMessage.value = getString(R.string.error_occurred)
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
        comment: String
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
                        comment = comment
                    )
                }
                recordDao.insertRecords(records)
                _snackbarMessage.value = getString(R.string.sets_recorded, valuesRight.size)
            } catch (e: Exception) {
                _snackbarMessage.value = getString(R.string.error_occurred)
            }
        }
    }

    fun updateRecord(record: TrainingRecord) {
        viewModelScope.launch {
            try {
                recordDao.updateRecord(record)
                _snackbarMessage.value = getString(R.string.record_updated)
            } catch (e: Exception) {
                _snackbarMessage.value = getString(R.string.error_occurred)
            }
        }
    }

    fun deleteSession(exerciseId: Long, date: String, time: String) {
        viewModelScope.launch {
            try {
                recordDao.deleteSession(exerciseId, date, time)
                _snackbarMessage.value = getString(R.string.record_deleted)
            } catch (e: Exception) {
                _snackbarMessage.value = getString(R.string.error_occurred)
            }
        }
    }

    // ========================================
    // Group operations
    // ========================================

    fun createGroup(name: String) {
        viewModelScope.launch {
            try {
                val group = ExerciseGroup(name = name)
                groupDao.insertGroup(group)
                _snackbarMessage.value = getString(R.string.group_created)
            } catch (e: Exception) {
                _snackbarMessage.value = getString(R.string.group_already_exists)
            }
        }
    }

    fun renameGroup(oldName: String, newName: String) {
        viewModelScope.launch {
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

                _snackbarMessage.value = getString(R.string.group_renamed)
            } catch (e: Exception) {
                _snackbarMessage.value = getString(R.string.error_occurred)
            }
        }
    }

    fun deleteGroup(groupName: String) {
        viewModelScope.launch {
            try {
                // 1. グループテーブルから削除
                groupDao.deleteGroupByName(groupName)

                // 2. 種目のgroupをnullに
                val affectedExercises = exercises.value.filter { it.group == groupName }
                affectedExercises.forEach { exercise ->
                    exerciseDao.updateExercise(exercise.copy(group = null, sortOrder = 0))
                }

                _snackbarMessage.value = getString(R.string.group_deleted)
            } catch (e: Exception) {
                _snackbarMessage.value = getString(R.string.error_occurred)
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

    // 階層データ準備
    val hierarchicalExercises: StateFlow<List<GroupWithExercises>> =
        combine(groups, exercises, expandedGroups) { groups, exercises, expanded ->
            prepareHierarchicalData(groups, exercises, expanded)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private fun prepareHierarchicalData(
        groups: List<ExerciseGroup>,
        exercises: List<Exercise>,
        expandedGroups: Set<String>
    ): List<GroupWithExercises> {
        // 1. groupsテーブルを基準にグループを表示（0件でも表示）
        val groupedExercises = groups.map { group ->
            val groupExercises = exercises.filter { it.group == group.name }
            GroupWithExercises(
                groupName = group.name,
                exercises = groupExercises.sortedBy { it.sortOrder },
                isExpanded = group.name in expandedGroups  // 全て閉じた状態も可能
            )
        }.sortedBy { it.groupName }

        // 2. グループなし種目
        val ungroupedExercises = exercises.filter { it.group == null }
        val ungroupedGroup = if (ungroupedExercises.isNotEmpty()) {
            listOf(
                GroupWithExercises(
                    groupName = null,
                    exercises = ungroupedExercises.sortedBy { it.name },
                    isExpanded = "ungrouped" in expandedGroups  // 全て閉じた状態も可能
                )
            )
        } else {
            emptyList()
        }

        return groupedExercises + ungroupedGroup
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
                    name = group.name
                )
            }

            val exportExercises = currentExercises.map { exercise ->
                ExportExercise(
                    id = exercise.id,
                    name = exercise.name,
                    type = exercise.type,
                    group = exercise.group,
                    sortOrder = exercise.sortOrder,
                    laterality = exercise.laterality,
                    targetSets = exercise.targetSets,
                    targetValue = exercise.targetValue
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
                    comment = record.comment
                )
            }

            val backupData = BackupData(
                version = 1,
                exportDate = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                app = "CalisthenicsMemory",
                groups = exportGroups,
                exercises = exportExercises,
                records = exportRecords
            )

            withContext(Dispatchers.Main) {
                _snackbarMessage.value = getString(R.string.export_complete, exportGroups.size, exportExercises.size, exportRecords.size)
            }

            Json.encodeToString(backupData)
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _snackbarMessage.value = getString(R.string.export_error, e.message ?: "")
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
                val backupData = Json.decodeFromString<BackupData>(jsonString)

                // 1. 既存データを全削除
                database.clearAllTables()

                // 2. グループをインポート
                backupData.groups.forEach { exportGroup ->
                    val group = ExerciseGroup(
                        id = exportGroup.id,
                        name = exportGroup.name
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
                        laterality = exportExercise.laterality,
                        targetSets = exportExercise.targetSets,
                        targetValue = exportExercise.targetValue
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
                        comment = exportRecord.comment
                    )
                    recordDao.insertRecord(record)
                }

                withContext(Dispatchers.Main) {
                    _snackbarMessage.value = getString(R.string.import_complete, backupData.groups.size, backupData.exercises.size, backupData.records.size)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _snackbarMessage.value = getString(R.string.import_error, e.message ?: "")
                }
            }
        }
    }

}