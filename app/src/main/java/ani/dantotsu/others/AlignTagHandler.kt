package ani.dantotsu.others

import android.text.Layout
import android.text.style.AlignmentSpan
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.RenderProps
import io.noties.markwon.html.HtmlTag
import io.noties.markwon.html.tag.SimpleTagHandler


class AlignTagHandler : SimpleTagHandler() {

    override fun getSpans(
        configuration: MarkwonConfiguration,
        renderProps: RenderProps,
        tag: HtmlTag
    ): Any {
        val alignment: Layout.Alignment = if (tag.attributes().containsKey("center")) {
            Layout.Alignment.ALIGN_CENTER
        } else if (tag.attributes().containsKey("end")) {
            Layout.Alignment.ALIGN_OPPOSITE
        } else {
            Layout.Alignment.ALIGN_NORMAL
        }

        return AlignmentSpan.Standard(alignment)
    }

    override fun supportedTags(): Collection<String> {
        return setOf("align")
    }
}
