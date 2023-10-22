package ani.dantotsu.media.manga

import java.util.regex.Matcher
import java.util.regex.Pattern

class MangaNameAdapter {
    companion object {
        fun findChapterNumber(text: String): Float? {
            val regex = "(chapter|chap|ch|c)[\\s:.\\-]*([\\d]+\\.?[\\d]*)"
            val pattern: Pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE)
            val matcher: Matcher = pattern.matcher(text)

            return if (matcher.find()) {
                matcher.group(2)?.toFloat()
            } else {
                null
            }
        }
    }
}