package ani.dantotsu.parsers.novel


sealed class NovelLoadResult {
    data class Success(val extension: NovelExtension.Installed) : NovelLoadResult()
    data class Error(val error: Exception) : NovelLoadResult()
}