package io.legado.app.ui.config.aiService

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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

@Composable
fun EditModelDialog(
    model: AiModel,
    onDismiss: () -> Unit,
    onSave: (AiModel) -> Unit
) {
    var modelId by remember { mutableStateOf(model.modelId) }
    var displayName by remember { mutableStateOf(model.displayName ?: "") }
    var contextWindow by remember { mutableStateOf(model.contextWindow?.toString() ?: "") }
    var maxOutputTokens by remember { mutableStateOf(model.maxOutputTokens?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_model)) },
        text = {
            androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    OutlinedTextField(
                        value = modelId,
                        onValueChange = { modelId = it },
                        label = { Text(stringResource(R.string.model_id)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text(stringResource(R.string.display_name)) },
                        placeholder = { Text(stringResource(R.string.optional)) },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    )
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                        OutlinedTextField(
                            value = contextWindow,
                            onValueChange = { contextWindow = it },
                            label = { Text(stringResource(R.string.context_window)) },
                            placeholder = { Text("128000") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = maxOutputTokens,
                            onValueChange = { maxOutputTokens = it },
                            label = { Text(stringResource(R.string.max_tokens)) },
                            placeholder = { Text("4096") },
                            modifier = Modifier.weight(1f).padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (modelId.isNotBlank()) {
                        onSave(
                            AiModel(
                                id = model.id,
                                modelId = modelId,
                                displayName = displayName.ifBlank { null },
                                contextWindow = contextWindow.toIntOrNull(),
                                maxOutputTokens = maxOutputTokens.toIntOrNull()
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