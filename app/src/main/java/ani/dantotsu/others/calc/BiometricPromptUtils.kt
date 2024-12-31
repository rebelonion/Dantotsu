package ani.dantotsu.others.calc

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import ani.dantotsu.R
import ani.dantotsu.util.Logger

object BiometricPromptUtils {
    private const val TAG = "BiometricPromptUtils"

    /**
     * Create a BiometricPrompt instance
     * @param activity: AppCompatActivity
     * @param processSuccess: success callback
     */
    fun createBiometricPrompt(
        activity: AppCompatActivity,
        processSuccess: (BiometricPrompt.AuthenticationResult) -> Unit
    ): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationError(errCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errCode, errString)
                Logger.log("$TAG errCode is $errCode and errString is: $errString")
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Logger.log("$TAG User biometric rejected.")
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Log.d(TAG, "Authentication was successful")
                processSuccess(result)
            }
        }
        return BiometricPrompt(activity, executor, callback)
    }

    /**
     * Create a BiometricPrompt.PromptInfo instance
     * @param activity: AppCompatActivity
     * @return BiometricPrompt.PromptInfo: instance
     */
    fun createPromptInfo(activity: AppCompatActivity): BiometricPrompt.PromptInfo =
        BiometricPrompt.PromptInfo.Builder().apply {
            setTitle(activity.getString(R.string.bio_prompt_info_title))
            setDescription(activity.getString(R.string.bio_prompt_info_desc))
            setConfirmationRequired(false)
            setNegativeButtonText(activity.getString(R.string.cancel))
        }.build()
}
