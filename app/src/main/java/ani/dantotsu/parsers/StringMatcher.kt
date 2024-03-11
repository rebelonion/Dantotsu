package ani.dantotsu.parsers

import ani.dantotsu.util.Logger

class StringMatcher {
    companion object {
        private fun levenshteinDistance(s1: String, s2: String): Int {
            val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
            for (i in 0..s1.length) {
                for (j in 0..s2.length) {
                    when {
                        i == 0 -> dp[i][j] = j
                        j == 0 -> dp[i][j] = i
                        else -> dp[i][j] = minOf(
                            dp[i - 1][j - 1] + if (s1[i - 1] == s2[j - 1]) 0 else 1,
                            dp[i - 1][j] + 1,
                            dp[i][j - 1] + 1
                        )
                    }
                }
            }
            return dp[s1.length][s2.length]
        }

        fun closestString(target: String, list: List<String>): Pair<String, Int> {
            var minDistance = Int.MAX_VALUE
            var closestString = ""
            var closestIndex = -1

            for ((index, str) in list.withIndex()) {
                val distance = levenshteinDistance(target, str)
                if (distance < minDistance) {
                    minDistance = distance
                    closestString = str
                    closestIndex = index
                }
            }

            return Pair(closestString, closestIndex)
        }

        fun closestStringMovedToTop(target: String, list: List<String>): List<String> {
            val (_, closestIndex) = closestString(target, list)
            if (closestIndex == -1) {
                return list // Return original list if no closest string found
            }
            return listOf(list[closestIndex]) + list.subList(0, closestIndex) + list.subList(
                closestIndex + 1,
                list.size
            )
        }

        fun closestShowMovedToTop(target: String, shows: List<ShowResponse>): List<ShowResponse> {
            val closestShowAndIndex = closestShow(target, shows)
            val closestIndex = closestShowAndIndex.second
            if (closestIndex == -1) {
                Logger.log("No closest show found for $target")
                return shows // Return original list if no closest show found
            }
            Logger.log("Closest show found for $target is ${closestShowAndIndex.first.name}")
            return listOf(shows[closestIndex]) + shows.subList(0, closestIndex) + shows.subList(
                closestIndex + 1,
                shows.size
            )
        }

        private fun closestShow(
            target: String,
            shows: List<ShowResponse>
        ): Pair<ShowResponse, Int> {
            var minDistance = Int.MAX_VALUE
            var closestShow = ShowResponse("", "", "")
            var closestIndex = -1

            for ((index, show) in shows.withIndex()) {
                val distance = levenshteinDistance(target, show.name)
                if (distance < minDistance) {
                    minDistance = distance
                    closestShow = show
                    closestIndex = index
                }
            }

            return Pair(closestShow, closestIndex)
        }
    }
}
