package ani.dantotsu.addons.torrent

sealed class TorrentLoadResult {
    class Success(val extension: TorrentExtension.Installed) : TorrentLoadResult()

    data object Error : TorrentLoadResult()
}