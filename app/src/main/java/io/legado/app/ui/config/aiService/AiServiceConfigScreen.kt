package io.legado.app.ui.config.aiService

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.model.ai.AiModel
import io.legado.app.model.ai.AiProvider
import io.legado.app.model.ai.AiServiceMode
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.theme.adaptiveContentPadding
import io.legado.app.ui.widget.components.AppScaffold
import io.legado.app.ui.widget.components.SplicedColumnGroup
import io.legado.app.ui.widget.components.settingItem.ToggleSettingItem
import io.legado.app.ui.widget.components.topbar.GlassMediumFlexibleTopAppBar
import io.legado.app.ui.widget.components.topbar.GlassTopAppBarDefaults
import io.legado.app.ui.widget.components.topbar.TopBarNavigationButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiServiceConfigScreen(
    onBackClick: () -> Unit
) {
    val scrollBehavior = GlassTopAppBarDefaults.defaultScrollBehavior()
    var showAddProviderDialog by remember { mutableStateOf(false) }
    var editingProvider by remember { mutableStateOf<AiProvider?>(null) }
    var editingModel by remember { mutableStateOf<Pair<AiProvider, AiModel>?>(null) }

    AppScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            GlassMediumFlexibleTopAppBar(
                title = stringResource(R.string.lab_ai_config),
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
                    ToggleSettingItem(
                        title = stringResource(R.string.lab_service_mode),
                        options = listOf(
                            AiServiceMode.Builtin.name to AiServiceMode.Builtin.displayName,
                            AiServiceMode.Custom.name to AiServiceMode.Custom.displayName
                        ),
                        selected = AiServiceConfig.serviceMode,
                        onSelectedChange = { AiServiceConfig.serviceMode = it }
                    )
                }

                AnimatedVisibility(visible = AiServiceConfig.serviceMode == AiServiceMode.Builtin.name) {
                    BuiltinConfigSection()
                }

                AnimatedVisibility(visible = AiServiceConfig.serviceMode == AiServiceMode.Custom.name) {
                    CustomConfigSection(
                        onAddProvider = { showAddProviderDialog = true },
                        onEditProvider = { editingProvider = it },
                        onDeleteProvider = { AiServiceConfig.deleteCustomProvider(it) },
                        onEditModel = { provider, model -> editingModel = provider to model }
                    )
                }
            }
        }
    }

    if (showAddProviderDialog) {
        CustomProviderConfigDialog(
            onDismiss = { showAddProviderDialog = false },
            onSave = { provider ->
                AiServiceConfig.upsertCustomProvider(provider)
                showAddProviderDialog = false
            }
        )
    }

    editingProvider?.let { provider ->
        CustomProviderConfigDialog(
            provider = provider,
            onDismiss = { editingProvider = null },
            onSave = { updated ->
                AiServiceConfig.upsertCustomProvider(updated)
                editingProvider = null
            }
        )
    }

    editingModel?.let { (provider, model) ->
        EditModelDialog(
            model = model,
            onDismiss = { editingModel = null },
            onSave = { updatedModel ->
                val updatedProvider = provider.copy(
                    models = provider.models.map { if (it.id == model.id) updatedModel else it }
                )
                AiServiceConfig.upsertCustomProvider(updatedProvider)
                editingModel = null
            }
        )
    }
}

@Composable
private fun BuiltinConfigSection() {
    SplicedColumnGroup(title = stringResource(R.string.lab_ai_service)) {
        val providerNames = AiServiceConfig.BuiltinProviders.values().associate {
            it.name to it.displayName
        }

        ToggleSettingItem(
            title = stringResource(R.string.lab_ai_provider),
            options = providerNames.toList(),
            selected = AiServiceConfig.builtinProvider,
            onSelectedChange = {
                AiServiceConfig.builtinProvider = it
                val provider = AiServiceConfig.getBuiltinProvider()
                AiServiceConfig.builtinModelId = provider?.models?.firstOrNull()?.modelId ?: ""
            }
        )

        val currentProvider = AiServiceConfig.getBuiltinProvider()
        currentProvider?.let { provider ->
            val modelOptions = provider.models.associate { it.id to it.displayNameOrId() }
            if (modelOptions.isNotEmpty()) {
                ToggleSettingItem(
                    title = stringResource(R.string.lab_ai_model),
                    options = modelOptions.toList(),
                    selected = AiServiceConfig.builtinModelId.ifBlank { provider.models.firstOrNull()?.id ?: "" },
                    onSelectedChange = { AiServiceConfig.builtinModelId = it }
                )
            }
        }
    }
}

@Composable
private fun CustomConfigSection(
    onAddProvider: () -> Unit,
    onEditProvider: (AiProvider) -> Unit,
    onDeleteProvider: (String) -> Unit,
    onEditModel: (AiProvider, AiModel) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.lab_ai_custom_providers),
                style = LegadoTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onAddProvider) {
                Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.add))
            }
        }

        val providers = AiServiceConfig.customProviders
        if (providers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.lab_ai_no_providers),
                    style = LegadoTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(providers) { provider ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = provider.name,
                                        style = LegadoTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = provider.apiMode.displayName,
                                        style = LegadoTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                Row {
                                    IconButton(onClick = { onEditProvider(provider) }) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                                    }
                                    IconButton(onClick = { onDeleteProvider(provider.id) }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                                    }
                                }
                            }

                            if (provider.models.isNotEmpty()) {
                                Text(
                                    text = stringResource(R.string.lab_ai_models),
                                    style = LegadoTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                                )
                                LazyColumn {
                                    items(provider.models) { model ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onEditModel(provider, model) }
                                                .padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(text = model.displayNameOrId())
                                                if (model.contextWindow != null || model.maxOutputTokens != null) {
                                                    Text(
                                                        text = buildString {
                                                            if (model.contextWindow != null) {
                                                                append("Context: ${model.contextWindow}")
                                                            }
                                                            if (model.maxOutputTokens != null) {
                                                                if (model.contextWindow != null) append(" | ")
                                                                append("Max: ${model.maxOutputTokens}")
                                                            }
                                                        },
                                                        style = LegadoTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = stringResource(R.string.edit),
                                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}