package ani.dantotsu.media

enum class MediaType {
    ANIME,
    MANGA,
    NOVEL;

    fun asText(): String {
        return when (this) {
            ANIME -> "Anime"
            MANGA -> "Manga"
            NOVEL -> "Novel"
        }
    }

    companion object {
        fun fromText(string : String): MediaType {
            return when (string) {
                "Anime" -> ANIME
                "Manga" -> MANGA
                "Novel" -> NOVEL
                else -> { ANIME }
            }
        }
    }
}