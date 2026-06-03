package io.legado.app.ui.config.translation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import io.legado.app.ui.config.translation.TranslationConfigViewModel
import io.legado.app.ui.config.translation.model.TranslationConfigState
import io.legado.app.ui.widget.components.SplicedColumnGroup
import io.legado.app.ui.widget.components.settingItem.ClickableSettingItem
import io.legado.app.ui.widget.components.settingItem.DropdownListSettingItem
import io.legado.app.ui.widget.components.settingItem.SwitchSettingItem
import androidx.compose.ui.res.stringResource
import io.legado.app.R

@Composable
fun TranslationConfigOptions(
    onNavigateToProviderList: () -> Unit
) {
    val viewModel: TranslationConfigViewModel = viewModel()
    val state by viewModel.state.collectAsState()
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
