package com.lumacam.core.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lumacam.core.ui.theme.LumaGray500
import com.lumacam.core.ui.theme.LumaSpacing

/**
 * Uppercase section label used to group settings rows (e.g. "AI", "General").
 */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = LumaGray500,
        modifier = modifier
            .padding(
                start = LumaSpacing.sm,
                top = LumaSpacing.md,
                bottom = LumaSpacing.xs
            )
    )
}
