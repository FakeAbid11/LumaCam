package com.lumacam.app.ui.camera

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.lumacam.core.ui.theme.LumaAccent

private val RecordRed = Color(0xFFFF3B30)

/**
 * Premium shutter button (PRD: press feedback + capture flash/ring animation).
 * Pure UI — the caller decides what [onClick] does based on the capture mode.
 *
 * @param captureKey increment this on each successful capture to fire the ring
 *   pulse animation.
 * @param scoreProgress optional AI composition score (0f..1f); when non-null an
 *   accent ring fills proportionally around the button.
 * @param glow when true, the score ring gains an accent glow (fired at 100%).
 */
@Composable
fun ShutterButton(
    mode: CaptureMode,
    isRecording: Boolean,
    captureKey: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    scoreProgress: Float? = null,
    glow: Boolean = false,
    lowEndMode: Boolean = false
) {
    val animatedScore by animateFloatAsState(
        targetValue = scoreProgress?.coerceIn(0f, 1f) ?: 0f,
        animationSpec = tween(durationMillis = 700),
        label = "shutterScore"
    )
    // Glow + pulse are non-essential flourishes; suppressed on low-end devices.
    val glowAlpha by animateFloatAsState(
        targetValue = if (glow && !lowEndMode) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "shutterGlow"
    )
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(),
        label = "shutterScale"
    )

    val isVideo = mode == CaptureMode.VIDEO
    val innerColor by animateColorAsState(
        targetValue = if (isVideo) RecordRed else Color.White,
        label = "shutterInnerColor"
    )
    val innerCorner by animateDpAsState(
        targetValue = if (isRecording) 10.dp else 40.dp,
        label = "shutterInnerCorner"
    )
    val innerFraction by animateFloatAsState(
        targetValue = if (isRecording) 0.5f else 0.82f,
        label = "shutterInnerFraction"
    )

    val pulse = remember { Animatable(0f) }
    LaunchedEffect(captureKey) {
        if (captureKey > 0 && !lowEndMode) {
            pulse.snapTo(0f)
            pulse.animateTo(1f, animationSpec = tween(durationMillis = 450))
        }
    }

    Box(
        modifier = modifier
            .size(80.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            if (pulse.value in 0.001f..0.999f) {
                drawCircle(
                    color = Color.White.copy(alpha = 1f - pulse.value),
                    radius = radius + pulse.value * radius * 0.6f,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
            drawCircle(
                color = Color.White,
                radius = radius - 3.dp.toPx(),
                style = Stroke(width = 5.dp.toPx())
            )
            if (scoreProgress != null) {
                val ringStroke = 4.dp.toPx()
                val inset = 1.dp.toPx()
                val diameter = size.minDimension - inset * 2
                val topLeft = Offset(inset, inset)
                val arcSize = Size(diameter, diameter)
                if (glowAlpha > 0f) {
                    drawArc(
                        color = LumaAccent.copy(alpha = 0.4f * glowAlpha),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedScore,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = ringStroke * 3f, cap = StrokeCap.Round)
                    )
                }
                drawArc(
                    color = Color.White.copy(alpha = 0.18f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = ringStroke)
                )
                drawArc(
                    color = LumaAccent,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedScore,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = ringStroke, cap = StrokeCap.Round)
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize(innerFraction)
                .clip(RoundedCornerShape(innerCorner))
                .background(innerColor)
        )
    }
}
