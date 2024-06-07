package ani.dantotsu.download.anime

import android.net.Uri

data class OfflineAnimeModel(
    var title: String,
    val score: String,
    val totalEpisode: String,
    val totalEpisodeList: String,
    val watchedEpisode: String,
    val type: String,
    val episodes: String,
    val isOngoing: Boolean,
    val isUserScored: Boolean,
    val image: Uri?,
    val banner: Uri?,
)