package com.example.ototp.settings

internal interface Settings {

    enum class ThemeMode {
        LIGHT, DARK, SYSTEM
    }

    data class State (
        val settingsItems: List<SettingsItem> = emptyList(),
        val themeMode: ThemeMode = ThemeMode.SYSTEM,
        val dynamicColorsEnabled: Boolean = true,
    )

    sealed interface Action {
        data class RequestSwitchableChange(
            val switchable: SettingsItem.SettingsSwitchableItem,
            val newValue: Boolean,
        ) : Action
        data class RequestThemeModeChange(val mode: ThemeMode) : Action
        data class RequestDynamicColorsChange(val enabled: Boolean) : Action
    }


    sealed class SettingsItem {

        internal data object Divider: SettingsItem()

        internal data class SubHeading(val text: String): SettingsItem()

        internal data class BiometricAuth(override val isEnabled: Boolean, val isAvailable: Boolean): SettingsItem(), SettingsSwitchableItem

        internal data class Passcode(override val isEnabled: Boolean): SettingsItem(), SettingsSwitchableItem

        internal data class DynamicColors(override val isEnabled: Boolean): SettingsItem(), SettingsSwitchableItem

        internal data class ThemeMode(val currentMode: Settings.ThemeMode): SettingsItem()

        internal sealed interface SettingsSwitchableItem {
            val isEnabled: Boolean
        }
    }
}