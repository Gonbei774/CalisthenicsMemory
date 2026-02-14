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
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.ui.theme.*
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel
import io.github.gonbei774.calisthenicsmemory.viewmodel.CommunityShareData
import io.github.gonbei774.calisthenicsmemory.viewmodel.CommunityShareImportReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareHubScreen(
    viewModel: TrainingViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCommunityShareExport: () -> Unit
) {
    val appColors = LocalAppColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showShareImportPreview by remember { mutableStateOf(false) }
    var showShareImportResult by remember { mutableStateOf(false) }
    var pendingShareImportJson by remember { mutableStateOf<String?>(null) }
    var shareImportFileName by remember { mutableStateOf<String?>(null) }
    var shareImportPreviewData by remember { mutableStateOf<CommunityShareData?>(null) }
    var shareImportPreviewReport by remember { mutableStateOf<CommunityShareImportReport?>(null) }
    var shareImportReport by remember { mutableStateOf<CommunityShareImportReport?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showBackupConfirmation by remember { mutableStateOf(false) }

    // Share インポート用ランチャー
    val shareImportLauncher = rememberLauncherForActivityResult(
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
                        } ?: "unknown.json"

                        val jsonData = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            inputStream.readBytes().decodeToString()
                        } ?: ""

                        if (jsonData.isNotEmpty()) {
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
                        android.util.Log.e("ShareHubScreen", "Failed to read share import file", e)
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
                    android.util.Log.e("ShareHubScreen", "Backup before import failed", e)
                    false
                }

                viewModel.showBackupResult(backupSuccess)

                try {
                    pendingShareImportJson?.let { jsonData ->
                        val report = withContext(Dispatchers.IO) {
                            viewModel.importCommunityShare(jsonData)
                        }
                        withContext(Dispatchers.Main) {
                            shareImportReport = report
                            showShareImportResult = true
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ShareHubScreen", "Import error after backup", e)
                } finally {
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        pendingShareImportJson = null
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
                        text = stringResource(R.string.share_section_title),
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
                    text = stringResource(R.string.share_section_description),
                    fontSize = 14.sp,
                    color = appColors.textSecondary,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
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
                            text = "\uD83D\uDCE4",
                            fontSize = 32.sp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.share_export_title),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.textPrimary
                            )
                            Text(
                                text = stringResource(R.string.share_export_description),
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
                            text = "\uD83D\uDCE5",
                            fontSize = 32.sp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.share_import_title),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.textPrimary
                            )
                            Text(
                                text = stringResource(R.string.share_import_description),
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
                    text = stringResource(R.string.share_import_dialog_title),
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
                                    text = stringResource(R.string.share_import_preview_new),
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
                                        Text(text = stringResource(R.string.share_tab_programs), fontSize = 14.sp, color = appColors.textTertiary)
                                        Text(text = "${preview.programsAdded}", fontSize = 14.sp, color = appColors.textPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (preview.intervalProgramsAdded > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = stringResource(R.string.share_tab_intervals), fontSize = 14.sp, color = appColors.textTertiary)
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
                                    text = stringResource(R.string.share_import_preview_exists),
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
                                        Text(text = stringResource(R.string.share_import_count_reused, preview.groupsReused), fontSize = 14.sp, color = appColors.textSecondary)
                                    }
                                }
                                if (preview.exercisesSkipped > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = stringResource(R.string.exercises), fontSize = 14.sp, color = appColors.textTertiary)
                                        Text(text = stringResource(R.string.share_import_count_skipped, preview.exercisesSkipped), fontSize = 14.sp, color = appColors.textSecondary)
                                    }
                                }
                                if (preview.programsSkipped > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = stringResource(R.string.share_tab_programs), fontSize = 14.sp, color = appColors.textTertiary)
                                        Text(text = stringResource(R.string.share_import_count_skipped, preview.programsSkipped), fontSize = 14.sp, color = appColors.textSecondary)
                                    }
                                }
                                if (preview.intervalProgramsSkipped > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = stringResource(R.string.share_tab_intervals), fontSize = 14.sp, color = appColors.textTertiary)
                                        Text(text = stringResource(R.string.share_import_count_skipped, preview.intervalProgramsSkipped), fontSize = 14.sp, color = appColors.textSecondary)
                                    }
                                }
                            }
                        }
                    }

                    // すべて既存の場合
                    if (!hasNewItems) {
                        Text(
                            text = stringResource(R.string.share_import_preview_nothing),
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
                    text = stringResource(R.string.share_import_complete),
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
                                    text = stringResource(R.string.share_import_added_reused, report.groupsAdded, report.groupsReused),
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
                                    text = stringResource(R.string.share_import_added_skipped, report.exercisesAdded, report.exercisesSkipped),
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
                                    text = stringResource(R.string.share_tab_programs),
                                    fontSize = 14.sp,
                                    color = appColors.textTertiary
                                )
                                Text(
                                    text = stringResource(R.string.share_import_added_skipped, report.programsAdded, report.programsSkipped),
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
                                    text = stringResource(R.string.share_tab_intervals),
                                    fontSize = 14.sp,
                                    color = appColors.textTertiary
                                )
                                Text(
                                    text = stringResource(R.string.share_import_added_skipped, report.intervalProgramsAdded, report.intervalProgramsSkipped),
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
                                    text = stringResource(R.string.share_import_errors, report.errors.size),
                                    fontSize = 14.sp,
                                    color = Red600,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
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
                                        android.util.Log.e("ShareHubScreen", "Share import error", e)
                                    } finally {
                                        isLoading = false
                                        pendingShareImportJson = null
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
                        pendingShareImportJson = null
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
