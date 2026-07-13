package com.lumacam.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import com.lumacam.core.ui.theme.LumaAccent
import com.lumacam.core.ui.theme.LumaAccentMuted
import com.lumacam.core.ui.theme.LumaGray300
import com.lumacam.core.ui.theme.LumaGray500
import com.lumacam.core.ui.theme.LumaShapes
import com.lumacam.core.ui.theme.LumaSpacing
import com.lumacam.core.ui.theme.LumaSurfaceElevated
import com.lumacam.core.ui.theme.LumaWhite

enum class InfoBannerTone { Info, Accent, Warning, Error }

/**
 * Consistent informational / notice banner. [subtle] renders plain helper text
 * (for optional tips) without a card; otherwise a hairline-bordered card with a
 * tone-tinted icon draws attention (e.g. "No on-device model yet").
 */
@Composable
fun InfoBanner(
    text: String,
    modifier: Modifier = Modifier,
    tone: InfoBannerTone = InfoBannerTone.Info,
    icon: ImageVector? = null,
    subtle: Boolean = false
) {
    val tint = when (tone) {
        InfoBannerTone.Info -> LumaGray300
        InfoBannerTone.Accent -> LumaAccent
        InfoBannerTone.Warning -> Color(0xFFFFD60A)
        InfoBannerTone.Error -> MaterialTheme.colorScheme.error
    }
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth().padding(LumaSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(it, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(LumaSpacing.sm))
            }
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (subtle) LumaGray500 else LumaWhite
            )
        }
    }
    if (subtle) {
        content()
    } else {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (tone == InfoBannerTone.Accent) LumaAccentMuted else LumaSurfaceElevated
            ),
            shape = LumaShapes.medium,
            border = BorderStroke(1.dp, tint.copy(alpha = 0.4f)),
            modifier = modifier.fillMaxWidth()
        ) { content() }
    }
}
