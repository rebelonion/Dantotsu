package ani.dantotsu.download

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import ani.dantotsu.R
import ani.dantotsu.connections.crashlytics.CrashlyticsInterface
import ani.dantotsu.currActivity
import ani.dantotsu.currContext
import ani.dantotsu.download.anime.OfflineAnimeModel
import ani.dantotsu.download.manga.OfflineMangaModel
import ani.dantotsu.media.Media
import ani.dantotsu.media.MediaNameAdapter
import ani.dantotsu.media.MediaType
import ani.dantotsu.parsers.Episode
import ani.dantotsu.parsers.MangaChapter
import ani.dantotsu.parsers.MangaImage
import ani.dantotsu.parsers.Subtitle
import ani.dantotsu.parsers.SubtitleType
import ani.dantotsu.util.Logger
import com.google.gson.GsonBuilder
import com.google.gson.InstanceCreator
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SAnimeImpl
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.SEpisodeImpl
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SChapterImpl
import eu.kanade.tachiyomi.source.model.SManga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.Locale

@Deprecated("external storage is deprecated, use SAF instead")
class DownloadCompat {
    companion object {
        @Deprecated("external storage is deprecated, use SAF instead")
        fun loadMediaCompat(downloadedType: DownloadedType): Media? {
            val type = when (downloadedType.type) {
                MediaType.MANGA -> "Manga"
                MediaType.ANIME -> "Anime"
                else -> "Novel"
            }
            val directory = File(
                currContext()?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "Dantotsu/$type/${downloadedType.titleName}"
            )
            //load media.json and convert to media class with gson
            return try {
                val gson = GsonBuilder()
                    .registerTypeAdapter(SChapter::class.java, InstanceCreator<SChapter> {
                        SChapterImpl() // Provide an instance of SChapterImpl
                    })
                    .registerTypeAdapter(SAnime::class.java, InstanceCreator<SAnime> {
                        SAnimeImpl() // Provide an instance of SAnimeImpl
                    })
                    .registerTypeAdapter(SEpisode::class.java, InstanceCreator<SEpisode> {
                        SEpisodeImpl() // Provide an instance of SEpisodeImpl
                    })
                    .create()
                val media = File(directory, "media.json")
                val mediaJson = media.readText()
                gson.fromJson(mediaJson, Media::class.java)
            } catch (e: Exception) {
                Logger.log("Error loading media.json: ${e.message}")
                Logger.log(e)
                Injekt.get<CrashlyticsInterface>().logException(e)
                null
            }
        }

        @Deprecated("external storage is deprecated, use SAF instead")
        fun loadOfflineAnimeModelCompat(downloadedType: DownloadedType): OfflineAnimeModel {
            val type = when (downloadedType.type) {
                MediaType.MANGA -> "Manga"
                MediaType.ANIME -> "Anime"
                else -> "Novel"
            }
            val directory = File(
                currContext()?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "Dantotsu/$type/${downloadedType.titleName}"
            )
            //load media.json and convert to media class with gson
            try {
                val mediaModel = loadMediaCompat(downloadedType)!!
                val cover = File(directory, "cover.jpg")
                val coverUri: Uri? = if (cover.exists()) {
                    Uri.fromFile(cover)
                } else null
                val banner = File(directory, "banner.jpg")
                val bannerUri: Uri? = if (banner.exists()) {
                    Uri.fromFile(banner)
                } else null
                val title = mediaModel.mainName()
                val score = ((if (mediaModel.userScore == 0) (mediaModel.meanScore
                    ?: 0) else mediaModel.userScore) / 10.0).toString()
                val isOngoing =
                    mediaModel.status == currActivity()!!.getString(R.string.status_releasing)
                val isUserScored = mediaModel.userScore != 0
                val watchedEpisodes = (mediaModel.userProgress ?: "~").toString()
                val totalEpisode =
                    if (mediaModel.anime?.nextAiringEpisode != null) (mediaModel.anime.nextAiringEpisode.toString() + " | " + (mediaModel.anime.totalEpisodes
                        ?: "~").toString()) else (mediaModel.anime?.totalEpisodes ?: "~").toString()
                val chapters = " Chapters"
                val totalEpisodesList =
                    if (mediaModel.anime?.nextAiringEpisode != null) (mediaModel.anime.nextAiringEpisode.toString()) else (mediaModel.anime?.totalEpisodes
                        ?: "~").toString()
                return OfflineAnimeModel(
                    title,
                    score,
                    totalEpisode,
                    totalEpisodesList,
                    watchedEpisodes,
                    type,
                    chapters,
                    isOngoing,
                    isUserScored,
                    coverUri,
                    bannerUri
                )
            } catch (e: Exception) {
                Logger.log("Error loading media.json: ${e.message}")
                Logger.log(e)
                Injekt.get<CrashlyticsInterface>().logException(e)
                return OfflineAnimeModel(
                    downloadedType.titleName,
                    "0",
                    "??",
                    "??",
                    "??",
                    "movie",
                    "hmm",
                    isOngoing = false,
                    isUserScored = false,
                    null,
                    null
                )
            }
        }

        @Deprecated("external storage is deprecated, use SAF instead")
        fun loadOfflineMangaModelCompat(downloadedType: DownloadedType): OfflineMangaModel {
            val type = when (downloadedType.type) {
                MediaType.MANGA -> "Manga"
                MediaType.ANIME -> "Anime"
                else -> "Novel"
            }
            val directory = File(
                currContext()?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "Dantotsu/$type/${downloadedType.titleName}"
            )
            //load media.json and convert to media class with gson
            try {
                val mediaModel = loadMediaCompat(downloadedType)!!
                val cover = File(directory, "cover.jpg")
                val coverUri: Uri? = if (cover.exists()) {
                    Uri.fromFile(cover)
                } else null
                val banner = File(directory, "banner.jpg")
                val bannerUri: Uri? = if (banner.exists()) {
                    Uri.fromFile(banner)
                } else null
                val title = mediaModel.mainName()
                val score = ((if (mediaModel.userScore == 0) (mediaModel.meanScore
                    ?: 0) else mediaModel.userScore) / 10.0).toString()
                val isOngoing =
                    mediaModel.status == currActivity()!!.getString(R.string.status_releasing)
                val isUserScored = mediaModel.userScore != 0
                val readchapter = (mediaModel.userProgress ?: "~").toString()
                val totalchapter = "${mediaModel.manga?.totalChapters ?: "??"}"
                val chapters = " Chapters"
                return OfflineMangaModel(
                    title,
                    score,
                    totalchapter,
                    readchapter,
                    type,
                    chapters,
                    isOngoing,
                    isUserScored,
                    coverUri,
                    bannerUri
                )
            } catch (e: Exception) {
                Logger.log("Error loading media.json: ${e.message}")
                Logger.log(e)
                Injekt.get<CrashlyticsInterface>().logException(e)
                return OfflineMangaModel(
                    downloadedType.titleName,
                    "0",
                    "??",
                    "??",
                    "movie",
                    "hmm",
                    isOngoing = false,
                    isUserScored = false,
                    null,
                    null
                )
            }
        }

        @Deprecated("external storage is deprecated, use SAF instead")
        suspend fun loadEpisodesCompat(
            animeLink: String,
            extra: Map<String, String>?,
            sAnime: SAnime
        ): List<Episode> {

            val directory = File(
                currContext()?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "${animeLocation}/$animeLink"
            )
            //get all of the folder names and add them to the list
            val episodes = mutableListOf<Episode>()
            if (directory.exists()) {
                directory.listFiles()?.forEach {
                    //put the title and episdode number in the extra data
                    val extraData = mutableMapOf<String, String>()
                    extraData["title"] = animeLink
                    extraData["episode"] = it.name
                    if (it.isDirectory) {
                        val episode = Episode(
                            it.name,
                            "$animeLink - ${it.name}",
                            it.name,
                            null,
                            null,
                            extra = extraData,
                            sEpisode = SEpisodeImpl()
                        )
                        episodes.add(episode)
                    }
                }
                episodes.sortBy { MediaNameAdapter.findEpisodeNumber(it.number) }
                return episodes
            }
            return emptyList()
        }

        @Deprecated("external storage is deprecated, use SAF instead")
        suspend fun loadChaptersCompat(
            mangaLink: String,
            extra: Map<String, String>?,
            sManga: SManga
        ): List<MangaChapter> {
            val directory = File(
                currContext()?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "Dantotsu/Manga/$mangaLink"
            )
            //get all of the folder names and add them to the list
            val chapters = mutableListOf<MangaChapter>()
            if (directory.exists()) {
                directory.listFiles()?.forEach {
                    if (it.isDirectory) {
                        val chapter = MangaChapter(
                            it.name,
                            "$mangaLink/${it.name}",
                            it.name,
                            null,
                            "Unknown",
                            SChapter.create()
                        )
                        chapters.add(chapter)
                    }
                }
                chapters.sortBy { MediaNameAdapter.findChapterNumber(it.number) }
                return chapters
            }
            return emptyList()
        }

        @Deprecated("external storage is deprecated, use SAF instead")
        suspend fun loadImagesCompat(chapterLink: String, sChapter: SChapter): List<MangaImage> {
            val directory = File(
                currContext()?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "Dantotsu/Manga/$chapterLink"
            )
            val images = mutableListOf<MangaImage>()
            val imageNumberRegex = Regex("""(\d+)\.jpg$""")
            if (directory.exists()) {
                directory.listFiles()?.forEach {
                    if (it.isFile) {
                        val image = MangaImage(it.absolutePath, false, null)
                        images.add(image)
                    }
                }
                images.sortBy { image ->
                    val matchResult = imageNumberRegex.find(image.url.url)
                    matchResult?.groups?.get(1)?.value?.toIntOrNull() ?: Int.MAX_VALUE
                }
                for (image in images) {
                    Logger.log("imageNumber: ${image.url.url}")
                }
                return images
            }
            return emptyList()
        }

        @Deprecated("external storage is deprecated, use SAF instead")
        fun loadSubtitleCompat(title: String, episode: String): List<Subtitle>? {
            currContext()?.let {
                File(
                    it.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    "$animeLocation/$title/$episode"
                ).listFiles()?.forEach {
                    if (it.name.contains("subtitle")) {
                        return listOf(
                            Subtitle(
                                "Downloaded Subtitle",
                                Uri.fromFile(it).toString(),
                                determineSubtitletype(it.absolutePath)
                            )
                        )
                    }
                }
            }
            return null
        }

        private fun determineSubtitletype(url: String): SubtitleType {
            return when {
                url.lowercase(Locale.ROOT).endsWith("ass") -> SubtitleType.ASS
                url.lowercase(Locale.ROOT).endsWith("vtt") -> SubtitleType.VTT
                else -> SubtitleType.SRT
            }
        }

        @Deprecated("external storage is deprecated, use SAF instead")
        fun removeMediaCompat(context: Context, title: String, type: MediaType) {
            val subDirectory = if (type == MediaType.MANGA) {
                "Manga"
            } else if (type == MediaType.ANIME) {
                "Anime"
            } else {
                "Novel"
            }
            val directory = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "Dantotsu/$subDirectory/$title"
            )
            if (directory.exists()) {
                directory.deleteRecursively()
            }
        }

        @Deprecated("external storage is deprecated, use SAF instead")
        fun removeDownloadCompat(context: Context, downloadedType: DownloadedType, toast: Boolean) {
            val directory = if (downloadedType.type == MediaType.MANGA) {
                File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    "Dantotsu/Manga/${downloadedType.titleName}/${downloadedType.chapterName}"
                )
            } else if (downloadedType.type == MediaType.ANIME) {
                File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    "Dantotsu/Anime/${downloadedType.titleName}/${downloadedType.chapterName}"
                )
            } else {
                File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    "Dantotsu/Novel/${downloadedType.titleName}/${downloadedType.chapterName}"
                )
            }

            // Check if the directory exists and delete it recursively
            if (directory.exists()) {
                val deleted = directory.deleteRecursively()
                if (toast) {
                    if (deleted) {
                        Toast.makeText(context, "Successfully deleted", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to delete directory", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }

        private val animeLocation = "Dantotsu/Anime"
    }
}