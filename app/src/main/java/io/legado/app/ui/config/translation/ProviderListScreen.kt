package io.legado.app.ui.config.translation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.legado.app.R
import io.legado.app.model.translation.ProviderConfigData
import io.legado.app.model.translation.ProviderRegistry
import io.legado.app.model.translation.TranslationProvider
import io.legado.app.ui.widget.components.AppScaffold
import io.legado.app.ui.widget.components.SplicedColumnGroup
import io.legado.app.ui.widget.components.settingItem.ClickableSettingItem
import io.legado.app.ui.widget.components.topbar.GlassMediumFlexibleTopAppBar
import io.legado.app.ui.widget.components.topbar.TopBarNavigationButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderListScreen(
    onBackClick: () -> Unit,
    onNavigateToProvider: (providerType: String, existingId: String?) -> Unit
) {
    AppScaffold(
        topBar = {
            GlassMediumFlexibleTopAppBar(
                title = stringResource(R.string.add_custom_translation),
                navigationIcon = { TopBarNavigationButton(onClick = onBackClick) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            val configs = TranslationConfig.providerConfigs
            val byType: Map<String, List<ProviderConfigData>> = configs.groupBy { it.type }

            ProviderRegistry.all().forEach { provider ->
                SplicedColumnGroup(title = provider.displayName) {
                    ClickableSettingItem(
                        title = stringResource(R.string.add_custom_translation),
                        description = "+",
                        onClick = { onNavigateToProvider(provider.type, null) }
                    )
                    byType[provider.type]?.forEach { instance ->
                        ClickableSettingItem(
                            title = instance.customName,
                            description = if (isInstanceConfigured(provider, instance))
                                stringResource(R.string.configured) else stringResource(R.string.not_configured),
                            onClick = { onNavigateToProvider(provider.type, instance.id) }
                        )
                    }
                }
            }
        }
    }
}

private fun isInstanceConfigured(
    provider: TranslationProvider,
    instance: ProviderConfigData
): Boolean {
    val required = provider.fields.filter { it.required }.map { it.key }
    return instance.allRequiredFilled(required)
}
