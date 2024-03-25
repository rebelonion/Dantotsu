package ani.dantotsu.connections.anilist

import ani.dantotsu.R
import ani.dantotsu.currContext
import ani.dantotsu.media.Media
import java.io.Serializable

data class SearchResults(
    val type: String,
    var isAdult: Boolean,
    var onList: Boolean? = null,
    var perPage: Int? = null,
    var search: String? = null,
    var countryOfOrigin :String? = null,
    var sort: String? = null,
    var genres: MutableList<String>? = null,
    var excludedGenres: MutableList<String>? = null,
    var tags: MutableList<String>? = null,
    var excludedTags: MutableList<String>? = null,
    var status: String? = null,
    var source: String? = null,
    var format: String? = null,
    var seasonYear: Int? = null,
    var startYear: Int? = null,
    var season: String? = null,
    var page: Int = 1,
    var results: MutableList<Media>,
    var hasNextPage: Boolean,
) : Serializable {
    fun toChipList(): List<SearchChip> {
        val list = mutableListOf<SearchChip>()
        sort?.let {
            val c = currContext()!!
            list.add(
                SearchChip(
                    "SORT",
                    c.getString(
                        R.string.filter_sort,
                        c.resources.getStringArray(R.array.sort_by)[Anilist.sortBy.indexOf(it)]
                    )
                )
            )
        }
        status?.let {
            list.add(SearchChip("STATUS", currContext()!!.getString(R.string.filter_status, it)))
        }
        source?.let {
            list.add(SearchChip("SOURCE", currContext()!!.getString(R.string.filter_source, it)))
        }
        format?.let {
            list.add(SearchChip("FORMAT", currContext()!!.getString(R.string.filter_format, it)))
        }
        countryOfOrigin?.let {
            list.add(SearchChip("COUNTRY", currContext()!!.getString(R.string.filter_country, it)))
        }
        season?.let {
            list.add(SearchChip("SEASON", it))
        }
        startYear?.let {
            list.add(SearchChip("START_YEAR", it.toString()))
        }
        seasonYear?.let {
            list.add(SearchChip("SEASON_YEAR", it.toString()))
        }
        genres?.forEach {
            list.add(SearchChip("GENRE", it))
        }
        excludedGenres?.forEach {
            list.add(
                SearchChip(
                    "EXCLUDED_GENRE",
                    currContext()!!.getString(R.string.filter_exclude, it)
                )
            )
        }
        tags?.forEach {
            list.add(SearchChip("TAG", it))
        }
        excludedTags?.forEach {
            list.add(
                SearchChip(
                    "EXCLUDED_TAG",
                    currContext()!!.getString(R.string.filter_exclude, it)
                )
            )
        }
        return list
    }

    fun removeChip(chip: SearchChip) {
        when (chip.type) {
            "SORT" -> sort = null
            "STATUS" -> status = null
            "SOURCE" -> source = null
            "FORMAT" -> format = null
            "COUNTRY" -> countryOfOrigin = null
            "SEASON" -> season = null
            "START_YEAR" -> startYear = null
            "SEASON_YEAR" -> seasonYear = null
            "GENRE" -> genres?.remove(chip.text)
            "EXCLUDED_GENRE" -> excludedGenres?.remove(chip.text)
            "TAG" -> tags?.remove(chip.text)
            "EXCLUDED_TAG" -> excludedTags?.remove(chip.text)
        }
    }

    data class SearchChip(
        val type: String,
        val text: String
    )
}