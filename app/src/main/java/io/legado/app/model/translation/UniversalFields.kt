package io.legado.app.model.translation

/**
 * 默认通用字段定义(限速/批量),供 ViewModel 等非 Composable 上下文读取。
 * 实际 UI 渲染由 ProviderConfigScreen 在 Composable 中根据 stringResource 重新构造。
 */
val DEFAULT_UNIVERSAL_FIELDS: List<ProviderField> = listOf(
    ProviderField(
        key = "rateLimit",
        label = "Requests per second",
        type = FieldType.NUMBER,
        default = "1"
    ),
    ProviderField(
        key = "maxChars",
        label = "Max chars per request",
        type = FieldType.NUMBER,
        default = "1800"
    ),
    ProviderField(
        key = "maxParas",
        label = "Max paragraphs per request",
        type = FieldType.NUMBER,
        default = "8"
    )
)
