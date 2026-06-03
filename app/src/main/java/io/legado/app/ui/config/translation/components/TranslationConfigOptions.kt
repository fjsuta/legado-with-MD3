package io.legado.app.ui.config.translation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import io.legado.app.constant.PreferKey
import io.legado.app.ui.config.translation.TranslationConfigViewModel
import io.legado.app.ui.config.translation.model.TranslationConfigState
import io.legado.app.ui.widget.components.SplicedColumnGroup
import io.legado.app.ui.widget.components.settingItem.ClickableSettingItem
import io.legado.app.ui.widget.components.settingItem.DropdownListSettingItem
import io.legado.app.ui.widget.components.settingItem.SwitchSettingItem
import androidx.compose.ui.res.stringResource
import io.legado.app.R
import io.legado.app.utils.LiveEventBus

@Composable
fun TranslationConfigOptions(
    onNavigateToProviderList: () -> Unit
) {
    val viewModel: TranslationConfigViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    // 进入时主动刷新一次,然后订阅 providerConfigs 变化,任何地方修改配置都自动同步显示
    DisposableEffect(Unit) {
        viewModel.refreshActiveProvider()
        val sub = androidx.lifecycle.Observer<String> { viewModel.refreshActiveProvider() }
        LiveEventBus.get(PreferKey.translationProviderConfigs, String::class.java)
            .observeForever(sub)
        onDispose {
            LiveEventBus.get(PreferKey.translationProviderConfigs, String::class.java)
                .removeObserver(sub)
        }
    }

    TranslationConfigBody(
        state = state,
        onEnabledChange = viewModel::setEnabled,
        onTargetLanguageChange = viewModel::setTargetLanguage,
        onClickProviderList = onNavigateToProviderList
    )
}

@Composable
private fun TranslationConfigBody(
    state: TranslationConfigState,
    onEnabledChange: (Boolean) -> Unit,
    onTargetLanguageChange: (String) -> Unit,
    onClickProviderList: () -> Unit
) {
    SplicedColumnGroup {
        SwitchSettingItem(
            title = stringResource(R.string.enable_translation),
            checked = state.enabled,
            onCheckedChange = onEnabledChange
        )
        DropdownListSettingItem(
            title = stringResource(R.string.target_language),
            selectedValue = state.targetLanguage,
            displayEntries = state.targetLanguageDisplayEntries,
            entryValues = state.targetLanguageValues,
            onValueChange = onTargetLanguageChange
        )
        ClickableSettingItem(
            title = stringResource(R.string.translation_provider),
            description = if (state.activeProviderName.isNullOrBlank())
                stringResource(R.string.translation_provider_unset)
            else state.activeProviderName,
            onClick = onClickProviderList
        )
    }
}
