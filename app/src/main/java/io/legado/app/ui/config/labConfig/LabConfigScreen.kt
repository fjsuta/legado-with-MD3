package io.legado.app.ui.config.labConfig

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.model.ai.AiSearchMode
import io.legado.app.model.ai.AiServiceMode
import io.legado.app.ui.config.aiService.AiServiceConfig
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.theme.adaptiveContentPadding
import io.legado.app.ui.widget.components.AppScaffold
import io.legado.app.ui.widget.components.SplicedColumnGroup
import io.legado.app.ui.widget.components.settingItem.ClickableSettingItem
import io.legado.app.ui.widget.components.settingItem.SwitchSettingItem
import io.legado.app.ui.widget.components.settingItem.ToggleSettingItem
import io.legado.app.ui.widget.components.topbar.GlassMediumFlexibleTopAppBar
import io.legado.app.ui.widget.components.topbar.GlassTopAppBarDefaults
import io.legado.app.ui.widget.components.topbar.TopBarNavigationButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabConfigScreen(
    onBackClick: () -> Unit,
    onNavigateToAiServiceConfig: () -> Unit
) {
    val scrollBehavior = GlassTopAppBarDefaults.defaultScrollBehavior()

    AppScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            GlassMediumFlexibleTopAppBar(
                title = stringResource(R.string.lab_setting),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    TopBarNavigationButton(onClick = onBackClick)
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = adaptiveContentPadding(
                top = paddingValues.calculateTopPadding(),
                bottom = 120.dp
            )
        ) {
            item {
                SplicedColumnGroup {
                    SwitchSettingItem(
                        title = stringResource(R.string.lab_enabled_title),
                        description = stringResource(R.string.lab_enabled_summary),
                        checked = LabConfig.labEnabled,
                        onCheckedChange = { LabConfig.labEnabled = it }
                    )
                }

                AnimatedVisibility(visible = LabConfig.labEnabled) {
                    SplicedColumnGroup(title = stringResource(R.string.lab_display)) {
                        SwitchSettingItem(
                            title = stringResource(R.string.lab_eink_display_title),
                            description = stringResource(R.string.lab_eink_display_summary),
                            checked = LabConfig.eInkDisplay,
                            onCheckedChange = {
                                LabConfig.eInkDisplay = it
                            }
                        )

                        if (LabConfig.eInkDisplay) {
                            Text(
                                text = stringResource(R.string.lab_eink_display_hint),
                                style = LegadoTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = LabConfig.labEnabled) {
                    SplicedColumnGroup(title = stringResource(R.string.lab_ai_service)) {
                        SwitchSettingItem(
                            title = stringResource(R.string.lab_ai_service_title),
                            description = stringResource(R.string.lab_ai_service_summary),
                            checked = AiServiceConfig.enabled,
                            onCheckedChange = { AiServiceConfig.enabled = it }
                        )

                        AnimatedVisibility(visible = AiServiceConfig.enabled) {
                            ToggleSettingItem(
                                title = stringResource(R.string.lab_search_mode),
                                options = listOf(
                                    AiSearchMode.Traditional.name to AiSearchMode.Traditional.displayName,
                                    AiSearchMode.Ai.name to AiSearchMode.Ai.displayName
                                ),
                                selected = AiServiceConfig.searchMode,
                                onSelectedChange = { AiServiceConfig.searchMode = it }
                            )

                            ToggleSettingItem(
                                title = stringResource(R.string.lab_service_mode),
                                options = listOf(
                                    AiServiceMode.Builtin.name to AiServiceMode.Builtin.displayName,
                                    AiServiceMode.Custom.name to AiServiceMode.Custom.displayName
                                ),
                                selected = AiServiceConfig.serviceMode,
                                onSelectedChange = { AiServiceConfig.serviceMode = it }
                            )

                            ClickableSettingItem(
                                title = stringResource(R.string.lab_ai_config),
                                onClick = onNavigateToAiServiceConfig
                            )
                        }
                    }
                }
            }
        }
    }
}