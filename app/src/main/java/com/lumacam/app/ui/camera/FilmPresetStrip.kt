package com.lumacam.app.ui.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumacam.core.common.film.FilmPreset

/**
 * Swipeable strip of film-simulation presets shown above the shutter. Each chip
 * previews the preset's color grade on a fixed reference gradient (via Compose
 * [ColorMatrix]); the live camera shows the full effect. Selecting a chip switches
 * the active preset immediately.
 */
@Composable
fun FilmPresetStrip(
    presets: List<FilmPreset>,
    selected: FilmPreset,
    onSelect: (FilmPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(presets, key = { it.id }) { preset ->
            PresetChip(
                preset = preset,
                selected = preset.id == selected.id,
                onClick = { onSelect(preset) }
            )
        }
    }
}

@Composable
private fun PresetChip(preset: FilmPreset, selected: Boolean, onClick: () -> Unit) {
    val colorFilter = remember(preset.id) {
        ColorFilter.colorMatrix(ColorMatrix(preset.colorMatrix.copyOf()))
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Canvas(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) Color.White else Color(0x55FFFFFF),
                    shape = RoundedCornerShape(10.dp)
                )
        ) {
            drawRect(brush = REFERENCE_GRADIENT, colorFilter = colorFilter)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = preset.name,
            color = if (selected) Color.White else Color(0xCCFFFFFF),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

private val REFERENCE_GRADIENT: Brush = Brush.linearGradient(
    colors = listOf(
        Color(0xFF2B4A6F),
        Color(0xFF7A7A7A),
        Color(0xFFE0B489),
        Color(0xFFF2F0EC)
    )
)
