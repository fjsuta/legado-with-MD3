# 翻译系统重构:机翻多服务多实例

- 日期:2026-06-02
- 作者:Claude
- 状态:已设计,待实现

## 1. 背景与目标

当前翻译子系统(`LlmGateway` / `LlmTranslateRepositoryImpl`)只支持两个提供商:Google 免费版与 OpenAI 兼容接口。UI 用一个下拉选择,baseUrl/apiKey/model 几个全局字段。

目标:
- 移除 OpenAI 接口(原 LLM 翻译只保留 Google)
- 接入 9 个机翻服务(Groq 是 LLM 但和机翻一起展示)
- 每个服务可添加多个实例(同一种服务不同账号)
- 每个服务独立的认证字段(AccessKey、appId、token 等)
- 每个实例独立的限速/批量参数
- 提供「测试服务」按钮

## 2. 范围

### 2.1 接入的服务(10 个)

| 服务 | type | 字段 | 通用字段 |
|---|---|---|---|
| Google Translate | `google` | (无,免配置) | ✅ |
| 火山机器翻译 | `volcano` | accessKeyId, accessKeySecret | ✅ |
| 百度机器翻译 | `baidu` | appId, secretKey | ✅ |
| 腾讯云机器翻译 | `tencent` | secretId, secretKey | ✅ |
| 有道智云 | `youdao` | appKey, appSecret | ✅ |
| Azure 机器翻译 | `azure` | region, apiKey, baseUrl(默认), enableRichText | ✅ |
| Groq | `groq` | apiKey, model, (可选 baseUrl) | ✅ |
| **小牛翻译** | `xiaoniu` | apiKey | ❌ |
| **彩云小译** | `caiyun` | token | ❌ |
| **阿里云机器翻译** | `aliyun` | accessKeyId, accessKeySecret, scene(SELECT) | ❌ |

### 2.2 通用字段(每实例独立,3 个)

只有 `showUniversalFields == true` 的服务才在配置页显示这 3 项:

- `rateLimit` (NUMBER, 默认 1) — 每秒最大请求数
- `maxChars` (NUMBER, 默认 1800) — 每次请求最大文本长度
- `maxParas` (NUMBER, 默认 8) — 每次请求最大段落数

### 2.3 移除的功能

- 现有 `OpenAI 适配接口` 翻译(下拉里也删除)
- 现有 `llmProvider`、`llmBaseUrl`、`llmApiKey`、`llmModel`、`llmTemperature`、`llmPrompt`、`llmConcurrentChunks`、`llmMaxCharsPerChunk` 8 个 `PreferKey` 字段全部废弃
- 现有 `TranslationConstants.kt` 里的 provider 常量删除

### 2.4 保留的功能

- `llmTranslateEnabled` 开关(总开关)
- `llmTargetLanguage` 目标语言
- Google Translate 逻辑(迁移到新 Provider 模型)

## 3. 架构

```
app/src/main/java/io/legado/app/model/translation/
  ProviderRegistry.kt              # 启动时注册 10 个 provider
  providers/
    GoogleProvider.kt              # 已有逻辑迁过来
    VolcanoProvider.kt
    BaiduProvider.kt
    TencentProvider.kt
    YoudaoProvider.kt
    AzureProvider.kt
    GroqProvider.kt
    XiaoniuProvider.kt
    CaiyunProvider.kt
    AliyunProvider.kt
  signer/
    BaiduSigner.kt                 # MD5(appId+q+salt+secretKey)
    TencentSigner.kt               # TC3-HMAC-SHA256
    YoudaoSigner.kt                # SHA256
    VolcanoSigner.kt               # Volcengine 签名
  RateLimiter.kt                   # 令牌桶,按 rateLimit 限速
  ChunkSplitter.kt                 # 按 maxChars / maxParas 切分

app/src/main/java/io/legado/app/ui/config/translation/
  TranslationConfig.kt             # 改造成读写 JSON 列表
  TranslationConfigScreen.kt       # 翻译设置首页
  ProviderListScreen.kt            # "添加自定义翻译服务"列表
  ProviderConfigScreen.kt          # 通用单服务配置页
  ProviderListViewModel.kt
  ProviderConfigViewModel.kt
```

## 4. 核心接口

```kotlin
// 模型
enum class FieldType { TEXT, PASSWORD, NUMBER, TOGGLE, SELECT }

data class ProviderField(
    val key: String,
    val label: String,
    val placeholder: String,
    val type: FieldType,
    val default: String? = null,
    val required: Boolean = true,
    val description: String? = null,
    val options: List<Pair<String, String>>? = null  // SELECT 用
)

@Serializable
data class ProviderConfigData(
    val id: String = UUID.randomUUID().toString(),
    val type: String,                // provider.type
    val customName: String,          // "阿里云机器翻译 1"
    val fields: Map<String, String>, // 服务字段 + 通用字段
    val createdAt: Long = System.currentTimeMillis()
)

interface TranslationProvider {
    val type: String
    val displayName: String
    val description: String
    val docUrl: String?
    val fields: List<ProviderField>
    val showUniversalFields: Boolean get() = true
    suspend fun translate(
        config: ProviderConfigData,
        text: String,
        targetLang: String
    ): Result<String>
}
```

## 5. 数据存储

所有 provider 实例存储为 JSON 列表,放在单个 `PreferKey` 下:

```kotlin
// PreferKey.kt
const val translationProviderConfigs = "translationProviderConfigs"
// 存储形如:[{"id":"...","type":"azure","customName":"Azure 1","fields":{...}}]

object TranslationConfig {
    // 替代现有 llmBaseUrl/llmApiKey 等
    var providerConfigs by prefDelegate(
        PreferKey.translationProviderConfigs,
        "[]"  // 空 JSON 列表
    ) { postEvent(PreferKey.translationProviderConfigs, it) }

    // 保留字段
    var enabled by prefDelegate(PreferKey.llmTranslateEnabled, false) {
        postEvent(PreferKey.llmTranslateEnabled, it)
    }
    var targetLanguage by prefDelegate(PreferKey.llmTargetLanguage, "zh") {
        postEvent(PreferKey.llmTargetLanguage, it)
    }
}
```

## 6. UI 结构(3 层)

```
Lab 入口(已有,不动)
  └─ 翻译设置项(已在 LabConfigScreen 中)
       └─ TranslationConfigScreen (设置首页)
            ├─ 总开关
            ├─ 目标语言下拉
            └─ 「提供商」分组(替换原"提供商"下拉)
                 └─→ ProviderListScreen (服务列表)
                      ├─ Google 行(永远在第一位,免配置)
                      ├─ 9 个新服务行(无配置时显示「+ 添加」)
                      ├─ 已配置的实例行(可重命名/编辑/删除)
                      └─ + 添加按钮
                       └─→ ProviderConfigScreen (单服务配置页)
                            ├─ 服务描述(含「密钥申请和配置教程」外链)
                            ├─ 警告:"需要填写密钥后才可用"
                            ├─ 自定义翻译服务名称输入
                            ├─ 服务字段(根据 provider.fields 渲染)
                            ├─ [可选] 通用 3 字段
                            ├─ 「点此测试服务」按钮
                            ├─ 底部按钮:恢复默认设置 / 删除
```

## 7. 翻译调度流程

```
TranslationManager.startTranslation(text, targetLang)
  ↓
  ProviderRegistry.loadConfigs() → List<ProviderConfigData>
  ↓
  找到第一个所有 required 字段非空的实例
  ↓
  实例化对应 TranslationProvider
  ↓
  ChunkSplitter.split(text, maxChars, maxParas) → List<chunk>
  ↓
  for each chunk:
    RateLimiter.acquire(rateLimit)
    provider.translate(config, chunk, targetLang)
  ↓
  拼接结果,写回缓存
```

## 8. 各服务实现要点

| 服务 | 端点 | 鉴权 | 备注 |
|---|---|---|---|
| Google | `https://translate.googleapis.com/translate_a/single` | 无 | 已有,迁移 |
| 火山 | `https://translate.volcengine.com/` | Volcengine 签名 | 参考官方 SDK 签名 |
| 百度 | `https://fanyi-api.baidu.com/api/trans/vip/translate` | appId + MD5(q+appId+salt+secretKey) | q 是 url encoded |
| 腾讯 | `https://tmt.tencentcloudapi.com/` | TC3-HMAC-SHA256 | 复杂,单独文件 |
| 有道 | `https://openapi.youdao.com/api` | appKey + SHA256(应用名+input+盐+应用名+应用密钥+appKey) | curtime 必须传 |
| Azure | `{baseUrl}/translate?api-version=3.0` | Ocp-Apim-Subscription-Key + Ocp-Apim-Subscription-Region header | body 为 `[{Text:"..."}]` |
| Groq | `https://api.groq.com/openai/v1/chat/completions` | Bearer token | OpenAI 兼容 |
| 小牛 | `https://api.niutrans.com/NiuTransServer/translation` | `?apikey=...` | GET 即可 |
| 彩云 | `https://api.interpreter.caiyunai.com/v1/translator` | Bearer token | body `{"source":[...], "trans_type":..., "request_id":"..."}` |
| 阿里云 | `https://mt.aliyuncs.com/` | Aliyun Signature v3 (HMAC-SHA256) | action=TranslateGeneral, scene 来自 fields |

## 9. 现有字段迁移

| 旧字段 | 处理 |
|---|---|
| `llmProvider` | 删除引用;TranslationConfig 内部不再使用 |
| `llmBaseUrl`、`llmApiKey`、`llmModel` | 废弃;Groq 用户需重新配置 |
| `llmTemperature`、`llmPrompt` | 废弃 |
| `llmConcurrentChunks`、`llmMaxCharsPerChunk` | 废弃;改为每个实例自己的 rateLimit/maxChars/maxParas |
| `llmTranslateEnabled` | 保留 |
| `llmTargetLanguage` | 保留 |

不主动迁移旧 OpenAI 配置 — 用户需要重新添加 Groq 或其他服务。

## 10. 测试

- 每个 provider 实现一个最小的烟雾测试:发个 "hello world" 看是否成功返回中文
- 限速器:在 `RateLimiterTest` 单元测试中验证 1 req/sec 的行为
- 切分器:在 `ChunkSplitterTest` 中验证 maxChars/maxParas 边界

## 11. 待确认点

1. 目标语言:维持现有 9 种(中英日韩法德西俄阿),还是扩展?  
   建议:保持 9 种,服务不支持时报错
2. 旧 OpenAI 配置:直接丢弃 / 自动迁移成 Groq 草稿?  
   建议:直接丢弃
3. 测试服务:仅对当前实例的 4 个字段发个 hello,成功显示 toast?  
   建议:实现
4. 缓存键:加 provider.type 进 key,避免不同服务的翻译串味?  
   建议:加
