package com.lumacam.app.ui.camera

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class CaptureMode { PHOTO, VIDEO }

private val SegmentWidth = 68.dp
private val SegmentHeight = 32.dp

/**
 * Animated Photo / Video segmented switcher (PRD: smooth transition between
 * photo and video). Neutral chrome — no accent, since the mode isn't AI.
 */
@Composable
fun ModeSwitcher(
    mode: CaptureMode,
    onModeChange: (CaptureMode) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val indicatorOffset by animateDpAsState(
        targetValue = if (mode == CaptureMode.PHOTO) 0.dp else SegmentWidth,
        animationSpec = spring(),
        label = "modeIndicatorOffset"
    )

    Box(
        modifier = modifier
            .background(Color(0x66000000), RoundedCornerShape(50))
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(start = indicatorOffset)
                .width(SegmentWidth)
                .height(SegmentHeight)
                .background(Color(0x33FFFFFF), RoundedCornerShape(50))
        )
        Row {
            ModeSegment("Photo", mode == CaptureMode.PHOTO, enabled) {
                onModeChange(CaptureMode.PHOTO)
            }
            ModeSegment("Video", mode == CaptureMode.VIDEO, enabled) {
                onModeChange(CaptureMode.VIDEO)
            }
        }
    }
}

@Composable
private fun ModeSegment(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else Color(0xFFB0B0B8),
        label = "modeSegmentColor"
    )
    Box(
        modifier = Modifier
            .width(SegmentWidth)
            .height(SegmentHeight)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}
