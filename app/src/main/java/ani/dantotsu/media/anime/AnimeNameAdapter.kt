package ani.dantotsu.media.anime

import java.util.regex.Matcher
import java.util.regex.Pattern

class AnimeNameAdapter {
    companion object {
        fun findSeasonNumber(text: String): Int? {
            val seasonRegex = "(season|s)[\\s:.\\-]*(\\d+)"
            val seasonPattern: Pattern = Pattern.compile(seasonRegex, Pattern.CASE_INSENSITIVE)
            val seasonMatcher: Matcher = seasonPattern.matcher(text)

            return if (seasonMatcher.find()) {
                seasonMatcher.group(2)?.toInt()
            } else {
                null
            }
        }
    }
}