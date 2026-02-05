package com.example.ototp.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.ototp.ui.theme.OTOTPTheme

@Composable
internal fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    SettingsScreen(
        state = state,
        onNavigateBack = onNavigateBack,
        onAction = {
            viewModel.handleAction(it)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    state: Settings.State,
    onNavigateBack: () -> Unit = {},
    onAction: (Settings.Action) -> Unit
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(state.settingsItems.size) { index ->
                val item = state.settingsItems[index]
                when (item) {
                    is Settings.SettingsItem.ThemeMode -> {
                        SettingsThemeModeSelector(
                            currentMode = item.currentMode,
                            onModeSelected = { mode ->
                                onAction(
                                    Settings.Action.RequestThemeModeChange(mode)
                                )
                            }
                        )
                    }
                    is Settings.SettingsItem.DynamicColors -> {
                        SettingsToggleItem(
                            title = "Dynamic Colors",
                            icon = Icons.Default.ColorLens,
                            summary = "Use system wallpaper colors (Android 12+)",
                            checked = item.isEnabled,
                            onCheckedChange = { enabled ->
                                onAction(
                                    Settings.Action.RequestDynamicColorsChange(enabled)
                                )
                            }
                        )
                    }
                    is Settings.SettingsItem.Divider -> {
                        HorizontalDivider()
                    }
                    is Settings.SettingsItem.SubHeading -> {
                        SettingsSubHeading(item.text)
                    }
                    else -> {
                        // Handle other settings items if needed
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun SettingsScreenPreview() {
    OTOTPTheme {
        SettingsScreen(
            state = Settings.State(),
            onNavigateBack = {},
            onAction = {},
        )
    }
}