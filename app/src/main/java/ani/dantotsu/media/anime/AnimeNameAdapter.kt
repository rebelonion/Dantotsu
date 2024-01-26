package ani.dantotsu.media.anime

import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern

class AnimeNameAdapter {
    companion object {
        const val episodeRegex =
            "(episode|ep|e)[\\s:.\\-]*([\\d]+\\.?[\\d]*)[\\s:.\\-]*\\(?\\s*(sub|subbed|dub|dubbed)*\\s*\\)?\\s*"
        const val failedEpisodeNumberRegex =
            "(?<!part\\s)\\b(\\d+)\\b"
        const val seasonRegex = "(season|s)[\\s:.\\-]*(\\d+)[\\s:.\\-]*"
        const val subdubRegex = "^(soft)?[\\s-]*(sub|dub|mixed)(bed|s)?\\s*$"

        fun setSubDub(text: String, typeToSetTo: SubDubType): String? {
            val subdubPattern: Pattern = Pattern.compile(subdubRegex, Pattern.CASE_INSENSITIVE)
            val subdubMatcher: Matcher = subdubPattern.matcher(text)

            return if (subdubMatcher.find()) {
                val soft = subdubMatcher.group(1)
                val subdub = subdubMatcher.group(2)
                val bed = subdubMatcher.group(3) ?: ""

                val toggled = when (typeToSetTo) {
                    SubDubType.SUB -> "sub"
                    SubDubType.DUB -> "dub"
                    SubDubType.NULL -> ""
                }
                val toggledCasePreserved =
                    if (subdub?.get(0)?.isUpperCase() == true || soft?.get(0)
                            ?.isUpperCase() == true
                    ) toggled.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(
                            Locale.ROOT
                        ) else it.toString()
                    } else toggled

                subdubMatcher.replaceFirst(toggledCasePreserved + bed)
            } else {
                null
            }
        }

        fun getSubDub(text: String): SubDubType {
            val subdubPattern: Pattern = Pattern.compile(subdubRegex, Pattern.CASE_INSENSITIVE)
            val subdubMatcher: Matcher = subdubPattern.matcher(text)

            return if (subdubMatcher.find()) {
                val subdub = subdubMatcher.group(2)?.lowercase(Locale.ROOT)
                when (subdub) {
                    "sub" -> SubDubType.SUB
                    "dub" -> SubDubType.DUB
                    else -> SubDubType.NULL
                }
            } else {
                SubDubType.NULL
            }
        }

        enum class SubDubType {
            SUB, DUB, NULL
        }

        fun findSeasonNumber(text: String): Int? {
            val seasonPattern: Pattern = Pattern.compile(seasonRegex, Pattern.CASE_INSENSITIVE)
            val seasonMatcher: Matcher = seasonPattern.matcher(text)

            return if (seasonMatcher.find()) {
                seasonMatcher.group(2)?.toInt()
            } else {
                null
            }
        }

        fun findEpisodeNumber(text: String): Float? {
            val episodePattern: Pattern = Pattern.compile(episodeRegex, Pattern.CASE_INSENSITIVE)
            val episodeMatcher: Matcher = episodePattern.matcher(text)

            return if (episodeMatcher.find()) {
                if (episodeMatcher.group(2) != null) {
                    episodeMatcher.group(2)?.toFloat()
                } else {
                    val failedEpisodeNumberPattern: Pattern =
                        Pattern.compile(failedEpisodeNumberRegex, Pattern.CASE_INSENSITIVE)
                    val failedEpisodeNumberMatcher: Matcher =
                        failedEpisodeNumberPattern.matcher(text)
                    if (failedEpisodeNumberMatcher.find()) {
                        failedEpisodeNumberMatcher.group(1)?.toFloat()
                    } else {
                        null
                    }
                }
            } else {
                null
            }
        }

        fun removeEpisodeNumber(text: String): String {
            val regexPattern = Regex(episodeRegex, RegexOption.IGNORE_CASE)
            val removedNumber = text.replace(regexPattern, "").ifEmpty {
                text
            }
            val letterPattern = Regex("[a-zA-Z]")
            return if (letterPattern.containsMatchIn(removedNumber)) {
                removedNumber
            } else {
                text
            }
        }


        fun removeEpisodeNumberCompletely(text: String): String {
            val regexPattern = Regex(episodeRegex, RegexOption.IGNORE_CASE)
            val removedNumber = text.replace(regexPattern, "")
            return if (removedNumber.equals(text, true)) {  // if nothing was removed
                val failedEpisodeNumberPattern: Regex =
                    Regex(failedEpisodeNumberRegex, RegexOption.IGNORE_CASE)
                failedEpisodeNumberPattern.replace(removedNumber) { mr ->
                    mr.value.replaceFirst(mr.groupValues[1], "")
                }
            } else {
                removedNumber
            }
        }
    }
}
