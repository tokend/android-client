package org.tokend.template.logic.fingerprint

import android.content.Context
import android.os.Build
import org.jetbrains.anko.defaultSharedPreferences
import org.tokend.template.logic.credentials.CredentialsProvider

/**
 * Manages fingerprint auth request to obtain saved credentials.
 */
class FingerprintAuthManager(
        applicationContext: Context,
        private val simpleCredentialsProvider: CredentialsProvider
) {
    private val fingerprintUtil = FingerprintUtil(applicationContext)
    private var isAuthCanceled = false

    private var successCallback: ((String, CharArray) -> Unit)? = null
    private var errorCallback: ((String?) -> Unit)? = null

    private val preferences = applicationContext.defaultSharedPreferences

    val isAuthAvailable: Boolean
        get() = preferences.getBoolean("fingerprint", true)
                && fingerprintUtil.isFingerprintAvailable
                && simpleCredentialsProvider.hasCredentials()

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

        fingerprintUtil.cancelAuth()

        successCallback = onSuccess
        errorCallback = onError
        isAuthCanceled = false

        if (isAuthAvailable && !isAuthCanceled) {
            onAuthStart()

            val handleErrorMessage: (String?) -> Unit = { errorMessage ->
                errorCallback?.invoke(errorMessage)
            }

            fingerprintUtil.requestAuth(
                    onSuccess = {
                        val (email, password) = simpleCredentialsProvider.getCredentials()
                        successCallback?.invoke(email, password)
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