package io.tokend.template.features.tfa.logic

import android.util.Log
import org.tokend.sdk.tfa.NeedTfaException
import org.tokend.sdk.tfa.TfaCallback
import org.tokend.sdk.tfa.TfaVerifier

/**
 * Application-wide TFA callback that allows screen-specific TFA handling.
 */
class AppTfaCallback : TfaCallback {
    private val handlers = linkedSetOf<TfaCallback>()

    /**
     * Registers provided callback as a top priority TFA handler.
     */
    fun registerHandler(tfaCallback: TfaCallback) {
        handlers.add(tfaCallback)
    }

    /**
     * Unregisters provided callback which makes previous one a top priority TFA handler.
     */
    fun unregisterHandler(tfaCallback: TfaCallback) {
        handlers.remove(tfaCallback)
    }

    override fun onTfaRequired(
        exception: NeedTfaException,
        verifierInterface: TfaVerifier.Interface
    ) {
        if (handlers.isNotEmpty()) {
            handlers.last().onTfaRequired(exception, verifierInterface)
        } else {
            Log.e(LOG_TAG, "No TFA handlers available, verification cancelled")
            verifierInterface.cancelVerification()
        }
    }

    companion object {
        private const val LOG_TAG = "AppTfaCallback"
    }
}