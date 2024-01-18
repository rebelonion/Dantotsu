package ani.dantotsu.media.anime

import java.util.regex.Matcher
import java.util.regex.Pattern

class AnimeNameAdapter {
    companion object {
        const val episodeRegex =
            "(episode|ep|e)[\\s:.\\-]*([\\d]+\\.?[\\d]*)[\\s:.\\-]*\\(?\\s*(sub|subbed|dub|dubbed)*\\s*\\)?\\s*"
        const val failedEpisodeNumberRegex =
            "(?<!part\\s)\\b(\\d+)\\b"
        const val seasonRegex = "(season|s)[\\s:.\\-]*(\\d+)[\\s:.\\-]*"

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
                    episodeMatcher.group(4)?.toFloat()
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
            return if (removedNumber.equals(text, true)) {
                val failedEpisodeNumberPattern: Regex = Regex(failedEpisodeNumberRegex, RegexOption.IGNORE_CASE)
                failedEpisodeNumberPattern.replace(removedNumber) { mr ->
                    mr.value.replaceFirst(mr.groupValues[1], "")
                }.ifEmpty { removedNumber }
            } else {
                removedNumber
            }
        }

        fun removeEpisodeNumberCompletely(text: String): String {
            val regexPattern = Regex(episodeRegex, RegexOption.IGNORE_CASE)
            val removedNumber = text.replace(regexPattern, "").ifEmpty {
                text
            }
            return if (removedNumber.equals(text, true)) {
                val failedEpisodeNumberPattern: Regex = Regex(failedEpisodeNumberRegex, RegexOption.IGNORE_CASE)
                failedEpisodeNumberPattern.replace(removedNumber) { mr ->
                    mr.value.replaceFirst(mr.groupValues[1], "")
                }.ifEmpty { removedNumber }
            } else {
                removedNumber
            }
        }
    }
}
