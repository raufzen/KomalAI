package com.komal.ai.gemini

import com.google.gson.annotations.SerializedName

/**
 * Model description returned from GET /v1beta/models
 */
data class GeminiModelsListResponse(
    @SerializedName("models") val models: List<GeminiModelItem>?
)

data class GeminiModelItem(
    @SerializedName("name") val name: String, // e.g. "models/gemini-1.5-flash"
    @SerializedName("version") val version: String?,
    @SerializedName("displayName") val displayName: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("inputTokenLimit") val inputTokenLimit: Long?,
    @SerializedName("outputTokenLimit") val outputTokenLimit: Long?,
    @SerializedName("supportedGenerationMethods") val supportedGenerationMethods: List<String>?
) {
    val cleanId: String
        get() = name.removePrefix("models/")

    val isChatCapable: Boolean
        get() = supportedGenerationMethods?.contains("generateContent") == true
}

/**
 * Request & Response payload for Gemini generateContent
 */
data class GeminiGenerateRequest(
    @SerializedName("contents") val contents: List<GeminiContent>,
    @SerializedName("systemInstruction") val systemInstruction: GeminiContent? = null,
    @SerializedName("tools") val tools: List<GeminiToolConfig>? = null,
    @SerializedName("generationConfig") val generationConfig: GeminiGenerationConfig? = null
)

data class GeminiContent(
    @SerializedName("role") val role: String? = "user",
    @SerializedName("parts") val parts: List<GeminiPart>
)

data class GeminiPart(
    @SerializedName("text") val text: String? = null,
    @SerializedName("functionCall") val functionCall: GeminiFunctionCall? = null,
    @SerializedName("functionResponse") val functionResponse: GeminiFunctionResponse? = null
)

data class GeminiFunctionCall(
    @SerializedName("name") val name: String,
    @SerializedName("args") val args: Map<String, Any?>?
)

data class GeminiFunctionResponse(
    @SerializedName("name") val name: String,
    @SerializedName("response") val response: Map<String, Any?>
)

data class GeminiToolConfig(
    @SerializedName("functionDeclarations") val functionDeclarations: List<GeminiFunctionDeclaration>
)

data class GeminiFunctionDeclaration(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("parameters") val parameters: Map<String, Any?>
)

data class GeminiGenerationConfig(
    @SerializedName("temperature") val temperature: Float? = 0.2f,
    @SerializedName("topP") val topP: Float? = 0.95f,
    @SerializedName("maxOutputTokens") val maxOutputTokens: Int? = 1024
)

data class GeminiGenerateResponse(
    @SerializedName("candidates") val candidates: List<GeminiCandidate>?,
    @SerializedName("promptFeedback") val promptFeedback: Any?
)

data class GeminiCandidate(
    @SerializedName("content") val content: GeminiContent?,
    @SerializedName("finishReason") val finishReason: String?
)
