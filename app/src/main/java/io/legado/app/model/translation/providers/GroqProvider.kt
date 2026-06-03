package io.legado.app.model.translation.providers

import io.legado.app.help.http.addHeaders
import io.legado.app.help.http.okHttpClient
import io.legado.app.help.http.postJson
import io.legado.app.model.translation.FieldType
import io.legado.app.model.translation.ProviderConfigData
import io.legado.app.model.translation.ProviderField
import io.legado.app.model.translation.TranslationProvider
import io.legado.app.utils.GSON

/**
 * Groq(OpenAI 兼容聊天完成接口)
 */
object GroqProvider : TranslationProvider {
    override val type = "groq"
    override val displayName = "Groq"
    override val description = "Groq 提供的 OpenAI 兼容翻译接口,响应速度快。"
    override val docUrl = "https://console.groq.com/docs/quickstart"

    override val fields: List<ProviderField> = listOf(
        ProviderField(key = "apiKey", label = "API Key", placeholder = "API Key", type = FieldType.PASSWORD),
        ProviderField(key = "model", label = "model", placeholder = "llama-3.1-70b-versatile", type = FieldType.TEXT,
            default = "llama-3.1-70b-versatile"),
        ProviderField(key = "baseUrl", label = "baseUrl", placeholder = "https://api.groq.com/openai/v1",
            type = FieldType.TEXT, required = false,
            default = "https://api.groq.com/openai/v1")
    )

    override suspend fun translate(
        config: ProviderConfigData,
        text: String,
        sourceLang: String,
        targetLang: String
    ): Result<String> = runCatching {
        if (text.isBlank()) return@runCatching text
        val apiKey = config.field("apiKey")
        val model = config.field("model").ifBlank { "llama-3.1-70b-versatile" }
        val baseUrl = config.field("baseUrl").ifBlank { "https://api.groq.com/openai/v1" }
        require(apiKey.isNotBlank()) { "API Key 未填写" }
        require(model.isNotBlank()) { "model 未填写" }
        val body = mapOf(
            "model" to model,
            "messages" to listOf(
                mapOf("role" to "system", "content" to "You are a translation assistant. " +
                    "Translate the following text into $targetLang and output only the translation."),
                mapOf("role" to "user", "content" to text)
            ),
            "temperature" to 0.3
        )
        val response = okHttpClient.newCallStrResponse {
            url(baseUrl.trimEnd('/') + "/chat/completions")
            addHeaders(mapOf("Authorization" to "Bearer $apiKey"))
            postJson(GSON.toJson(body))
        }
        if (!response.isSuccessful()) {
            throw RuntimeException("HTTP ${response.code()}: ${response.message()}")
        }
        val parsed = GSON.fromJson(response.body, Resp::class.java)
            ?: throw RuntimeException("Empty response")
        parsed.choices?.firstOrNull()?.message?.content?.trim()
            ?: throw RuntimeException("Empty translation result")
    }

    data class Resp(val choices: List<Choice>?)
    data class Choice(val message: Message?)
    data class Message(val content: String?)
}
