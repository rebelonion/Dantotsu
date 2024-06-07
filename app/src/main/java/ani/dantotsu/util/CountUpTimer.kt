package ani.dantotsu.util

import android.os.CountDownTimer

// https://stackoverflow.com/a/40422151/461982
abstract class CountUpTimer protected constructor(
    private val duration: Long
) : CountDownTimer(duration, INTERVAL_MS) {
    abstract fun onTick(second: Int)
    override fun onTick(msUntilFinished: Long) {
        val second = ((duration - msUntilFinished) / 1000).toInt()
        onTick(second)
    }

    override fun onFinish() {
        onTick(duration / 1000)
    }

    companion object {
        private const val INTERVAL_MS: Long = 1000
    }
}