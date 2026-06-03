package io.legado.app.ui.config.translation.model

import androidx.annotation.Keep
import io.legado.app.model.translation.ProviderConfigData

@Keep
data class TranslationConfigState(
    val enabled: Boolean = false,
    val targetLanguage: String = "zh",
    val targetLanguageValues: List<String> = emptyList(),
    val targetLanguageDisplayEntries: Array<String> = emptyArray(),
    val activeProviderName: String? = null
)

@Keep
fun buildTargetLanguageValues(): List<String> = listOf(
    "zh", "zh-Hant", "en", "ja", "ko", "fr", "de", "ru", "es"
)

@Keep
fun buildTargetLanguageDisplayEntries(): Array<String> = arrayOf(
    "Chinese (Simplified)",
    "Chinese (Traditional)",
    "English",
    "Japanese",
    "Korean",
    "French",
    "German",
    "Russian",
    "Spanish"
)

@Keep
fun resolveActiveProviderName(configs: List<ProviderConfigData>): String? {
    val first = configs.firstOrNull { cfg ->
        val provider = io.legado.app.model.translation.ProviderRegistry.get(cfg.type) ?: return@firstOrNull false
        val required = provider.fields.filter { it.required }.map { it.key }
        cfg.allRequiredFilled(required)
    } ?: return null
    return first.customName
}
