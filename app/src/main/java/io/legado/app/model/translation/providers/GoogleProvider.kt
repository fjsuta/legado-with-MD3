package io.legado.app.model.translation.providers

import io.legado.app.help.http.okHttpClient
import io.legado.app.model.translation.FieldType
import io.legado.app.model.translation.ProviderConfigData
import io.legado.app.model.translation.ProviderField
import io.legado.app.model.translation.TranslationProvider
import io.legado.app.utils.GSON

/**
 * Google Translate 免费接口(无需 key)
 */
object GoogleProvider : TranslationProvider {
    override val type = "google"
    override val displayName = "Google 翻译"
    override val description = "谷歌提供的免费翻译服务,支持多种语言。"
    override val docUrl = null
    override val fields: List<ProviderField> = emptyList()

    override suspend fun translate(
        config: ProviderConfigData,
        text: String,
        targetLang: String
    ): Result<String> = runCatching {
        if (text.isBlank()) return@runCatching text
        val encoded = java.net.URLEncoder.encode(text, "UTF-8")
        val url = "https://translate.googleapis.com/translate_a/single" +
            "?client=gtx&sl=auto&tl=$targetLang&dj=1&dt=t&ie=UTF-8&q=$encoded"
        val response = okHttpClient.newCallStrResponse { url(url) }
        if (!response.isSuccessful()) {
            throw RuntimeException("HTTP ${response.code()}: ${response.message()}")
        }
        val parsed = GSON.fromJson(response.body, GoogleResp::class.java)
            ?: throw RuntimeException("Empty response")
        val translated = parsed.sentences?.mapNotNull { it.trans }?.joinToString("").orEmpty()
        if (translated.isEmpty()) throw RuntimeException("Empty translation result")
        translated
    }

    data class GoogleResp(
        val sentences: List<GoogleSentence>?
    )
    data class GoogleSentence(
        val trans: String?
    )
}
