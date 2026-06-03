package io.legado.app.model.translation.providers

import io.legado.app.help.http.okHttpClient
import io.legado.app.model.translation.FieldType
import io.legado.app.model.translation.ProviderConfigData
import io.legado.app.model.translation.ProviderField
import io.legado.app.model.translation.TranslationProvider
import io.legado.app.utils.GSON
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * 阿里云机器翻译
 *
 * API:TranslateGeneral (2018-10-12)
 * 端点:https://mt.aliyuncs.com/
 * 鉴权:阿里云 V3 (HMAC-SHA256)
 */
object AliyunProvider : TranslationProvider {
    override val type = "aliyun"
    override val displayName = "阿里云机器翻译"
    override val description = "阿里云推出的机器翻译服务,支持多种语言。"
    override val docUrl = "https://help.aliyun.com/zh/machine-translation"
    override val showUniversalFields = false

    override val fields: List<ProviderField> = listOf(
        ProviderField(key = "accessKeyId", label = "AccessKeyId", placeholder = "AccessKeyId", type = FieldType.TEXT),
        ProviderField(key = "accessKeySecret", label = "AccessKeySecret", placeholder = "AccessKeySecret", type = FieldType.PASSWORD),
        ProviderField(
            key = "scene",
            label = "场景",
            placeholder = "general",
            type = FieldType.SELECT,
            default = "general",
            description = "默认为 general。仅在阿里云 API 控制台开通机器翻译专业版的情况下可设置,支持场景见官网文档。",
            options = listOf(
                "general" to "general",
                "ecommerce" to "ecommerce",
                "social" to "social"
            )
        )
    )

    override suspend fun translate(
        config: ProviderConfigData,
        text: String,
        targetLang: String
    ): Result<String> = runCatching {
        val ak = config.field("accessKeyId")
        val sk = config.field("accessKeySecret")
        val scene = config.field("scene").ifBlank { "general" }
        require(ak.isNotBlank()) { "AccessKeyId 未填写" }
        require(sk.isNotBlank()) { "AccessKeySecret 未填写" }
        val params = linkedMapOf<String, String>(
            "Action" to "TranslateGeneral",
            "Format" to "JSON",
            "Version" to "2018-10-12",
            "RegionId" to "cn-hangzhou",
            "AccessKeyId" to ak,
            "SignatureMethod" to "HMAC-SHA1",
            "SignatureNonce" to java.util.UUID.randomUUID().toString(),
            "SignatureVersion" to "1.0",
            "Timestamp" to Instant.now().atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")),
            "SourceText" to text,
            "SourceLanguage" to "auto",
            "TargetLanguage" to targetLang,
            "Scene" to scene
        )
        val sortedQuery = params.toSortedMap().entries.joinToString("&") {
            "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}"
        }
        val stringToSign = "GET&" + URLEncoder.encode("/", "UTF-8") + "&" +
            URLEncoder.encode(sortedQuery, "UTF-8")
        val signature = hmacSha1("$sk&", stringToSign)
        val finalUrl = "https://mt.aliyuncs.com/?$sortedQuery&Signature=$signature"
        val response = okHttpClient.newCallStrResponse { url(finalUrl) }
        if (!response.isSuccessful()) {
            throw RuntimeException("HTTP ${response.code()}: ${response.message()}")
        }
        val parsed = GSON.fromJson(response.body, Resp::class.java)
            ?: throw RuntimeException("Empty response")
        parsed.data?.translated
            ?: parsed.message?.let { throw RuntimeException("阿里云错误 $it: ${parsed.code.orEmpty()}") }
            ?: throw RuntimeException("Empty translation result")
    }

    private fun hmacSha1(key: String, data: String): String {
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA1"))
        val raw = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return raw.joinToString("") { "%02x".format(it) }
    }

    data class Resp(
        val data: Data?,
        val code: String?,
        val message: String?
    )
    data class Data(val translated: String?)
}
