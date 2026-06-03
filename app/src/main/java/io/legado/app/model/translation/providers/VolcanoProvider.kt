package io.legado.app.model.translation.providers

import io.legado.app.help.http.addHeaders
import io.legado.app.help.http.okHttpClient
import io.legado.app.help.http.postJson
import io.legado.app.model.translation.FieldType
import io.legado.app.model.translation.ProviderConfigData
import io.legado.app.model.translation.ProviderField
import io.legado.app.model.translation.TranslationProvider
import io.legado.app.model.translation.signer.VolcanoSigner
import io.legado.app.utils.GSON

/**
 * 火山引擎机器翻译
 */
object VolcanoProvider : TranslationProvider {
    override val type = "volcano"
    override val displayName = "火山机器翻译"
    override val description = "字节跳动火山引擎提供的机器翻译服务。"
    override val docUrl = "https://www.volcengine.com/docs/4640/65007"

    override val fields: List<ProviderField> = listOf(
        ProviderField(key = "accessKeyId", label = "AccessKeyId", placeholder = "AccessKeyId", type = FieldType.TEXT),
        ProviderField(key = "accessKeySecret", label = "AccessKeySecret", placeholder = "AccessKeySecret", type = FieldType.PASSWORD)
    )

    override suspend fun translate(
        config: ProviderConfigData,
        text: String,
        targetLang: String
    ): Result<String> = runCatching {
        val ak = config.field("accessKeyId")
        val sk = config.field("accessKeySecret")
        require(ak.isNotBlank()) { "AccessKeyId 未填写" }
        require(sk.isNotBlank()) { "AccessKeySecret 未填写" }
        val body = mapOf(
            "SourceLanguage" to "zh",
            "TargetLanguage" to targetLang,
            "TextList" to listOf(text)
        )
        val json = GSON.toJson(body)
        val headers = VolcanoSigner.buildHeaders(ak, sk, body = json)
        val response = okHttpClient.newCallStrResponse {
            url("https://open.volcengineapi.com/")
            addHeaders(headers)
            postJson(json)
        }
        if (!response.isSuccessful()) {
            throw RuntimeException("HTTP ${response.code()}: ${response.message()}")
        }
        val parsed = GSON.fromJson(response.body, Resp::class.java)
            ?: throw RuntimeException("Empty response")
        parsed.translation_list?.firstOrNull()?.translation
            ?: parsed.error?.let { throw RuntimeException("火山错误 ${it.code}: ${it.message}") }
            ?: throw RuntimeException("Empty translation result")
    }

    data class Resp(
        val translation_list: List<TransItem>?,
        val error: ErrInfo?
    )
    data class TransItem(val translation: String?)
    data class ErrInfo(val code: String?, val message: String?)
}
