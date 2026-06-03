package io.legado.app.ui.config.translation

import androidx.lifecycle.ViewModel
import io.legado.app.model.translation.DEFAULT_UNIVERSAL_FIELDS
import io.legado.app.model.translation.ProviderConfigData
import io.legado.app.model.translation.ProviderField
import io.legado.app.model.translation.ProviderRegistry
import io.legado.app.model.translation.TranslationProvider
import io.legado.app.model.translation.ChunkSplitter
import io.legado.app.model.translation.RateLimiter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

class ProviderConfigViewModel(
    providerType: String,
    existingId: String?
) : ViewModel() {

    private val provider: TranslationProvider =
        ProviderRegistry.get(providerType)
            ?: error("Unknown provider: $providerType")

    private val existing: ProviderConfigData? =
        TranslationConfig.providerConfigs.firstOrNull { it.id == existingId }

    private val _state = MutableStateFlow(buildInitialState())
    val state: StateFlow<ProviderConfigState> = _state.asStateFlow()

    fun setCustomName(name: String) {
        _state.update { it.copy(customName = name) }
        persist()
    }

    fun setField(key: String, value: String) {
        _state.update { s -> s.copy(fieldsMap = s.fieldsMap + (key to value)) }
        persist()
    }

    fun resetToDefault() {
        _state.update { s ->
            val defaults = provider.fields.associate { it.key to it.default.orEmpty() } +
                if (provider.showUniversalFields) UNIVERSAL_FIELDS.associate { it.key to it.default.orEmpty() }
                else emptyMap()
            s.copy(
                customName = "${provider.displayName} ${numberedCount()}",
                fieldsMap = defaults
            )
        }
        persist()
    }

    fun delete() {
        if (existing != null) {
            TranslationConfig.deleteProviderConfig(existing.id)
        }
    }

    suspend fun test(): Result<String> = runCatching {
        val config = currentConfig()
        val targetLang = TranslationConfig.targetLanguage.ifBlank { "zh" }
        val provider = this.provider
        val fields = if (provider.showUniversalFields) {
            val rateLimit = (config.fields["rateLimit"]?.toIntOrNull() ?: 1).coerceAtLeast(1)
            val maxChars = (config.fields["maxChars"]?.toIntOrNull() ?: 1800).coerceAtLeast(100)
            val maxParas = (config.fields["maxParas"]?.toIntOrNull() ?: 8).coerceAtLeast(1)
            Triple(rateLimit, maxChars, maxParas)
        } else Triple(1, 5000, 100)
        val rateLimiter = RateLimiter(fields.first)
        val chunks = ChunkSplitter.split("Hello world", fields.second, fields.third)
        if (chunks.isEmpty()) throw RuntimeException("Empty text")
        rateLimiter.acquire()
        val result = provider.translate(config, chunks.first().text, targetLang).getOrThrow()
        result
    }

    fun currentConfig(): ProviderConfigData = ProviderConfigData(
        id = existing?.id ?: UUID.randomUUID().toString(),
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

    private fun persist() {
        val cfg = currentConfig()
        TranslationConfig.upsertProviderConfig(cfg)
    }
}
