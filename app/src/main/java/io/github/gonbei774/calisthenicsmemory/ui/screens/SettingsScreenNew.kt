package io.github.gonbei774.calisthenicsmemory.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import io.github.gonbei774.calisthenicsmemory.BuildConfig
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.data.AppLanguage
import io.github.gonbei774.calisthenicsmemory.data.AppTheme
import io.github.gonbei774.calisthenicsmemory.data.LanguagePreferences
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import io.github.gonbei774.calisthenicsmemory.viewmodel.BackupData
import io.github.gonbei774.calisthenicsmemory.viewmodel.CommunityShareData
import io.github.gonbei774.calisthenicsmemory.viewmodel.CommunityShareImportReport
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
    onNavigateBack: () -> Unit,
    onNavigateToLicenses: () -> Unit = {},
    onNavigateToCommunityShareExport: () -> Unit = {},
    currentTheme: AppTheme = AppTheme.SYSTEM,
    onThemeChange: (AppTheme) -> Unit = {}
) {
    val appColors = LocalAppColors.current
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

    // Share インポート関連
    var showShareImportPreview by remember { mutableStateOf(false) }
    var showShareImportResult by remember { mutableStateOf(false) }
    var pendingShareImportJson by remember { mutableStateOf<String?>(null) }
    var shareImportFileName by remember { mutableStateOf<String?>(null) }
    var shareImportPreviewData by remember { mutableStateOf<CommunityShareData?>(null) }
    var shareImportPreviewReport by remember { mutableStateOf<CommunityShareImportReport?>(null) }
    var shareImportReport by remember { mutableStateOf<CommunityShareImportReport?>(null) }

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
                            // ファイル種別チェック: コミュニティ共有JSONが渡された場合はエラー
                            val fileType = viewModel.detectJsonFileType(jsonData)
                            if (fileType == "share") {
                                withContext(Dispatchers.Main) {
                                    viewModel.showWrongFileTypeMessage(
                                        detected = "share",
                                        expected = "backup"
                                    )
                                }
                                return@withContext
                            }

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

    // Share インポート用ランチャー
    val shareImportLauncher = rememberLauncherForActivityResult(
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

                        // JSONを読み込む
                        val jsonData = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            inputStream.readBytes().decodeToString()
                        } ?: ""

                        if (jsonData.isNotEmpty()) {
                            // ファイル種別チェック: バックアップJSONが渡された場合はエラー
                            val fileType = viewModel.detectJsonFileType(jsonData)
                            if (fileType == "backup") {
                                withContext(Dispatchers.Main) {
                                    viewModel.showWrongFileTypeMessage(
                                        detected = "backup",
                                        expected = "share"
                                    )
                                }
                                return@withContext
                            }

                            val json = Json { ignoreUnknownKeys = true }
                            val shareData = json.decodeFromString<CommunityShareData>(jsonData)
                            val preview = viewModel.previewCommunityShareImport(shareData)

                            withContext(Dispatchers.Main) {
                                pendingShareImportJson = jsonData
                                shareImportFileName = fileName
                                shareImportPreviewData = shareData
                                shareImportPreviewReport = preview
                                showShareImportPreview = true
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        android.util.Log.e("SettingsScreen", "Failed to read share import file", e)
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
                    color = appColors.textPrimary,
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
                        color = appColors.textPrimary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = stringResource(R.string.section_full_backup_description),
                        fontSize = 14.sp,
                        color = appColors.textSecondary,
                        lineHeight = 20.sp
                    )
                }
            }

            // エクスポートボタン
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = appColors.cardBackground
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
                                color = appColors.textPrimary
                            )
                            Text(
                                text = stringResource(R.string.create_backup),
                                fontSize = 14.sp,
                                color = appColors.textSecondary,
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
                        containerColor = appColors.cardBackground
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
                                color = appColors.textPrimary
                            )
                            Text(
                                text = stringResource(R.string.restore_from_backup),
                                fontSize = 14.sp,
                                color = appColors.textSecondary,
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
                                color = appColors.textPrimary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = stringResource(R.string.import_warning),
                                fontSize = 14.sp,
                                color = appColors.textTertiary,
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
                        color = appColors.textPrimary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = stringResource(R.string.section_partial_data_management_description),
                        fontSize = 14.sp,
                        color = appColors.textSecondary,
                        lineHeight = 20.sp
                    )
                }
            }

            // CSVエクスポートボタン
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = appColors.cardBackground
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
                                color = appColors.textPrimary
                            )
                            Text(
                                text = stringResource(R.string.csv_export_description),
                                fontSize = 14.sp,
                                color = appColors.textSecondary,
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
                        containerColor = appColors.cardBackground
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
                                color = appColors.textPrimary
                            )
                            Text(
                                text = stringResource(R.string.csv_import_description),
                                fontSize = 14.sp,
                                color = appColors.textSecondary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // ========================================
            // セクション: Share
            // ========================================

            // セクションタイトルと説明
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Share",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.textPrimary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Share your programs and exercises with others",
                        fontSize = 14.sp,
                        color = appColors.textSecondary,
                        lineHeight = 20.sp
                    )
                }
            }

            // Community Share エクスポートボタン
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = appColors.cardBackground
                    ),
                    shape = RoundedCornerShape(12.dp),
                    onClick = { onNavigateToCommunityShareExport() }
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
                                text = "Export for sharing",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.textPrimary
                            )
                            Text(
                                text = "Select programs and exercises to share",
                                fontSize = 14.sp,
                                color = appColors.textSecondary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // Community Share インポートボタン
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = appColors.cardBackground
                    ),
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        if (!isLoading) {
                            shareImportLauncher.launch(arrayOf("application/json"))
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
                                text = "Import shared JSON file",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.textPrimary
                            )
                            Text(
                                text = "Import programs and exercises from a shared JSON file",
                                fontSize = 14.sp,
                                color = appColors.textSecondary,
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
                        color = appColors.textPrimary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = stringResource(R.string.section_language_description),
                        fontSize = 14.sp,
                        color = appColors.textSecondary,
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
                        containerColor = appColors.cardBackground
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
                                color = appColors.textPrimary
                            )
                            Text(
                                text = stringResource(
                                    R.string.current_language,
                                    selectedLanguage.getDisplayName(currentLocale)
                                ),
                                fontSize = 14.sp,
                                color = appColors.textSecondary,
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
                                                appColors.cardBackgroundSecondary
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
                                            color = appColors.textPrimary,
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

            // ========================================
            // セクション4: テーマ設定
            // ========================================

            // セクションタイトルと説明
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.section_theme),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.textPrimary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = stringResource(R.string.section_theme_description),
                        fontSize = 14.sp,
                        color = appColors.textSecondary,
                        lineHeight = 20.sp
                    )
                }
            }

            // テーマ選択カード
            item {
                var showThemeDialog by remember { mutableStateOf(false) }

                val currentLocale = Locale.getDefault().language
                val themeDisplayName = when (currentTheme) {
                    AppTheme.SYSTEM -> stringResource(R.string.theme_system)
                    AppTheme.LIGHT -> stringResource(R.string.theme_light)
                    AppTheme.DARK -> stringResource(R.string.theme_dark)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = appColors.cardBackground
                    ),
                    shape = RoundedCornerShape(12.dp),
                    onClick = { showThemeDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎨",
                            fontSize = 32.sp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.theme_setting),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.textPrimary
                            )
                            Text(
                                text = stringResource(R.string.current_theme, themeDisplayName),
                                fontSize = 14.sp,
                                color = appColors.textSecondary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // テーマ選択ダイアログ
                if (showThemeDialog) {
                    AlertDialog(
                        onDismissRequest = { showThemeDialog = false },
                        title = {
                            Text(
                                text = stringResource(R.string.theme_setting),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                AppTheme.entries.forEach { theme ->
                                    val displayName = when (theme) {
                                        AppTheme.SYSTEM -> stringResource(R.string.theme_system)
                                        AppTheme.LIGHT -> stringResource(R.string.theme_light)
                                        AppTheme.DARK -> stringResource(R.string.theme_dark)
                                    }
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (currentTheme == theme) {
                                                Purple600.copy(alpha = 0.3f)
                                            } else {
                                                appColors.cardBackgroundSecondary
                                            }
                                        ),
                                        onClick = {
                                            onThemeChange(theme)
                                            showThemeDialog = false
                                        }
                                    ) {
                                        Text(
                                            text = displayName,
                                            modifier = Modifier.padding(16.dp),
                                            color = appColors.textPrimary,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showThemeDialog = false }) {
                                Text(stringResource(R.string.close))
                            }
                        }
                    )
                }
            }

            // ========================================
            // セクション5: オートフィル
            // ========================================

            // セクションタイトルと説明
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_autofill_section),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.textPrimary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = stringResource(R.string.settings_autofill_section_description),
                        fontSize = 14.sp,
                        color = appColors.textSecondary,
                        lineHeight = 20.sp
                    )
                }
            }

            // オートフィル設定カード
            item {
                val workoutPrefs = remember { io.github.gonbei774.calisthenicsmemory.data.WorkoutPreferences(context) }
                var prefillEnabled by remember { mutableStateOf(workoutPrefs.isPrefillPreviousRecordEnabled()) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = appColors.cardBackground
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📝",
                            fontSize = 32.sp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_prefill_previous),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.textPrimary
                            )
                            Text(
                                text = stringResource(R.string.settings_prefill_previous_description),
                                fontSize = 14.sp,
                                color = appColors.textSecondary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Switch(
                            checked = prefillEnabled,
                            onCheckedChange = { enabled ->
                                prefillEnabled = enabled
                                workoutPrefs.setPrefillPreviousRecordEnabled(enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = appColors.switchThumb,
                                checkedTrackColor = Orange600,
                                uncheckedThumbColor = appColors.switchThumb,
                                uncheckedTrackColor = Slate600
                            )
                        )
                    }
                }
            }

            // ========================================
            // セクション5: ワークアウト設定
            // ========================================

            // セクションタイトルと説明
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.section_workout_settings),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.textPrimary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = stringResource(R.string.section_workout_settings_description),
                        fontSize = 14.sp,
                        color = appColors.textSecondary,
                        lineHeight = 20.sp
                    )
                }
            }

            // ワークアウト設定カード
            item {
                val workoutPrefs = remember { io.github.gonbei774.calisthenicsmemory.data.WorkoutPreferences(context) }
                var startCountdown by remember { mutableStateOf(workoutPrefs.getStartCountdown()) }
                var setInterval by remember { mutableStateOf(workoutPrefs.getSetInterval()) }
                var startCountdownEnabled by remember { mutableStateOf(workoutPrefs.isStartCountdownEnabled()) }
                var setIntervalEnabled by remember { mutableStateOf(workoutPrefs.isSetIntervalEnabled()) }
                var flashNotificationEnabled by remember { mutableStateOf(workoutPrefs.isFlashNotificationEnabled()) }
                var keepScreenOnEnabled by remember { mutableStateOf(workoutPrefs.isKeepScreenOnEnabled()) }
                var showStartCountdownDialog by remember { mutableStateOf(false) }
                var showSetIntervalDialog by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 開始カウントダウン設定
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = appColors.cardBackground
                        ),
                        shape = RoundedCornerShape(12.dp),
                        onClick = { if (startCountdownEnabled) showStartCountdownDialog = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⏱️",
                                fontSize = 32.sp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.start_countdown_setting),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = appColors.textPrimary
                                )
                                Text(
                                    text = stringResource(R.string.current_start_countdown, startCountdown),
                                    fontSize = 14.sp,
                                    color = if (startCountdownEnabled) appColors.textSecondary else appColors.textSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            Switch(
                                checked = startCountdownEnabled,
                                onCheckedChange = { enabled ->
                                    startCountdownEnabled = enabled
                                    workoutPrefs.setStartCountdownEnabled(enabled)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = appColors.switchThumb,
                                    checkedTrackColor = Orange600,
                                    uncheckedThumbColor = appColors.switchThumb,
                                    uncheckedTrackColor = Slate600
                                )
                            )
                        }
                    }

                    // セット間インターバル設定
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = appColors.cardBackground
                        ),
                        shape = RoundedCornerShape(12.dp),
                        onClick = { if (setIntervalEnabled) showSetIntervalDialog = true }
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⏸️",
                                    fontSize = 32.sp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.set_interval_setting),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = appColors.textPrimary
                                    )
                                    Text(
                                        text = stringResource(R.string.current_set_interval, setInterval),
                                        fontSize = 14.sp,
                                        color = if (setIntervalEnabled) appColors.textSecondary else appColors.textSecondary.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                Switch(
                                    checked = setIntervalEnabled,
                                    onCheckedChange = { enabled ->
                                        setIntervalEnabled = enabled
                                        workoutPrefs.setSetIntervalEnabled(enabled)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = appColors.switchThumb,
                                        checkedTrackColor = Orange600,
                                        uncheckedThumbColor = appColors.switchThumb,
                                        uncheckedTrackColor = Slate600
                                    )
                                )
                            }
                            // 注意書き
                            Text(
                                text = stringResource(R.string.set_interval_note),
                                fontSize = 12.sp,
                                color = appColors.textSecondary,
                                modifier = Modifier.padding(start = 68.dp, end = 20.dp, bottom = 16.dp)
                            )
                        }
                    }

                    // LEDフラッシュ通知設定
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = appColors.cardBackground
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📸",
                                    fontSize = 32.sp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.flash_notification_setting),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = appColors.textPrimary
                                    )
                                    Text(
                                        text = stringResource(R.string.flash_notification_description),
                                        fontSize = 14.sp,
                                        color = appColors.textSecondary,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                Switch(
                                    checked = flashNotificationEnabled,
                                    onCheckedChange = { enabled ->
                                        flashNotificationEnabled = enabled
                                        workoutPrefs.setFlashNotificationEnabled(enabled)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = appColors.switchThumb,
                                        checkedTrackColor = Orange600,
                                        uncheckedThumbColor = appColors.switchThumb,
                                        uncheckedTrackColor = Slate600
                                    )
                                )
                            }
                        }
                    }

                    // 画面オン維持設定
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = appColors.cardBackground
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🔆",
                                    fontSize = 32.sp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.keep_screen_on_setting),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = appColors.textPrimary
                                    )
                                    Text(
                                        text = stringResource(R.string.keep_screen_on_description),
                                        fontSize = 14.sp,
                                        color = appColors.textSecondary,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                Switch(
                                    checked = keepScreenOnEnabled,
                                    onCheckedChange = { enabled ->
                                        keepScreenOnEnabled = enabled
                                        workoutPrefs.setKeepScreenOnEnabled(enabled)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = appColors.switchThumb,
                                        checkedTrackColor = Orange600,
                                        uncheckedThumbColor = appColors.switchThumb,
                                        uncheckedTrackColor = Slate600
                                    )
                                )
                            }
                        }
                    }
                }

                // 開始カウントダウン設定ダイアログ
                if (showStartCountdownDialog) {
                    var inputValue by remember { mutableStateOf(startCountdown.toString()) }

                    AlertDialog(
                        onDismissRequest = { showStartCountdownDialog = false },
                        title = {
                            Text(
                                text = stringResource(R.string.start_countdown_dialog_title),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            OutlinedTextField(
                                value = inputValue,
                                onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) inputValue = it },
                                label = { Text(stringResource(R.string.enter_seconds)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Orange600,
                                    focusedLabelColor = Orange600,
                                    cursorColor = Orange600
                                )
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val newValue = inputValue.toIntOrNull() ?: io.github.gonbei774.calisthenicsmemory.data.WorkoutPreferences.DEFAULT_START_COUNTDOWN
                                    workoutPrefs.setStartCountdown(newValue)
                                    startCountdown = newValue
                                    showStartCountdownDialog = false
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = Orange600
                                )
                            ) {
                                Text(stringResource(R.string.save))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showStartCountdownDialog = false }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    )
                }

                // セット間インターバル設定ダイアログ
                if (showSetIntervalDialog) {
                    var inputValue by remember { mutableStateOf(setInterval.toString()) }
                    val maxInterval = io.github.gonbei774.calisthenicsmemory.data.WorkoutPreferences.MAX_SET_INTERVAL

                    AlertDialog(
                        onDismissRequest = { showSetIntervalDialog = false },
                        title = {
                            Text(
                                text = stringResource(R.string.set_interval_dialog_title),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            OutlinedTextField(
                                value = inputValue,
                                onValueChange = { newValue ->
                                    if (newValue.isEmpty() || newValue.all { c -> c.isDigit() }) {
                                        // 上限チェック
                                        val intValue = newValue.toIntOrNull()
                                        if (intValue == null || intValue <= maxInterval) {
                                            inputValue = newValue
                                        }
                                    }
                                },
                                label = { Text(stringResource(R.string.enter_seconds_max, maxInterval)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Orange600,
                                    focusedLabelColor = Orange600,
                                    cursorColor = Orange600
                                )
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val newValue = (inputValue.toIntOrNull() ?: io.github.gonbei774.calisthenicsmemory.data.WorkoutPreferences.DEFAULT_SET_INTERVAL)
                                        .coerceIn(1, maxInterval)
                                    workoutPrefs.setSetInterval(newValue)
                                    setInterval = newValue
                                    showSetIntervalDialog = false
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = Orange600
                                )
                            ) {
                                Text(stringResource(R.string.save))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showSetIntervalDialog = false }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    )
                }
            }

            // ========================================
            // セクション5: アプリ情報
            // ========================================

            // セクションタイトルと説明
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.section_app_info),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.textPrimary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = stringResource(R.string.section_app_info_description),
                        fontSize = 14.sp,
                        color = appColors.textSecondary,
                        lineHeight = 20.sp
                    )
                }
            }

            // アプリ情報カード（Auxioスタイル）
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = appColors.cardBackground
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // アプリ名と説明（中央揃え）
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.app_name),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.textPrimary
                            )
                            Text(
                                text = stringResource(R.string.app_description),
                                fontSize = 14.sp,
                                color = appColors.textSecondary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        // バージョン
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = appColors.textSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.app_version),
                                    fontSize = 16.sp,
                                    color = appColors.textPrimary
                                )
                                Text(
                                    text = BuildConfig.VERSION_NAME,
                                    fontSize = 14.sp,
                                    color = appColors.textSecondary
                                )
                            }
                        }

                        // ソースコード
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://codeberg.org/Gonbei774/CalisthenicsMemory"))
                                    context.startActivity(intent)
                                },
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "<>",
                                fontSize = 20.sp,
                                color = appColors.textSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.app_source_code),
                                fontSize = 16.sp,
                                color = appColors.textPrimary
                            )
                        }

                        // 使用許諾（ライセンス）
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToLicenses() },
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📄",
                                fontSize = 20.sp
                            )
                            Text(
                                text = stringResource(R.string.open_source_licenses),
                                fontSize = 16.sp,
                                color = appColors.textPrimary
                            )
                        }
                    }
                }
            }

            // 著者カード
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = appColors.cardBackground
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // セクションタイトル
                        Text(
                            text = stringResource(R.string.app_author),
                            fontSize = 14.sp,
                            color = appColors.textSecondary
                        )

                        // 開発者
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = appColors.textSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Gonbei774",
                                fontSize = 16.sp,
                                color = appColors.textPrimary
                            )
                        }
                    }
                }
            }

            // フィードバックカード
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = appColors.cardBackground
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // セクションタイトル
                        Text(
                            text = stringResource(R.string.app_feedback),
                            fontSize = 14.sp,
                            color = appColors.textSecondary
                        )

                        // Codebergで問題を報告
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://codeberg.org/Gonbei774/CalisthenicsMemory/issues"))
                                    context.startActivity(intent)
                                },
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📝",
                                fontSize = 20.sp
                            )
                            Text(
                                text = stringResource(R.string.report_issue_codeberg),
                                fontSize = 16.sp,
                                color = appColors.textPrimary
                            )
                        }

                        // GitHubで問題を報告
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Gonbei774/CalisthenicsMemory/issues"))
                                    context.startActivity(intent)
                                },
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📝",
                                fontSize = 20.sp
                            )
                            Text(
                                text = stringResource(R.string.report_issue_github),
                                fontSize = 16.sp,
                                color = appColors.textPrimary
                            )
                        }
                    }
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
                            containerColor = appColors.cardBackgroundSecondary
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
                                color = appColors.textSecondary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Text(
                                text = importFileName ?: "unknown.json",
                                fontSize = 16.sp,
                                color = appColors.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // データ件数
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = appColors.cardBackgroundSecondary
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
                                color = appColors.textSecondary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.groups),
                                    fontSize = 16.sp,
                                    color = appColors.textTertiary
                                )
                                Text(
                                    text = stringResource(R.string.count_items, importGroupCount),
                                    fontSize = 16.sp,
                                    color = appColors.textPrimary,
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
                                    color = appColors.textTertiary
                                )
                                Text(
                                    text = stringResource(R.string.count_items, importExerciseCount),
                                    fontSize = 16.sp,
                                    color = appColors.textPrimary,
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
                                    color = appColors.textTertiary
                                )
                                Text(
                                    text = stringResource(R.string.count_records, importRecordCount),
                                    fontSize = 16.sp,
                                    color = appColors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = stringResource(R.string.import_data_preview_message),
                        fontSize = 16.sp,
                        color = appColors.textTertiary,
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
                                    } else if (backupImportType == "Share") {
                                        // Shareインポート実行
                                        pendingShareImportJson?.let { jsonData ->
                                            val report = withContext(Dispatchers.IO) {
                                                viewModel.importCommunityShare(jsonData)
                                            }
                                            withContext(Dispatchers.Main) {
                                                shareImportReport = report
                                                showShareImportResult = true
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("SettingsScreen", "Import error after backup", e)
                                } finally {
                                    withContext(Dispatchers.Main) {
                                        isLoading = false
                                        pendingCsvString = null
                                        pendingShareImportJson = null
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
                            } else if (backupImportType == "Share") {
                                // Shareインポート実行
                                pendingShareImportJson?.let { jsonData ->
                                    scope.launch {
                                        isLoading = true
                                        try {
                                            val report = withContext(Dispatchers.IO) {
                                                viewModel.importCommunityShare(jsonData)
                                            }
                                            shareImportReport = report
                                            showShareImportResult = true
                                        } catch (e: Exception) {
                                            android.util.Log.e("SettingsScreen", "Share import error", e)
                                        } finally {
                                            isLoading = false
                                            pendingShareImportJson = null
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
                            contentColor = appColors.textPrimary
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
                        pendingShareImportJson = null
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
                            containerColor = appColors.cardBackgroundSecondary
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
                                    color = appColors.textPrimary
                                )
                                Text(
                                    text = stringResource(R.string.csv_export_groups_description),
                                    fontSize = 14.sp,
                                    color = appColors.textSecondary
                                )
                            }
                        }
                    }

                    // 種目
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = appColors.cardBackgroundSecondary
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
                                    color = appColors.textPrimary
                                )
                                Text(
                                    text = stringResource(R.string.csv_export_exercises_description),
                                    fontSize = 14.sp,
                                    color = appColors.textSecondary
                                )
                            }
                        }
                    }

                    // 記録（実データ）
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = appColors.cardBackgroundSecondary
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
                                    color = appColors.textPrimary
                                )
                                Text(
                                    text = stringResource(R.string.csv_export_records_description),
                                    fontSize = 14.sp,
                                    color = appColors.textSecondary
                                )
                            }
                        }
                    }

                    // 記録テンプレート
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = appColors.cardBackgroundSecondary
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
                                    color = appColors.textPrimary
                                )
                                Text(
                                    text = stringResource(R.string.csv_export_record_template_description),
                                    fontSize = 14.sp,
                                    color = appColors.textSecondary
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
                            containerColor = appColors.cardBackgroundSecondary
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
                                    color = appColors.textSecondary
                                )
                                Text(
                                    text = csvFileName ?: "unknown.csv",
                                    fontSize = 14.sp,
                                    color = appColors.textPrimary,
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
                                    color = appColors.textSecondary
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
                                    color = appColors.textSecondary
                                )
                                Text(
                                    text = "$csvImportDataCount",
                                    fontSize = 14.sp,
                                    color = appColors.textPrimary,
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
                            containerColor = appColors.cardBackgroundSecondary
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
                                    color = appColors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = stringResource(R.string.csv_import_skipped_label), fontSize = 14.sp, color = appColors.textSecondary)
                                Text(
                                    text = "${report.skippedCount}",
                                    fontSize = 14.sp,
                                    color = appColors.textPrimary,
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
                                    color = appColors.textPrimary,
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
                                containerColor = appColors.cardBackgroundSecondary
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
                                        color = appColors.textTertiary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (showSkippedItems) "▼" else "▶",
                                        fontSize = 12.sp,
                                        color = appColors.textSecondary
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
                                                color = appColors.textSecondary,
                                                lineHeight = 16.sp
                                            )
                                        }
                                        if (report.skippedItems.size > 10) {
                                            Text(
                                                text = "... and ${report.skippedItems.size - 10} more",
                                                fontSize = 12.sp,
                                                color = appColors.textSecondary
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
                                        color = appColors.textSecondary
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

    // Share インポートプレビューダイアログ
    if (showShareImportPreview && shareImportPreviewReport != null) {
        val preview = shareImportPreviewReport!!
        AlertDialog(
            onDismissRequest = {
                showShareImportPreview = false
                pendingShareImportJson = null
                shareImportPreviewData = null
                shareImportPreviewReport = null
            },
            title = {
                Text(
                    text = "Import Shared JSON File",
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
                            containerColor = appColors.cardBackgroundSecondary
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
                                color = appColors.textSecondary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Text(
                                text = shareImportFileName ?: "unknown.json",
                                fontSize = 16.sp,
                                color = appColors.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // 新規追加
                    val hasNewItems = preview.groupsAdded > 0 || preview.exercisesAdded > 0 ||
                            preview.programsAdded > 0 || preview.intervalProgramsAdded > 0
                    if (hasNewItems) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Green400.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "New (will be added)",
                                    fontSize = 14.sp,
                                    color = Green400,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                if (preview.groupsAdded > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = stringResource(R.string.groups), fontSize = 14.sp, color = appColors.textTertiary)
                                        Text(text = "${preview.groupsAdded}", fontSize = 14.sp, color = appColors.textPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (preview.exercisesAdded > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = stringResource(R.string.exercises), fontSize = 14.sp, color = appColors.textTertiary)
                                        Text(text = "${preview.exercisesAdded}", fontSize = 14.sp, color = appColors.textPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (preview.programsAdded > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Programs", fontSize = 14.sp, color = appColors.textTertiary)
                                        Text(text = "${preview.programsAdded}", fontSize = 14.sp, color = appColors.textPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (preview.intervalProgramsAdded > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Intervals", fontSize = 14.sp, color = appColors.textTertiary)
                                        Text(text = "${preview.intervalProgramsAdded}", fontSize = 14.sp, color = appColors.textPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // スキップ/再利用
                    val hasSkippedItems = preview.groupsReused > 0 || preview.exercisesSkipped > 0 ||
                            preview.programsSkipped > 0 || preview.intervalProgramsSkipped > 0
                    if (hasSkippedItems) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = appColors.cardBackgroundSecondary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Already exists (will be skipped)",
                                    fontSize = 14.sp,
                                    color = appColors.textSecondary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                if (preview.groupsReused > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = stringResource(R.string.groups), fontSize = 14.sp, color = appColors.textTertiary)
                                        Text(text = "${preview.groupsReused} reused", fontSize = 14.sp, color = appColors.textSecondary)
                                    }
                                }
                                if (preview.exercisesSkipped > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = stringResource(R.string.exercises), fontSize = 14.sp, color = appColors.textTertiary)
                                        Text(text = "${preview.exercisesSkipped} skipped", fontSize = 14.sp, color = appColors.textSecondary)
                                    }
                                }
                                if (preview.programsSkipped > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Programs", fontSize = 14.sp, color = appColors.textTertiary)
                                        Text(text = "${preview.programsSkipped} skipped", fontSize = 14.sp, color = appColors.textSecondary)
                                    }
                                }
                                if (preview.intervalProgramsSkipped > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Intervals", fontSize = 14.sp, color = appColors.textTertiary)
                                        Text(text = "${preview.intervalProgramsSkipped} skipped", fontSize = 14.sp, color = appColors.textSecondary)
                                    }
                                }
                            }
                        }
                    }

                    // すべて既存の場合
                    if (!hasNewItems) {
                        Text(
                            text = "All items already exist. Nothing will be added.",
                            fontSize = 14.sp,
                            color = appColors.textSecondary,
                            lineHeight = 20.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showShareImportPreview = false
                        shareImportPreviewData = null
                        shareImportPreviewReport = null
                        backupImportType = "Share"
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
                        showShareImportPreview = false
                        pendingShareImportJson = null
                        shareImportPreviewData = null
                        shareImportPreviewReport = null
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Share インポート結果ダイアログ
    if (showShareImportResult && shareImportReport != null) {
        val report = shareImportReport!!
        AlertDialog(
            onDismissRequest = {
                showShareImportResult = false
                shareImportReport = null
            },
            title = {
                Text(
                    text = "Import Complete",
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
                            containerColor = appColors.cardBackgroundSecondary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Groups
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.groups),
                                    fontSize = 14.sp,
                                    color = appColors.textTertiary
                                )
                                Text(
                                    text = "${report.groupsAdded} added, ${report.groupsReused} reused",
                                    fontSize = 14.sp,
                                    color = appColors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            // Exercises
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.exercises),
                                    fontSize = 14.sp,
                                    color = appColors.textTertiary
                                )
                                Text(
                                    text = "${report.exercisesAdded} added, ${report.exercisesSkipped} skipped",
                                    fontSize = 14.sp,
                                    color = appColors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            // Programs
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Programs",
                                    fontSize = 14.sp,
                                    color = appColors.textTertiary
                                )
                                Text(
                                    text = "${report.programsAdded} added, ${report.programsSkipped} skipped",
                                    fontSize = 14.sp,
                                    color = appColors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            // Intervals
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Intervals",
                                    fontSize = 14.sp,
                                    color = appColors.textTertiary
                                )
                                Text(
                                    text = "${report.intervalProgramsAdded} added, ${report.intervalProgramsSkipped} skipped",
                                    fontSize = 14.sp,
                                    color = appColors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // エラーがあれば表示
                    if (report.errors.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Red600.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Errors (${report.errors.size})",
                                    fontSize = 14.sp,
                                    color = Red600,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
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
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showShareImportResult = false
                        shareImportReport = null
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