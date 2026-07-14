package com.lumacam.app.ui.camera.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.lumacam.app.BuildConfig
import com.lumacam.core.ui.theme.LumaAccent
import com.lumacam.core.ui.theme.LumaGray300
import com.lumacam.core.ui.theme.LumaWhite
import com.lumacam.feature.ai.CompositionResult

/**
 * Content for the AI Assistant [com.lumacam.core.ui.components.LumaBottomSheet]:
 * scene type, composition score, lighting assessment, and coaching tips.
 */
@Composable
fun AiAssistantSheetContent(
    result: CompositionResult,
    rawResponse: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    result.sceneType.displayName,
                    color = LumaAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Composition ${result.compositionScore}%",
                    color = LumaWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        InfoBlock(title = result.lighting.label, body = result.lighting.description)

        if (result.suggestions.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Suggestions",
                    color = LumaGray300,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                result.suggestions.forEach { tip -> SuggestionRow(tip) }
            }
        }

        // On-device debugging aid: show the raw Local-AI model text so a 0% or
        // unexpected result can be diagnosed without logcat. Debug builds only.
        if (BuildConfig.DEBUG && !rawResponse.isNullOrBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Raw model output (debug)",
                    color = LumaGray300,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = rawResponse,
                    color = LumaWhite,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .verticalScroll(rememberScrollState())
                        .background(Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun InfoBlock(title: String, body: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0x22FFFFFF), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(title, color = LumaWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(body, color = LumaGray300, fontSize = 13.sp)
    }
}

@Composable
private fun SuggestionRow(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier.padding(top = 2.dp).size(20.dp).background(Color(0x333A6FF8), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = LumaAccent,
                modifier = Modifier.size(12.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(text, color = LumaWhite, fontSize = 14.sp)
    }
}
