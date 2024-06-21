package ani.dantotsu.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import ani.dantotsu.R
import ani.dantotsu.currContext
import ani.dantotsu.databinding.ActivityFaqBinding
import ani.dantotsu.initActivity
import ani.dantotsu.themes.ThemeManager

class FAQActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFaqBinding

    private val faqs by lazy {
        listOf(

            Triple(
                R.drawable.ic_round_help_24,
                currContext()?.getString(R.string.question_1) ?: "",
                currContext()?.getString(R.string.answer_1) ?: ""
            ),
            Triple(
                R.drawable.ic_round_auto_awesome_24,
                currContext()?.getString(R.string.question_2) ?: "",
                currContext()?.getString(R.string.answer_2) ?: ""
            ),
            Triple(
                R.drawable.ic_round_auto_awesome_24,
                currContext()?.getString(R.string.question_17) ?: "",
                currContext()?.getString(R.string.answer_17) ?: ""
            ),
            Triple(
                R.drawable.ic_download_24,
                currContext()?.getString(R.string.question_3) ?: "",
                currContext()?.getString(R.string.answer_3) ?: ""
            ),
            Triple(
                R.drawable.ic_round_help_24,
                currContext()?.getString(R.string.question_16) ?: "",
                currContext()?.getString(R.string.answer_16) ?: ""
            ),
            Triple(
                R.drawable.ic_round_dns_24,
                currContext()?.getString(R.string.question_4) ?: "",
                currContext()?.getString(R.string.answer_4) ?: ""
            ),
            Triple(
                R.drawable.ic_baseline_screen_lock_portrait_24,
                currContext()?.getString(R.string.question_5) ?: "",
                currContext()?.getString(R.string.answer_5) ?: ""
            ),
            Triple(
                R.drawable.ic_anilist,
                currContext()?.getString(R.string.question_18) ?: "",
                currContext()?.getString(R.string.answer_18) ?: ""
            ),
            Triple(
                R.drawable.ic_anilist,
                currContext()?.getString(R.string.question_6) ?: "",
                currContext()?.getString(R.string.answer_6) ?: ""
            ),
            Triple(
                R.drawable.ic_round_movie_filter_24,
                currContext()?.getString(R.string.question_7) ?: "",
                currContext()?.getString(R.string.answer_7) ?: ""
            ),
            Triple(
                R.drawable.ic_round_magnet_24,
                currContext()?.getString(R.string.question_19) ?: "",
                currContext()?.getString(R.string.answer_19) ?: ""
            ),
            Triple(
                R.drawable.ic_round_lock_open_24,
                currContext()?.getString(R.string.question_9) ?: "",
                currContext()?.getString(R.string.answer_9) ?: ""
            ),
            Triple(
                R.drawable.ic_round_smart_button_24,
                currContext()?.getString(R.string.question_10) ?: "",
                currContext()?.getString(R.string.answer_10) ?: ""
            ),
            Triple(
                R.drawable.ic_round_smart_button_24,
                currContext()?.getString(R.string.question_11) ?: "",
                currContext()?.getString(R.string.answer_11) ?: ""
            ),
            Triple(
                R.drawable.ic_round_info_24,
                currContext()?.getString(R.string.question_12) ?: "",
                currContext()?.getString(R.string.answer_12) ?: ""
            ),
            Triple(
                R.drawable.ic_round_help_24,
                currContext()?.getString(R.string.question_13) ?: "",
                currContext()?.getString(R.string.answer_13) ?: ""
            ),
            Triple(
                R.drawable.ic_round_art_track_24,
                currContext()?.getString(R.string.question_14) ?: "",
                currContext()?.getString(R.string.answer_14) ?: ""
            ),
            Triple(
                R.drawable.ic_round_video_settings_24,
                currContext()?.getString(R.string.question_15) ?: "",
                currContext()?.getString(R.string.answer_15) ?: ""
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ThemeManager(this).applyTheme()
        binding = ActivityFaqBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initActivity(this)

        binding.devsTitle2.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.devsRecyclerView.adapter = FAQAdapter(faqs, supportFragmentManager)
        binding.devsRecyclerView.layoutManager = LinearLayoutManager(this)
    }
}
