package com.lumacam.app.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumacam.core.camera.CameraCapabilities
import com.lumacam.core.camera.LensInfo
import com.lumacam.core.camera.ManualCameraState
import com.lumacam.core.camera.WhiteBalanceMode
import com.lumacam.core.camera.defaultExposureTimeNanos
import com.lumacam.core.camera.defaultIso
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProControlsPanel(
    capabilities: CameraCapabilities,
    manualState: ManualCameraState,
    lenses: List<LensInfo>,
    onIso: (Int?) -> Unit,
    onExposureTime: (Long?) -> Unit,
    onExposureCompensation: (Int) -> Unit,
    onWhiteBalance: (WhiteBalanceMode) -> Unit,
    onFocusDistance: (Float?) -> Unit,
    onExposureLock: (Boolean) -> Unit,
    onFocusLock: (Boolean) -> Unit,
    onHdr: (Boolean) -> Unit,
    onSelectLens: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 320.dp)
            .background(Color(0xE6101010), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (lenses.size > 1) {
            SectionLabel("Lens")
            ChipRow {
                lenses.forEach { lens ->
                    val selected = manualState.selectedLensId == lens.id ||
                        (manualState.selectedLensId == null && lens == lenses.first())
                    ProChip(lens.label, selected) { onSelectLens(lens.id) }
                }
            }
        }

        if (capabilities.supportsExposureCompensation &&
            capabilities.exposureCompensationRange.first != capabilities.exposureCompensationRange.last
        ) {
            val range = capabilities.exposureCompensationRange
            val ev = manualState.exposureCompensation * capabilities.exposureCompensationStep
            SectionLabel("Exposure (EV)  ${"%+.1f".format(ev)}")
            Slider(
                value = manualState.exposureCompensation.toFloat(),
                onValueChange = { onExposureCompensation(it.roundToInt()) },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                steps = (range.last - range.first - 1).coerceAtLeast(0)
            )
        }

        if (capabilities.supportsManualSensor) {
            val isoRange = capabilities.isoRange
            SectionLabel(
                "ISO  " + if (manualState.isoValue != null) "${manualState.isoValue}" else "Auto"
            )
            ChipRow {
                ProChip("Auto", !manualState.isManualExposure) {
                    onIso(null)
                    onExposureTime(null)
                }
            }
            if (isoRange != null) {
                Slider(
                    value = (manualState.isoValue ?: defaultIso(isoRange)).toFloat(),
                    onValueChange = { onIso(it.roundToInt()) },
                    valueRange = isoRange.first.toFloat()..isoRange.last.toFloat()
                )
            }
            val expRange = capabilities.exposureTimeRange
            if (expRange != null) {
                val current = manualState.exposureTimeNanos ?: defaultExposureTimeNanos(expRange)
                SectionLabel("Shutter  ${formatShutter(current)}")
                Slider(
                    value = shutterToFraction(current, expRange),
                    onValueChange = { onExposureTime(fractionToShutter(it, expRange)) },
                    valueRange = 0f..1f
                )
            }
        }

        if (capabilities.supportedWhiteBalance.size > 1) {
            SectionLabel("White balance")
            ChipRow {
                capabilities.supportedWhiteBalance.forEach { mode ->
                    ProChip(mode.label, manualState.whiteBalance == mode) { onWhiteBalance(mode) }
                }
            }
        }

        if (capabilities.supportsManualFocus) {
            val minDist = capabilities.minFocusDistance ?: 0f
            val focus = manualState.focusDistance
            SectionLabel(
                "Focus  " + if (focus != null) formatFocus(focus) else "Auto"
            )
            ChipRow {
                ProChip("Auto", !manualState.isManualFocus) { onFocusDistance(null) }
            }
            if (minDist > 0f) {
                Slider(
                    value = manualState.focusDistance ?: 0f,
                    onValueChange = { onFocusDistance(it) },
                    valueRange = 0f..minDist
                )
            }
        }

        if (capabilities.supportsExposureLock || capabilities.supportsManualFocus) {
            SectionLabel("Locks")
            ChipRow {
                if (capabilities.supportsExposureLock) {
                    ProChip("AE Lock", manualState.exposureLocked) {
                        onExposureLock(!manualState.exposureLocked)
                    }
                }
                ProChip("AF Lock", manualState.focusLocked) {
                    onFocusLock(!manualState.focusLocked)
                }
            }
        }

        if (capabilities.hdrSupported) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel("HDR")
                Switch(
                    checked = manualState.hdrEnabled,
                    onCheckedChange = { onHdr(it) }
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            labelColor = Color.White,
            selectedContainerColor = Color(0xFF3A6FF8),
            selectedLabelColor = Color.White
        )
    )
}

private fun formatShutter(nanos: Long): String {
    val seconds = nanos / 1_000_000_000.0
    if (seconds <= 0.0) return "--"
    return if (seconds >= 1.0) "%.1fs".format(seconds) else "1/${(1.0 / seconds).roundToInt()}s"
}

private fun formatFocus(diopters: Float): String {
    if (diopters <= 0f) return "∞"
    val meters = 1f / diopters
    return if (meters >= 1f) "%.1fm".format(meters) else "${(meters * 100).roundToInt()}cm"
}

private fun shutterToFraction(nanos: Long, range: LongRange): Float {
    val lnMin = ln(range.first.coerceAtLeast(1).toDouble())
    val lnMax = ln(range.last.coerceAtLeast(1).toDouble())
    if (lnMax <= lnMin) return 0f
    return ((ln(nanos.coerceAtLeast(1).toDouble()) - lnMin) / (lnMax - lnMin))
        .toFloat().coerceIn(0f, 1f)
}

private fun fractionToShutter(fraction: Float, range: LongRange): Long {
    val lnMin = ln(range.first.coerceAtLeast(1).toDouble())
    val lnMax = ln(range.last.coerceAtLeast(1).toDouble())
    return exp(lnMin + (lnMax - lnMin) * fraction).toLong().coerceIn(range.first, range.last)
}
