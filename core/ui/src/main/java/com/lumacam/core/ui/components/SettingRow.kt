package com.lumacam.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.lumacam.core.ui.theme.LumaAccent
import com.lumacam.core.ui.theme.LumaGray500
import com.lumacam.core.ui.theme.LumaSpacing
import com.lumacam.core.ui.theme.LumaWhite

/**
 * A single settings list row: leading icon + title/subtitle, with an optional
 * [trailing] slot (e.g. a status chip) and optional [onClick] for navigation.
 * [accent] tints the icon with the reserved AI accent.
 */
@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    accent: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (accent) LumaAccent else LumaWhite
        )
        Column(
            Modifier
                .padding(start = LumaSpacing.md)
                .weight(1f)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = LumaWhite)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LumaGray500
                )
            }
        }
        trailing?.invoke()
    }
}

/**
 * A settings row with a trailing [Switch] (e.g. "Visual effects").
 */
@Composable
fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = LumaWhite)
        Column(
            Modifier
                .padding(start = LumaSpacing.md)
                .weight(1f)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = LumaWhite)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LumaGray500
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = LumaAccent,
                checkedTrackColor = LumaAccent.copy(alpha = 0.5f)
            )
        )
    }
}
