package io.github.gonbei774.calisthenicsmemory.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import io.github.gonbei774.calisthenicsmemory.R
import io.github.gonbei774.calisthenicsmemory.Screen
import io.github.gonbei774.calisthenicsmemory.ui.theme.*

@Composable
fun HomeScreen(
    onNavigate: (Screen) -> Unit
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigate(Screen.Settings) },
                containerColor = Slate600,
                contentColor = Color.White
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title
            Text(
                text = stringResource(R.string.bodyweight_training),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 64.dp)
            )

            // Create Button (旧: Settings Button)
            GradientButton(
                text = stringResource(R.string.home_create),
                gradient = Brush.horizontalGradient(
                    colors = listOf(Blue600, Cyan600)
                ),
                onClick = { onNavigate(Screen.Create) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // Record Button
            GradientButton(
                text = stringResource(R.string.home_record),
                gradient = Brush.horizontalGradient(
                    colors = listOf(Green600, Emerald600)
                ),
                onClick = { onNavigate(Screen.Record) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // Workout Button
            GradientButton(
                text = stringResource(R.string.home_workout),
                gradient = Brush.horizontalGradient(
                    colors = listOf(Orange600, Amber600)
                ),
                onClick = { onNavigate(Screen.Workout) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // View Button
            GradientButton(
                text = stringResource(R.string.home_view),
                gradient = Brush.horizontalGradient(
                    colors = listOf(Purple600, Pink600)
                ),
                onClick = { onNavigate(Screen.View) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun GradientButton(
    text: String,
    gradient: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(1.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(1.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
