package io.legado.app.ui.config.translation

import io.legado.app.constant.PreferKey
import io.legado.app.model.translation.ProviderConfigData
import io.legado.app.ui.config.prefDelegate
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonArray
import io.legado.app.utils.postEvent

object TranslationConfig {
    var enabled by prefDelegate(PreferKey.llmTranslateEnabled, false) {
        postEvent(PreferKey.llmTranslateEnabled, it)
    }
    var targetLanguage by prefDelegate(PreferKey.llmTargetLanguage, "zh") {
        postEvent(PreferKey.llmTargetLanguage, it)
    }

    /** 翻译服务实例列表(以 JSON 字符串存储) */
    var providerConfigsJson by prefDelegate(
        PreferKey.translationProviderConfigs,
        "[]"
    ) { postEvent(PreferKey.translationProviderConfigs, it) }

    /** 解析后的实例列表(只读) */
    val providerConfigs: List<ProviderConfigData>
        get() = GSON.fromJsonArray<ProviderConfigData>(providerConfigsJson).orEmpty()

    /** 第一个所有 required 字段都填好的实例;若没有则返回 null */
    fun firstValidConfig(): Pair<io.legado.app.model.translation.TranslationProvider, ProviderConfigData>? {
        val providers = io.legado.app.model.translation.ProviderRegistry.all()
        for (cfg in providerConfigs) {
            val provider = providers.firstOrNull { it.type == cfg.type } ?: continue
            val required = provider.fields.filter { it.required }.map { it.key } +
                if (provider.showUniversalFields)
                    listOf("rateLimit", "maxChars", "maxParas") else emptyList()
            if (cfg.allRequiredFilled(required)) {
                return provider to cfg
            }
        }
        return null
    }

    fun writeProviderConfigs(list: List<ProviderConfigData>) {
        providerConfigsJson = GSON.toJson(list)
    }

    fun upsertProviderConfig(config: ProviderConfigData) {
        val list = providerConfigs.toMutableList()
        val idx = list.indexOfFirst { it.id == config.id }
        if (idx >= 0) list[idx] = config else list.add(config)
        writeProviderConfigs(list)
    }

    fun deleteProviderConfig(id: String) {
        writeProviderConfigs(providerConfigs.filter { it.id != id })
    }

    val targetLanguages = listOf(
        "zh" to "简体中文",
        "zh-Hant" to "繁體中文",
        "en" to "English",
        "ja" to "日本語",
        "ko" to "한국어",
        "fr" to "Français",
        "de" to "Deutsch",
        "es" to "Español",
        "ru" to "Русский"
    )
}
