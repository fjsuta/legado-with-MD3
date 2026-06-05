package io.legado.app.ui.config.aiService

import io.legado.app.constant.PreferKey
import io.legado.app.model.ai.AiModel
import io.legado.app.model.ai.AiProvider
import io.legado.app.model.ai.AiSearchMode
import io.legado.app.model.ai.AiServiceMode
import io.legado.app.model.ai.ApiMode
import io.legado.app.ui.config.prefDelegate
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonArray
import io.legado.app.utils.postEvent

object AiServiceConfig {

    var enabled by prefDelegate(PreferKey.aiServiceEnabled, false) {
        postEvent(PreferKey.aiServiceEnabled, it)
    }

    var serviceMode by prefDelegate(
        PreferKey.aiServiceMode,
        AiServiceMode.Builtin.name
    ) {
        postEvent(PreferKey.aiServiceMode, it)
    }

    var searchMode by prefDelegate(
        PreferKey.aiSearchMode,
        AiSearchMode.Traditional.name
    ) {
        postEvent(PreferKey.aiSearchMode, it)
    }

    var builtinProvider by prefDelegate(
        PreferKey.aiBuiltinProvider,
        BuiltinProviders.OpenRouter.name
    ) {
        postEvent(PreferKey.aiBuiltinProvider, it)
    }

    var builtinModelId by prefDelegate(PreferKey.aiBuiltinModelId, "") {
        postEvent(PreferKey.aiBuiltinModelId, it)
    }

    private var customProvidersJson by prefDelegate(
        PreferKey.aiCustomProviders,
        "[]"
    ) {
        postEvent(PreferKey.aiCustomProviders, it)
    }

    var activeCustomProviderId by prefDelegate(PreferKey.aiActiveCustomProviderId, "") {
        postEvent(PreferKey.aiActiveCustomProviderId, it)
    }

    var activeModelId by prefDelegate(PreferKey.aiActiveModelId, "") {
        postEvent(PreferKey.aiActiveModelId, it)
    }

    val customProviders: List<AiProvider>
        get() = GSON.fromJsonArray<AiProvider>(customProvidersJson).getOrNull() ?: emptyList()

    fun writeCustomProviders(list: List<AiProvider>) {
        customProvidersJson = GSON.toJson(list)
    }

    fun upsertCustomProvider(provider: AiProvider) {
        val list = customProviders.toMutableList()
        val idx = list.indexOfFirst { it.id == provider.id }
        if (idx >= 0) {
            list[idx] = provider
        } else {
            list.add(provider)
        }
        writeCustomProviders(list)
    }

    fun deleteCustomProvider(id: String) {
        writeCustomProviders(customProviders.filter { it.id != id })
        if (activeCustomProviderId == id) {
            activeCustomProviderId = customProviders.firstOrNull()?.id ?: ""
        }
    }

    fun getActiveProvider(): AiProvider? {
        return when (AiServiceMode.valueOf(serviceMode)) {
            AiServiceMode.Builtin -> getBuiltinProvider()
            AiServiceMode.Custom -> {
                if (activeCustomProviderId.isBlank()) null
                else customProviders.firstOrNull { it.id == activeCustomProviderId }
            }
        }
    }

    fun getActiveModelId(): String {
        return when (AiServiceMode.valueOf(serviceMode)) {
            AiServiceMode.Builtin -> builtinModelId
            AiServiceMode.Custom -> activeModelId
        }
    }

    fun getBuiltinProvider(): AiProvider? {
        return when (BuiltinProviders.valueOf(builtinProvider)) {
            BuiltinProviders.OpenRouter -> openRouterProvider
            BuiltinProviders.Ollama -> ollamaProvider
        }
    }

    enum class BuiltinProviders(val displayName: String) {
        OpenRouter("OpenRouter"),
        Ollama("Ollama")
    }

    private val openRouterProvider = AiProvider(
        id = "builtin_openrouter",
        name = "OpenRouter",
        apiMode = ApiMode.OpenAI,
        apiKey = "",
        apiHost = "https://openrouter.ai/api/v1",
        apiPath = "/chat/completions",
        models = listOf(
            AiModel(modelId = "moonshotai/kimi-k2.6:free", displayName = "Kimi K2.6 (Free)"),
            AiModel(modelId = "google/gemma-4-26b-a4b-it:free", displayName = "Gemma 4 26B (Free)"),
            AiModel(modelId = "openai/gpt-oss-120b:free", displayName = "GPT OSS 120B (Free)")
        )
    )

    private val ollamaProvider = AiProvider(
        id = "builtin_ollama",
        name = "Ollama",
        apiMode = ApiMode.OpenAI,
        apiKey = "",
        apiHost = "https://api.ollama.ai/v1",
        apiPath = "/chat/completions",
        models = listOf(
            AiModel(modelId = "minimax-m3:cloud", displayName = "MiniMax M3"),
            AiModel(modelId = "qwen3.5:397b-cloud", displayName = "Qwen 3.5 397B"),
            AiModel(modelId = "glm-5.1:cloud", displayName = "GLM 5.1"),
            AiModel(modelId = "kimi-k2.6:cloud", displayName = "Kimi K2.6"),
            AiModel(modelId = "deepseek-v3.2:cloud", displayName = "DeepSeek V3.2"),
            AiModel(modelId = "gemini-3-flash-preview:cloud", displayName = "Gemini 3 Flash"),
            AiModel(modelId = "gemma4:31b-cloud", displayName = "Gemma 4 31B"),
            AiModel(modelId = "nemotron-3-ultra:cloud", displayName = "Nemotron 3 Ultra"),
            AiModel(modelId = "deepseek-v4-pro:cloud", displayName = "DeepSeek V4 Pro")
        )
    )
}
