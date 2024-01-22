package ani.dantotsu.others

class LanguageMapper {
    companion object {

        fun mapLanguageCodeToName(code: String): String {
            return when (code) {
                "all" -> "Multi"
                "af" -> "Afrikaans"
                "am" -> "Amharic"
                "ar" -> "Arabic"
                "as" -> "Assamese"
                "az" -> "Azerbaijani"
                "be" -> "Belarusian"
                "bg" -> "Bulgarian"
                "bn" -> "Bengali"
                "bs" -> "Bosnian"
                "ca" -> "Catalan"
                "ceb" -> "Cebuano"
                "cs" -> "Czech"
                "da" -> "Danish"
                "de" -> "German"
                "el" -> "Greek"
                "en" -> "English"
                "en-Us" -> "English (United States)"
                "eo" -> "Esperanto"
                "es" -> "Spanish"
                "es-419" -> "Spanish (Latin America)"
                "et" -> "Estonian"
                "eu" -> "Basque"
                "fa" -> "Persian"
                "fi" -> "Finnish"
                "fil" -> "Filipino"
                "fo" -> "Faroese"
                "fr" -> "French"
                "ga" -> "Irish"
                "gn" -> "Guarani"
                "gu" -> "Gujarati"
                "ha" -> "Hausa"
                "he" -> "Hebrew"
                "hi" -> "Hindi"
                "hr" -> "Croatian"
                "ht" -> "Haitian Creole"
                "hu" -> "Hungarian"
                "hy" -> "Armenian"
                "id" -> "Indonesian"
                "ig" -> "Igbo"
                "is" -> "Icelandic"
                "it" -> "Italian"
                "ja" -> "Japanese"
                "jv" -> "Javanese"
                "ka" -> "Georgian"
                "kk" -> "Kazakh"
                "km" -> "Khmer"
                "kn" -> "Kannada"
                "ko" -> "Korean"
                "ku" -> "Kurdish"
                "ky" -> "Kyrgyz"
                "la" -> "Latin"
                "lb" -> "Luxembourgish"
                "lo" -> "Lao"
                "lt" -> "Lithuanian"
                "lv" -> "Latvian"
                "mg" -> "Malagasy"
                "mi" -> "Maori"
                "mk" -> "Macedonian"
                "ml" -> "Malayalam"
                "mn" -> "Mongolian"
                "mo" -> "Moldovan"
                "mr" -> "Marathi"
                "ms" -> "Malay"
                "mt" -> "Maltese"
                "my" -> "Burmese"
                "ne" -> "Nepali"
                "nl" -> "Dutch"
                "no" -> "Norwegian"
                "ny" -> "Chichewa"
                "pl" -> "Polish"
                "pt" -> "Portuguese"
                "pt-BR" -> "Portuguese (Brazil)"
                "pt-PT" -> "Portuguese (Portugal)"
                "ps" -> "Pashto"
                "ro" -> "Romanian"
                "rm" -> "Romansh"
                "ru" -> "Russian"
                "sd" -> "Sindhi"
                "sh" -> "Serbo-Croatian"
                "si" -> "Sinhala"
                "sk" -> "Slovak"
                "sl" -> "Slovenian"
                "sm" -> "Samoan"
                "sn" -> "Shona"
                "so" -> "Somali"
                "sq" -> "Albanian"
                "sr" -> "Serbian"
                "st" -> "Southern Sotho"
                "sv" -> "Swedish"
                "sw" -> "Swahili"
                "ta" -> "Tamil"
                "te" -> "Telugu"
                "tg" -> "Tajik"
                "th" -> "Thai"
                "ti" -> "Tigrinya"
                "tk" -> "Turkmen"
                "tl" -> "Tagalog"
                "to" -> "Tongan"
                "tr" -> "Turkish"
                "uk" -> "Ukrainian"
                "ur" -> "Urdu"
                "uz" -> "Uzbek"
                "vi" -> "Vietnamese"
                "yo" -> "Yoruba"
                "zh" -> "Chinese"
                "zh-Hans" -> "Chinese (Simplified)"
                "zh-Hant" -> "Chinese (Traditional)"
                "zh-Habt" -> "Chinese (Hakka)"
                "zu" -> "Zulu"
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

