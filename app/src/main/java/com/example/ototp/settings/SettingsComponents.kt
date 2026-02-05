package com.example.ototp.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ototp.ui.theme.OTOTPTheme

@Composable
private fun SettingsItem(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    summary: String? = null,
    showToggle: Boolean = false,
    toggleChecked: Boolean = false,
    onToggleChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null && !showToggle) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    Row(
        modifier = clickableModifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (showToggle && onToggleChange != null) {
            Switch(
                checked = toggleChecked,
                onCheckedChange = onToggleChange
            )
        }
    }
}

@Composable
fun SettingsToggleItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    summary: String? = null
) {
    SettingsItem(
        title = title,
        modifier = modifier,
        icon = icon,
        summary = summary,
        showToggle = true,
        toggleChecked = checked,
        onToggleChange = onCheckedChange
    )
}

@Composable
fun SettingsClickableItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    summary: String? = null
) {
    SettingsItem(
        title = title,
        modifier = modifier,
        icon = icon,
        summary = summary,
        onClick = onClick
    )
}

@Composable
internal fun SettingsThemeModeSelector(
    currentMode: Settings.ThemeMode,
    onModeSelected: (Settings.ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = true }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Brightness4,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Theme Mode",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = currentMode.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Settings.ThemeMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    onClick = {
                        onModeSelected(mode)
                        isExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsSubHeading(
    text: String,
    paddingValues: PaddingValues = PaddingValues(start = 56.dp, top = 8.dp, bottom = 4.dp, end = 16.dp)
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(paddingValues)
    )
}

@Preview(showBackground = true)
@Composable
fun SettingsComponentsPreview() {
    OTOTPTheme {
        Surface {
            Column {
                var toggle1 by remember { mutableStateOf(true) }
                var toggle2 by remember { mutableStateOf(false) }
                var themeMode by remember { mutableStateOf(Settings.ThemeMode.SYSTEM) }
                var dynamicColors by remember { mutableStateOf(true) }

                SettingsSubHeading("Super cool Settings")

                SettingsToggleItem(
                    title = "Enable Notifications",
                    summary = "Receive alerts for important updates",
                    icon = Icons.Default.Notifications,
                    checked = toggle1,
                    onCheckedChange = { toggle1 = it }
                )

                SettingsThemeModeSelector(
                    currentMode = themeMode,
                    onModeSelected = { themeMode = it }
                )

                SettingsToggleItem(
                    title = "Dynamic Colors",
                    summary = "Use system wallpaper colors",
                    icon = Icons.Default.Palette,
                    checked = dynamicColors,
                    onCheckedChange = { dynamicColors = it }
                )

                SettingsToggleItem(
                    title = "Security",
                    checked = toggle2,
                    onCheckedChange = { toggle2 = it }
                )

                SettingsClickableItem(
                    title = "Advanced Settings",
                    summary = "Configure advanced options",
                    icon = Icons.Default.Settings,
                    onClick = {}
                )

                SettingsClickableItem(
                    title = "About",
                    onClick = {}
                )

                SettingsItem(
                    title = "Simple Item",
                    icon = Icons.Default.Security,
                    summary = "This is a basic settings item"
                )

                SettingsItem(
                    title = "Title Only"
                )
            }
        }
    }
}
