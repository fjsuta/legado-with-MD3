package io.legado.app.model.translation.providers

import io.legado.app.help.http.okHttpClient
import io.legado.app.help.http.postForm
import io.legado.app.model.translation.FieldType
import io.legado.app.model.translation.LanguageCodes
import io.legado.app.model.translation.ProviderConfigData
import io.legado.app.model.translation.ProviderField
import io.legado.app.model.translation.TranslationProvider
import io.legado.app.model.translation.signer.YoudaoSigner
import io.legado.app.utils.GSON

/**
 * 有道智云(应用名 = appKey)
 */
object YoudaoProvider : TranslationProvider {
    override val type = "youdao"
    override val displayName = "有道智云"
    override val description = "网易有道推出的翻译服务,支持多种语言。"
    override val docUrl = "https://ai.youdao.com/doc.s#guide"

    override val fields: List<ProviderField> = listOf(
        ProviderField(key = "appKey", label = "appKey", placeholder = "appKey", type = FieldType.TEXT),
        ProviderField(key = "appSecret", label = "appSecret", placeholder = "appSecret", type = FieldType.PASSWORD)
    )

    override suspend fun translate(
        config: ProviderConfigData,
        text: String,
        sourceLang: String,
        targetLang: String
    ): Result<String> = runCatching {
        if (text.isBlank()) return@runCatching text
        val appKey = config.field("appKey")
        val appSecret = config.field("appSecret")
        require(appKey.isNotBlank()) { "appKey 未填写" }
        require(appSecret.isNotBlank()) { "appSecret 未填写" }
        val salt = YoudaoSigner.randomSalt()
        val curtime = YoudaoSigner.currentTime()
        val from = if (sourceLang.isBlank()) "auto" else LanguageCodes.toYoudao(sourceLang)
        val to = LanguageCodes.toYoudao(targetLang)
        val sign = YoudaoSigner.sign(appKey, text, salt, curtime, appSecret)
        val encodedQ = java.net.URLEncoder.encode(text, "UTF-8")
        val form = mapOf(
            "q" to encodedQ,
            "from" to from,
            "to" to to,
            "appKey" to appKey,
            "salt" to salt,
            "sign" to sign,
            "signType" to "v3",
            "curtime" to curtime
        )
        val response = okHttpClient.newCallStrResponse {
            url("https://openapi.youdao.com/api")
            postForm(form, encoded = true)
        }
        if (!response.isSuccessful()) {
            throw RuntimeException("HTTP ${response.code()}: ${response.message()}")
        }
        val parsed = GSON.fromJson(response.body, Resp::class.java)
            ?: throw RuntimeException("Empty response")
        parsed.translation?.firstOrNull()?.takeIf { it.isNotEmpty() }
            ?: parsed.errorCode?.let { throw RuntimeException("有道错误 $it: ${parsed.errorMsg.orEmpty()}") }
            ?: throw RuntimeException("Empty translation result")
    }

    data class Resp(
        val translation: List<String>?,
        val errorCode: String?,
        val errorMsg: String?
    )
}
