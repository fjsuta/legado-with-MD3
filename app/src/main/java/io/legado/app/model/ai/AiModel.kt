package io.legado.app.model.ai

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class AiModel(
    val id: String = UUID.randomUUID().toString(),
    val modelId: String,
    val displayName: String? = null,
    val contextWindow: Int? = null,
    val maxOutputTokens: Int? = null
) {
    fun displayNameOrId(): String = displayName ?: modelId
}

@Serializable
data class AiProvider(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val apiMode: ApiMode = ApiMode.OpenAI,
    val apiKey: String,
    val apiHost: String,
    val apiPath: String = "/chat/completions",
    val models: List<AiModel> = emptyList()
) {
    fun fullApiUrl(): String {
        val host = apiHost.removeSuffix("/")
        val path = apiPath.removePrefix("/")
        return "$host/$path"
    }
}

enum class ApiMode(val displayName: String) {
    OpenAI("OpenAI API 兼容"),
    OpenAIResponses("OpenAI Responses API 兼容"),
    Claude("Claude API 兼容"),
    Gemini("Google Gemini API 兼容")
}

enum class AiServiceMode(val displayName: String) {
    Builtin("内置模型"),
    Custom("自定义配置")
}

enum class AiSearchMode(val displayName: String) {
    Traditional("传统搜索"),
    Ai("AI 模式")
}