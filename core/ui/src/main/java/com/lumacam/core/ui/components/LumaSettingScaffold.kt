package com.lumacam.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lumacam.core.ui.theme.LumaBlack
import com.lumacam.core.ui.theme.LumaSpacing

/**
 * Shared screen frame for the settings-style screens (Settings, Cloud AI, Local
 * AI, Benchmark). Dark scaffold + standard [LumaTopAppBar], content centered and
 * capped at 600.dp, scrollable, with consistent 16.dp gaps. Removes the
 * near-identical Scaffold/TopAppBar/card boilerplate duplicated across screens.
 */
@Composable
fun LumaSettingScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        containerColor = LumaBlack,
        topBar = { LumaTopAppBar(title = title, onBack = onBack, actions = actions) }
    ) { inner ->
        BoxWithConstraints(
            modifier = modifier.fillMaxSize().padding(inner)
        ) {
            val listModifier = if (maxWidth > 600.dp) {
                Modifier.widthIn(max = 600.dp).fillMaxWidth()
            } else {
                Modifier.fillMaxWidth()
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = listModifier
                        .padding(horizontal = LumaSpacing.lg)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(LumaSpacing.lg)
                ) {
                    Spacer(Modifier.height(LumaSpacing.xs))
                    content()
                    Spacer(Modifier.height(LumaSpacing.xl))
                }
            }
        }
    }
}
