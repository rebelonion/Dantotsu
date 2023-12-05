package ani.dantotsu.connections.anilist.api

import kotlinx.serialization.SerialName
import java.io.Serializable
import java.text.DateFormatSymbols
import java.util.Calendar

@kotlinx.serialization.Serializable
data class FuzzyDate(
    @SerialName("year") val year: Int? = null,
    @SerialName("month") val month: Int? = null,
    @SerialName("day") val day: Int? = null,
) : Serializable, Comparable<FuzzyDate> {


    fun isEmpty(): Boolean {
        return year == null && month == null && day == null
    }

    override fun toString(): String {
        return if (isEmpty()) "??" else toStringOrEmpty()
    }

    fun toStringOrEmpty(): String {
        return listOfNotNull(
            day?.toString(),
            month?.let { DateFormatSymbols().months.elementAt(it - 1) },
            year?.toString()
        ).joinToString(" ")
    }

    fun getToday(): FuzzyDate {
        val cal = Calendar.getInstance()
        return FuzzyDate(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun toVariableString(): String {
        return listOfNotNull(
            year?.let { "year:$it" },
            month?.let { "month:$it" },
            day?.let { "day:$it" }
        ).joinToString(",", "{", "}")
    }

    fun toMALString(): String {
        val padding = '0'
        val values = listOf(
            year?.toString()?.padStart(4, padding),
            month?.toString()?.padStart(2, padding),
            day?.toString()?.padStart(2, padding)
        )
        return values.takeWhile { it is String }.joinToString("-")
    }

//    fun toInt(): Int {
//        return 10000 * (this.year ?: 0) + 100 * (this.month ?: 0) + (this.day ?: 0)
//    }

    override fun compareTo(other: FuzzyDate): Int = when {
        year != other.year -> (year ?: 0) - (other.year ?: 0)
        month != other.month -> (month ?: 0) - (other.month ?: 0)
        else -> (day ?: 0) - (other.day ?: 0)
    }
}