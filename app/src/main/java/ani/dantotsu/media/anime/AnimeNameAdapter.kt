package ani.dantotsu.media.anime

import java.util.regex.Matcher
import java.util.regex.Pattern

class AnimeNameAdapter {
    companion object {
        val episodeRegex = "(episode|ep|e)[\\s:.\\-]*([\\d]+\\.?[\\d]*)[\\s:.\\-]*"
        val seasonRegex = "(season|s)[\\s:.\\-]*(\\d+)[\\s:.\\-]*"

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
    }
}
