package ani.dantotsu.others

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ani.dantotsu.BottomSheetDialogFragment
import ani.dantotsu.databinding.BottomSheetCustomBinding

open class CustomBottomDialog : BottomSheetDialogFragment() {
    private var _binding: BottomSheetCustomBinding? = null
    private val binding get() = _binding!!

    private val viewList = mutableListOf<View>()
    fun addView(view: View) {
        viewList.add(view)
    }

    var title: String? = null
    fun setTitleText(string: String) {
        title = string
    }

    private var checkText: String? = null
    private var checkChecked: Boolean = false
    private var checkCallback: ((Boolean) -> Unit)? = null

    fun setCheck(text: String, checked: Boolean, callback: ((Boolean) -> Unit)) {
        checkText = text
        checkChecked = checked
        checkCallback = callback
    }

    private var negativeText: String? = null
    private var negativeCallback: (() -> Unit)? = null
    fun setNegativeButton(text: String, callback: (() -> Unit)) {
        negativeText = text
        negativeCallback = callback
    }

    private var positiveText: String? = null
    private var positiveCallback: (() -> Unit)? = null
    fun setPositiveButton(text: String, callback: (() -> Unit)) {
        positiveText = text
        positiveCallback = callback
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetCustomBinding.inflate(inflater, container, false)
        val window = dialog?.window
        window?.statusBarColor = Color.TRANSPARENT
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.bottomSheerCustomTitle.text = title
        viewList.forEach {
            binding.bottomDialogCustomContainer.addView(it)
        }
        if (checkText != null) binding.bottomDialogCustomCheckBox.apply {
            visibility = View.VISIBLE
            text = checkText
            isChecked = checkChecked
            setOnCheckedChangeListener { _, checked ->
                checkCallback?.invoke(checked)
            }
        }

        if (negativeText != null) binding.bottomDialogCustomNegative.apply {
            visibility = View.VISIBLE
            text = negativeText
            setOnClickListener {
                negativeCallback?.invoke()
            }
        }

        if (positiveText != null) binding.bottomDialogCustomPositive.apply {
            visibility = View.VISIBLE
            text = positiveText
            setOnClickListener {
                positiveCallback?.invoke()
            }
        }

    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

    companion object {
        fun newInstance() = CustomBottomDialog()
    }

}