package ani.dantotsu.others.calc

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.doOnAttach
import androidx.core.view.updateLayoutParams
import ani.dantotsu.MainActivity
import ani.dantotsu.R
import ani.dantotsu.databinding.ActivityCalcBinding
import ani.dantotsu.getThemeColor
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.util.NumberConverter.Companion.toBinary
import ani.dantotsu.util.NumberConverter.Companion.toHex

class CalcActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCalcBinding
    private lateinit var code: String
    private val handler = Handler(Looper.getMainLooper())
    private val runnable = Runnable {
        success()
    }
    private val stack = CalcStack()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        binding = ActivityCalcBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.doOnAttach {
            initActivity(this)
            binding.displayContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin += statusBarHeight
            }
            binding.buttonContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin += navBarHeight
            }
        }
        code = intent.getStringExtra("code") ?: "0"

        binding.apply {
            button0.setOnClickListener { stack.add('0'); updateDisplay() }
            button1.setOnClickListener { stack.add('1'); updateDisplay() }
            button2.setOnClickListener { stack.add('2'); updateDisplay() }
            button3.setOnClickListener { stack.add('3'); updateDisplay() }
            button4.setOnClickListener { stack.add('4'); updateDisplay() }
            button5.setOnClickListener { stack.add('5'); updateDisplay() }
            button6.setOnClickListener { stack.add('6'); updateDisplay() }
            button7.setOnClickListener { stack.add('7'); updateDisplay() }
            button8.setOnClickListener { stack.add('8'); updateDisplay() }
            button9.setOnClickListener { stack.add('9'); updateDisplay() }
            buttonDot.setOnClickListener { stack.add('.'); updateDisplay() }
            buttonAdd.setOnClickListener { stack.add('+'); updateDisplay() }
            buttonSubtract.setOnClickListener { stack.add('-'); updateDisplay() }
            buttonMultiply.setOnClickListener { stack.add('*'); updateDisplay() }
            buttonDivide.setOnClickListener { stack.add('/'); updateDisplay() }
            buttonEquals.setOnClickListener {
                try {
                    val ans = stack.evaluate()
                    updateDisplay()
                    binding.displayBinary.text = ans.toBinary()
                    binding.displayHex.text = ans.toHex()
                } catch (e: Exception) {
                    display.text = getString(R.string.error)
                }
            }
            buttonClear.setOnClickListener {
                stack.clear()
                binding.displayBinary.text = ""
                binding.displayHex.text = ""
                binding.display.text = "0"
            }
            if (PrefManager.getVal(PrefName.OverridePassword, false)) {
                buttonClear.setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            handler.postDelayed(runnable, 10000)
                            true
                        }

                        MotionEvent.ACTION_UP -> {
                            v.performClick()
                            handler.removeCallbacks(runnable)
                            true
                        }

                        MotionEvent.ACTION_CANCEL -> {
                            handler.removeCallbacks(runnable)
                            true
                        }

                        else -> false
                    }
                }
            }
            buttonBackspace.setOnClickListener {
                stack.remove()
                updateDisplay()
            }
            display.text = "0"
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasPermission) {
            success()
        }
        if (PrefManager.getVal(PrefName.BiometricToken, "").isNotEmpty()) {
            val bioMetricPrompt = BiometricPromptUtils.createBiometricPrompt(this) {
                success()
            }
            val promptInfo = BiometricPromptUtils.createPromptInfo(this)
            bioMetricPrompt.authenticate(promptInfo)
        }
    }

    private fun success() {
        hasPermission = true
        ContextCompat.startActivity(
            this,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK),
            null
        )
    }

    private fun updateDisplay() {
        if (stack.getExpression().isEmpty()) {
            binding.display.text = "0"
            return
        }
        val expression = stack.getExpression().replace("*", "×").replace("/", "÷")
        val spannable = SpannableString(expression)

        val operators = arrayOf('+', '-', '×', '÷')

        expression.forEachIndexed { index, char ->
            if (char in operators) {
                val color = getThemeColor(com.google.android.material.R.attr.colorSecondary)
                spannable.setSpan(
                    ForegroundColorSpan(color),
                    index,
                    index + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        binding.display.text = spannable
        val text = binding.display.text.toString()
        if (text == code) {
            success()
        }
    }

    companion object {
        var hasPermission = false
    }
}