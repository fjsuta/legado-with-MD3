package io.legado.app.ui.config.translation

import androidx.lifecycle.ViewModel
import io.legado.app.ui.config.translation.model.TranslationConfigState
import io.legado.app.ui.config.translation.model.buildTargetLanguageDisplayEntries
import io.legado.app.ui.config.translation.model.buildTargetLanguageValues
import io.legado.app.ui.config.translation.model.resolveActiveProviderName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TranslationConfigViewModel : ViewModel() {

    private val _state = MutableStateFlow(initial())
    val state: StateFlow<TranslationConfigState> = _state.asStateFlow()

    private fun initial(): TranslationConfigState = TranslationConfigState(
        enabled = TranslationConfig.enabled,
        targetLanguage = TranslationConfig.targetLanguage,
        targetLanguageValues = buildTargetLanguageValues(),
        targetLanguageDisplayEntries = buildTargetLanguageDisplayEntries(),
        activeProviderName = resolveActiveProviderName(TranslationConfig.providerConfigs)
    )

    fun setEnabled(value: Boolean) {
        TranslationConfig.enabled = value
        _state.update { it.copy(enabled = value) }
    }

    fun setTargetLanguage(value: String) {
        TranslationConfig.targetLanguage = value
        _state.update { it.copy(targetLanguage = value) }
    }

    fun refreshActiveProvider() {
        _state.update {
            it.copy(activeProviderName = resolveActiveProviderName(TranslationConfig.providerConfigs))
        }
    }
}
