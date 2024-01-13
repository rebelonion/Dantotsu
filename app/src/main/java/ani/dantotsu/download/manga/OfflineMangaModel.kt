package ani.dantotsu.download.manga

import android.net.Uri

data class OfflineMangaModel(
    val title: String,
    val score: String,
    val totalchapter: String,
    val readchapter: String,
    val type: String,
    val chapters: String,
    val isOngoing: Boolean,
    val isUserScored: Boolean,
    val image: Uri?,
    val banner: Uri?
)