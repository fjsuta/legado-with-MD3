package io.legado.app.model.translation.providers

import io.legado.app.help.http.okHttpClient
import io.legado.app.model.translation.FieldType
import io.legado.app.model.translation.LanguageCodes
import io.legado.app.model.translation.ProviderConfigData
import io.legado.app.model.translation.ProviderField
import io.legado.app.model.translation.TranslationProvider
import io.legado.app.model.translation.signer.BaiduSigner
import io.legado.app.utils.GSON

/**
 * 百度机器翻译
 *
 * https://fanyi-api.baidu.com/api/trans/vip/translate
 */
object BaiduProvider : TranslationProvider {
    override val type = "baidu"
    override val displayName = "百度机器翻译"
    override val description = "百度推出的机器翻译服务,支持多种语言。"
    override val docUrl = "https://api.fanyi.baidu.com/product/111"

    override val fields: List<ProviderField> = listOf(
        ProviderField(key = "appId", label = "appId", placeholder = "appId", type = FieldType.TEXT),
        ProviderField(key = "secretKey", label = "secretKey", placeholder = "secretKey", type = FieldType.PASSWORD)
    )

    override suspend fun translate(
        config: ProviderConfigData,
        text: String,
        targetLang: String
    ): Result<String> = runCatching {
        val appId = config.field("appId")
        val secretKey = config.field("secretKey")
        require(appId.isNotBlank()) { "appId 未填写" }
        require(secretKey.isNotBlank()) { "secretKey 未填写" }
        val salt = BaiduSigner.randomSalt()
        val from = "auto"
        val to = LanguageCodes.toBaidu(targetLang)
        val sign = BaiduSigner.sign(appId, text, salt, secretKey)
        val encodedQ = java.net.URLEncoder.encode(text, "UTF-8")
        val url = "https://fanyi-api.baidu.com/api/trans/vip/translate" +
            "?q=$encodedQ&from=$from&to=$to&appid=$appId&salt=$salt&sign=$sign"
        val response = okHttpClient.newCallStrResponse { url(url) }
        if (!response.isSuccessful()) {
            throw RuntimeException("HTTP ${response.code()}: ${response.message()}")
        }
        val parsed = GSON.fromJson(response.body, Resp::class.java)
            ?: throw RuntimeException("Empty response")
        parsed.trans_result?.joinToString("\n") { it.dst.orEmpty() }
            ?.takeIf { it.isNotEmpty() }
            ?: parsed.error_code?.let { throw RuntimeException("百度错误 $it: ${parsed.error_msg.orEmpty()}") }
            ?: throw RuntimeException("Empty translation result")
    }

    data class Resp(
        val trans_result: List<TransItem>?,
        val error_code: String?,
        val error_msg: String?
    )
    data class TransItem(val src: String?, val dst: String?)
}
