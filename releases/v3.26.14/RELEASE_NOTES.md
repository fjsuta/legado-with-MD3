# v3.26.14 Release Notes

> 发布时间: 2026-06-03
> Git tag: `v3.26.14`
> Commit:  `d52858a6`

## 更新内容

### 翻译系统重构
- 移除原 OpenAI 翻译接口
- 新增 10 个机翻服务,支持手动配置:
  - Google Translate(免密钥)
  - 火山机器翻译
  - 百度机器翻译
  - 腾讯云机器翻译
  - 有道智云
  - Azure 机器翻译(默认 `https://api.cognitive.microsofttranslator.com/`,支持 `region`、APIKEY、自定义 URL、启用富文本翻译)
  - Groq
  - 小牛翻译
  - 彩云小译
  - 阿里云机器翻译(带 `scene` 下拉)
- 每服务支持多实例(同服务不同账号)
- 每实例独立的 3 个通用字段:
  - `rateLimit` 每秒最大请求数(默认 1)
  - `maxChars` 每次请求最大文本长度(默认 1800)
  - `maxParas` 每次请求最大段落数(默认 8)
- 小牛、彩云、阿里云不显示通用字段
- 翻译调度时不需要选 active,谁配置完整就用谁,多个都用就取第一个
- "测试服务"功能:发 hello world 验证

### 界面调整
- "翻译设置"从设置页移到实验室
- "阅读界面"与"封面设置"整合到外观页

## 修复的 BUG

### 严重
1. **VolcanoSigner 签名格式错误** — `canonicalHeaders` 写成 `shortDate\nregion\nservice`,正确应为 `host:<v>\nx-date:<v>\n`,导致 401
2. **VolcanoProvider 缺 query 参数** — 需 `Action=TranslateText&Version=2020-06-01`
3. **VolcanoProvider 源语言写死 zh** — 改为 `auto`
4. **VolcanoSigner 默认 host 错误** — `open.volcengineapi.com` → `translate.volcengineapi.com`
5. **GoogleProvider 响应解析错误** — Google 免费接口响应是嵌套数组 `[[["trans","orig",...]],...]`,按 JSON 对象 + `sentences[0].trans` 解析必定失败,改为 `JsonParser` 提取 `res[0][i][0]`
6. **ProviderConfigViewModel 重复 UUID** — 每次 `currentConfig()` 都 `UUID.randomUUID()`,导致每打一个字就建一个新空实例,列表被冲爆
7. **ProviderConfigViewModel 写盘抖动** — 每次输入字符都触发 GSON 序列化 + SharedPreferences 写入,改用 `debounce(500ms)` 合并
8. **TranslationProvider 接口与实现参数不一致** — 接口加 `sourceLang` 第四参数,所有 10 个 Provider 适配
9. **TranslationAssembler 线程不安全** — `HashMap` 多协程并发写可能死循环,改 `ConcurrentHashMap`

### 中等
10. **FieldInputRow 无法清空输入框** — `value.ifEmpty { default }` 让清空后立即变回默认值
11. **TencentSigner `toLowerCase()` 废弃** — 改 `lowercase()`
12. **CaiyunProvider 不支持语言报错不友好** — 加显式 "暂不支持目标语言" 检查
13. **Azure/Baidu 错误响应丢失** — 把 HTTP code + body 写进异常
14. **目标语言列表不一致** — 统一为 9 种主流语言
15. **displayName/description 一致性** — `override val name` 应为 `displayName`
16. **UI 不响应 providerConfigs 变化** — `TranslationConfigOptions` 订阅 LiveEventBus
17. **refreshActiveProvider 阻塞主线程** — 改用 `viewModelScope + Dispatchers.IO`

### 改进
18. **LanguageCodes 漏 zh-Hant 映射** — 补全百度 `cht`、有道 `zh-CHT`、Azure `zh-Hant`、腾讯 `zh-TW`、彩云降级、Groq `Traditional Chinese`
19. **LanguageCodes.toYoudao 漏 ko/fr/de** — 已补全

## 下载

- **源代码 zip**: `releases/v3.26.14/source.zip` (8.5 MB,基于 `d52858a6`)
- GitHub 上 checkout tag: `git checkout v3.26.14`

## 本地构建

```bash
./gradlew :app:assembleAppDebug      # debug APK
./gradlew :app:assembleAppRelease    # release APK
```

要求: JDK 21+, Gradle 9.4.1(wrapper 自动下载),Android SDK 36。

## 提交记录

```
fce133e27 chore: 翻译系统 v3.26.14 release - 补充 zh-Hant/UI 响应式/版本号
cbdc7d0c3 fix: 修复翻译系统多个严重 BUG
76843aa51 fix: 修复翻译 Provider 配置的 viewModel 工厂与常量引用
23341cb39 feat: 检查 WebDav 备份无响应问题
df322a6e8 docs: 添加翻译系统重构设计文档
9e5263749 refactor: 将「阅读界面」「封面设置」移入外观设置页
d094b81b7 refactor: 将「翻译设置」入口从设置页移至实验室
```
