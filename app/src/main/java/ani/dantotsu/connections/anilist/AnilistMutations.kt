package ani.dantotsu.connections.anilist

import ani.dantotsu.connections.anilist.Anilist.executeQuery
import ani.dantotsu.connections.anilist.api.FuzzyDate
import ani.dantotsu.connections.anilist.api.Query
import ani.dantotsu.currContext
import com.google.gson.Gson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class AnilistMutations {

    suspend fun toggleFav(anime: Boolean = true, id: Int) {
        val query =
            """mutation (${"$"}animeId: Int,${"$"}mangaId:Int) { ToggleFavourite(animeId:${"$"}animeId,mangaId:${"$"}mangaId){ anime { edges { id } } manga { edges { id } } } }"""
        val variables = if (anime) """{"animeId":"$id"}""" else """{"mangaId":"$id"}"""
        executeQuery<JsonObject>(query, variables)
    }

    suspend fun toggleFav(type: FavType, id: Int): Boolean {
        val filter = when (type) {
            FavType.ANIME -> "animeId"
            FavType.MANGA -> "mangaId"
            FavType.CHARACTER -> "characterId"
            FavType.STAFF -> "staffId"
            FavType.STUDIO -> "studioId"
        }
        val query = """mutation{ToggleFavourite($filter:$id){anime{pageInfo{total}}}}"""
        val result = executeQuery<JsonObject>(query)
        return result?.get("errors") == null && result != null
    }

    enum class FavType {
        ANIME, MANGA, CHARACTER, STAFF, STUDIO
    }

    suspend fun editList(
        mediaID: Int,
        progress: Int? = null,
        score: Int? = null,
        repeat: Int? = null,
        notes: String? = null,
        status: String? = null,
        private: Boolean? = null,
        startedAt: FuzzyDate? = null,
        completedAt: FuzzyDate? = null,
        customList: List<String>? = null
    ) {

        val query = """
            mutation ( ${"$"}mediaID: Int, ${"$"}progress: Int,${"$"}private:Boolean,${"$"}repeat: Int, ${"$"}notes: String, ${"$"}customLists: [String], ${"$"}scoreRaw:Int, ${"$"}status:MediaListStatus, ${"$"}start:FuzzyDateInput${if (startedAt != null) "=" + startedAt.toVariableString() else ""}, ${"$"}completed:FuzzyDateInput${if (completedAt != null) "=" + completedAt.toVariableString() else ""} ) {
                SaveMediaListEntry( mediaId: ${"$"}mediaID, progress: ${"$"}progress, repeat: ${"$"}repeat, notes: ${"$"}notes, private: ${"$"}private, scoreRaw: ${"$"}scoreRaw, status:${"$"}status, startedAt: ${"$"}start, completedAt: ${"$"}completed , customLists: ${"$"}customLists ) {
                    score(format:POINT_10_DECIMAL) startedAt{year month day} completedAt{year month day}
                }
            }
        """.replace("\n", "").replace("""    """, "")

        val variables = """{"mediaID":$mediaID
            ${if (private != null) ""","private":$private""" else ""}
            ${if (progress != null) ""","progress":$progress""" else ""}
            ${if (score != null) ""","scoreRaw":$score""" else ""}
            ${if (repeat != null) ""","repeat":$repeat""" else ""}
            ${if (notes != null) ""","notes":"${notes.replace("\n", "\\n")}"""" else ""}
            ${if (status != null) ""","status":"$status"""" else ""}
            ${if (customList != null) ""","customLists":[${customList.joinToString { "\"$it\"" }}]""" else ""}
            }""".replace("\n", "").replace("""    """, "")
        println(variables)
        executeQuery<JsonObject>(query, variables, show = true)
    }

    suspend fun deleteList(listId: Int) {
        val query = "mutation(${"$"}id:Int){DeleteMediaListEntry(id:${"$"}id){deleted}}"
        val variables = """{"id":"$listId"}"""
        executeQuery<JsonObject>(query, variables)
    }


    suspend fun rateReview(reviewId: Int, rating: String): Query.RateReviewResponse? {
        val query = "mutation{RateReview(reviewId:$reviewId,rating:$rating){id mediaId mediaType summary body(asHtml:true)rating ratingAmount userRating score private siteUrl createdAt updatedAt user{id name bannerImage avatar{medium large}}}}"
        return executeQuery<Query.RateReviewResponse>(query)
    }

    suspend fun postActivity(text:String): String {
        val encodedText = text.stringSanitizer()
        val query = "mutation{SaveTextActivity(text:$encodedText){siteUrl}}"
        val result = executeQuery<JsonObject>(query)
        val errors = result?.get("errors")
        return errors?.toString()
            ?: (currContext()?.getString(ani.dantotsu.R.string.success) ?: "Success")
    }

    suspend fun postReview(summary: String, body: String, mediaId: Int, score: Int): String {
        val encodedSummary = summary.stringSanitizer()
        val encodedBody = body.stringSanitizer()
        val query = "mutation{SaveReview(mediaId:$mediaId,summary:$encodedSummary,body:$encodedBody,score:$score){siteUrl}}"
        val result = executeQuery<JsonObject>(query)
        val errors = result?.get("errors")
        return errors?.toString()
            ?: (currContext()?.getString(ani.dantotsu.R.string.success) ?: "Success")
    }

    suspend fun postReply(activityId: Int, text: String): String {
        val encodedText = text.stringSanitizer()
        val query = "mutation{SaveActivityReply(activityId:$activityId,text:$encodedText){id}}"
        val result = executeQuery<JsonObject>(query)
        val errors = result?.get("errors")
        return errors?.toString()
            ?: (currContext()?.getString(ani.dantotsu.R.string.success) ?: "Success")
    }

    private fun String.stringSanitizer(): String {
        val sb = StringBuilder()
        var i = 0
        while (i < this.length) {
            val codePoint = this.codePointAt(i)
            if (codePoint > 0xFFFF) {
                sb.append("&#").append(codePoint).append(";")
                i += 2
            } else {
                sb.append(this[i])
                i++
            }
        }
        return Gson().toJson(sb.toString())
    }
}