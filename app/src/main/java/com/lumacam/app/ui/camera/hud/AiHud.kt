package com.lumacam.app.ui.camera.hud

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumacam.core.ui.theme.LumaAccent
import com.lumacam.feature.ai.AnalysisStage
import com.lumacam.feature.ai.CropBounds
import com.lumacam.feature.ai.MoveDirection
import kotlin.math.roundToInt

private val LevelColor = LumaAccent
private val OffLevelColor = Color(0xCCFFFFFF)
private val GridColor = Color(0x40FFFFFF)

/**
 * Rule-of-thirds grid. User-toggleable framing aid (not AI-driven), so it stays
 * neutral white per the palette rules.
 */
@Composable
fun CompositionGridOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val stroke = 1.dp.toPx()
        for (i in 1..2) {
            val x = w * i / 3f
            drawLine(GridColor, Offset(x, 0f), Offset(x, h), strokeWidth = stroke)
            val y = h * i / 3f
            drawLine(GridColor, Offset(0f, y), Offset(w, y), strokeWidth = stroke)
        }
    }
}

/**
 * Artificial horizon: a center line tilted by [tiltAngle] degrees. Turns accent
 * blue (AI "good" state) once the frame is level; semi-transparent white while
 * off-level.
 */
@Composable
fun HorizonOverlay(
    tiltAngle: Float,
    isLevel: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (isLevel) LevelColor else OffLevelColor
    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val half = size.width * 0.22f
        val gap = size.width * 0.05f
        val stroke = 2.dp.toPx()
        rotate(degrees = tiltAngle, pivot = Offset(cx, cy)) {
            drawLine(color, Offset(cx - half, cy), Offset(cx - gap, cy), strokeWidth = stroke)
            drawLine(color, Offset(cx + gap, cy), Offset(cx + half, cy), strokeWidth = stroke)
        }
        drawCircle(color, radius = 3.dp.toPx(), center = Offset(cx, cy))
    }
}

/**
 * Ghost-crop suggestion: dims the area outside the recommended [bounds] and
 * outlines it with a dashed accent rectangle.
 */
@Composable
fun GhostCropOverlay(bounds: CropBounds, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val rect = Rect(
            left = bounds.left * size.width,
            top = bounds.top * size.height,
            right = bounds.right * size.width,
            bottom = bounds.bottom * size.height
        )
        val dim = Color(0x66000000)
        drawRect(dim, topLeft = Offset.Zero, size = Size(size.width, rect.top))
        drawRect(dim, topLeft = Offset(0f, rect.bottom), size = Size(size.width, size.height - rect.bottom))
        drawRect(dim, topLeft = Offset(0f, rect.top), size = Size(rect.left, rect.height))
        drawRect(dim, topLeft = Offset(rect.right, rect.top), size = Size(size.width - rect.right, rect.height))
        drawRect(
            color = LumaAccent,
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width, rect.height),
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
            )
        )
    }
}

/**
 * Pulsing accent arrow nudging the user toward [direction]. Hidden on
 * [MoveDirection.NONE].
 */
@Composable
fun DirectionalArrowOverlay(direction: MoveDirection, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = direction != MoveDirection.NONE,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val transition = rememberInfiniteTransition(label = "arrow")
        val shift by transition.animateFloat(
            initialValue = 0f,
            targetValue = 12f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "arrowShift"
        )
        val icon: ImageVector = when (direction) {
            MoveDirection.UP -> Icons.Filled.ArrowUpward
            MoveDirection.DOWN -> Icons.Filled.ArrowDownward
            MoveDirection.LEFT -> Icons.Filled.ArrowBack
            MoveDirection.RIGHT -> Icons.Filled.ArrowForward
            MoveDirection.NONE -> Icons.Filled.ArrowUpward
        }
        val alignment = when (direction) {
            MoveDirection.UP -> Alignment.TopCenter
            MoveDirection.DOWN -> Alignment.BottomCenter
            MoveDirection.LEFT -> Alignment.CenterStart
            MoveDirection.RIGHT -> Alignment.CenterEnd
            MoveDirection.NONE -> Alignment.Center
        }
        Box(Modifier.fillMaxSize().padding(48.dp), contentAlignment = alignment) {
            val (dx, dy) = when (direction) {
                MoveDirection.UP -> 0f to -shift
                MoveDirection.DOWN -> 0f to shift
                MoveDirection.LEFT -> -shift to 0f
                MoveDirection.RIGHT -> shift to 0f
                MoveDirection.NONE -> 0f to 0f
            }
            Box(
                Modifier
                    .offset { IntOffset(dx.roundToInt(), dy.roundToInt()) }
                    .size(52.dp)
                    .background(Color(0x55000000), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = "Move", tint = LumaAccent, modifier = Modifier.size(30.dp))
            }
        }
    }
}

/**
 * Step-by-step "analyzing" card shown while the AI works. Highlights the
 * [current] stage and checks off completed ones.
 */
@Composable
fun AnalyzingOverlay(current: AnalysisStage, modifier: Modifier = Modifier) {
    val stages = listOf(
        AnalysisStage.DETECTING_SCENE,
        AnalysisStage.FINDING_SUBJECT,
        AnalysisStage.BUILDING_COMPOSITION
    )
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .background(Color(0xE6121218), RoundedCornerShape(20.dp))
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Analyzing scene",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            stages.forEach { stage ->
                val done = stage.ordinal < current.ordinal
                val active = stage == current
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                        when {
                            done -> Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = LumaAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            active -> CircularProgressIndicator(
                                color = LumaAccent,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            else -> Box(
                                Modifier.size(8.dp).background(Color(0x55FFFFFF), CircleShape)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stage.label,
                        color = if (active || done) Color.White else Color(0x88FFFFFF),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
