package org.tokend.template.logic.fingerprint

import android.app.KeyguardManager
import android.content.Context
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat
import android.support.v4.os.CancellationSignal
import org.tokend.template.R

/**
 * Simplifies interaction with Android fingerprint api
 */
class FingerprintUtil(private val context: Context) {
    private val fingerprintManager: FingerprintManagerCompat =
            FingerprintManagerCompat.from(context)
    private val keyguardManager: KeyguardManager =
            context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

    private val isHardwareAvailable: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && fingerprintManager.isHardwareDetected

    private val isKeyguardSecure: Boolean
        get() = keyguardManager.isKeyguardSecure

    private val hasEnrolledFingerprints: Boolean
        get() = fingerprintManager.hasEnrolledFingerprints()

    val isFingerprintAvailable: Boolean
        get() {
            return isHardwareAvailable
                    && isKeyguardSecure
                    && hasEnrolledFingerprints
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        }

    private var authCancellationSignal: CancellationSignal? = null
    private fun requestAuth(callback: FingerprintManagerCompat.AuthenticationCallback) {
        authCancellationSignal?.cancel()
        authCancellationSignal = CancellationSignal()

        fingerprintManager.authenticate(null, 0,
                authCancellationSignal, callback, null)
    }

    /**
     * Requests fingerprint auth
     *
     * @see cancelAuth
     */
    fun requestAuth(onSuccess: () -> Unit,
                    onError: (String?) -> Unit,
                    onHelp: (String?) -> Unit) {
        val callback = object : FingerprintManagerCompat.AuthenticationCallback() {
            override fun onAuthenticationError(errMsgId: Int, errString: CharSequence?) {
                super.onAuthenticationError(errMsgId, errString)
                if (errMsgId != FingerprintManager.FINGERPRINT_ERROR_CANCELED) {
                    val message = when (errMsgId) {
                        FingerprintManager.FINGERPRINT_ERROR_LOCKOUT -> context.getString(R.string.error_fingerprint_locked)
                        else -> errString?.toString()
                    }
                    onError(message)
                }
            }

            override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult?) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationHelp(helpMsgId: Int, helpString: CharSequence?) {
                super.onAuthenticationHelp(helpMsgId, helpString)
                // Help messages with > 1000 code seems to be vendor-specific
                if (helpMsgId < 1000) {
                    onHelp(helpString?.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError(null)
            }
        }

        requestAuth(callback)
    }

    /**
     * Cancels current auth request
     */
    fun cancelAuth() {
        authCancellationSignal?.cancel()
    }
}