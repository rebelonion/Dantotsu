package ani.dantotsu.addons.torrent

import ani.dantotsu.addons.LoadResult

open class TorrentLoadResult : LoadResult() {
    class Success(val extension: TorrentAddon.Installed) : TorrentLoadResult()
}