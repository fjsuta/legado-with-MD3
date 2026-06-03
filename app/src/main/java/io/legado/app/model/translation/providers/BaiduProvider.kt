package io.legado.app.model.translation.providers

import com.google.gson.annotations.SerializedName
import io.legado.app.help.http.newCallStrResponse
import io.legado.app.help.http.okHttpClient
import io.legado.app.model.translation.FieldType
import io.legado.app.model.translation.ProviderConfigData
import io.legado.app.model.translation.ProviderField
import io.legado.app.model.translation.TranslationProvider
import io.legado.app.model.translation.signer.BaiduSigner
import io.legado.app.utils.GSON
import java.net.URLEncoder

/**
 * 百度机器翻译
 *
 * 端点:https://fanyi-api.baidu.com/api/trans/vip/translate
 */
object BaiduProvider : TranslationProvider {
    override val type = "baidu"
    override val displayName = "百度机器翻译"
    override val description = "百度推出的机器翻译服务,支持多种语言。"
    override val docUrl = "https://api.fanyi.baidu.com/doc/21"

    override val fields: List<ProviderField> = listOf(
        ProviderField(key = "appId", label = "AppID", type = FieldType.TEXT, required = true),
        ProviderField(key = "secretKey", label = "Secret Key", type = FieldType.PASSWORD, required = true)
    )

    override suspend fun translate(
        config: ProviderConfigData,
        text: String,
        sourceLang: String,
        targetLang: String
    ): Result<String> = runCatching {
        if (text.isBlank()) return@runCatching text
        val appId = config.field("appId")
        val sk = config.field("secretKey")
        require(appId.isNotBlank()) { "AppID 未填写" }
        require(sk.isNotBlank()) { "Secret Key 未填写" }
        val src = if (sourceLang.isBlank()) "auto" else sourceLang
        val salt = (1..65536).random().toString()
        val sign = BaiduSigner.sign(appId, text, salt, sk)
        val encodedQ = URLEncoder.encode(text, "UTF-8")
        val url = "https://fanyi-api.baidu.com/api/trans/vip/translate" +
            "?q=$encodedQ&from=$src&to=$targetLang&appid=${URLEncoder.encode(appId, "UTF-8")}" +
            "&salt=$salt&sign=$sign"
        val response = okHttpClient.newCallStrResponse { url(url) }
        if (!response.isSuccessful()) {
            throw RuntimeException("HTTP ${response.code()}: ${response.message()}")
        }
        val parsed = GSON.fromJson(response.body, BaiduResp::class.java)
            ?: throw RuntimeException("Empty response body")
        if (!parsed.errorCode.isNullOrEmpty() && parsed.errorCode != "52001") {
            // 52001 是请求超时,可重试;其他错误直接报错
            throw RuntimeException("百度错误 ${parsed.errorCode}: ${parsed.errorMsg}")
        }
        val translated = parsed.transResult
            ?.mapNotNull { it.dst?.takeIf { d -> d.isNotEmpty() } }
            ?.joinToString("\n")
        if (translated.isNullOrEmpty()) throw RuntimeException("Empty translation result")
        translated
    }

    data class BaiduResp(
        @SerializedName("error_code") val errorCode: String? = null,
        @SerializedName("error_msg") val errorMsg: String? = null,
        @SerializedName("trans_result") val transResult: List<BaiduItem>? = null
    )

    data class BaiduItem(
        @SerializedName("src") val src: String? = null,
        @SerializedName("dst") val dst: String? = null
    )
}
