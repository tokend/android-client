package org.tokend.template.logic.persistance

import android.content.Context
import android.os.Build
import org.jetbrains.anko.defaultSharedPreferences

/**
 * Manages fingerprint auth request to obtain saved credentials.
 */
class FingerprintAuthManager(
        private val applicationContext: Context,
        private val credentialsPersistor: CredentialsPersistor
) {
    private val fingerprintUtil = FingerprintUtil(applicationContext)
    private var isAuthCanceled = false

    private var successCallback: ((String, CharArray) -> Unit)? = null
    private var errorCallback: ((String?) -> Unit)? = null

    /**
     * @param onAuthStart will be called when auth is available and started
     * @param onSuccess will be called after successful auth, receives
     * saved email and password
     * @param onError will be called on auth error, receives system error message
     */
    fun requestAuthIfAvailable(onAuthStart: () -> Unit,
                               onSuccess: (String, CharArray) -> Unit,
                               onError: (String?) -> Unit) {


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }

        val preferences = applicationContext.defaultSharedPreferences
        if(!preferences.getBoolean("fingerprint", true)) {
            return
        }

        fingerprintUtil.cancelAuth()

        successCallback = onSuccess
        errorCallback = onError
        isAuthCanceled = false

        val savedEmail = credentialsPersistor.getSavedEmail()
                ?: return

        val fingerprintAuthAvailable = credentialsPersistor.hasSavedPassword()
                && fingerprintUtil.isFingerprintAvailable

        if (fingerprintAuthAvailable && !isAuthCanceled) {
            onAuthStart()

            val handleErrorMessage: (String?) -> Unit = { errorMessage ->
                    errorCallback?.invoke(errorMessage)
            }

            fingerprintUtil.requestAuth(
                    onSuccess = {
                        val password = credentialsPersistor.getSavedPassword()
                                ?: CharArray(0)
                        successCallback?.invoke(savedEmail, password)
                    },
                    onError = handleErrorMessage,
                    onHelp = handleErrorMessage
            )
        }
    }

    fun cancelAuth() {
        fingerprintUtil.cancelAuth()
        isAuthCanceled = true
        successCallback = null
        errorCallback = null
    }
}