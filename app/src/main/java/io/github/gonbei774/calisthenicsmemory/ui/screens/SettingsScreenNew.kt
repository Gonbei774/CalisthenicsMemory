package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.AppLanguage
import io.github.gonbei774.calisthenicsmemory.data.LanguagePreferences
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import io.github.gonbei774.calisthenicsmemory.viewmodel.BackupData
import io.github.gonbei774.calisthenicsmemory.viewmodel.CsvImportReport
import io.github.gonbei774.calisthenicsmemory.viewmodel.CsvType
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenNew(
    viewModel: TrainingViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showDataPreview by remember { mutableStateOf(false) }
    var showBackupConfirmation by remember { mutableStateOf(false) }
    var backupImportType by remember { mutableStateOf<String?>(null) } // "JSON" or "CSV"
    var showImportWarning by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var importFileName by remember { mutableStateOf<String?>(null) }
    var importGroupCount by remember { mutableStateOf(0) }
    var importExerciseCount by remember { mutableStateOf(0) }
    var importRecordCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }

    // CSV関連のダイアログとstate
    var showCsvExportDialog by remember { mutableStateOf(false) }
    var showCsvImportPreview by remember { mutableStateOf(false) }
    var csvImportType by remember { mutableStateOf<CsvType?>(null) }
    var csvImportDataCount by remember { mutableStateOf(0) }
    var pendingCsvString by remember { mutableStateOf<String?>(null) }
    var csvFileName by remember { mutableStateOf<String?>(null) }
    var showImportResult by remember { mutableStateOf(false) }
    var importReport by remember { mutableStateOf<CsvImportReport?>(null) }

    // JSONエクスポート用ランチャー
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isLoading = true
                try {
                    withContext(Dispatchers.IO) {
                        val jsonData = viewModel.exportData()

                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(jsonData.toByteArray())
                        }
                    }
                    // エクスポート成功メッセージはViewModelのexportData内で設定される
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        // ViewModelでエラーメッセージが設定されるため、ここでは何もしない
                    }
                } finally {
                    isLoading = false
                }
            }
        }
    }

    // JSONインポート用ランチャー
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isLoading = true
                try {
                    withContext(Dispatchers.IO) {
                        // ファイル名を取得
                        val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            cursor.moveToFirst()
                            cursor.getString(nameIndex)
                        } ?: "unknown.json"

                        // JSONを読み込んで解析
                        val jsonData = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            inputStream.readBytes().decodeToString()
                        } ?: ""

                        if (jsonData.isNotEmpty()) {
                            val json = Json { ignoreUnknownKeys = true }
                            val backupData = json.decodeFromString<BackupData>(jsonData)

                            withContext(Dispatchers.Main) {
                                // データ情報を保存
                                pendingImportUri = uri
                                importFileName = fileName
                                importGroupCount = backupData.groups.size
                                importExerciseCount = backupData.exercises.size
                                importRecordCount = backupData.records.size

                                // データ確認ダイアログを表示
                                showDataPreview = true
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        // JSONパースエラーを表示（エラーメッセージはシンプルに）
                        android.util.Log.e("SettingsScreen", "Failed to read import file", e)
                    }
                } finally {
                    isLoading = false
                }
            }
        }
    }

    // CSVエクスポート用ランチャー（グループ）
    val csvExportGroupsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isLoading = true
                try {
                    withContext(Dispatchers.IO) {
                        val csvData = viewModel.exportGroups()

                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(csvData.toByteArray())
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        // ViewModelでエラーメッセージが設定される
                    }
                } finally {
                    isLoading = false
                }
            }
        }
    }

    // CSVエクスポート用ランチャー（種目）
    val csvExportExercisesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isLoading = true
                try {
                    withContext(Dispatchers.IO) {
                        val csvData = viewModel.exportExercises()

                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(csvData.toByteArray())
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        // ViewModelでエラーメッセージが設定される
                    }
                } finally {
                    isLoading = false
                }
            }
        }
    }

    // CSVエクスポート用ランチャー（記録データ）
    val csvExportRecordsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isLoading = true
                try {
                    withContext(Dispatchers.IO) {
                        val csvData = viewModel.exportRecords()

                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(csvData.toByteArray())
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        // ViewModelでエラーメッセージが設定される
                    }
                } finally {
                    isLoading = false
                }
            }
        }
    }

    // CSVエクスポート用ランチャー（記録テンプレート）
    val csvExportRecordTemplateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isLoading = true
                try {
                    withContext(Dispatchers.IO) {
                        val csvData = viewModel.exportRecordTemplate()

                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(csvData.toByteArray())
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        // ViewModelでエラーメッセージが設定される
                    }
                } finally {
                    isLoading = false
                }
            }
        }
    }

    // CSVインポート用ランチャー（自動判定機能付き）
    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isLoading = true
                try {
                    withContext(Dispatchers.IO) {
                        // ファイル名を取得
                        val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            cursor.moveToFirst()
                            cursor.getString(nameIndex)
                        } ?: "unknown.csv"

                        // CSVを読み込む
                        val csvData = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            inputStream.readBytes().decodeToString()
                        } ?: ""

                        if (csvData.isNotEmpty()) {
                            // CSV種類を自動判定
                            val detectedType = detectCsvType(csvData)

                            if (detectedType != null) {
                                // データ件数をカウント
                                val lines = csvData.lines().filter { it.isNotBlank() && !it.startsWith("#") }
                                val dataCount = if (lines.size > 1) lines.size - 1 else 0 // ヘッダー行を除く

                                withContext(Dispatchers.Main) {
                                    csvImportType = detectedType
                                    csvFileName = fileName
                                    csvImportDataCount = dataCount
                                    pendingCsvString = csvData
                                    showCsvImportPreview = true
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    // 判定失敗のエラーメッセージ
                                    android.util.Log.e("SettingsScreen", "CSV type detection failed")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        android.util.Log.e("SettingsScreen", "CSV import error", e)
                    }
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                color = Slate600
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
                        text = stringResource(R.string.settings),
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
            // メインタイトル
            item {
                Text(
                    text = stringResource(R.string.data_management),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // ========================================
            // セクション1: 完全バックアップ (JSON)
            // ========================================

            // セクションタイトルと説明
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.section_full_backup),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = stringResource(R.string.section_full_backup_description),
                        fontSize = 14.sp,
                        color = Slate400,
                        lineHeight = 20.sp
                    )
                }
            }

            // エクスポートボタン
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Slate800
                    ),
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        if (!isLoading) {
                            val dateTime = LocalDateTime.now()
                            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
                            val fileName = "calisthenics_memory_backup_${dateTime.format(formatter)}.json"
                            exportLauncher.launch(fileName)
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📤",
                            fontSize = 32.sp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.export_data),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = stringResource(R.string.create_backup),
                                fontSize = 14.sp,
                                color = Slate400,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // インポートボタン
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Slate800
                    ),
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        if (!isLoading) {
                            importLauncher.launch(arrayOf("application/json"))
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📥",
                            fontSize = 32.sp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.import_data),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = stringResource(R.string.restore_from_backup),
                                fontSize = 14.sp,
                                color = Slate400,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // 注意書き（JSONインポート用）
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Red600.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "ℹ️",
                            fontSize = 20.sp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.warning_title),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = stringResource(R.string.import_warning),
                                fontSize = 14.sp,
                                color = Slate300,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            // ========================================
            // セクション2: 記録の追加 (CSV)
            // ========================================

            // セクションタイトルと説明
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.section_partial_data_management),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = stringResource(R.string.section_partial_data_management_description),
                        fontSize = 14.sp,
                        color = Slate400,
                        lineHeight = 20.sp
                    )
                }
            }

            // CSVエクスポートボタン
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Slate800
                    ),
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        if (!isLoading) {
                            showCsvExportDialog = true
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📋",
                            fontSize = 32.sp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.csv_export),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = stringResource(R.string.csv_export_description),
                                fontSize = 14.sp,
                                color = Slate400,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // CSVインポートボタン
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Slate800
                    ),
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        if (!isLoading) {
                            csvImportLauncher.launch(arrayOf("text/csv", "text/comma-separated-values"))
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📊",
                            fontSize = 32.sp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.csv_import),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = stringResource(R.string.csv_import_description),
                                fontSize = 14.sp,
                                color = Slate400,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // ========================================
            // セクション3: 言語設定
            // ========================================

            // セクションタイトルと説明
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.section_language),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = stringResource(R.string.section_language_description),
                        fontSize = 14.sp,
                        color = Slate400,
                        lineHeight = 20.sp
                    )
                }
            }

            // 言語選択カード
            item {
                val languagePrefs = remember { LanguagePreferences(context) }
                var selectedLanguage by remember { mutableStateOf(languagePrefs.getLanguage()) }
                var showLanguageDialog by remember { mutableStateOf(false) }

                // 現在のシステム言語を取得
                val currentLocale = Locale.getDefault().language

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Slate800
                    ),
                    shape = RoundedCornerShape(12.dp),
                    onClick = { showLanguageDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🌐",
                            fontSize = 32.sp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.language_setting),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = stringResource(
                                    R.string.current_language,
                                    selectedLanguage.getDisplayName(currentLocale)
                                ),
                                fontSize = 14.sp,
                                color = Slate400,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // 言語選択ダイアログ
                if (showLanguageDialog) {
                    AlertDialog(
                        onDismissRequest = { showLanguageDialog = false },
                        title = {
                            Text(
                                text = stringResource(R.string.language_setting),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                AppLanguage.entries.forEach { language ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selectedLanguage == language) {
                                                Purple600.copy(alpha = 0.3f)
                                            } else {
                                                Slate700
                                            }
                                        ),
                                        onClick = {
                                            android.util.Log.d("SettingsScreen", "Language selected: ${language.code}")
                                            selectedLanguage = language
                                            languagePrefs.setLanguage(language)
                                            android.util.Log.d("SettingsScreen", "Language saved, recreating activity")
                                            showLanguageDialog = false

                                            // Activity を再作成して言語を適用
                                            (context as? Activity)?.recreate()
                                        }
                                    ) {
                                        Text(
                                            text = language.getDisplayName(currentLocale),
                                            modifier = Modifier.padding(16.dp),
                                            color = Color.White,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showLanguageDialog = false }) {
                                Text(stringResource(R.string.close))
                            }
                        }
                    )
                }
            }

            // ローディング表示
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Purple600
                        )
                    }
                }
            }
        }
    }

    // 第1段階: データ確認ダイアログ
    if (showDataPreview) {
        AlertDialog(
            onDismissRequest = {
                showDataPreview = false
                pendingImportUri = null
            },
            title = {
                Text(
                    text = stringResource(R.string.import_data_preview_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ファイル名
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Slate700
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.file_name),
                                fontSize = 14.sp,
                                color = Slate400,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Text(
                                text = importFileName ?: "unknown.json",
                                fontSize = 16.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // データ件数
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Slate700
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.data_contents),
                                fontSize = 14.sp,
                                color = Slate400,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.groups),
                                    fontSize = 16.sp,
                                    color = Slate300
                                )
                                Text(
                                    text = stringResource(R.string.count_items, importGroupCount),
                                    fontSize = 16.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.exercises),
                                    fontSize = 16.sp,
                                    color = Slate300
                                )
                                Text(
                                    text = stringResource(R.string.count_items, importExerciseCount),
                                    fontSize = 16.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.records),
                                    fontSize = 16.sp,
                                    color = Slate300
                                )
                                Text(
                                    text = stringResource(R.string.count_records, importRecordCount),
                                    fontSize = 16.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = stringResource(R.string.import_data_preview_message),
                        fontSize = 16.sp,
                        color = Slate300,
                        lineHeight = 22.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // バックアップ確認ダイアログへ
                        showDataPreview = false
                        backupImportType = "JSON"
                        showBackupConfirmation = true
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Purple600
                    )
                ) {
                    Text(
                        text = stringResource(R.string.next),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDataPreview = false
                        pendingImportUri = null
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // 第2段階: インポート警告ダイアログ
    if (showImportWarning) {
        AlertDialog(
            onDismissRequest = {
                showImportWarning = false
                pendingImportUri = null
            },
            title = {
                Text(
                    text = stringResource(R.string.data_overwrite_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.existing_data_will_be_deleted),
                        fontSize = 16.sp
                    )
                    Text(
                        text = stringResource(R.string.cannot_undo),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Red600
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingImportUri?.let { uri ->
                            scope.launch {
                                isLoading = true
                                try {
                                    withContext(Dispatchers.IO) {
                                        val jsonData = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                            inputStream.readBytes().decodeToString()
                                        } ?: ""

                                        if (jsonData.isNotEmpty()) {
                                            viewModel.importData(jsonData)
                                        } else {
                                            withContext(Dispatchers.Main) {
                                                // 空ファイルのエラーメッセージは不要（ViewModelで処理される）
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        // ViewModelでエラーメッセージが設定されるため、ここでは何もしない
                                    }
                                } finally {
                                    isLoading = false
                                    showImportWarning = false
                                    pendingImportUri = null
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Red600
                    )
                ) {
                    Text(
                        text = stringResource(R.string.import_confirm),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImportWarning = false
                        pendingImportUri = null
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // バックアップ確認ダイアログ
    if (showBackupConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showBackupConfirmation = false
                backupImportType = null
            },
            title = {
                Text(
                    text = stringResource(R.string.backup_before_import_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.backup_before_import_message),
                        fontSize = 16.sp,
                        lineHeight = 22.sp
                    )

                    // バックアップして続行（推奨）
                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                showBackupConfirmation = false

                                // バックアップを実行
                                val backupSuccess = saveAutoBackup(context, viewModel)

                                // バックアップ結果をSnackbarで通知
                                viewModel.showBackupResult(backupSuccess)

                                // バックアップが失敗してもインポートは続行
                                try {
                                    if (backupImportType == "JSON") {
                                        withContext(Dispatchers.Main) {
                                            showImportWarning = true
                                        }
                                    } else if (backupImportType == "CSV") {
                                        // CSVインポート実行（ヘルパー関数使用）
                                        pendingCsvString?.let { csvData ->
                                            val report = executeCsvImport(viewModel, csvData, csvImportType)
                                            withContext(Dispatchers.Main) {
                                                if (report != null) {
                                                    importReport = report
                                                    showImportResult = true
                                                }
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("SettingsScreen", "Import error after backup", e)
                                } finally {
                                    withContext(Dispatchers.Main) {
                                        isLoading = false
                                        pendingCsvString = null
                                        backupImportType = null
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Purple600
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.backup_and_continue),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    // スキップして続行
                    OutlinedButton(
                        onClick = {
                            showBackupConfirmation = false
                            if (backupImportType == "JSON") {
                                showImportWarning = true
                            } else if (backupImportType == "CSV") {
                                // CSVインポート実行（ヘルパー関数使用）
                                pendingCsvString?.let { csvData ->
                                    scope.launch {
                                        isLoading = true
                                        try {
                                            val report = executeCsvImport(viewModel, csvData, csvImportType)
                                            withContext(Dispatchers.Main) {
                                                if (report != null) {
                                                    importReport = report
                                                    showImportResult = true
                                                }
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("SettingsScreen", "CSV import error", e)
                                        } finally {
                                            isLoading = false
                                            pendingCsvString = null
                                        }
                                    }
                                }
                            }
                            backupImportType = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.skip_and_continue),
                            fontSize = 16.sp
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        showBackupConfirmation = false
                        pendingImportUri = null
                        pendingCsvString = null
                        backupImportType = null
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // CSVエクスポート選択ダイアログ
    if (showCsvExportDialog) {
        AlertDialog(
            onDismissRequest = { showCsvExportDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.csv_export),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // グループ
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Slate700
                        ),
                        onClick = {
                            showCsvExportDialog = false
                            val dateTime = LocalDateTime.now()
                            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")
                            val fileName = "groups_${dateTime.format(formatter)}.csv"
                            csvExportGroupsLauncher.launch(fileName)
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "📁", fontSize = 24.sp)
                            Column {
                                Text(
                                    text = stringResource(R.string.csv_export_groups),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = stringResource(R.string.csv_export_groups_description),
                                    fontSize = 14.sp,
                                    color = Slate400
                                )
                            }
                        }
                    }

                    // 種目
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Slate700
                        ),
                        onClick = {
                            showCsvExportDialog = false
                            val dateTime = LocalDateTime.now()
                            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")
                            val fileName = "exercises_${dateTime.format(formatter)}.csv"
                            csvExportExercisesLauncher.launch(fileName)
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "💪", fontSize = 24.sp)
                            Column {
                                Text(
                                    text = stringResource(R.string.csv_export_exercises),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = stringResource(R.string.csv_export_exercises_description),
                                    fontSize = 14.sp,
                                    color = Slate400
                                )
                            }
                        }
                    }

                    // 記録（実データ）
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Slate700
                        ),
                        onClick = {
                            showCsvExportDialog = false
                            val dateTime = LocalDateTime.now()
                            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")
                            val fileName = "records_${dateTime.format(formatter)}.csv"
                            csvExportRecordsLauncher.launch(fileName)
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "📊", fontSize = 24.sp)
                            Column {
                                Text(
                                    text = stringResource(R.string.csv_export_records),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = stringResource(R.string.csv_export_records_description),
                                    fontSize = 14.sp,
                                    color = Slate400
                                )
                            }
                        }
                    }

                    // 記録テンプレート
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Slate700
                        ),
                        onClick = {
                            showCsvExportDialog = false
                            val dateTime = LocalDateTime.now()
                            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")
                            val fileName = "record_template_${dateTime.format(formatter)}.csv"
                            csvExportRecordTemplateLauncher.launch(fileName)
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "📋", fontSize = 24.sp)
                            Column {
                                Text(
                                    text = stringResource(R.string.csv_export_record_template),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = stringResource(R.string.csv_export_record_template_description),
                                    fontSize = 14.sp,
                                    color = Slate400
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCsvExportDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // CSVインポートプレビューダイアログ
    if (showCsvImportPreview) {
        AlertDialog(
            onDismissRequest = {
                showCsvImportPreview = false
                pendingCsvString = null
            },
            title = {
                Text(
                    text = stringResource(R.string.csv_import_preview),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ファイル情報
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Slate700
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // ファイル名
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.csv_file),
                                    fontSize = 14.sp,
                                    color = Slate400
                                )
                                Text(
                                    text = csvFileName ?: "unknown.csv",
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // CSV種類
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.csv_type),
                                    fontSize = 14.sp,
                                    color = Slate400
                                )
                                Text(
                                    text = getCsvTypeLocalizedString(csvImportType),
                                    fontSize = 14.sp,
                                    color = Green400,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // データ件数
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.csv_items),
                                    fontSize = 14.sp,
                                    color = Slate400
                                )
                                Text(
                                    text = "$csvImportDataCount",
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // バックアップ確認ダイアログへ
                        showCsvImportPreview = false
                        backupImportType = "CSV"
                        showBackupConfirmation = true
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Purple600
                    )
                ) {
                    Text(
                        text = stringResource(R.string.import_action),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCsvImportPreview = false
                        pendingCsvString = null
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // CSVインポート結果ダイアログ
    if (showImportResult && importReport != null) {
        val report = importReport!!
        var showSkippedItems by remember { mutableStateOf(false) }
        var showErrors by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                showImportResult = false
                importReport = null
            },
            title = {
                Text(
                    text = stringResource(R.string.csv_import_completed),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // サマリー
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Slate700
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = stringResource(R.string.csv_import_success_label), fontSize = 14.sp, color = Green400)
                                Text(
                                    text = "${report.successCount}",
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = stringResource(R.string.csv_import_skipped_label), fontSize = 14.sp, color = Slate400)
                                Text(
                                    text = "${report.skippedCount}",
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = stringResource(R.string.csv_import_error_label), fontSize = 14.sp, color = Red600)
                                Text(
                                    text = "${report.errorCount}",
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // スキップ項目の詳細（折りたたみ可能）
                    if (report.skippedCount > 0) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Slate700
                            ),
                            shape = RoundedCornerShape(8.dp),
                            onClick = { showSkippedItems = !showSkippedItems }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.csv_import_skipped_items, report.skippedCount),
                                        fontSize = 14.sp,
                                        color = Slate300,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (showSkippedItems) "▼" else "▶",
                                        fontSize = 12.sp,
                                        color = Slate400
                                    )
                                }

                                if (showSkippedItems) {
                                    Column(
                                        modifier = Modifier.padding(top = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        report.skippedItems.take(10).forEach { item ->
                                            Text(
                                                text = "• $item",
                                                fontSize = 12.sp,
                                                color = Slate400,
                                                lineHeight = 16.sp
                                            )
                                        }
                                        if (report.skippedItems.size > 10) {
                                            Text(
                                                text = "... and ${report.skippedItems.size - 10} more",
                                                fontSize = 12.sp,
                                                color = Slate400
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // エラーの詳細（折りたたみ可能）
                    if (report.errorCount > 0) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Red600.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            onClick = { showErrors = !showErrors }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.csv_import_errors, report.errorCount),
                                        fontSize = 14.sp,
                                        color = Red600,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (showErrors) "▼" else "▶",
                                        fontSize = 12.sp,
                                        color = Slate400
                                    )
                                }

                                if (showErrors) {
                                    Column(
                                        modifier = Modifier.padding(top = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        report.errors.take(10).forEach { error ->
                                            Text(
                                                text = "• $error",
                                                fontSize = 12.sp,
                                                color = Red600.copy(alpha = 0.8f),
                                                lineHeight = 16.sp
                                            )
                                        }
                                        if (report.errors.size > 10) {
                                            Text(
                                                text = "... and ${report.errors.size - 10} more",
                                                fontSize = 12.sp,
                                                color = Red600.copy(alpha = 0.8f)
                                            )
                                        }
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
                        showImportResult = false
                        importReport = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Purple600
                    )
                ) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}

/**
 * CSV種類を多言語化された文字列に変換する関数
 */
@Composable
fun getCsvTypeLocalizedString(csvType: CsvType?): String {
    return when (csvType) {
        CsvType.GROUPS -> stringResource(R.string.groups)
        CsvType.EXERCISES -> stringResource(R.string.exercises)
        CsvType.RECORDS -> stringResource(R.string.records)
        null -> stringResource(R.string.unknown_short)
    }
}

/**
 * CSV種類を自動判定する関数
 */
fun detectCsvType(csvString: String): CsvType? {
    val firstLine = csvString.lines()
        .filter { it.isNotBlank() && !it.startsWith("#") }
        .firstOrNull() ?: return null

    return when {
        firstLine.trim() == "name" -> CsvType.GROUPS
        firstLine.startsWith("name,type,group,sortOrder") -> CsvType.EXERCISES
        firstLine.startsWith("exerciseName,exerciseType") -> CsvType.RECORDS
        else -> null
    }
}

/**
 * バックアップを自動保存する関数
 * @return 成功: true, 失敗: false
 */
suspend fun saveAutoBackup(
    context: android.content.Context,
    viewModel: TrainingViewModel
): Boolean {
    return try {
        withContext(Dispatchers.IO) {
            val jsonData = viewModel.exportData()
            val dateTime = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
            val fileName = "backup_${dateTime.format(formatter)}.json"

            // MediaStoreを使ってDownloadsフォルダに保存
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = context.contentResolver.insert(
                android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            )

            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(jsonData.toByteArray())
                }
            }

            uri != null
        }
    } catch (e: Exception) {
        android.util.Log.e("SettingsScreen", "Auto backup failed", e)
        false
    }
}

/**
 * CSVインポートを実行する関数
 */
suspend fun executeCsvImport(
    viewModel: TrainingViewModel,
    csvData: String,
    csvImportType: CsvType?
): CsvImportReport? {
    return withContext(Dispatchers.IO) {
        when (csvImportType) {
            CsvType.GROUPS -> viewModel.importGroups(csvData)
            CsvType.EXERCISES -> viewModel.importExercises(csvData)
            CsvType.RECORDS -> viewModel.importRecordsFromCsv(csvData)
            else -> null
        }
    }
}