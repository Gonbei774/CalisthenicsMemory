package io.github.gonbei774.calisthenicsmemory

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import io.github.gonbei774.calisthenicsmemory.data.AppLanguage
import io.github.gonbei774.calisthenicsmemory.data.LanguagePreferences
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.gonbei774.calisthenicsmemory.ui.UiMessage
import io.github.gonbei774.calisthenicsmemory.ui.screens.HomeScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.RecordScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.CreateScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.SettingsScreenNew
import io.github.gonbei774.calisthenicsmemory.ui.screens.WorkoutScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.view.ViewScreen
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(updateBaseContextLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CalisthenicsMemoryTheme {
                CalisthenicsMemoryApp()
            }
        }
    }

    /**
     * Context の言語設定を更新する（全Androidバージョン対応）
     */
    private fun updateBaseContextLocale(context: Context): Context {
        val languagePrefs = LanguagePreferences(context)
        val selectedLanguage = languagePrefs.getLanguage()

        android.util.Log.d("MainActivity", "Selected language: ${selectedLanguage.code}")

        // システム設定に従う場合は何もしない
        if (selectedLanguage == AppLanguage.SYSTEM) {
            android.util.Log.d("MainActivity", "Using system language")
            return context
        }

        val locale = when (selectedLanguage) {
            AppLanguage.JAPANESE -> Locale("ja")
            AppLanguage.ENGLISH -> Locale("en")
            AppLanguage.SPANISH -> Locale("es")
            AppLanguage.GERMAN -> Locale("de")
            AppLanguage.CHINESE -> Locale("zh", "CN")
            AppLanguage.FRENCH -> Locale("fr")
            AppLanguage.SYSTEM -> return context
        }

        android.util.Log.d("MainActivity", "Setting locale to: ${locale.language}")
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }
}

/**
 * UiMessageを現在の言語の文字列に変換
 * UI層で文字列リソースを取得することで、言語変更に即座に対応
 */
@Composable
fun UiMessage.toMessageString(): String {
    return when (this) {
        is UiMessage.ExerciseAdded -> stringResource(R.string.exercise_added)
        is UiMessage.ExerciseUpdated -> stringResource(R.string.exercise_updated)
        is UiMessage.ExerciseDeleted -> stringResource(R.string.exercise_deleted)
        is UiMessage.ExerciseAlreadyExists -> stringResource(R.string.exercise_already_exists)
        is UiMessage.AlreadyRegistered -> {
            val typeLabel = stringResource(if (type == "Dynamic") R.string.dynamic_label else R.string.isometric_label)
            stringResource(R.string.already_registered_format, name, typeLabel)
        }
        is UiMessage.AlreadyInUse -> {
            val typeLabel = stringResource(if (type == "Dynamic") R.string.dynamic_label else R.string.isometric_label)
            stringResource(R.string.already_in_use_format, name, typeLabel)
        }
        is UiMessage.SetsRecorded -> stringResource(R.string.sets_recorded, count)
        is UiMessage.RecordUpdated -> stringResource(R.string.record_updated)
        is UiMessage.RecordDeleted -> stringResource(R.string.record_deleted)
        is UiMessage.GroupCreated -> stringResource(R.string.group_created)
        is UiMessage.GroupRenamed -> stringResource(R.string.group_renamed)
        is UiMessage.GroupDeleted -> stringResource(R.string.group_deleted)
        is UiMessage.GroupAlreadyExists -> stringResource(R.string.group_already_exists)
        is UiMessage.ExportComplete -> stringResource(R.string.export_complete, groupCount, exerciseCount, recordCount)
        is UiMessage.ImportComplete -> stringResource(R.string.import_complete, groupCount, exerciseCount, recordCount)
        is UiMessage.ExportError -> stringResource(R.string.export_error, errorMessage)
        is UiMessage.ImportError -> stringResource(R.string.import_error, errorMessage)
        is UiMessage.CsvExportSuccess -> stringResource(R.string.csv_export_success, type, count)
        is UiMessage.CsvTemplateExported -> stringResource(R.string.csv_template_exported, exerciseCount)
        is UiMessage.CsvEmpty -> stringResource(R.string.csv_empty)
        is UiMessage.CsvImportSuccess -> stringResource(R.string.csv_import_success, successCount)
        is UiMessage.CsvImportPartial -> stringResource(R.string.csv_import_partial, successCount, errorCount)
        is UiMessage.BackupSaved -> stringResource(R.string.backup_saved_successfully)
        is UiMessage.BackupFailed -> stringResource(R.string.backup_failed)
        is UiMessage.ErrorOccurred -> stringResource(R.string.error_occurred)
    }
}

@Composable
fun CalisthenicsMemoryApp() {
    val viewModel: TrainingViewModel = viewModel()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Snackbar message handling
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    // UiMessageを文字列に変換（Composable関数内で実行）
    val messageString = snackbarMessage?.toMessageString()

    LaunchedEffect(snackbarMessage) {
        messageString?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSnackbarMessage()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A), // slate900
                            Color(0xFF1E293B)  // slate800
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            when (currentScreen) {
                is Screen.Home -> HomeScreen(
                    onNavigate = { screen -> currentScreen = screen }
                )
                is Screen.Create -> {
                    BackHandler { currentScreen = Screen.Home }
                    CreateScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = Screen.Home }
                    )
                }
                is Screen.Settings -> {
                    BackHandler { currentScreen = Screen.Home }
                    SettingsScreenNew(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = Screen.Home }
                    )
                }
                is Screen.Record -> {
                    BackHandler { currentScreen = Screen.Home }
                    RecordScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = Screen.Home }
                    )
                }
                is Screen.View -> {
                    BackHandler { currentScreen = Screen.Home }
                    ViewScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = Screen.Home }
                    )
                }
                is Screen.Workout -> {
                    BackHandler { currentScreen = Screen.Home }
                    WorkoutScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = Screen.Home }
                    )
                }
            }
        }
    }
}

sealed class Screen {
    object Home : Screen()
    object Create : Screen()
    object Settings : Screen()
    object Record : Screen()
    object View : Screen()
    object Workout : Screen()
}