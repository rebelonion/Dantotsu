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

class Forks {

    fun getForks(): Array<Developer> {
        var forks = arrayOf<Developer>()
        runBlocking(Dispatchers.IO) {
            val res = client.get("https://api.github.com/repos/rebelonion/Dantotsu/forks")
                .parsed<JsonArray>().map {
                    Mapper.json.decodeFromJsonElement<GithubResponse>(it)
                }
            res.forEach {
                forks = forks.plus(
                    Developer(
                        it.name,
                        it.owner.avatarUrl,
                        it.owner.login,
                        it.htmlUrl
                    )
                )
            }
        }
        return forks
    }


    @Serializable
    data class GithubResponse(
        @SerialName("name")
        val name: String,
        val owner: Owner,
        @SerialName("html_url")
        val htmlUrl: String,
    ) {
        @Serializable
        data class Owner(
            @SerialName("login")
            val login: String,
            @SerialName("avatar_url")
            val avatarUrl: String
        )
    }
}