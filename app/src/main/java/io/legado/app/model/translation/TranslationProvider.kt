package io.legado.app.model.translation

import io.legado.app.model.translation.providers.AliyunProvider
import io.legado.app.model.translation.providers.AzureProvider
import io.legado.app.model.translation.providers.BaiduProvider
import io.legado.app.model.translation.providers.CaiyunProvider
import io.legado.app.model.translation.providers.GoogleProvider
import io.legado.app.model.translation.providers.GroqProvider
import io.legado.app.model.translation.providers.TencentProvider
import io.legado.app.model.translation.providers.VolcanoProvider
import io.legado.app.model.translation.providers.XiaoniuProvider
import io.legado.app.model.translation.providers.YoudaoProvider
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * 字段 UI 渲染类型
 */
enum class FieldType { TEXT, PASSWORD, NUMBER, TOGGLE, SELECT }

/**
 * 单个配置字段定义,UI 层根据 type 渲染
 */
data class ProviderField(
    val key: String,
    val label: String,
    val placeholder: String = key,
    val type: FieldType = FieldType.TEXT,
    val default: String? = null,
    val required: Boolean = true,
    val description: String? = null,
    val options: List<Pair<String, String>>? = null  // SELECT 专用
)

/**
 * 单个翻译服务实例的配置数据
 */
@Serializable
data class ProviderConfigData(
    val id: String = UUID.randomUUID().toString(),
    val type: String,
    val customName: String,
    val fields: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis()
) {
    fun field(key: String): String = fields[key].orEmpty()
    fun isFieldFilled(key: String): Boolean = fields[key].isNullOrBlank().not()
    fun allRequiredFilled(required: List<String>): Boolean =
        required.all { isFieldFilled(it) }
}

/**
 * 翻译服务能力接口
 */
interface TranslationProvider {
    val type: String
    val displayName: String
    val description: String
    val docUrl: String?
    val fields: List<ProviderField>

    /** 是否在配置页底部追加 rateLimit/maxChars/maxParas 三个通用字段 */
    val showUniversalFields: Boolean get() = true

    /**
     * 执行翻译。
     *
     * @param config 当前实例的字段配置
     * @param text 待翻译文本
     * @param sourceLang 源语言代码,空字符串表示自动检测
     * @param targetLang 目标语言代码
     */
    suspend fun translate(
        config: ProviderConfigData,
        text: String,
        sourceLang: String,
        targetLang: String
    ): Result<String>
}

/**
 * Provider 注册表(单例)
 */
object ProviderRegistry {
    private val providers = mutableMapOf<String, TranslationProvider>()

    fun register(provider: TranslationProvider) {
        providers[provider.type] = provider
    }

    fun get(type: String): TranslationProvider? = providers[type]

    fun all(): List<TranslationProvider> = providers.values.toList()

    fun registerAll() {
        // 重复注册是幂等的
        register(GoogleProvider)
        register(AliyunProvider)
        register(VolcanoProvider)
        register(BaiduProvider)
        register(TencentProvider)
        register(YoudaoProvider)
        register(AzureProvider)
        register(GroqProvider)
        register(XiaoniuProvider)
        register(CaiyunProvider)
    }
}
