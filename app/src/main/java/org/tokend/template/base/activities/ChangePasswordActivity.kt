package org.tokend.template.base.activities

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.view.View
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_change_password.*
import kotlinx.android.synthetic.main.layout_progress.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.enabled
import org.jetbrains.anko.onClick
import org.tokend.sdk.api.tfa.TfaBackend
import org.tokend.sdk.api.tfa.TfaVerifier
import org.tokend.sdk.federation.NeedTfaException
import org.tokend.template.R
import org.tokend.template.base.logic.SignUpManager
import org.tokend.template.base.logic.WalletPasswordManager
import org.tokend.template.base.logic.persistance.FingerprintAuthManager
import org.tokend.template.base.tfa.PasswordTfaOtpGenerator
import org.tokend.template.base.view.util.AnimationUtil
import org.tokend.template.base.view.util.EditTextHelper
import org.tokend.template.base.view.util.LoadingIndicatorManager
import org.tokend.template.base.view.util.SimpleTextWatcher
import org.tokend.template.extensions.getChars
import org.tokend.template.extensions.hasError
import org.tokend.template.extensions.onEditorAction
import org.tokend.template.extensions.setErrorAndFocus
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.SoftInputUtil
import org.tokend.template.util.ToastManager
import org.tokend.template.util.error_handlers.ErrorHandlerFactory

class ChangePasswordActivity : BaseActivity() {
    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { progress.show() },
            hideLoading = { progress.hide() }
    )
    private var isLoading: Boolean = false
        set(value) {
            field = value
            loadingIndicator.setLoading(value)
            updateChangeAvailability()
        }

    private lateinit var fingerprintAuthManager: FingerprintAuthManager

    private var canChange: Boolean = false
        set(value) {
            field = value
            change_password_button.enabled = value
        }

    private var passwordsMatch = false

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_change_password)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initFields()
        initButtons()

        fingerprintAuthManager = FingerprintAuthManager(this, credentialsPersistor)

        canChange = false
    }

    // region Init
    private fun initFields() {
        EditTextHelper.initPasswordEditText(current_password_edit_text)
        current_password_edit_text.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                current_password_edit_text.error = null
                updateChangeAvailability()
            }
        })

        val passwordTextWatcher = object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                checkPasswordsMatch()
                updateChangeAvailability()
            }
        }

        new_password_edit_text.addTextChangedListener(passwordTextWatcher)
        EditTextHelper.initPasswordEditText(new_password_edit_text)
        confirm_password_edit_text.addTextChangedListener(passwordTextWatcher)

        confirm_password_edit_text.onEditorAction {
            tryToChangePassword()
        }
    }

    private fun initButtons() {
        change_password_button.onClick {
            tryToChangePassword()
        }
    }
    // endregion

    // region Fingerprint
    private fun requestFingerprintAuthIfAvailable() {
        fingerprint_indicator.visibility = View.GONE
        fingerprintAuthManager.requestAuthIfAvailable(
                onAuthStart = { AnimationUtil.fadeInView(fingerprint_indicator) },
                onSuccess = { _, password ->
                    current_password_edit_text.setText(password, 0, password.size)
                    new_password_edit_text.requestFocus()
                    password.fill('0')
                },
                onError = { ToastManager.short(it) }
        )
    }

    private fun cancelFingerprintAuth() {
        fingerprintAuthManager.cancelAuth()
    }
    // endregion

    private fun checkPasswordsMatch() {
        val passwordChars = new_password_edit_text.text.getChars()
        val confirmationChars = confirm_password_edit_text.text.getChars()

        passwordsMatch = passwordChars.contentEquals(confirmationChars)

        passwordChars.fill('0')
        confirmationChars.fill('0')

        if (!passwordsMatch && !confirm_password_edit_text.text.isEmpty()) {
            confirm_password_edit_text.error = getString(R.string.error_passwords_mismatch)
        } else {
            confirm_password_edit_text.error = null
        }
    }

    private fun updateChangeAvailability() {
        current_password_edit_text.isEnabled = !isLoading

        canChange = !isLoading
                && !current_password_edit_text.hasError()
                && passwordsMatch
                && !new_password_edit_text.text.isEmpty()
                && !new_password_edit_text.hasError()
                && !confirm_password_edit_text.hasError()
    }

    private fun tryToChangePassword() {
        checkPasswordsMatch()
        updateChangeAvailability()

        if (canChange) {
            SoftInputUtil.hideSoftInput(this)
            changePassword()
        }
    }

    private fun changePassword() {
        val passwordChars = new_password_edit_text.text.getChars()
        val walletPasswordManager =
                WalletPasswordManager(repositoryProvider.systemInfo(), urlConfigProvider)

        SignUpManager.getRandomAccount()
                .flatMapCompletable { account ->
                    walletPasswordManager.changePassword(
                            apiProvider,
                            accountProvider,
                            walletInfoProvider,
                            credentialsPersistor,
                            account,
                            passwordChars
                    )
                }
                .compose(ObservableTransformers.defaultSchedulersCompletable())
                .doOnSubscribe {
                    isLoading = true
                }
                .doOnTerminate {
                    isLoading = false
                    passwordChars.fill('0')
                }
                .subscribeBy(
                        onComplete = {
                            ToastManager.long(R.string.password_was_changed)
                            finishWithSuccess()
                        },
                        onError = { error ->
                            ErrorHandlerFactory.getDefault().handle(error)
                            updateChangeAvailability()
                        }
                )
                .addTo(compositeDisposable)
    }

    override fun onTfaRequired(exception: NeedTfaException,
                               verifierInterface: TfaVerifier.Interface) {
        when (exception.backendType) {
            TfaBackend.Type.PASSWORD -> verifyPasswordTfaSilently(exception, verifierInterface)
            else -> super.onTfaRequired(exception, verifierInterface)
        }
    }

    private fun verifyPasswordTfaSilently(exception: NeedTfaException,
                                          verifierInterface: TfaVerifier.Interface) {
        walletInfoProvider.getWalletInfo()?.email?.let { email ->
            val passwordChars = current_password_edit_text.text.getChars()
            doAsync {
                val otp = PasswordTfaOtpGenerator().generate(exception, email, passwordChars)

                if (!isFinishing) {
                    verifierInterface.verify(otp,
                            onError = {
                                passwordChars.fill('0')
                                current_password_edit_text
                                        .setErrorAndFocus(R.string.error_invalid_password)
                                verifierInterface.cancelVerification()
                            },
                            onSuccess = {
                                passwordChars.fill('0')
                            })
                } else {
                    verifierInterface.cancelVerification()
                    passwordChars.fill('0')
                }
            }
        }
    }

    private fun finishWithSuccess() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onResume() {
        super.onResume()
        requestFingerprintAuthIfAvailable()
    }

    override fun onPause() {
        super.onPause()
        cancelFingerprintAuth()
    }
}
