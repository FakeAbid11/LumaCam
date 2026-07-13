package com.lumacam.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lumacam.core.ui.theme.LumaGray700
import com.lumacam.core.ui.theme.LumaShapes
import com.lumacam.core.ui.theme.LumaSpacing
import com.lumacam.core.ui.theme.LumaSurfaceElevated

/**
 * Standard surface card for settings content. Uses an elevated surface with a
 * subtle hairline border so cards read as distinct panels on the black chrome
 * (previously cards used a black container and were invisible).
 */
@Composable
fun LumaSettingCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = LumaSurfaceElevated),
        shape = LumaShapes.large,
        border = BorderStroke(1.dp, LumaGray700),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LumaSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(LumaSpacing.md),
            content = content
        )
    }
}
