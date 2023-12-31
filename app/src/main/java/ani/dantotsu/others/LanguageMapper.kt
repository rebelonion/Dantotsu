package ani.dantotsu.others

class LanguageMapper {
    companion object {

        fun mapLanguageCodeToName(code: String): String {
            return when (code) {
                "all" -> "Multi"
                "ar" -> "Arabic"
                "de" -> "German"
                "en" -> "English"
                "es" -> "Spanish"
                "fr" -> "French"
                "id" -> "Indonesian"
                "it" -> "Italian"
                "ja" -> "Japanese"
                "ko" -> "Korean"
                "pl" -> "Polish"
                "pt-BR" -> "Portuguese (Brazil)"
                "ru" -> "Russian"
                "th" -> "Thai"
                "tr" -> "Turkish"
                "uk" -> "Ukrainian"
                "vi" -> "Vietnamese"
                "zh" -> "Chinese"
                "zh-Hans" -> "Chinese (Simplified)"
                "es-419" -> "Spanish (Latin America)"
                "hu" -> "Hungarian"
                "zh-habt" -> "Chinese (Hakka)"
                "zh-hant" -> "Chinese (Traditional)"
                "ca" -> "Catalan"
                "bg" -> "Bulgarian"
                "fa" -> "Persian"
                "mn" -> "Mongolian"
                "ro" -> "Romanian"
                "he" -> "Hebrew"
                "ms" -> "Malay"
                "tl" -> "Tagalog"
                "hi" -> "Hindi"
                "my" -> "Burmese"
                "cs" -> "Czech"
                "pt" -> "Portuguese"
                "nl" -> "Dutch"
                "sv" -> "Swedish"
                "bn" -> "Bengali"
                "no" -> "Norwegian"
                "el" -> "Greek"
                "sr" -> "Serbian"
                "da" -> "Danish"
                "lt" -> "Lithuanian"
                "ml" -> "Malayalam"
                "mr" -> "Marathi"
                "ta" -> "Tamil"
                "te" -> "Telugu"
                else -> code
            }
        }

        enum class Language(val code: String) {
            ALL("all"),
            ARABIC("ar"),
            GERMAN("de"),
            ENGLISH("en"),
            SPANISH("es"),
            FRENCH("fr"),
            INDONESIAN("id"),
            ITALIAN("it"),
            JAPANESE("ja"),
            KOREAN("ko"),
            POLISH("pl"),
            PORTUGUESE_BRAZIL("pt-BR"),
            RUSSIAN("ru"),
            THAI("th"),
            TURKISH("tr"),
            UKRAINIAN("uk"),
            VIETNAMESE("vi"),
            CHINESE("zh"),
            CHINESE_SIMPLIFIED("zh-Hans")
        }
    }
}

