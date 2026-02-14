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
import io.github.gonbei774.calisthenicsmemory.viewmodel.BackupData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: TrainingViewModel,
    onNavigateBack: () -> Unit
) {
    val appColors = LocalAppColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showDataPreview by remember { mutableStateOf(false) }
    var showBackupConfirmation by remember { mutableStateOf(false) }
    var showImportWarning by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var importFileName by remember { mutableStateOf<String?>(null) }
    var importGroupCount by remember { mutableStateOf(0) }
    var importExerciseCount by remember { mutableStateOf(0) }
    var importRecordCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }

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
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
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
                    android.util.Log.e("BackupScreen", "Backup before import failed", e)
                    false
                }

                viewModel.showBackupResult(backupSuccess)

                try {
                    withContext(Dispatchers.Main) {
                        showImportWarning = true
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BackupScreen", "Import error after backup", e)
                } finally {
                    withContext(Dispatchers.Main) {
                        isLoading = false
                    }
                }
            }
        } else {
            showBackupConfirmation = true
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
                                pendingImportUri = uri
                                importFileName = fileName
                                importGroupCount = backupData.groups.size
                                importExerciseCount = backupData.exercises.size
                                importRecordCount = backupData.records.size

                                showDataPreview = true
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        android.util.Log.e("BackupScreen", "Failed to read import file", e)
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
                        text = stringResource(R.string.section_full_backup),
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
                    text = stringResource(R.string.section_full_backup_description),
                    fontSize = 14.sp,
                    color = appColors.textSecondary,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
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
                            text = "\uD83D\uDCE4",
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
                            text = "\uD83D\uDCE5",
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

            // 注意書き
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
                            text = "\u2139\uFE0F",
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
                        showDataPreview = false
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
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
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
                            showImportWarning = true
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
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
