package io.github.gonbei774.calisthenicsmemory.ui.screens

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
import io.github.gonbei774.calisthenicsmemory.ui.UiMessage
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import io.github.gonbei774.calisthenicsmemory.viewmodel.CsvImportReport
import io.github.gonbei774.calisthenicsmemory.viewmodel.CsvType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CsvDataManagementScreen(
    viewModel: TrainingViewModel,
    onNavigateBack: () -> Unit
) {
    val appColors = LocalAppColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showCsvExportDialog by remember { mutableStateOf(false) }
    var showCsvImportPreview by remember { mutableStateOf(false) }
    var csvImportType by remember { mutableStateOf<CsvType?>(null) }
    var csvImportDataCount by remember { mutableStateOf(0) }
    var pendingCsvString by remember { mutableStateOf<String?>(null) }
    var csvFileName by remember { mutableStateOf<String?>(null) }
    var showImportResult by remember { mutableStateOf(false) }
    var importReport by remember { mutableStateOf<CsvImportReport?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showBackupConfirmation by remember { mutableStateOf(false) }

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
                    withContext(Dispatchers.Main) { }
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
                    withContext(Dispatchers.Main) { }
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
                    withContext(Dispatchers.Main) { }
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
                    withContext(Dispatchers.Main) { }
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
                        val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            cursor.moveToFirst()
                            cursor.getString(nameIndex)
                        } ?: "unknown.csv"

                        val maxSize = 50 * 1024 * 1024 // 50MB
                        val csvData = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            val bytes = inputStream.readBytes()
                            if (bytes.size > maxSize) {
                                withContext(Dispatchers.Main) {
                                    viewModel.showSnackbar(UiMessage.FileTooLarge(bytes.size / (1024 * 1024), 50))
                                }
                                return@withContext
                            }
                            bytes.decodeToString()
                        } ?: ""

                        if (csvData.isNotEmpty()) {
                            val detectedType = detectCsvType(csvData)

                            if (detectedType != null) {
                                val lines = csvData.lines().filter { it.isNotBlank() && !it.startsWith("#") }
                                val dataCount = if (lines.size > 1) lines.size - 1 else 0

                                withContext(Dispatchers.Main) {
                                    csvImportType = detectedType
                                    csvFileName = fileName
                                    csvImportDataCount = dataCount
                                    pendingCsvString = csvData
                                    showCsvImportPreview = true
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    android.util.Log.e("CsvDataManagementScreen", "CSV type detection failed")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        android.util.Log.e("CsvDataManagementScreen", "CSV import error", e)
                    }
                } finally {
                    isLoading = false
                }
            }
        }
    }

    // インポート前バックアップ用ランチャー（SAFでユーザーが保存先を選択）
    val backupBeforeImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isLoading = true
                val backupSuccess = try {
                    withContext(Dispatchers.IO) {
                        val jsonData = viewModel.exportData()
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(jsonData.toByteArray())
                        }
                        true
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CsvDataManagementScreen", "Backup before import failed", e)
                    false
                }

                viewModel.showBackupResult(backupSuccess)

                try {
                    pendingCsvString?.let { csvData ->
                        val report = executeCsvImport(viewModel, csvData, csvImportType)
                        withContext(Dispatchers.Main) {
                            if (report != null) {
                                importReport = report
                                showImportResult = true
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CsvDataManagementScreen", "Import error after backup", e)
                } finally {
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        pendingCsvString = null
                    }
                }
            }
        } else {
            showBackupConfirmation = true
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
                        text = stringResource(R.string.section_partial_data_management),
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
            // セクション説明
            item {
                Text(
                    text = stringResource(R.string.section_partial_data_management_description),
                    fontSize = 14.sp,
                    color = appColors.textSecondary,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
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
                            text = "\uD83D\uDCCB",
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
                            text = "\uD83D\uDCCA",
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
                            Text(text = "\uD83D\uDCC1", fontSize = 24.sp)
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
                            Text(text = "\uD83D\uDCAA", fontSize = 24.sp)
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
                            Text(text = "\uD83D\uDCCA", fontSize = 24.sp)
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
                            Text(text = "\uD83D\uDCCB", fontSize = 24.sp)
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
                        showCsvImportPreview = false
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
                                        text = if (showSkippedItems) "\u25BC" else "\u25B6",
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
                                                text = "\u2022 $item",
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
                                        text = if (showErrors) "\u25BC" else "\u25B6",
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
                                                text = "\u2022 $error",
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

    // バックアップ確認ダイアログ
    if (showBackupConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showBackupConfirmation = false
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

                    Button(
                        onClick = {
                            showBackupConfirmation = false
                            val dateTime = LocalDateTime.now()
                            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
                            val fileName = "calisthenics_memory_backup_${dateTime.format(formatter)}.json"
                            backupBeforeImportLauncher.launch(fileName)
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

                    OutlinedButton(
                        onClick = {
                            showBackupConfirmation = false
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
                                        android.util.Log.e("CsvDataManagementScreen", "CSV import error", e)
                                    } finally {
                                        isLoading = false
                                        pendingCsvString = null
                                    }
                                }
                            }
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
                        pendingCsvString = null
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
