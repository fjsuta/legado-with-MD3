package io.legado.app.ui.config.aiService

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.model.ai.AiModel
import io.legado.app.model.ai.AiProvider
import io.legado.app.model.ai.ApiMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomProviderConfigDialog(
    provider: AiProvider? = null,
    onDismiss: () -> Unit,
    onSave: (AiProvider) -> Unit
) {
    var name by remember { mutableStateOf(provider?.name ?: "") }
    var apiMode by remember { mutableStateOf(provider?.apiMode ?: ApiMode.OpenAI) }
    var apiKey by remember { mutableStateOf(provider?.apiKey ?: "") }
    var apiHost by remember { mutableStateOf(provider?.apiHost ?: "") }
    var apiPath by remember { mutableStateOf(provider?.apiPath ?: "/chat/completions") }
    var models by remember { mutableStateOf(provider?.models?.toMutableList() ?: mutableListOf()) }
    var showApiModeMenu by remember { mutableStateOf(false) }
    var editingModel by remember { mutableStateOf<AiModel?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (provider == null) stringResource(R.string.add_provider) else stringResource(R.string.edit_provider))
        },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.name)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    ExposedDropdownMenuBox(
                        expanded = showApiModeMenu,
                        onExpandedChange = { showApiModeMenu = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) {
                        OutlinedTextField(
                            value = apiMode.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.api_mode)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showApiModeMenu) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = showApiModeMenu,
                            onDismissRequest = { showApiModeMenu = false }
                        ) {
                            ApiMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.displayName) },
                                    onClick = {
                                        apiMode = mode
                                        showApiModeMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text(stringResource(R.string.api_key)) },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = apiHost,
                        onValueChange = { apiHost = it },
                        label = { Text(stringResource(R.string.api_host)) },
                        placeholder = { Text("https://api.example.com/v1") },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = apiPath,
                        onValueChange = { apiPath = it },
                        label = { Text(stringResource(R.string.api_path)) },
                        placeholder = { Text("/chat/completions") },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = stringResource(R.string.models))
                        IconButton(onClick = {
                            models.add(AiModel(modelId = "", displayName = ""))
                        }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.add))
                        }
                    }
                }

                items(models) { model ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            OutlinedTextField(
                                value = model.modelId,
                                onValueChange = {
                                    models[models.indexOf(model)] = model.copy(modelId = it)
                                },
                                label = { Text(stringResource(R.string.model_id)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = model.displayName ?: "",
                                onValueChange = {
                                    models[models.indexOf(model)] = model.copy(displayName = it.ifBlank { null })
                                },
                                label = { Text(stringResource(R.string.display_name)) },
                                placeholder = { Text(stringResource(R.string.optional)) },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                OutlinedTextField(
                                    value = model.contextWindow?.toString() ?: "",
                                    onValueChange = {
                                        models[models.indexOf(model)] = model.copy(contextWindow = it.toIntOrNull())
                                    },
                                    label = { Text(stringResource(R.string.context_window)) },
                                    placeholder = { Text("128000") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = model.maxOutputTokens?.toString() ?: "",
                                    onValueChange = {
                                        models[models.indexOf(model)] = model.copy(maxOutputTokens = it.toIntOrNull())
                                    },
                                    label = { Text(stringResource(R.string.max_tokens)) },
                                    placeholder = { Text("4096") },
                                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                                )
                            }
                            if (models.size > 1) {
                                IconButton(
                                    onClick = { models.remove(model) },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && apiKey.isNotBlank() && apiHost.isNotBlank()) {
                        val validModels = models.filter { it.modelId.isNotBlank() }
                        onSave(
                            AiProvider(
                                id = provider?.id ?: "",
                                name = name,
                                apiMode = apiMode,
                                apiKey = apiKey,
                                apiHost = apiHost,
                                apiPath = apiPath,
                                models = validModels
                            )
                        )
                    }
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}