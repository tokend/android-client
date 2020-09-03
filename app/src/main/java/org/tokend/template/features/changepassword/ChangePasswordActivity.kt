package org.tokend.template.features.changepassword

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_change_password.*
import kotlinx.android.synthetic.main.include_appbar_elevation.*
import kotlinx.android.synthetic.main.layout_progress.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.enabled
import org.jetbrains.anko.onClick
import org.tokend.sdk.api.tfa.model.TfaFactor
import org.tokend.sdk.tfa.NeedTfaException
import org.tokend.sdk.tfa.PasswordTfaOtpGenerator
import org.tokend.sdk.tfa.TfaVerifier
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.extensions.getChars
import org.tokend.template.extensions.hasError
import org.tokend.template.extensions.onEditorAction
import org.tokend.template.extensions.setErrorAndFocus
import org.tokend.template.logic.fingerprint.FingerprintAuthManager
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.FingerprintIndicatorManager
import org.tokend.template.view.util.ElevationUtil
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.view.util.input.EditTextHelper
import org.tokend.template.view.util.input.SimpleTextWatcher
import org.tokend.template.view.util.input.SoftInputUtil

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
    private lateinit var fingerprintIndicatorManager: FingerprintIndicatorManager

    private var canChange: Boolean = false
        set(value) {
            field = value
            change_password_button.enabled = value
        }

    private var passwordsMatch = false

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_change_password)
        setSupportActionBar(toolbar)
        setTitle(R.string.change_password_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initFields()
        initButtons()
        ElevationUtil.initScrollElevation(scroll_view, appbar_elevation_view)

        fingerprintAuthManager = FingerprintAuthManager(applicationContext, credentialsPersistence)
        fingerprintIndicatorManager =
                FingerprintIndicatorManager(applicationContext, fingerprint_indicator, toastManager)

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
        fingerprintIndicatorManager.hide()
        fingerprintAuthManager.requestAuthIfAvailable(
                onAuthStart = { fingerprintIndicatorManager.show() },
                onSuccess = { _, password ->
                    current_password_edit_text.setText(password, 0, password.size)
                    new_password_edit_text.requestFocus()
                    password.fill('0')
                },
                onError = {
                    toastManager.short(it)
                    fingerprintIndicatorManager.error()
                }
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

        if (!passwordsMatch && !confirm_password_edit_text.text.isNullOrEmpty()) {
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
                && !new_password_edit_text.text.isNullOrEmpty()
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
        val newPassword = new_password_edit_text.text.getChars()

        ChangePasswordUseCase(
                newPassword,
                apiProvider,
                accountProvider,
                walletInfoProvider,
                repositoryProvider,
                credentialsPersistence,
                walletInfoPersistence
        )
                .perform()
                .compose(ObservableTransformers.defaultSchedulersCompletable())
                .doOnSubscribe {
                    isLoading = true
                }
                .doOnTerminate {
                    isLoading = false
                    newPassword.fill('0')
                }
                .subscribeBy(
                        onComplete = {
                            toastManager.long(R.string.password_changed_successfully)
                            finishWithSuccess()
                        },
                        onError = { error ->
                            errorHandlerFactory.getDefault().handle(error)
                            updateChangeAvailability()
                        }
                )
                .addTo(compositeDisposable)
    }

    override fun onTfaRequired(exception: NeedTfaException,
                               verifierInterface: TfaVerifier.Interface) {
        when (exception.factorType) {
            TfaFactor.Type.PASSWORD -> verifyPasswordTfaSilently(exception, verifierInterface)
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
