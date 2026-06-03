package io.legado.app.model.translation.providers

import io.legado.app.help.http.okHttpClient
import io.legado.app.model.translation.FieldType
import io.legado.app.model.translation.ProviderConfigData
import io.legado.app.model.translation.ProviderField
import io.legado.app.model.translation.TranslationProvider
import io.legado.app.utils.GSON

/**
 * 小牛翻译(GET + apikey)
 *
 * https://api.niutrans.com/NiuTransServer/translation
 */
object XiaoniuProvider : TranslationProvider {
    override val type = "xiaoniu"
    override val displayName = "小牛翻译"
    override val description = "小牛翻译提供的机器翻译服务,支持多种语言。"
    override val docUrl = "https://niutrans.com/documents"
    override val showUniversalFields = false

    override val fields: List<ProviderField> = listOf(
        ProviderField(
            key = "apiKey",
            label = "API Key",
            placeholder = "apiKey",
            type = FieldType.PASSWORD
        )
    )

    override suspend fun translate(
        config: ProviderConfigData,
        text: String,
        sourceLang: String,
        targetLang: String
    ): Result<String> = runCatching {
        if (text.isBlank()) return@runCatching text
        val apiKey = config.field("apiKey")
        require(apiKey.isNotBlank()) { "API Key 未填写" }
        val from = if (sourceLang.isBlank()) "auto" else sourceLang
        val to = targetLang
        val encoded = java.net.URLEncoder.encode(text, "UTF-8")
        val url = "https://api.niutrans.com/NiuTransServer/translation" +
            "?from=$from&to=$to&apikey=$apiKey&src_text=$encoded"
        val response = okHttpClient.newCallStrResponse { url(url) }
        if (!response.isSuccessful()) {
            throw RuntimeException("HTTP ${response.code()}: ${response.message()}")
        }
        val parsed = GSON.fromJson(response.body, Resp::class.java)
            ?: throw RuntimeException("Empty response")
        parsed.tgt_text?.takeIf { it.isNotEmpty() }
            ?: throw RuntimeException("Empty translation result")
    }

    data class Resp(val tgt_text: String?)
}
