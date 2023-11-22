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
                else -> ""
            }
        }
    }
}

