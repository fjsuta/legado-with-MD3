package io.legado.app.model.translation.providers

import io.legado.app.help.http.addHeaders
import io.legado.app.help.http.newCallStrResponse
import io.legado.app.help.http.okHttpClient
import io.legado.app.help.http.postJson
import io.legado.app.model.translation.FieldType
import io.legado.app.model.translation.LanguageCodes
import io.legado.app.model.translation.ProviderConfigData
import io.legado.app.model.translation.ProviderField
import io.legado.app.model.translation.TranslationProvider
import io.legado.app.utils.GSON
import java.util.UUID

/**
 * 彩云小译(Bearer token + JSON)
 */
object CaiyunProvider : TranslationProvider {
    override val type = "caiyun"
    override val displayName = "彩云小译"
    override val description = "彩云小译推出的翻译服务,支持中英日韩法德等多种语言。"
    override val docUrl = "https://docs.caiyunapp.com/lingocloud/"
    override val showUniversalFields = false

    override val fields: List<ProviderField> = listOf(
        ProviderField(
            key = "token",
            label = "token",
            placeholder = "token",
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
        val token = config.field("token")
        require(token.isNotBlank()) { "token 未填写" }
        val caiyunLang = LanguageCodes.toCaiyun(targetLang)
        if (caiyunLang == targetLang && targetLang !in setOf("zh", "en", "ja", "ko")) {
            throw RuntimeException("彩云小译暂不支持目标语言 $targetLang")
        }
        val from = if (sourceLang.isBlank()) "auto" else LanguageCodes.toCaiyun(sourceLang)
        val transType = "${from}2${caiyunLang}"
        val body = mapOf(
            "source" to listOf(text),
            "trans_type" to transType,
            "request_id" to UUID.randomUUID().toString()
        )
        val response = okHttpClient.newCallStrResponse {
            url("https://api.interpreter.caiyunai.com/v1/translator")
            addHeaders(mapOf("Authorization" to "Bearer $token"))
            postJson(GSON.toJson(body))
        }
        if (!response.isSuccessful()) {
            throw RuntimeException("HTTP ${response.code()}: ${response.message()}")
        }
        val parsed = GSON.fromJson(response.body, Resp::class.java)
            ?: throw RuntimeException("Empty response")
        parsed.target?.firstOrNull()?.takeIf { it.isNotEmpty() }
            ?: throw RuntimeException("Empty translation result")
    }

    data class Resp(val target: List<String>?)
}
