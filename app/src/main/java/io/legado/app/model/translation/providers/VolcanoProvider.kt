package io.legado.app.model.translation.providers

import com.google.gson.annotations.SerializedName
import io.legado.app.help.http.addHeaders
import io.legado.app.help.http.newCallStrResponse
import io.legado.app.help.http.okHttpClient
import io.legado.app.help.http.postJson
import io.legado.app.model.translation.FieldType
import io.legado.app.model.translation.ProviderConfigData
import io.legado.app.model.translation.ProviderField
import io.legado.app.model.translation.TranslationProvider
import io.legado.app.model.translation.signer.VolcanoSigner
import io.legado.app.utils.GSON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class VolcanoResp(
    @SerializedName("TranslationList") val translationList: List<VolcanoTrans>? = null,
    @SerializedName("ResponseMetadata") val responseMetadata: VolcanoMeta? = null
)

data class VolcanoTrans(
    @SerializedName("Translation") val translation: String? = null,
    @SerializedName("SourceLanguage") val sourceLanguage: String? = null,
    @SerializedName("TargetLanguage") val targetLanguage: String? = null
)

data class VolcanoMeta(
    @SerializedName("Error") val error: VolcanoError? = null
)

data class VolcanoError(
    @SerializedName("Code") val code: String? = null,
    @SerializedName("Message") val message: String? = null
)

object VolcanoProvider : TranslationProvider {

    override val type = "volcano"
    override val displayName = "火山机器翻译"
    override val description = "字节跳动火山引擎提供的机器翻译服务。"
    override val docUrl = "https://www.volcengine.com/docs/4640/65007"
    override val fields = listOf(
        ProviderField(
            key = "accessKeyId",
            label = "AccessKey ID",
            placeholder = "AK 访问密钥 ID",
            type = FieldType.TEXT,
            required = true
        ),
        ProviderField(
            key = "accessKeySecret",
            label = "AccessKey Secret",
            placeholder = "SK 访问密钥",
            type = FieldType.PASSWORD,
            required = true
        )
    )

    override suspend fun translate(
        config: ProviderConfigData,
        text: String,
        sourceLang: String,
        targetLang: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (text.isBlank()) return@runCatching text
            val ak = config.field("accessKeyId")
            val sk = config.field("accessKeySecret")
            require(ak.isNotEmpty() && sk.isNotEmpty()) {
                "请先在配置中填写 AccessKey ID 与 Secret"
            }

            val src = if (sourceLang.isBlank()) "auto" else sourceLang
            val body = GSON.toJson(
                mapOf(
                    "SourceLanguage" to src,
                    "TargetLanguage" to targetLang,
                    "TextList" to listOf(text)
                )
            )
            val query = "Action=TranslateText&Version=2020-06-01"
            val host = "translate.volcengineapi.com"
            val path = "/"
            val headers = VolcanoSigner.buildHeaders(
                accessKeyId = ak,
                accessKeySecret = sk,
                host = host,
                path = path,
                query = query,
                body = body
            )
            val url = "https://$host$path?$query"
            val response = okHttpClient.newCallStrResponse {
                url(url)
                addHeaders(headers)
                postJson(body)
            }
            if (!response.isSuccessful()) {
                throw RuntimeException("HTTP ${response.code()}: ${response.body.take(200)}")
            }
            val parsed = GSON.fromJson(response.body, VolcanoResp::class.java)
                ?: throw RuntimeException("Empty response body")
            val err = parsed.responseMetadata?.error
            if (err != null && !err.code.isNullOrEmpty()) {
                throw RuntimeException("火山 ${err.code}: ${err.message}")
            }
            val translated = parsed.translationList
                ?.mapNotNull { it.translation?.takeIf { t -> t.isNotEmpty() } }
                ?.joinToString("")
            if (translated.isNullOrEmpty()) throw RuntimeException("Empty translation result")
            translated
        }
    }
}
