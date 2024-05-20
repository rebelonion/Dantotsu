package ani.dantotsu.connections.github

import ani.dantotsu.settings.Developer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class Forks {

    fun getForks(): Array<Developer> {
        var forks = arrayOf<Developer>()
        runBlocking(Dispatchers.IO) {
            val trustedForks = arrayOf(
                GithubResponse(
                    "Awery",
                    GithubResponse.Owner(
                        "MrBoomDeveloper",
                        "https://avatars.githubusercontent.com/u/92123190?v=4"
                    ),
                    "https://github.com/MrBoomDeveloper/Awery"
                ),
            )
            trustedForks.forEach {
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