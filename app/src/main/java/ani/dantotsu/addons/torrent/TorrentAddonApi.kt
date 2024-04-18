package ani.dantotsu.addons.torrent

import eu.kanade.tachiyomi.data.torrentServer.model.Torrent

interface TorrentAddonApi {

    fun startServer(path: String)

    fun stopServer()

    fun echo(): String

    fun removeTorrent(torrent: String)

    fun addTorrent(
        link: String,
        title: String,
        poster: String,
        data: String,
        save: Boolean,
    ): Torrent

    fun getLink(torrent: Torrent, index: Int): String
}