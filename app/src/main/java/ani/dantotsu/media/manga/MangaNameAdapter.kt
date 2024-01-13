package ani.dantotsu.media.manga

import java.util.regex.Matcher
import java.util.regex.Pattern

class MangaNameAdapter {
    companion object {
        val chapterRegex = "(chapter|chap|ch|c)[\\s:.\\-]*([\\d]+\\.?[\\d]*)[\\s:.\\-]*"
        fun findChapterNumber(text: String): Float? {
            val pattern: Pattern = Pattern.compile(chapterRegex, Pattern.CASE_INSENSITIVE)
            val matcher: Matcher = pattern.matcher(text)

            return if (matcher.find()) {
                matcher.group(2)?.toFloat()
            } else {
                null
            }
        }
    }
}
