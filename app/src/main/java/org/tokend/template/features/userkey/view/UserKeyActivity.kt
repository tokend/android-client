package org.tokend.template.features.userkey.view

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.rengwuxian.materialedittext.MaterialEditText
import kotlinx.android.synthetic.main.include_fingerprint_field_hint.*
import org.tokend.template.activities.BaseActivity
import org.tokend.template.extensions.setErrorAndFocus
import org.tokend.template.features.userkey.logic.persistence.UserKeyPersistor
import org.tokend.template.logic.fingerprint.FingerprintAuthManager
import org.tokend.template.view.FingerprintIndicatorManager
import org.tokend.template.view.util.AnimationUtil
import org.tokend.template.view.util.input.SoftInputUtil

abstract class UserKeyActivity : BaseActivity() {
    override val allowUnauthorized: Boolean = true

    abstract val errorMessage: String
    abstract val entryEditText: MaterialEditText

    abstract val userKeyPersistor: UserKeyPersistor

    protected val isRetry
        get() = intent.getBooleanExtra(IS_RETRY_EXTRA, false)

    protected lateinit var fingerprintAuthManager: FingerprintAuthManager
    protected lateinit var fingerprintIndicatorManager: FingerprintIndicatorManager

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
        fingerprintAuthManager =
                FingerprintAuthManager(this, userKeyPersistor)
        fingerprintIndicatorManager =
                FingerprintIndicatorManager(this, fingerprint_indicator, toastManager)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && fingerprintAuthManager.isAuthAvailable) {
            requestFingerprintAuth()
        }
    }

    protected open fun requestFingerprintAuth() {
        fingerprintAuthManager.requestAuthIfAvailable(
                onAuthStart = {
                    AnimationUtil.fadeInView(fingerprint_hint_layout)
                    fingerprintIndicatorManager.show()
                },
                onSuccess = { _, key -> onFingerprintAuthSuccess(key) },
                onError = {
                    onFingerprintAuthMessage(it)
                    fingerprintIndicatorManager.error()
                }
        )
    }

    protected open fun cancelFingerprintAuth() {
        fingerprintAuthManager.cancelAuth()
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