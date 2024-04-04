package ani.dantotsu.connections.github

import ani.dantotsu.Mapper
import ani.dantotsu.R
import ani.dantotsu.client
import ani.dantotsu.getAppString
import ani.dantotsu.settings.Developer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement

class Contributors {

    fun getContributors(): Array<Developer> {
        var developers = arrayOf<Developer>()
        runBlocking(Dispatchers.IO) {
            val repo = getAppString(R.string.repo)
            val res = client.get("https://api.github.com/repos/$repo/contributors")
                .parsed<JsonArray>().map {
                    Mapper.json.decodeFromJsonElement<GithubResponse>(it)
                }
            res.find { it.login == "rebelonion"}?.let { first ->
                developers = developers.plus(
                    Developer(
                        first.login,
                        first.avatarUrl,
                         "Owner and Maintainer",
                        first.htmlUrl
                    )
                ).plus(arrayOf(
                    Developer(
                        "Wai What",
                        "https://avatars.githubusercontent.com/u/149729762?v=4",
                        "Icon Designer",
                        "https://github.com/WaiWhat"
                    ),
                    Developer(
                        "MarshMeadow",
                        "https://avatars.githubusercontent.com/u/88599122?v=4",
                        "Beta Icon Designer",
                        "https://github.com/MarshMeadow?tab=repositories"
                    ),
                    Developer(
                        "Zaxx69",
                        "https://avatars.githubusercontent.com/u/138523882?v=4",
                        "Telegram Admin",
                        "https://github.com/Zaxx69"
                    ),
                    Developer(
                        "Arif Alam",
                        "https://avatars.githubusercontent.com/u/70383209?v=4",
                        "Head Discord Moderator",
                        "https://youtube.com/watch?v=dQw4w9WgXcQ"
                    )
                ))
            }
            res.filter {it.login != "rebelonion"}.forEach {
                developers = developers.plus(
                    Developer(
                        it.login,
                        it.avatarUrl,
                        "Contributor",
                        it.htmlUrl
                    )
                )
            }
        }
        return developers
    }


    @Serializable
    data class GithubResponse(
        @SerialName("login")
        val login: String,
        @SerialName("avatar_url")
        val avatarUrl: String,
        @SerialName("html_url")
        val htmlUrl: String
    )
}