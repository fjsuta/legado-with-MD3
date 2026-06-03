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
import io.legado.app.model.translation.signer.TencentSigner
import io.legado.app.utils.GSON

/**
 * 腾讯云机器翻译
 */
object TencentProvider : TranslationProvider {
    override val type = "tencent"
    override val displayName = "腾讯云机器翻译"
    override val description = "腾讯云提供的机器翻译服务,支持多种语言。"
    override val docUrl = "https://cloud.tencent.com/document/api/551/30636"

    override val fields: List<ProviderField> = listOf(
        ProviderField(key = "secretId", label = "SecretId", placeholder = "SecretId", type = FieldType.TEXT),
        ProviderField(key = "secretKey", label = "SecretKey", placeholder = "SecretKey", type = FieldType.PASSWORD)
    )

    override suspend fun translate(
        config: ProviderConfigData,
        text: String,
        sourceLang: String,
        targetLang: String
    ): Result<String> = runCatching {
        if (text.isBlank()) return@runCatching text
        val sid = config.field("secretId")
        val skey = config.field("secretKey")
        require(sid.isNotBlank()) { "SecretId 未填写" }
        require(skey.isNotBlank()) { "SecretKey 未填写" }
        val source = if (sourceLang.isBlank()) "auto" else LanguageCodes.toTencent(sourceLang)
        val target = LanguageCodes.toTencent(targetLang)
        val body = mapOf(
            "SourceText" to text,
            "Source" to source,
            "Target" to target,
            "ProjectId" to 0
        )
        val json = GSON.toJson(body)
        val headers = TencentSigner.buildHeaders(
            secretId = sid, secretKey = skey, payload = json
        )
        val response = okHttpClient.newCallStrResponse {
            url("https://tmt.tencentcloudapi.com/")
            addHeaders(headers)
            postJson(json)
        }
        if (!response.isSuccessful()) {
            throw RuntimeException("HTTP ${response.code()}: ${response.message()}")
        }
        val parsed = GSON.fromJson(response.body, Resp::class.java)
            ?: throw RuntimeException("Empty response")
        parsed.response?.targetText
            ?: parsed.response?.error?.let { throw RuntimeException("腾讯错误 ${it.code}: ${it.message}") }
            ?: throw RuntimeException("Empty translation result")
    }

    data class Resp(val response: Inner?)
    data class Inner(
        val targetText: String?,
        val error: ErrInfo?
    )
    data class ErrInfo(val code: String?, val message: String?)
}
