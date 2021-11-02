package org.tokend.template.features.signin.unlock

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_unlock_app.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import kotlinx.android.synthetic.main.layout_error_empty_view.view.*
import org.tokend.crypto.ecdsa.erase
import org.tokend.sdk.api.wallets.model.InvalidCredentialsException
import org.tokend.template.App
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.extensions.getChars
import org.tokend.template.extensions.hasError
import org.tokend.template.extensions.onEditorAction
import org.tokend.template.extensions.setErrorAndFocus
import org.tokend.template.features.signin.logic.SignInUseCase
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.ProfileUtil
import org.tokend.template.util.biometric.BiometricAuthManager
import org.tokend.template.util.navigation.Navigator
import org.tokend.template.view.dialog.SignOutDialogFactory
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.view.util.input.SimpleTextWatcher
import org.tokend.template.view.util.input.SoftInputUtil

class UnlockAppActivity : BaseActivity() {
    override val allowUnauthorized: Boolean = true

    private val biometricAuthManager by lazy {
        BiometricAuthManager(this, credentialsPersistence)
    }

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { showLoading() },
            hideLoading = { showPasswordUnlock(clear = false) }
    )

    private var isLoading: Boolean
        get() = loadingIndicator.isLoading
        set(value) {
            loadingIndicator.setLoading(value)
        }

    private var canUnlockWithPassword = false
        set(value) {
            field = value
            unlock_button.isEnabled = value
        }

    private lateinit var login: String
    private var lastEnteredPassword: CharArray? = null
    private var lastTimePasswordWasEnteredManually: Boolean = false

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_unlock_app)

        val login = credentialsPersistence.getSavedLogin()
        if (login == null) {
            errorHandlerFactory.getDefault().handle(
                    IllegalStateException("No saved email, unlock is not possible")
            )
            (application as? App)?.signOut(this)
            return
        }
        this.login = login

        initViews()

        if (!backgroundLockManager.isBackgroundLockEnabled && credentialsPersistence.hasSavedPassword()) {
            performAutoUnlock()
        } else if (biometricAuthManager.isAuthPossible) {
            requestFingerprintAuthIfAvailable()
        }
    }

    private fun initViews() {
        initButtons()
        initErrorEmptyView()
        user_email_text.text = login
        ProfileUtil.setAvatar(user_logo, login, urlConfigProvider, activeKycPersistence.loadItem())
    }

    private fun initButtons() {
        password_edit_text.addTextChangedListener(SimpleTextWatcher {
            password_edit_text.error = null
            updatePasswordUnlockAvailability()
        })

        password_edit_text.onEditorAction {
            tryToUnlockWithPassword()
        }

        unlock_button.setOnClickListener {
            tryToUnlockWithPassword()
        }

        sign_out_button.setOnClickListener {
            if (!loadingIndicator.isLoading) {
                SignOutDialogFactory.getDialog(this) {
                    (application as App).signOut(this)
                }.show()
            }
        }

        recovery_button.setOnClickListener {
            Navigator.from(this).openRecovery(login)
        }

        fingerprint_button.visibility =
                if (biometricAuthManager.isAuthPossible)
                    View.VISIBLE
                else
                    View.GONE
        fingerprint_button.setOnClickListener {
            requestFingerprintAuthIfAvailable()
        }
    }

    private fun initErrorEmptyView() {
        error_empty_view.setErrorDrawable(null)
        error_empty_view.layoutParams = error_empty_view.layoutParams.apply {
            height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
        error_empty_view.error_action_button.layoutParams =
                error_empty_view.error_action_button.layoutParams.let {
                    (it as? ViewGroup.MarginLayoutParams)?.apply {
                        topMargin = resources.getDimensionPixelSize(R.dimen.quadra_margin)
                    }
                }
    }

    private fun showPasswordUnlock(clear: Boolean = true) {
        if (clear) {
            password_edit_text.text?.clear()
        }
        password_edit_text.error = null
        password_edit_text.requestFocus()
        SoftInputUtil.showSoftInputOnView(password_edit_text)

        progress.visibility = View.GONE
        password_layout.visibility = View.VISIBLE
        error_empty_view.hide()
        sign_out_button.visibility = View.VISIBLE
    }

    private fun showLoading() {
        SoftInputUtil.hideSoftInput(this)
        progress.visibility = View.VISIBLE
        password_layout.visibility = View.INVISIBLE
        error_empty_view.hide()
        sign_out_button.visibility = View.GONE
    }

    private fun showError(error: Throwable) {
        progress.visibility = View.GONE
        password_layout.visibility = View.INVISIBLE
        error_empty_view.showError(error, errorHandlerFactory.getDefault()) {
            lastEnteredPassword?.also {
                unlock(login, it, isPasswordEnteredManually = lastTimePasswordWasEnteredManually)
            }
        }
        sign_out_button.visibility = View.VISIBLE
    }

    private fun updatePasswordUnlockAvailability() {
        canUnlockWithPassword = !password_edit_text.text.isNullOrEmpty()
                && !password_edit_text.hasError()
    }

    private fun requestFingerprintAuthIfAvailable() {
        biometricAuthManager.requestAuthIfPossible(
                onSuccess = { email, password ->
                    unlock(email, password, isPasswordEnteredManually = false)
                },
                onError = {
                    toastManager.short(it?.toString())
                },
        )
    }

    private fun cancelFingerprintAuth() {
        biometricAuthManager.cancelAuth()
    }

    private fun tryToUnlockWithPassword() {
        if (password_edit_text.text.isNullOrEmpty()) {
            password_edit_text.setErrorAndFocus(R.string.error_cannot_be_empty)
        }

        updatePasswordUnlockAvailability()

        if (canUnlockWithPassword) {
            SoftInputUtil.hideSoftInput(this)
            unlock(login, password_edit_text.text.getChars(), isPasswordEnteredManually = true)
        }
    }

    private fun performAutoUnlock() {
        val savedPassword = credentialsPersistence.getSavedPassword()!!
        unlock(login, savedPassword, isPasswordEnteredManually = false)
    }

    private fun unlock(email: String, password: CharArray, isPasswordEnteredManually: Boolean) {
        if (lastEnteredPassword !== password) {
            lastEnteredPassword?.erase()
        }
        lastEnteredPassword = password
        lastTimePasswordWasEnteredManually = isPasswordEnteredManually

        SignInUseCase(
                email,
                password,
                apiProvider.getKeyServer(),
                session,
                credentialsPersistence,
                walletInfoPersistence,
                postSignInManagerFactory.get()::doPostSignIn
        )
                .perform()
                .compose(ObservableTransformers.defaultSchedulersCompletable())
                .doOnSubscribe {
                    isLoading = true
                    SoftInputUtil.hideSoftInput(this)
                }
                .subscribeBy(
                        onComplete = {
                            Navigator.from(this).toMainActivity()
                        },
                        onError = {
                            isLoading = false
                            onUnlockError(it, isPasswordEnteredManually)
                        }
                )
                .addTo(compositeDisposable)
    }

    private fun onUnlockError(error: Throwable, isPasswordEnteredManually: Boolean) {
        when {
            error is InvalidCredentialsException -> {
                if (isPasswordEnteredManually) {
                    showPasswordUnlock(clear = false)
                    password_edit_text.setErrorAndFocus(R.string.error_invalid_password)
                } else {
                    showPasswordUnlock(clear = true)
                    password_edit_text.setErrorAndFocus(R.string.error_password_changed)
                }
                updatePasswordUnlockAvailability()
            }

            isPasswordEnteredManually -> {
                errorHandlerFactory.getDefault().handle(error)
                showPasswordUnlock(clear = false)
            }

            else -> {
                showError(error)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        cancelFingerprintAuth()
    }

    override fun onDestroy() {
        super.onDestroy()
        lastEnteredPassword?.erase()
    }
}
