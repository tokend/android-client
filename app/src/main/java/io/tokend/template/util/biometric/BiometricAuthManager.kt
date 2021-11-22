package io.tokend.template.util.biometric

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import io.tokend.template.R
import io.tokend.template.extensions.defaultSharedPreferences
import io.tokend.template.logic.credentials.providers.CredentialsProvider

/**
 * Manages biometric auth request to obtain saved credentials.
 */
class BiometricAuthManager {
    private val appSharedPreferences: SharedPreferences
    private val credentialsProvider: CredentialsProvider
    private val context: Context
    private val biometricManager: BiometricManager
    private val biometricPrompt: BiometricPrompt

    private var successCallback: ((String, CharArray) -> Unit)? = null
    private var errorCallback: ((CharSequence?) -> Unit)? = null
    private var userCancelCallback: (() -> Unit)? = null

    constructor(
        activity: FragmentActivity,
        credentialsProvider: CredentialsProvider,
    ) {
        this.context = activity
        this.appSharedPreferences = this.context.defaultSharedPreferences
        this.credentialsProvider = credentialsProvider
        this.biometricManager = BiometricManager.from(context)
        this.biometricPrompt = BiometricPrompt(
            activity, ContextCompat.getMainExecutor(context),
            getAuthCallback(credentialsProvider)
        )
    }

    constructor(
        fragment: Fragment,
        credentialsProvider: CredentialsProvider,
    ) {
        this.context = fragment.requireContext()
        this.appSharedPreferences = this.context.defaultSharedPreferences
        this.credentialsProvider = credentialsProvider
        this.biometricManager = BiometricManager.from(context)
        this.biometricPrompt = BiometricPrompt(
            fragment, ContextCompat.getMainExecutor(context),
            getAuthCallback(credentialsProvider)
        )
    }

    var isAuthEnabled: Boolean
        get() = appSharedPreferences.getBoolean(PREFERENCE_KEY, IS_ENABLED_BY_DEFAULT)
        set(value) {
            appSharedPreferences.edit().putBoolean(PREFERENCE_KEY, value).apply()
        }

    /**
     * true if auth is enabled and it is possible
     * to get saved credentials with biometric prompt.
     *
     * @see isAuthEnabled
     */
    val isAuthPossible: Boolean
        get() = isAuthEnabled
                && isHardwareDetected
                && credentialsProvider.hasCredentials()

    val isHardwareDetected: Boolean
        get() = biometricManager.canAuthenticate(ALLOWED_AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    /**
     * @param onStart will be called when auth is available and started
     * @param onSuccess will be called after successful auth, receives
     * saved login and password
     * @param onUserCancel will be called when auth is canceled by user, not by [cancelAuth]
     * @param onError will be called on auth error, receives system error message
     */
    fun requestAuthIfPossible(
        onSuccess: (String, CharArray) -> Unit,
        onUserCancel: () -> Unit = {},
        onError: (CharSequence?) -> Unit = {},
        onStart: () -> Unit = {},
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }

        successCallback = onSuccess
        errorCallback = onError
        userCancelCallback = onUserCancel

        if (isAuthPossible) {
            onStart()

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
                .setConfirmationRequired(false)
                .setTitle(context.getString(R.string.use_biometrics))
                .setNegativeButtonText(context.getString(R.string.use_password))
                .build()

            biometricPrompt.authenticate(promptInfo)
        }
    }

    fun cancelAuth() {
        biometricPrompt.cancelAuthentication()
        successCallback = null
        errorCallback = null
    }

    private fun getAuthCallback(credentialsProvider: CredentialsProvider) =
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val (login, password) = credentialsProvider.getCredentials()
                successCallback?.invoke(login, password)
            }

            override fun onAuthenticationError(
                errorCode: Int,
                errString: CharSequence,
            ) {
                if (errorCode in setOf(
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    )
                ) {
                    userCancelCallback?.invoke()
                } else {
                    errorCallback?.invoke(errString)
                }
            }

            override fun onAuthenticationFailed() {
                errorCallback?.invoke(null)
            }
        }

    companion object {
        const val PREFERENCE_KEY = "fingerprint"
        const val IS_ENABLED_BY_DEFAULT = true
        private const val ALLOWED_AUTHENTICATORS =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
    }
}