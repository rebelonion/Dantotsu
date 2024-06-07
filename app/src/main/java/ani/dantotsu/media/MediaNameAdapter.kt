package ani.dantotsu.media

import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern

object MediaNameAdapter {

    private const val REGEX_ITEM = "[\\s:.\\-]*(\\d+\\.?\\d*)[\\s:.\\-]*"
    private const val REGEX_PART_NUMBER = "(?<!part\\s)\\b(\\d+)\\b"
    private const val REGEX_EPISODE =
        "(episode|episodio|ep|e)${REGEX_ITEM}\\(?\\s*(sub|subbed|dub|dubbed)*\\s*\\)?\\s*"
    private const val REGEX_SEASON = "(season|s)[\\s:.\\-]*(\\d+)[\\s:.\\-]*"
    private const val REGEX_SUBDUB = "^(soft)?[\\s-]*(sub|dub|mixed)(bed|s)?\\s*$"
    private const val REGEX_CHAPTER = "(chapter|chap|ch|c)${REGEX_ITEM}"

    fun setSubDub(text: String, typeToSetTo: SubDubType): String? {
        val subdubPattern: Pattern = Pattern.compile(REGEX_SUBDUB, Pattern.CASE_INSENSITIVE)
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
        val subdubPattern: Pattern = Pattern.compile(REGEX_SUBDUB, Pattern.CASE_INSENSITIVE)
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
        val seasonPattern: Pattern = Pattern.compile(REGEX_SEASON, Pattern.CASE_INSENSITIVE)
        val seasonMatcher: Matcher = seasonPattern.matcher(text)

        return if (seasonMatcher.find()) {
            seasonMatcher.group(2)?.toInt()
        } else {
            text.toIntOrNull()
        }
    }

    fun findEpisodeNumber(text: String): Float? {
        val episodePattern: Pattern = Pattern.compile(REGEX_EPISODE, Pattern.CASE_INSENSITIVE)
        val episodeMatcher: Matcher = episodePattern.matcher(text)

        return if (episodeMatcher.find()) {
            if (episodeMatcher.group(2) != null) {
                episodeMatcher.group(2)?.toFloat()
            } else {
                val failedEpisodeNumberPattern: Pattern =
                    Pattern.compile(REGEX_PART_NUMBER, Pattern.CASE_INSENSITIVE)
                val failedEpisodeNumberMatcher: Matcher =
                    failedEpisodeNumberPattern.matcher(text)
                if (failedEpisodeNumberMatcher.find()) {
                    failedEpisodeNumberMatcher.group(1)?.toFloat()
                } else {
                    null
                }
            }
        } else {
            text.toFloatOrNull()
        }
    }

    fun removeEpisodeNumber(text: String): String {
        val regexPattern = Regex(REGEX_EPISODE, RegexOption.IGNORE_CASE)
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
        val regexPattern = Regex(REGEX_EPISODE, RegexOption.IGNORE_CASE)
        val removedNumber = text.replace(regexPattern, "")
        return if (removedNumber.equals(text, true)) {  // if nothing was removed
            val failedEpisodeNumberPattern =
                Regex(REGEX_PART_NUMBER, RegexOption.IGNORE_CASE)
            failedEpisodeNumberPattern.replace(removedNumber) { mr ->
                mr.value.replaceFirst(mr.groupValues[1], "")
            }
        } else {
            removedNumber
        }
    }

    fun findChapterNumber(text: String): Float? {
        val pattern: Pattern = Pattern.compile(REGEX_CHAPTER, Pattern.CASE_INSENSITIVE)
        val matcher: Matcher = pattern.matcher(text)

        return if (matcher.find()) {
            matcher.group(2)?.toFloat()
        } else {
            val failedChapterNumberPattern: Pattern =
                Pattern.compile(REGEX_PART_NUMBER, Pattern.CASE_INSENSITIVE)
            val failedChapterNumberMatcher: Matcher =
                failedChapterNumberPattern.matcher(text)
            if (failedChapterNumberMatcher.find()) {
                failedChapterNumberMatcher.group(1)?.toFloat()
            } else {
                text.toFloatOrNull()
            }
        }
    }
}