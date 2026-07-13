package com.lumacam.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lumacam.core.ui.theme.LumaColors

/**
 * Shared translucent "pill" chrome used across the camera HUD (recording badge,
 * AI-mode chip, preview-filter toggle, AE/AF locks). Centralizes the
 * semi-transparent black fill so every floating control on the preview reads as
 * the same material. [soft] uses a lighter fill for low-emphasis chips.
 */
@Composable
fun LumaPill(
    modifier: Modifier = Modifier,
    soft: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.background(
            if (soft) LumaColors.chromePillSoft else LumaColors.chromePill,
            RoundedCornerShape(50)
        ),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
