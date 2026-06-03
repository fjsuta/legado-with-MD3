package io.legado.app.model.translation

/**
 * 统一的目标语言代码 -> 各服务实际使用的代码
 *
 * 用户界面语言代码: zh / en / ja / ko / fr / de / es / ru / ar
 */
object LanguageCodes {
    val IETF_CODES = listOf(
        "zh" to "zh-CN",
        "en" to "en",
        "ja" to "ja",
        "ko" to "ko",
        "fr" to "fr",
        "de" to "de",
        "es" to "es",
        "ru" to "ru",
        "ar" to "ar"
    )

    fun toIetf(lang: String): String =
        IETF_CODES.firstOrNull { it.first == lang }?.second ?: lang

    fun toBaidu(lang: String): String = when (lang) {
        "zh" -> "zh"
        "en" -> "en"
        "ja" -> "jp"
        "ko" -> "kor"
        "fr" -> "fra"
        "de" -> "de"
        "es" -> "spa"
        "ru" -> "ru"
        "ar" -> "ara"
        else -> lang
    }

    fun toYoudao(lang: String): String = when (lang) {
        "zh" -> "zh-CHS"
        else -> lang
    }

    fun toAzure(lang: String): String = when (lang) {
        "zh" -> "zh-Hans"
        else -> lang
    }

    fun toCaiyun(lang: String): String = when (lang) {
        "zh" -> "zh"
        "en" -> "en"
        "ja" -> "ja"
        else -> lang
    }

    fun toTencent(lang: String): String = when (lang) {
        "zh" -> "zh"
        "en" -> "en"
        "ja" -> "ja"
        "ko" -> "ko"
        "fr" -> "fr"
        "de" -> "de"
        "es" -> "es"
        "ru" -> "ru"
        "ar" -> "ar"
        else -> lang
    }

    fun toGroqName(lang: String): String = when (lang) {
        "zh" -> "Simplified Chinese"
        "en" -> "English"
        "ja" -> "Japanese"
        "ko" -> "Korean"
        "fr" -> "French"
        "de" -> "German"
        "es" -> "Spanish"
        "ru" -> "Russian"
        "ar" -> "Arabic"
        else -> lang
    }
}
