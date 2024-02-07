package ani.dantotsu.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import ani.dantotsu.databinding.ItemQuestionBinding
import ani.dantotsu.others.CustomBottomDialog
import ani.dantotsu.setAnimation
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin

class FAQAdapter(
    private val questions: List<Triple<Int, String, String>>,
    private val manager: FragmentManager
) :
    RecyclerView.Adapter<FAQAdapter.FAQViewHolder>() {

    inner class FAQViewHolder(val binding: ItemQuestionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FAQViewHolder {
        return FAQViewHolder(
            ItemQuestionBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: FAQViewHolder, position: Int) {
        val b = holder.binding.root
        setAnimation(b.context, b)
        val faq = questions[position]
        b.text = faq.second
        b.setCompoundDrawablesWithIntrinsicBounds(faq.first, 0, 0, 0)
        b.setOnClickListener {
            CustomBottomDialog.newInstance().apply {
                setTitleText(faq.second)
                addView(
                    TextView(b.context).apply {
                        val markWon = Markwon.builder(b.context)
                            .usePlugin(SoftBreakAddsNewLinePlugin.create()).build()
                        markWon.setMarkdown(this, faq.third)
                    }
                )
            }.show(manager, "dialog")
        }
    }

    override fun getItemCount(): Int = questions.size
}