package io.legado.app.ui.config.translation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.legado.app.R
import io.legado.app.model.translation.FieldType
import io.legado.app.model.translation.ProviderField
import io.legado.app.ui.widget.components.AppScaffold
import io.legado.app.ui.widget.components.SplicedColumnGroup
import io.legado.app.ui.widget.components.settingItem.ClickableSettingItem
import io.legado.app.ui.widget.components.settingItem.DropdownListSettingItem
import io.legado.app.ui.widget.components.settingItem.SwitchSettingItem
import io.legado.app.ui.widget.components.topbar.GlassMediumFlexibleTopAppBar
import io.legado.app.ui.widget.components.topbar.TopBarNavigationButton
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderConfigScreen(
    providerType: String,
    existingId: String?,
    onBackClick: () -> Unit,
    viewModel: ProviderConfigViewModel = viewModel {
        ProviderConfigViewModel(providerType, existingId)
    }
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showTestResult by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    AppScaffold(
        topBar = {
            GlassMediumFlexibleTopAppBar(
                title = state.customName,
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
            // 描述
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    text = state.description,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        R.string.provider_need_secret,
                        state.displayName
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            // 文档链接
            if (!state.docUrl.isNullOrBlank()) {
                SplicedColumnGroup {
                    ClickableSettingItem(
                        title = stringResource(R.string.key_tutorial),
                        description = state.docUrl,
                        onClick = {
                            runCatching {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(state.docUrl)
                                )
                                context.startActivity(intent)
                            }
                        }
                    )
                }
            }

            // 自定义名称
            SplicedColumnGroup {
                FieldInputRow(
                    field = ProviderField(
                        key = "customName",
                        label = stringResource(R.string.custom_provider_name),
                        placeholder = stringResource(R.string.custom_provider_name),
                        type = FieldType.TEXT
                    ),
                    value = state.customName,
                    onValueChange = viewModel::setCustomName
                )
            }

            // 服务字段
            if (state.fields.isNotEmpty()) {
                SplicedColumnGroup {
                    state.fields.forEach { field ->
                        FieldRow(
                            field = field,
                            value = state.fieldsMap[field.key].orEmpty(),
                            onValueChange = { viewModel.setField(field.key, it) }
                        )
                    }
                }
            }

            // 通用字段
            if (state.showUniversalFields) {
                UniversalFieldsSection(
                    fieldsMap = state.fieldsMap,
                    onValueChange = viewModel::setField
                )
            }

            // 测试服务
            SplicedColumnGroup {
                ClickableSettingItem(
                    title = stringResource(R.string.test_service),
                    onClick = {
                        scope.launch {
                            val result = viewModel.test()
                            showTestResult = result.fold(
                                onSuccess = { "测试成功: $it" },
                                onFailure = { "测试失败: ${it.message}" }
                            )
                        }
                    }
                )
            }

            // 底部操作
            SplicedColumnGroup {
                ClickableSettingItem(
                    title = stringResource(R.string.reset_to_default),
                    onClick = { viewModel.resetToDefault() }
                )
                if (existingId != null) {
                    ClickableSettingItem(
                        title = stringResource(R.string.delete),
                        onClick = { showDeleteDialog = true }
                    )
                }
            }
        }
    }

    showTestResult?.let { msg ->
        AlertDialog(
            onDismissRequest = { showTestResult = null },
            title = { Text(stringResource(R.string.test_result)) },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { showTestResult = null }) { Text("OK") }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.confirm_delete)) },
            text = { Text(stringResource(R.string.confirm_delete_provider, state.customName)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete()
                    showDeleteDialog = false
                    onBackClick()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun UniversalFieldsSection(
    fieldsMap: Map<String, String>,
    onValueChange: (String, String) -> Unit
) {
    SplicedColumnGroup(title = stringResource(R.string.universal_settings)) {
        universalFields().forEach { field ->
            FieldRow(
                field = field,
                value = fieldsMap[field.key].orEmpty(),
                onValueChange = { onValueChange(field.key, it) }
            )
        }
    }
}

@Composable
private fun FieldRow(
    field: ProviderField,
    value: String,
    onValueChange: (String) -> Unit
) {
    when (field.type) {
        FieldType.TEXT, FieldType.PASSWORD, FieldType.NUMBER -> {
            FieldInputRow(
                field = field,
                value = value,
                onValueChange = onValueChange
            )
        }
        FieldType.TOGGLE -> {
            SwitchSettingItem(
                title = field.label,
                description = field.description,
                checked = value == "true" || (value.isEmpty() && field.default == "true"),
                onCheckedChange = { onValueChange(if (it) "true" else "false") }
            )
        }
        FieldType.SELECT -> {
            val options = field.options.orEmpty()
            val entryLabels = options.map { it.second }.toTypedArray()
            val entryValues = options.map { it.first }.toTypedArray()
            val current = value.ifEmpty { field.default.orEmpty() }
            DropdownListSettingItem(
                title = field.label,
                description = field.description,
                selectedValue = current,
                displayEntries = entryLabels,
                entryValues = entryValues,
                onValueChange = onValueChange
            )
        }
    }
}

@Composable
private fun universalFields(): List<ProviderField> = listOf(
    ProviderField(
        key = "rateLimit",
        label = stringResource(R.string.universal_rate_limit),
        type = FieldType.NUMBER,
        default = "1",
        description = stringResource(R.string.universal_rate_limit_desc)
    ),
    ProviderField(
        key = "maxChars",
        label = stringResource(R.string.universal_max_chars),
        type = FieldType.NUMBER,
        default = "1800",
        description = stringResource(R.string.universal_max_chars_desc)
    ),
    ProviderField(
        key = "maxParas",
        label = stringResource(R.string.universal_max_paras),
        type = FieldType.NUMBER,
        default = "8",
        description = stringResource(R.string.universal_max_paras_desc)
    )
)
