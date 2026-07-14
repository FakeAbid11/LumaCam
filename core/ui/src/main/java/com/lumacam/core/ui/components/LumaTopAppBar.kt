package com.lumacam.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lumacam.core.ui.theme.LumaBlack
import com.lumacam.core.ui.theme.LumaWhite

/**
 * Standard dark LumaCam app bar used by every screen. Keeps the back affordance
 * and content color consistent so the chrome reads the same app-wide.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LumaTopAppBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showBack: Boolean = true,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {}
) {
    val backIcon: @Composable (() -> Unit)? = if (showBack) {
        @Composable {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = LumaWhite)
            }
        }
    } else null
    TopAppBar(
        title = { Text(title) },
        navigationIcon = backIcon,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = LumaBlack,
            titleContentColor = LumaWhite,
            navigationIconContentColor = LumaWhite
        ),
        modifier = modifier
    )
}
