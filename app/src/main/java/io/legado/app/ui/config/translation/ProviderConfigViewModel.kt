package io.legado.app.ui.config.translation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.legado.app.model.translation.ChunkSplitter
import io.legado.app.model.translation.DEFAULT_UNIVERSAL_FIELDS
import io.legado.app.model.translation.ProviderConfigData
import io.legado.app.model.translation.ProviderField
import io.legado.app.model.translation.ProviderRegistry
import io.legado.app.model.translation.RateLimiter
import io.legado.app.model.translation.TranslationProvider
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ProviderConfigState(
    val providerType: String,
    val displayName: String,
    val description: String,
    val docUrl: String?,
    val fields: List<ProviderField>,
    val showUniversalFields: Boolean,
    val customName: String,
    val fieldsMap: Map<String, String>
)

@OptIn(FlowPreview::class)
class ProviderConfigViewModel(
    providerType: String,
    existingId: String?
) : ViewModel() {

    private val provider: TranslationProvider =
        ProviderRegistry.get(providerType)
            ?: error("Unknown provider: $providerType")

    private val existing: ProviderConfigData? =
        TranslationConfig.providerConfigs.firstOrNull { it.id == existingId }

    /**
     * 实例 id 一次性确定。新建场景下在 ViewModel 构造时生成,避免每次 currentConfig()
     * 都新建 UUID 造成配置列表里堆出大量空实例。
     */
    private val instanceId: String = existing?.id ?: UUID.randomUUID().toString()

    private val _state = MutableStateFlow(buildInitialState())
    val state: StateFlow<ProviderConfigState> = _state.asStateFlow()

    // 写盘触发器:每次输入字段/名称都发一个事件,debounce 500ms 后再实际写盘,
    // 避免每敲一个字符就触发一次 GSON 序列化 + SharedPreferences 写入。
    private val dirty = MutableSharedFlow<Unit>(extraBufferCapacity = 64)

    init {
        viewModelScope.launch {
            dirty.debounce(500).collect {
                TranslationConfig.upsertProviderConfig(currentConfig())
            }
        }
    }

    fun setCustomName(name: String) {
        _state.update { it.copy(customName = name) }
        dirty.tryEmit(Unit)
    }

    fun setField(key: String, value: String) {
        _state.update { s -> s.copy(fieldsMap = s.fieldsMap + (key to value)) }
        dirty.tryEmit(Unit)
    }

    fun resetToDefault() {
        _state.update { s ->
            val defaults = provider.fields.associate { it.key to it.default.orEmpty() } +
                if (provider.showUniversalFields) DEFAULT_UNIVERSAL_FIELDS.associate { it.key to it.default.orEmpty() }
                else emptyMap()
            s.copy(
                customName = "${provider.displayName} ${numberedCount()}",
                fieldsMap = defaults
            )
        }
        dirty.tryEmit(Unit)
    }

    fun delete() {
        // 即使是新建中的实例(existing == null)也尝试按 id 删除,理论上不会找到
        TranslationConfig.deleteProviderConfig(instanceId)
    }

    /**
     * 测试当前配置。返回 Result 是为了 UI 层通过 isSuccess / exceptionOrNull 提示用户。
     */
    suspend fun test(): Result<String> = runCatching {
        val config = currentConfig()
        val targetLang = TranslationConfig.targetLanguage.ifBlank { "zh" }
        val showUniversal = provider.showUniversalFields
        val rateLimit = if (showUniversal) {
            (config.fields["rateLimit"]?.toIntOrNull() ?: 1).coerceAtLeast(1)
        } else 1
        val maxChars = if (showUniversal) {
            (config.fields["maxChars"]?.toIntOrNull() ?: 1800).coerceAtLeast(100)
        } else 5000
        val maxParas = if (showUniversal) {
            (config.fields["maxParas"]?.toIntOrNull() ?: 8).coerceAtLeast(1)
        } else 100

        val rateLimiter = RateLimiter(rateLimit)
        val chunks = ChunkSplitter.split("Hello world", maxChars, maxParas)
        if (chunks.isEmpty()) throw RuntimeException("Empty text")
        rateLimiter.acquire()
        provider.translate(config, chunks.first().text, "", targetLang).getOrThrow()
    }

    fun currentConfig(): ProviderConfigData = ProviderConfigData(
        id = instanceId,
        type = provider.type,
        customName = _state.value.customName,
        fields = _state.value.fieldsMap
    )

    private fun buildInitialState(): ProviderConfigState {
        val defaultName = "${provider.displayName} ${numberedCount()}"
        val fields = (existing?.fields ?: defaultFieldMap())
        return ProviderConfigState(
            providerType = provider.type,
            displayName = provider.displayName,
            description = provider.description,
            docUrl = provider.docUrl,
            fields = provider.fields,
            showUniversalFields = provider.showUniversalFields,
            customName = existing?.customName ?: defaultName,
            fieldsMap = fields
        )
    }

    private fun defaultFieldMap(): Map<String, String> {
        val map = provider.fields.associate { it.key to it.default.orEmpty() }.toMutableMap()
        if (provider.showUniversalFields) {
            DEFAULT_UNIVERSAL_FIELDS.forEach { f -> map.putIfAbsent(f.key, f.default.orEmpty()) }
        }
        return map
    }

    private fun numberedCount(): Int {
        val sameType = TranslationConfig.providerConfigs.count {
            it.type == provider.type
        } + 1
        return sameType
    }
}
