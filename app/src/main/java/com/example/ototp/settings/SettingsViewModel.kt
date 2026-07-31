package com.example.ototp.settings

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

internal class SettingsViewModel(context: Context): ViewModel() {
    private val themePreferences = ThemePreferences(context)

    private val _state = MutableStateFlow(Settings.State())
    val state: StateFlow<Settings.State> = _state

    init {
        viewModelScope.launch {
            themePreferences.themeMode.combine(themePreferences.dynamicColorsEnabled) { themeMode, dynamicColors ->
                Settings.State(
                    settingsItems = buildSettingsItems(themeMode, dynamicColors),
                    themeMode = themeMode,
                    dynamicColorsEnabled = dynamicColors
                )
            }.collect { newState ->
                _state.value = newState
            }
        }
    }

    fun handleAction(action: Settings.Action) {
        viewModelScope.launch {
            when (action) {
                is Settings.Action.RequestThemeModeChange -> {
                    themePreferences.setThemeMode(action.mode)
                }
                is Settings.Action.RequestDynamicColorsChange -> {
                    themePreferences.setDynamicColorsEnabled(action.enabled)
                }
                is Settings.Action.RequestSwitchableChange -> {
                    // Handle other switchable changes if needed
                }
            }
        }
    }

    private fun buildSettingsItems(themeMode: Settings.ThemeMode, dynamicColorsEnabled: Boolean): List<Settings.SettingsItem> {
        val items = mutableListOf<Settings.SettingsItem>()

        items.add(Settings.SettingsItem.SubHeading("Appearance"))

        // Add theme mode selector
        items.add(Settings.SettingsItem.ThemeMode(themeMode))

        // Add dynamic colors toggle only if available (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            items.add(Settings.SettingsItem.DynamicColors(dynamicColorsEnabled))
        }

        return items
    }
}