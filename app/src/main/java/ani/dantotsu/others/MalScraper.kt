package ani.dantotsu.others

import ani.dantotsu.client
import ani.dantotsu.media.Media
import kotlinx.coroutines.withTimeout

object MalScraper {
    private val headers = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Safari/537.36"
    )

    suspend fun loadMedia(media: Media) {
        try {
            withTimeout(6000) {
                if (media.anime != null) {
                    val res =
                        client.get("https://myanimelist.net/anime/${media.idMAL}", headers).document
                    val a = res.select(".title-english").text()
                    media.nameMAL = if (a != "") a else res.select(".title-name").text()
                    media.typeMAL =
                        if (res.select("div.spaceit_pad > a")
                                .isNotEmpty()
                        ) res.select("div.spaceit_pad > a")[0].text() else null
                    media.anime.op = arrayListOf()
                    res.select(".opnening > table > tbody > tr").forEach {
                        val text = it.text()
                        if (!text.contains("Help improve our database"))
                            media.anime.op.add(it.text())
                    }
                    media.anime.ed = arrayListOf()
                    res.select(".ending > table > tbody > tr").forEach {
                        val text = it.text()
                        if (!text.contains("Help improve our database"))
                            media.anime.ed.add(it.text())
                    }
                } else {
                    val res =
                        client.get("https://myanimelist.net/manga/${media.idMAL}", headers).document
                    val b = res.select(".title-english").text()
                    val a = res.select(".h1-title").text().removeSuffix(b)
                    media.nameMAL = a
                    media.typeMAL =
                        if (res.select("div.spaceit_pad > a")
                                .isNotEmpty()
                        ) res.select("div.spaceit_pad > a")[0].text() else null
                }
            }
        } catch (e: Exception) {
            // if (e is TimeoutCancellationException) snackString(currContext()?.getString(R.string.error_loading_mal_data))
        }
    }
}
