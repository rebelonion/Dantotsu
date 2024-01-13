package ani.dantotsu.media.anime

import java.util.regex.Matcher
import java.util.regex.Pattern

class AnimeNameAdapter {
    companion object {
        const val episodeRegex = "(episode|ep|e)[\\s:.\\-]*([\\d]+\\.?[\\d]*)[\\s:.\\-]*\\(\\s*(sub|subbed|dub|dubbed)*\\s*\\)\\s*"
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
                episodeMatcher.group(2)?.toFloat()
            } else {
                null
            }
        }

        fun removeEpisodeNumber(text: String): String {
            val regexPattern = Regex(episodeRegex, RegexOption.IGNORE_CASE)
            return text.replace(regexPattern, "").ifEmpty {
                text
            }
        }
    }
}
