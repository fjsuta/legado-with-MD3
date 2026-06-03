package io.legado.app.model.translation.providers

import io.legado.app.help.http.addHeaders
import io.legado.app.help.http.okHttpClient
import io.legado.app.help.http.postJson
import io.legado.app.model.translation.FieldType
import io.legado.app.model.translation.LanguageCodes
import io.legado.app.model.translation.ProviderConfigData
import io.legado.app.model.translation.ProviderField
import io.legado.app.model.translation.TranslationProvider
import io.legado.app.utils.GSON

/**
 * Azure 机器翻译
 *
 * 默认端点:https://api.cognitive.microsofttranslator.com/
 */
object AzureProvider : TranslationProvider {
    override val type = "azure"
    override val displayName = "Azure 机器翻译"
    override val description = "微软 Azure 提供的翻译 API,支持多种语言。"
    override val docUrl = "https://learn.microsoft.com/azure/ai-services/translator/"

    override val fields: List<ProviderField> = listOf(
        ProviderField(key = "region", label = "region", placeholder = "eastasia", type = FieldType.TEXT),
        ProviderField(key = "apiKey", label = "API Key", placeholder = "API Key", type = FieldType.PASSWORD),
        ProviderField(
            key = "baseUrl",
            label = "自定义 API 接口地址",
            placeholder = "https://api.cognitive.microsofttranslator.com/",
            type = FieldType.TEXT,
            required = false,
            default = "https://api.cognitive.microsofttranslator.com/"
        ),
        ProviderField(
            key = "enableRichText",
            label = "启用富文本翻译",
            placeholder = "",
            type = FieldType.TOGGLE,
            required = false,
            default = "false"
        )
    )

    override suspend fun translate(
        config: ProviderConfigData,
        text: String,
        sourceLang: String,
        targetLang: String
    ): Result<String> = runCatching {
        if (text.isBlank()) return@runCatching text
        val region = config.field("region")
        val apiKey = config.field("apiKey")
        val baseUrl = config.field("baseUrl")
            .ifBlank { "https://api.cognitive.microsofttranslator.com/" }
            .trimEnd('/')
        require(region.isNotBlank()) { "region 未填写" }
        require(apiKey.isNotBlank()) { "API Key 未填写" }
        val from = if (sourceLang.isBlank()) "" else LanguageCodes.toAzure(sourceLang)
        val to = LanguageCodes.toAzure(targetLang)
        val url = buildString {
            append(baseUrl).append("/translate?api-version=3.0&to=$to")
            if (from.isNotEmpty()) append("&from=$from")
        }
        val textValue = if (config.field("enableRichText") == "true") {
            // 富文本模式:Azure 默认就传 <>,= 等字符,无需额外处理
            text
        } else text
        val body = listOf(mapOf("Text" to textValue))
        val response = okHttpClient.newCallStrResponse {
            url(url)
            addHeaders(mapOf(
                "Ocp-Apim-Subscription-Key" to apiKey,
                "Ocp-Apim-Subscription-Region" to region,
                "Content-Type" to "application/json"
            ))
            postJson(GSON.toJson(body))
        }
        if (!response.isSuccessful()) {
            throw RuntimeException("HTTP ${response.code()}: ${response.message()}")
        }
        val parsed = GSON.fromJson(response.body, Array<TransResp>::class.java)
            ?: throw RuntimeException("Empty response")
        parsed.firstOrNull()?.translations?.firstOrNull()?.text
            ?: throw RuntimeException("Empty translation result")
    }

    data class TransResp(val translations: List<TransText>?)
    data class TransText(val text: String?)
}
