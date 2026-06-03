package io.legado.app.ui.config.translation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.legado.app.model.translation.FieldType
import io.legado.app.model.translation.ProviderField

/**
 * 单个服务配置字段的可编辑行
 * - TEXT/PASSWORD/NUMBER 用 OutlinedTextField
 * - TOGGLE/SELECT 由 caller 用现有 SwitchSettingItem/DropdownListSettingItem 渲染
 */
@Composable
fun FieldInputRow(
    field: ProviderField,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPassword by remember { mutableStateOf(false) }
    val isPassword = field.type == FieldType.PASSWORD

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(
            text = field.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (!field.description.isNullOrBlank()) {
            Text(
                text = field.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // 直接绑定 value,允许用户清空输入框。placeholder 显示在空值时作为提示。
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            placeholder = { Text(field.default ?: field.placeholder) },
            visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = if (field.type == FieldType.NUMBER) KeyboardType.Number else KeyboardType.Text
            ),
            trailingIcon = if (isPassword) {
                {
                    androidx.compose.material3.TextButton(onClick = { showPassword = !showPassword }) {
                        Text(if (showPassword) "隐藏" else "显示")
                    }
                }
            } else null
        )
    }
}
