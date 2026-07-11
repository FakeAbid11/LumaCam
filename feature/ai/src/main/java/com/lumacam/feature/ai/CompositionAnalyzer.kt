package com.lumacam.feature.ai

import kotlinx.coroutines.flow.Flow

/**
 * Source of AI composition guidance. Prompt 5 ships [MockCompositionAnalyzer];
 * Prompt 6/7 will bind a real on-device / Gemini implementation via Hilt without
 * touching the HUD presentation layer.
 */
interface CompositionAnalyzer {
    /**
     * Runs one analysis pass, emitting [AnalysisState.InProgress] for each stage
     * and finishing with a single [AnalysisState.Ready].
     */
    fun analyze(): Flow<AnalysisState>
}
