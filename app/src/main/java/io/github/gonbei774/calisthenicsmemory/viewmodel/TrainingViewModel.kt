package io.github.gonbei774.calisthenicsmemory.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.gonbei774.calisthenicsmemory.data.AppDatabase
import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ExerciseGroup
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
    val targetValue: Int? = null,
    val isFavorite: Boolean = false  // お気に入り（デフォルト値で後方互換）
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
        isFavorite: Boolean = false
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
                    isFavorite = isFavorite
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
            try {
                val group = ExerciseGroup(name = name)
                groupDao.insertGroup(group)
                _snackbarMessage.value = UiMessage.GroupCreated
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.GroupAlreadyExists
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

                _snackbarMessage.value = UiMessage.GroupRenamed
            } catch (e: Exception) {
                _snackbarMessage.value = UiMessage.ErrorOccurred
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

    // 階層データ準備
    val hierarchicalExercises: StateFlow<List<GroupWithExercises>> =
        combine(groups, exercises, expandedGroups) { groups, exercises, expanded ->
            prepareHierarchicalData(groups, exercises, expanded)
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
        val favoriteExercises = exercises.filter { it.isFavorite }.sortedBy { it.name }
        val favoriteGroup = listOf(
            GroupWithExercises(
                groupName = favoriteGroupKey,
                exercises = favoriteExercises,
                isExpanded = favoriteGroupKey in expandedGroups
            )
        )

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

        // お気に入りグループを先頭に配置
        return favoriteGroup + groupedExercises + ungroupedGroup
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
                    targetValue = exercise.targetValue,
                    isFavorite = exercise.isFavorite
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
                version = 2,  // お気に入り機能追加のためバージョンアップ
                exportDate = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                app = "CalisthenicsMemory",
                groups = exportGroups,
                exercises = exportExercises,
                records = exportRecords
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
                        targetValue = exportExercise.targetValue,
                        isFavorite = exportExercise.isFavorite
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
            csvBuilder.appendLine("name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite")

            currentExercises.forEach { exercise ->
                csvBuilder.appendLine(
                    "${exercise.name},${exercise.type},${exercise.group ?: ""}," +
                    "${exercise.sortOrder},${exercise.laterality}," +
                    "${exercise.targetSets ?: ""},${exercise.targetValue ?: ""},${exercise.isFavorite}"
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

            // ヘッダー
            csvBuilder.appendLine("exerciseName,exerciseType,date,time,setNumber,valueRight,valueLeft,comment")

            // 入力例（コメント - 英語のみ）
            csvBuilder.appendLine("# Example: Multiple sets with same date/time (one session)")
            csvBuilder.appendLine("# Wall Push-up,Dynamic,2025-11-09,10:00,1,20,,Morning session")
            csvBuilder.appendLine("# Wall Push-up,Dynamic,2025-11-09,10:00,2,19,,Morning session")
            csvBuilder.appendLine("# Wall Push-up,Dynamic,2025-11-09,10:00,3,15,,Morning session")
            csvBuilder.appendLine("# Unilateral exercise example (with valueLeft)")
            csvBuilder.appendLine("# One-leg Squat,Dynamic,2025-11-09,10:30,1,8,7,Right leg stronger")
            csvBuilder.appendLine("#")

            // 種目リスト（空欄テンプレート）
            currentExercises.forEach { exercise ->
                csvBuilder.appendLine("${exercise.name},${exercise.type},,,,,, ")
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
     * 実際の記録データをCSV形式でエクスポート
     */
    suspend fun exportRecords(): String = withContext(Dispatchers.IO) {
        try {
            val currentRecords = records.value
            val currentExercises = exercises.value

            // 種目IDから種目情報へのマップを作成
            val exerciseMap = currentExercises.associateBy { it.id }

            val csvBuilder = StringBuilder()

            // ヘッダー
            csvBuilder.appendLine("exerciseName,exerciseType,date,time,setNumber,valueRight,valueLeft,comment")

            // 記録データを出力（日付・時刻・セット番号で並べ替え）
            currentRecords
                .sortedWith(compareBy({ it.date }, { it.time }, { it.setNumber }))
                .forEach { record ->
                    val exercise = exerciseMap[record.exerciseId]
                    if (exercise != null) {
                        val valueLeft = record.valueLeft?.toString() ?: ""
                        csvBuilder.appendLine(
                            "${exercise.name},${exercise.type},${record.date},${record.time}," +
                            "${record.setNumber},${record.valueRight},$valueLeft,${record.comment}"
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
     */
    suspend fun importExercises(csvString: String): CsvImportReport = withContext(Dispatchers.IO) {
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
                return@withContext CsvImportReport(CsvType.EXERCISES, 0, 0, 0, emptyList(), emptyList())
            }

            // ヘッダー行をスキップ
            val dataLines = lines.drop(1)

            dataLines.forEachIndexed { index, line ->
                try {
                    val columns = line.split(",")
                    if (columns.size < 8) {
                        errors.add("Line ${index + 2}: Invalid format (expected 8 columns)")
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

                    // グループの存在チェック
                    if (group != null) {
                        val groupExists = groupDao.getGroupByName(group) != null
                        if (!groupExists) {
                            errors.add("Line ${index + 2}: Group \"$group\" not found")
                            errorCount++
                            return@forEachIndexed
                        }
                    }

                    // 数値変換
                    val sortOrder = sortOrderStr.toIntOrNull() ?: 0
                    val targetSets = targetSetsStr.toIntOrNull()
                    val targetValue = targetValueStr.toIntOrNull()
                    val isFavorite = isFavoriteStr.toBooleanStrictOrNull() ?: false

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
                        laterality = laterality,
                        targetSets = targetSets,
                        targetValue = targetValue,
                        isFavorite = isFavorite
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

        return@withContext CsvImportReport(CsvType.EXERCISES, successCount, skippedCount, errorCount, skippedItems, errors)
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
                        comment = comment
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

}