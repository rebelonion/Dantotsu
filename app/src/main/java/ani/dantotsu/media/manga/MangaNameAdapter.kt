package ani.dantotsu.media.manga

import java.util.regex.Matcher
import java.util.regex.Pattern

class MangaNameAdapter {
    companion object {
        const val chapterRegex = "(chapter|chap|ch|c)[\\s:.\\-]*([\\d]+\\.?[\\d]*)[\\s:.\\-]*"
        const val filedChapterNumberRegex = "(?<!part\\s)\\b(\\d+)\\b"
        fun findChapterNumber(text: String): Float? {
            val pattern: Pattern = Pattern.compile(chapterRegex, Pattern.CASE_INSENSITIVE)
            val matcher: Matcher = pattern.matcher(text)

            return if (matcher.find()) {
                matcher.group(2)?.toFloat()
            } else {
                val failedChapterNumberPattern: Pattern =
                    Pattern.compile(filedChapterNumberRegex, Pattern.CASE_INSENSITIVE)
                val failedChapterNumberMatcher: Matcher =
                    failedChapterNumberPattern.matcher(text)
                if (failedChapterNumberMatcher.find()) {
                    failedChapterNumberMatcher.group(1)?.toFloat()
                } else {
                    null
                }
            }
        }
    }
}
