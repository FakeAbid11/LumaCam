package com.lumacam.app.ui.camera.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumacam.core.ui.theme.LumaAccent
import com.lumacam.feature.ai.CompositionResult

/**
 * Content for the AI Assistant [com.lumacam.core.ui.components.LumaBottomSheet]:
 * scene type, composition score, lighting assessment, and coaching tips.
 */
@Composable
fun AiAssistantSheetContent(result: CompositionResult, modifier: Modifier = Modifier) {
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
                    color = androidx.compose.ui.graphics.Color.White,
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
                    color = androidx.compose.ui.graphics.Color(0xFFC7C7D1),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                result.suggestions.forEach { tip -> SuggestionRow(tip) }
            }
        }
    }
}

@Composable
private fun InfoBlock(title: String, body: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(androidx.compose.ui.graphics.Color(0x22FFFFFF), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(title, color = androidx.compose.ui.graphics.Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(body, color = androidx.compose.ui.graphics.Color(0xFFC7C7D1), fontSize = 13.sp)
    }
}

@Composable
private fun SuggestionRow(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier.padding(top = 2.dp).size(20.dp).background(androidx.compose.ui.graphics.Color(0x333A6FF8), CircleShape),
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
        Text(text, color = androidx.compose.ui.graphics.Color.White, fontSize = 14.sp)
    }
}
