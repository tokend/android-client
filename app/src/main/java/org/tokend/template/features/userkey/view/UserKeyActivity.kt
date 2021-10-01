package org.tokend.template.features.userkey.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.rengwuxian.materialedittext.MaterialEditText
import kotlinx.android.synthetic.main.include_biometrics_field_hint.*
import org.tokend.template.activities.BaseActivity
import org.tokend.template.extensions.setErrorAndFocus
import org.tokend.template.features.userkey.logic.persistence.UserKeyPersistence
import org.tokend.template.util.biometric.BiometricAuthManager
import org.tokend.template.view.util.input.SoftInputUtil

abstract class UserKeyActivity : BaseActivity() {
    override val allowUnauthorized: Boolean = true

    abstract val errorMessage: String
    abstract val entryEditText: MaterialEditText

    abstract val userKeyPersistence: UserKeyPersistence

    protected val isRetry
        get() = intent.getBooleanExtra(IS_RETRY_EXTRA, false)

    private val biometricAuthManager by lazy {
        BiometricAuthManager(this, userKeyPersistence)
    }

    protected open fun finishWithKey(key: CharArray) {
        setResult(
                Activity.RESULT_OK,
                Intent().putExtra(USER_KEY_RESULT_EXTRA, key)
        )
        finish()
    }

    protected open fun onUserKeyEntered(key: CharArray) {
        SoftInputUtil.hideSoftInput(this)
        finishWithKey(key)
    }

    protected open fun finishWithCancellation() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        if (biometricAuthManager.isAuthPossible) {
            biometrics_hint_layout.visibility = View.VISIBLE
            biometrics_hint_layout.setOnClickListener {
                requestFingerprintAuthIfAvailable()
            }
            requestFingerprintAuthIfAvailable()
        } else {
            biometrics_hint_layout.visibility = View.GONE
        }

        focusOnEditText()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishWithCancellation()
    }

    protected open fun focusOnEditText() {
        entryEditText.isFocusableInTouchMode = true
        if (isRetry) {
            entryEditText.setErrorAndFocus(errorMessage)
        } else {
            entryEditText.requestFocus()
            SoftInputUtil.showSoftInputOnView(entryEditText)
        }
    }

    // region Fingerprint
    protected open fun requestFingerprintAuthIfAvailable() {
        biometricAuthManager.requestAuthIfPossible(
                onError = {
                    toastManager.short(it?.toString())
                },
                onSuccess = { _, key ->
                    onFingerprintAuthSuccess(key)
                }
        )
    }

    private fun cancelFingerprintAuth() {
        biometricAuthManager.cancelAuth()
    }

    protected open fun onFingerprintAuthSuccess(userKey: CharArray) {
        onUserKeyEntered(userKey)
    }

    protected open fun onFingerprintAuthMessage(message: String?) {
        message ?: return
        toastManager.short(message)
    }
    // endregion

    override fun onResume() {
        super.onResume()
        requestFingerprintAuthIfAvailable()
    }

    override fun onPause() {
        super.onPause()
        cancelFingerprintAuth()
    }

    companion object {
        const val USER_KEY_RESULT_EXTRA = "user_key"
        const val IS_RETRY_EXTRA = "is_retry"
    }
}