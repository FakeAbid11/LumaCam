package com.lumacam.core.ui.components

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.lumacam.core.ui.theme.LumaGradient

/**
 * Renders a Material [ImageVector] tinted with a gradient brush, used for AI-active
 * icon states (analyze, pro, completed-step check, etc.).
 *
 * It draws the icon in white, then composites the [brush] through it using
 * [BlendMode.SrcIn] so only the icon's pixels take the gradient — the standard
 * Compose technique for gradient-tinted vector icons.
 */
@Composable
fun GradientIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    brush: Brush = LumaGradient.aiAccent
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = Color.White,
        modifier = modifier.drawWithContent {
            drawContent()
            drawRect(brush = brush, blendMode = BlendMode.SrcIn)
        }
    )
}
