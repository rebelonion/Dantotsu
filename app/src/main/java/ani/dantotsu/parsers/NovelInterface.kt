package ani.dantotsu.parsers

import com.lagradost.nicehttp.Requests


interface NovelInterface {
    suspend fun search(query: String, client: Requests): List<ShowResponse>
    suspend fun loadBook(link: String, extra: Map<String, String>?, client: Requests): Book
}