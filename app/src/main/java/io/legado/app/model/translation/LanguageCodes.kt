package io.legado.app.model.translation

/**
 * 统一的目标语言代码 -> 各服务实际使用的代码
 */
object LanguageCodes {
    val IETF_CODES = listOf(
        "zh" to "zh-CN",
        "zh-Hant" to "zh-TW",
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
        "zh-Hant" -> "cht"
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
        "zh-Hant" -> "zh-CHT"
        "ko" -> "ko"
        "fr" -> "fr"
        "de" -> "de"
        else -> lang
    }

    fun toAzure(lang: String): String = when (lang) {
        "zh" -> "zh-Hans"
        "zh-Hant" -> "zh-Hant"
        "fr" -> "fr"
        "de" -> "de"
        else -> lang
    }

    fun toCaiyun(lang: String): String = when (lang) {
        "zh" -> "zh"
        "zh-Hant" -> "zh"
        "en" -> "en"
        "ja" -> "ja"
        else -> lang
    }

    fun toTencent(lang: String): String = when (lang) {
        "zh" -> "zh"
        "zh-Hant" -> "zh-TW"
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
        "zh-Hant" -> "Traditional Chinese"
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
