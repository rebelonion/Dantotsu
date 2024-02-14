package ani.dantotsu.media.comments

import android.content.Context
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import io.noties.markwon.Markwon

class SpoilerTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private val replaceWith = "▓"
    private var originalSpanned: SpannableStringBuilder? = null

    fun setSpoilerText(text: Spanned, markwon: Markwon) : Spanned {
        val pattern = Regex("\\|\\|(.*?)\\|\\|")
        val matcher = pattern.toPattern().matcher(text)
        val spannableBuilder = SpannableStringBuilder(text)
        //remove the "||" from the text
        val originalBuilder = SpannableStringBuilder(text)
        originalSpanned = originalBuilder

        val map = mutableMapOf<Int, Int>()
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            map[start] = end
        }

        map.forEach { (start, end) ->
            val replacement = replaceWith.repeat(end - start)
            spannableBuilder.replace(start, end, replacement)
        }
        val spannableString = SpannableString(spannableBuilder)
        map.forEach { (start, end) ->
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    markwon.setParsedMarkdown(this@SpoilerTextView, originalSpanned!!.delete(end - 2, end).delete(start, start + 2))
                }
            }
            spannableString.setSpan(
                clickableSpan,
                start,
                end,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        movementMethod = LinkMovementMethod.getInstance()
        return spannableString
    }
}