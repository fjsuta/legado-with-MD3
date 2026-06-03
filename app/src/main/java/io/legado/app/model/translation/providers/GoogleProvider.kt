package io.legado.app.model.translation.providers

import com.google.gson.JsonParser
import io.legado.app.help.http.newCallStrResponse
import io.legado.app.help.http.okHttpClient
import io.legado.app.model.translation.FieldType
import io.legado.app.model.translation.ProviderConfigData
import io.legado.app.model.translation.ProviderField
import io.legado.app.model.translation.TranslationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Google Translate 免费接口(无需 key)。
 *
 * 响应是嵌套数组:
 *   [[["trans","orig",null,null,10]], null, "zh-CN", null, ...]
 * 翻译结果在 res[0][i][0]
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
        sourceLang: String,
        targetLang: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (text.isBlank()) return@runCatching text
            val encoded = java.net.URLEncoder.encode(text, "UTF-8")
            val sl = if (sourceLang.isBlank()) "auto" else sourceLang
            val url = "https://translate.googleapis.com/translate_a/single" +
                "?client=gtx&sl=$sl&tl=$targetLang&dt=t&ie=UTF-8&q=$encoded"
            val response = okHttpClient.newCallStrResponse { url(url) }
            if (!response.isSuccessful()) {
                throw RuntimeException("HTTP ${response.code()}: ${response.message()}")
            }
            val root = JsonParser.parseString(response.body)
            if (!root.isJsonArray) throw RuntimeException("Unexpected response: ${response.body?.take(200) ?: ""}")
            val outer = root.asJsonArray
            if (outer.size() == 0 || !outer[0].isJsonArray) {
                throw RuntimeException("Empty translation result")
            }
            val segments = outer[0].asJsonArray
            val translated = StringBuilder()
            for (seg in segments) {
                if (seg.isJsonArray && seg.asJsonArray.size() > 0) {
                    val first = seg.asJsonArray[0]
                    if (first.isJsonPrimitive && first.asJsonPrimitive.isString) {
                        translated.append(first.asString)
                    }
                }
            }
            val out = translated.toString()
            if (out.isEmpty()) throw RuntimeException("Empty translation result")
            out
        }
    }
}
