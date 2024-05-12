package ani.dantotsu.util

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.core.widget.addTextChangedListener
import ani.dantotsu.R
import ani.dantotsu.buildMarkwon
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.databinding.ActivityMarkdownCreatorBinding
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.toast
import io.noties.markwon.editor.MarkwonEditor
import io.noties.markwon.editor.MarkwonEditorTextWatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import tachiyomi.core.util.lang.launchIO

class MarkdownCreatorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMarkdownCreatorBinding
    private lateinit var type: String
    private var text: String = ""
    private var parentId: Int = 0
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        initActivity(this)
        binding = ActivityMarkdownCreatorBinding.inflate(layoutInflater)
        binding.markdownCreatorToolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
        }
        binding.buttonContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin += navBarHeight
        }
        setContentView(binding.root)
        if (intent.hasExtra("type")) {
            type = intent.getStringExtra("type")!!
        } else {
            toast("Error: No type")
            finish()
            return
        }
        binding.markdownCreatorTitle.text = when (type) {
            "activity" -> getString(R.string.create_new_activity)
            "review" -> getString(R.string.create_new_review)
            "replyActivity" -> {
                parentId = intent.getIntExtra("parentId", -1)
                if (parentId == -1) {
                    toast("Error: No parent ID")
                    finish()
                    return
                }
                getString(R.string.create_new_reply)
            }
            else -> ""
        }
        binding.editText.setText(text)
        binding.editText.addTextChangedListener {
            if (!binding.markdownCreatorPreviewCheckbox.isChecked) {
                text = it.toString()
            }
        }
        previewMarkdown(false)
        binding.markdownCreatorPreviewCheckbox.setOnClickListener {
            previewMarkdown(binding.markdownCreatorPreviewCheckbox.isChecked)
        }
        binding.cancelButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.markdownCreatorBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.createButton.setOnClickListener {
            if (text.isBlank()) {
                toast(getString(R.string.cannot_be_empty))
                return@setOnClickListener
            }
            launchIO {
                val success = when (type) {
                    "activity" -> Anilist.mutation.postActivity(text)
                    //"review" -> Anilist.mutation.postReview(text)
                    "replyActivity" -> Anilist.mutation.postReply(parentId, text)
                    else -> "Error: Unknown type"
                }
                toast(success)
            }
            onBackPressedDispatcher.onBackPressed()
        }

        binding.editText.requestFocus()
    }

    private fun previewMarkdown(preview: Boolean) {
        val markwon = buildMarkwon(this, false, anilist = true)
        if (preview) {
            binding.editText.isEnabled = false
            markwon.setMarkdown(binding.editText, text)
        } else {
            binding.editText.setText(text)
            binding.editText.isEnabled = true
            val markwonEditor = MarkwonEditor.create(markwon)
            binding.editText.addTextChangedListener(
                MarkwonEditorTextWatcher.withProcess(markwonEditor)
            )
        }
    }
}