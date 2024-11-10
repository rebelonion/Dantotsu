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
            res.forEach {
                if (it.login == "SunglassJerry") return@forEach
                val role = when (it.login) {
                    "rebelonion" -> "Owner & Maintainer"
                    "sneazy-ibo" -> "Contributor & Comment Moderator"
                    "WaiWhat" -> "Icon Designer"
                    "itsmechinmoy" -> "Discord and Telegram Admin/Helper, Comment Moderator & Translator"
                    else -> "Contributor"
                }
                developers = developers.plus(
                    Developer(
                        it.login,
                        it.avatarUrl,
                        role,
                        it.htmlUrl
                    )
                )
            }
            developers = developers.plus(
                arrayOf(
                    Developer(
                        "MarshMeadow",
                        "https://avatars.githubusercontent.com/u/88599122?v=4",
                        "Beta Icon Designer & Website Maintainer",
                        "https://github.com/MarshMeadow?tab=repositories"
                    ),
                    Developer(
                        "Zaxx69",
                        "https://s4.anilist.co/file/anilistcdn/user/avatar/large/b6342562-kxE8m4i7KUMK.png",
                        "Telegram Admin",
                        "https://anilist.co/user/6342562"
                    ),
                    Developer(
                        "Arif Alam",
                        "https://s4.anilist.co/file/anilistcdn/user/avatar/large/b6011177-2n994qtayiR9.jpg",
                        "Discord & Comment Moderator",
                        "https://anilist.co/user/6011177"
                    ),
                    Developer(
                        "SunglassJeery",
                        "https://s4.anilist.co/file/anilistcdn/user/avatar/large/b5804776-FEKfP5wbz2xv.png",
                        "Head Discord & Comment Moderator",
                        "https://anilist.co/user/5804776"
                    ),
                    Developer(
                        "Excited",
                        "https://s4.anilist.co/file/anilistcdn/user/avatar/large/b6131921-toSoGWmKbRA1.png",
                        "Comment Moderator",
                        "https://anilist.co/user/6131921"
                    ),
                    Developer(
                        "Gurjshan",
                        "https://s4.anilist.co/file/anilistcdn/user/avatar/large/b6363228-rWQ3Pl3WuxzL.png",
                        "Comment Moderator",
                        "https://anilist.co/user/6363228"
                    ),
                    Developer(
                        "NekoMimi",
                        "https://s4.anilist.co/file/anilistcdn/user/avatar/large/b6244220-HOpImMGMQAxW.jpg",
                        "Comment Moderator",
                        "https://anilist.co/user/6244220"
                    ),
                    Developer(
                        "Ziadsenior",
                        "https://s4.anilist.co/file/anilistcdn/user/avatar/large/b6049773-8cjYeUOFUguv.jpg",
                        "Comment Moderator and Arabic Translator",
                        "https://anilist.co/user/6049773"
                    ),
                    Developer(
                        "Dawnusedyeet",
                        "https://s4.anilist.co/file/anilistcdn/user/avatar/large/b6237399-RHFvRHriXjwS.png",
                        "Contributor",
                        "https://anilist.co/user/Dawnusedyeet/"
                    ),
                    Developer(
                        "hastsu",
                        "https://s4.anilist.co/file/anilistcdn/user/avatar/large/b6183359-9os7zUhYdF64.jpg",
                        "Comment Moderator and Arabic Translator",
                        "https://anilist.co/user/6183359"
                    ),
                )
            )
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
