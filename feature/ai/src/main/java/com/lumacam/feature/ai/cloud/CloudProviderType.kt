package com.lumacam.feature.ai.cloud

/**
 * The Cloud AI backends LumaCam can talk to (PRD §4 Tier 4). Every provider
 * normalizes its response into the shared [com.lumacam.feature.ai.CompositionResult],
 * so the rest of the app never depends on a specific vendor.
 *
 * All types except [GEMINI] speak the OpenAI-compatible `/chat/completions` shape;
 * [GEMINI] uses Google's `generateContent` shape.
 *
 * @param displayName human-readable label for Settings.
 * @param defaultBaseUrl the API root; empty when the user must supply one.
 * @param defaultModel a sensible vision-capable default; empty when user-chosen.
 * @param requiresBaseUrl true when the user must enter a base URL themselves.
 * @param requiresModel true when the user must enter a model id themselves.
 */
enum class CloudProviderType(
    val displayName: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val requiresBaseUrl: Boolean = false,
    val requiresModel: Boolean = false
) {
    GEMINI(
        displayName = "Google AI Studio",
        defaultBaseUrl = "https://generativelanguage.googleapis.com",
        defaultModel = "gemini-2.0-flash"
    ),
    OPENAI(
        displayName = "OpenAI",
        defaultBaseUrl = "https://api.openai.com",
        defaultModel = "gpt-4o-mini"
    ),
    QWEN(
        displayName = "Qwen VL (DashScope)",
        defaultBaseUrl = "https://dashscope-intl.aliyuncs.com/compatible-mode",
        defaultModel = "qwen-vl-plus"
    ),
    HUNYUAN(
        displayName = "Hunyuan Vision",
        defaultBaseUrl = "https://api.hunyuan.cloud.tencent.com",
        defaultModel = "hunyuan-vision"
    ),
    OPENROUTER(
        displayName = "OpenRouter",
        defaultBaseUrl = "https://openrouter.ai/api",
        defaultModel = "",
        requiresModel = true
    ),
    CUSTOM(
        displayName = "Custom (OpenAI-compatible)",
        defaultBaseUrl = "",
        defaultModel = "",
        requiresBaseUrl = true,
        requiresModel = true
    );

    /** True for every provider that uses the OpenAI chat-completions wire format. */
    val isOpenAiCompatible: Boolean get() = this != GEMINI
}
