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
import io.github.gonbei774.calisthenicsmemory.data.TodoTask
import io.github.gonbei774.calisthenicsmemory.data.AppTheme
import io.github.gonbei774.calisthenicsmemory.data.LanguagePreferences
import io.github.gonbei774.calisthenicsmemory.data.ThemePreferences
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
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
import io.github.gonbei774.calisthenicsmemory.ui.screens.LicensesScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.WorkoutScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.view.ViewScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.ToDoScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.ProgramListScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.ProgramEditScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.ProgramExecutionScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.IntervalListScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.IntervalEditScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.IntervalExecutionScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.CommunityShareExportScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.BackupScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.CsvDataManagementScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.ShareHubScreen
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import io.github.gonbei774.calisthenicsmemory.ui.theme.LocalAppColors
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel

class MainActivity : ComponentActivity() {
    private val systemDarkMode = mutableStateOf(false)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(updateBaseContextLocale(newBase))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        systemDarkMode.value =
            (newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        systemDarkMode.value =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        val themePrefs = ThemePreferences(this)

        setContent {
            val isSystemDark by systemDarkMode
            val savedTheme = remember { themePrefs.getTheme() }
            var currentTheme by remember { mutableStateOf(savedTheme) }

            val darkTheme = when (currentTheme) {
                AppTheme.SYSTEM -> isSystemDark
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
            }

            CalisthenicsMemoryTheme(darkTheme = darkTheme) {
                CalisthenicsMemoryApp(
                    currentTheme = currentTheme,
                    onThemeChange = { newTheme ->
                        themePrefs.setTheme(newTheme)
                        currentTheme = newTheme
                    }
                )
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
            AppLanguage.ITALIAN -> Locale("it")
            AppLanguage.UKRAINIAN -> Locale("uk")
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
        is UiMessage.CopiedToClipboard -> stringResource(R.string.copied_to_clipboard)
        is UiMessage.ProgramDuplicated -> stringResource(R.string.program_duplicated)
        is UiMessage.CommunityShareExportComplete ->
            "Export complete: $exerciseCount exercises, $programCount programs, $intervalProgramCount interval programs"
        is UiMessage.CommunityShareImportComplete -> {
            val r = report
            "Import complete: ${r.exercisesAdded} added, ${r.exercisesSkipped} skipped, ${r.programsAdded} programs, ${r.intervalProgramsAdded} intervals"
        }
        is UiMessage.CommunityShareImportError -> "Import error: $errorMessage"
        is UiMessage.WrongFileType -> "Wrong file type: $detected (expected: $expected)"
        is UiMessage.ErrorOccurred -> stringResource(R.string.error_occurred)
    }
}

@Composable
fun CalisthenicsMemoryApp(
    currentTheme: AppTheme = AppTheme.SYSTEM,
    onThemeChange: (AppTheme) -> Unit = {}
) {
    val viewModel: TrainingViewModel = viewModel()
    var currentScreen by rememberSaveable(stateSaver = ScreenSaver) { mutableStateOf<Screen>(Screen.Home) }
    val snackbarHostState = remember { SnackbarHostState() }
    val appColors = LocalAppColors.current

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
                            appColors.backgroundGradientStart,
                            appColors.backgroundGradientEnd
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            when (currentScreen) {
                is Screen.Home -> HomeScreen(
                    onNavigate = { screen -> currentScreen = screen }
                )
                is Screen.ToDo -> {
                    BackHandler { currentScreen = Screen.Home }
                    ToDoScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = Screen.Home },
                        onNavigateToRecord = { exerciseId ->
                            currentScreen = Screen.Record(exerciseId = exerciseId, fromToDo = true)
                        },
                        onNavigateToWorkout = { exerciseId ->
                            currentScreen = Screen.Workout(exerciseId = exerciseId, fromToDo = true)
                        },
                        onNavigateToProgramPreview = { programId ->
                            currentScreen = Screen.ProgramExecution(programId = programId, fromToDo = true)
                        },
                        onNavigateToIntervalPreview = { programId ->
                            currentScreen = Screen.IntervalExecution(programId = programId, fromToDo = true)
                        }
                    )
                }
                is Screen.Create -> {
                    BackHandler {
                        viewModel.saveGroupOrder()
                        currentScreen = Screen.Home
                    }
                    CreateScreen(
                        viewModel = viewModel,
                        onNavigateBack = {
                            viewModel.saveGroupOrder()
                            currentScreen = Screen.Home
                        }
                    )
                }
                is Screen.Settings -> {
                    BackHandler { currentScreen = Screen.Home }
                    SettingsScreenNew(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = Screen.Home },
                        onNavigateToLicenses = { currentScreen = Screen.Licenses },
                        onNavigateToBackup = { currentScreen = Screen.Backup },
                        onNavigateToCsvDataManagement = { currentScreen = Screen.CsvDataManagement },
                        onNavigateToShareHub = { currentScreen = Screen.ShareHub },
                        currentTheme = currentTheme,
                        onThemeChange = onThemeChange
                    )
                }
                is Screen.Licenses -> {
                    BackHandler { currentScreen = Screen.Settings }
                    LicensesScreen(
                        onNavigateBack = { currentScreen = Screen.Settings }
                    )
                }
                is Screen.Record -> {
                    val recordScreen = currentScreen as Screen.Record
                    val backDestination = if (recordScreen.fromToDo) Screen.ToDo else Screen.Home
                    BackHandler { currentScreen = backDestination }
                    RecordScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = backDestination },
                        initialExerciseId = recordScreen.exerciseId,
                        fromToDo = recordScreen.fromToDo
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
                    val workoutScreen = currentScreen as Screen.Workout
                    val backDestination = if (workoutScreen.fromToDo) Screen.ToDo else Screen.Home
                    BackHandler { currentScreen = backDestination }
                    WorkoutScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = backDestination },
                        onNavigateToProgramList = { currentScreen = Screen.ProgramList },
                        onNavigateToIntervalList = { currentScreen = Screen.IntervalList },
                        initialExerciseId = workoutScreen.exerciseId,
                        fromToDo = workoutScreen.fromToDo
                    )
                }
                is Screen.ProgramList -> {
                    BackHandler { currentScreen = Screen.Workout() }
                    ProgramListScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = Screen.Workout() },
                        onNavigateToEdit = { programId -> currentScreen = Screen.ProgramEdit(programId) },
                        onNavigateToExecute = { programId ->
                            currentScreen = Screen.ProgramExecution(programId)
                        },
                        onNavigateToResume = { programId ->
                            currentScreen = Screen.ProgramExecution(programId, resumeSavedState = true)
                        }
                    )
                }
                is Screen.ProgramEdit -> {
                    val editScreen = currentScreen as Screen.ProgramEdit
                    BackHandler { currentScreen = Screen.ProgramList }
                    ProgramEditScreen(
                        viewModel = viewModel,
                        programId = editScreen.programId,
                        onNavigateBack = { currentScreen = Screen.ProgramList },
                        onSaved = { currentScreen = Screen.ProgramList }
                    )
                }
                is Screen.ProgramExecution -> {
                    val execScreen = currentScreen as Screen.ProgramExecution
                    val backDestination = if (execScreen.fromToDo) Screen.ToDo else Screen.ProgramList
                    BackHandler { currentScreen = backDestination }
                    ProgramExecutionScreen(
                        viewModel = viewModel,
                        programId = execScreen.programId,
                        resumeSavedState = execScreen.resumeSavedState,
                        onNavigateBack = { currentScreen = backDestination },
                        onComplete = {
                            if (execScreen.fromToDo) {
                                viewModel.completeTodoTaskByReference(TodoTask.TYPE_PROGRAM, execScreen.programId)
                            }
                            currentScreen = backDestination
                        }
                    )
                }
                is Screen.IntervalList -> {
                    BackHandler { currentScreen = Screen.Workout() }
                    IntervalListScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = Screen.Workout() },
                        onNavigateToEdit = { programId -> currentScreen = Screen.IntervalEdit(programId) },
                        onNavigateToExecute = { programId ->
                            currentScreen = Screen.IntervalExecution(programId)
                        }
                    )
                }
                is Screen.IntervalEdit -> {
                    val editScreen = currentScreen as Screen.IntervalEdit
                    BackHandler { currentScreen = Screen.IntervalList }
                    IntervalEditScreen(
                        viewModel = viewModel,
                        programId = editScreen.programId,
                        onNavigateBack = { currentScreen = Screen.IntervalList },
                        onSaved = { currentScreen = Screen.IntervalList }
                    )
                }
                is Screen.IntervalExecution -> {
                    val execScreen = currentScreen as Screen.IntervalExecution
                    val backDestination = if (execScreen.fromToDo) Screen.ToDo else Screen.IntervalList
                    BackHandler { currentScreen = backDestination }
                    IntervalExecutionScreen(
                        viewModel = viewModel,
                        programId = execScreen.programId,
                        onNavigateBack = { currentScreen = backDestination },
                        onComplete = {
                            if (execScreen.fromToDo) {
                                viewModel.completeTodoTaskByReference(TodoTask.TYPE_INTERVAL, execScreen.programId)
                            }
                            currentScreen = backDestination
                        }
                    )
                }
                is Screen.CommunityShareExport -> {
                    BackHandler { currentScreen = Screen.ShareHub }
                    CommunityShareExportScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = Screen.ShareHub }
                    )
                }
                is Screen.Backup -> {
                    BackHandler { currentScreen = Screen.Settings }
                    BackupScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = Screen.Settings }
                    )
                }
                is Screen.CsvDataManagement -> {
                    BackHandler { currentScreen = Screen.Settings }
                    CsvDataManagementScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = Screen.Settings }
                    )
                }
                is Screen.ShareHub -> {
                    BackHandler { currentScreen = Screen.Settings }
                    ShareHubScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = Screen.Settings },
                        onNavigateToCommunityShareExport = { currentScreen = Screen.CommunityShareExport }
                    )
                }
            }
        }
    }
}

sealed class Screen {
    object Home : Screen()
    object ToDo : Screen()
    object Create : Screen()
    object Settings : Screen()
    object Licenses : Screen()
    data class Record(val exerciseId: Long? = null, val fromToDo: Boolean = false) : Screen()
    object View : Screen()
    data class Workout(val exerciseId: Long? = null, val fromToDo: Boolean = false) : Screen()
    object ProgramList : Screen()
    data class ProgramEdit(val programId: Long?) : Screen()
    data class ProgramExecution(val programId: Long, val resumeSavedState: Boolean = false, val fromToDo: Boolean = false) : Screen()
    object IntervalList : Screen()
    data class IntervalEdit(val programId: Long?) : Screen()
    data class IntervalExecution(val programId: Long, val fromToDo: Boolean = false) : Screen()
    object CommunityShareExport : Screen()
    object Backup : Screen()
    object CsvDataManagement : Screen()
    object ShareHub : Screen()
}

private val ScreenSaver = mapSaver(
    save = { screen: Screen ->
        buildMap {
            when (screen) {
                Screen.Home -> put("type", "Home")
                Screen.ToDo -> put("type", "ToDo")
                Screen.Create -> put("type", "Create")
                Screen.Settings -> put("type", "Settings")
                Screen.Licenses -> put("type", "Licenses")
                Screen.View -> put("type", "View")
                Screen.ProgramList -> put("type", "ProgramList")
                Screen.IntervalList -> put("type", "IntervalList")
                is Screen.Record -> {
                    put("type", "Record")
                    put("exerciseId", screen.exerciseId ?: -1L)
                    put("fromToDo", screen.fromToDo)
                }
                is Screen.Workout -> {
                    put("type", "Workout")
                    put("exerciseId", screen.exerciseId ?: -1L)
                    put("fromToDo", screen.fromToDo)
                }
                is Screen.ProgramEdit -> {
                    put("type", "ProgramEdit")
                    put("programId", screen.programId ?: -1L)
                }
                is Screen.ProgramExecution -> {
                    put("type", "ProgramExecution")
                    put("programId", screen.programId)
                    put("resumeSavedState", screen.resumeSavedState)
                    put("fromToDo", screen.fromToDo)
                }
                is Screen.IntervalEdit -> {
                    put("type", "IntervalEdit")
                    put("programId", screen.programId ?: -1L)
                }
                is Screen.IntervalExecution -> {
                    put("type", "IntervalExecution")
                    put("programId", screen.programId)
                    put("fromToDo", screen.fromToDo)
                }
                Screen.CommunityShareExport -> put("type", "CommunityShareExport")
                Screen.Backup -> put("type", "Backup")
                Screen.CsvDataManagement -> put("type", "CsvDataManagement")
                Screen.ShareHub -> put("type", "ShareHub")
            }
        }
    },
    restore = { map ->
        when (map["type"] as String) {
            "ToDo" -> Screen.ToDo
            "Create" -> Screen.Create
            "Settings" -> Screen.Settings
            "Licenses" -> Screen.Licenses
            "View" -> Screen.View
            "ProgramList" -> Screen.ProgramList
            "IntervalList" -> Screen.IntervalList
            "Record" -> Screen.Record(
                exerciseId = (map["exerciseId"] as Long).takeIf { it != -1L },
                fromToDo = map["fromToDo"] as Boolean
            )
            "Workout" -> Screen.Workout(
                exerciseId = (map["exerciseId"] as Long).takeIf { it != -1L },
                fromToDo = map["fromToDo"] as Boolean
            )
            "ProgramEdit" -> Screen.ProgramEdit(
                programId = (map["programId"] as Long).takeIf { it != -1L }
            )
            "ProgramExecution" -> Screen.ProgramExecution(
                programId = map["programId"] as Long,
                resumeSavedState = map["resumeSavedState"] as Boolean,
                fromToDo = map["fromToDo"] as Boolean
            )
            "IntervalEdit" -> Screen.IntervalEdit(
                programId = (map["programId"] as Long).takeIf { it != -1L }
            )
            "IntervalExecution" -> Screen.IntervalExecution(
                programId = map["programId"] as Long,
                fromToDo = map["fromToDo"] as Boolean
            )
            "CommunityShareExport" -> Screen.CommunityShareExport
            "Backup" -> Screen.Backup
            "CsvDataManagement" -> Screen.CsvDataManagement
            "ShareHub" -> Screen.ShareHub
            else -> Screen.Home
        }
    }
)