package io.github.gonbei774.calisthenicsmemory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
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
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.gonbei774.calisthenicsmemory.ui.screens.HomeScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.RecordScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.CreateScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.SettingsScreenNew
import io.github.gonbei774.calisthenicsmemory.ui.screens.WorkoutScreen
import io.github.gonbei774.calisthenicsmemory.ui.screens.view.ViewScreen
import io.github.gonbei774.calisthenicsmemory.ui.theme.CalisthenicsMemoryTheme
import io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalisthenicsMemoryTheme {
                CalisthenicsMemoryApp()
            }
        }
    }
}

@Composable
fun CalisthenicsMemoryApp() {
    val viewModel: TrainingViewModel = viewModel()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Snackbar message handling
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { message ->
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