package org.tokend.template.features.changepassword

import android.app.Activity
import android.os.Bundle
import android.view.View
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_change_password.*
import kotlinx.android.synthetic.main.include_appbar_elevation.*
import kotlinx.android.synthetic.main.layout_progress.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.crypto.ecdsa.erase
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
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.biometric.BiometricAuthManager
import org.tokend.template.util.validator.PasswordValidator
import org.tokend.template.view.util.ElevationUtil
import org.tokend.template.view.util.LoadingIndicatorManager
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

    private var canChange: Boolean = false
        set(value) {
            field = value
            change_password_button.isEnabled = value
        }

    private val biometricAuthManager by lazy {
        BiometricAuthManager(this, credentialsPersistence)
    }

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_change_password)
        setSupportActionBar(toolbar)
        setTitle(R.string.change_password_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initFields()
        initButtons()
        ElevationUtil.initScrollElevation(scroll_view, appbar_elevation_view)

        canChange = false
    }

    // region Init
    private fun initFields() {
        current_password_edit_text.addTextChangedListener(SimpleTextWatcher {
            current_password_edit_text.error = null
            updateChangeAvailability()
        })

        new_password_edit_text.addTextChangedListener(SimpleTextWatcher {
            new_password_edit_text.error = null
            updateChangeAvailability()
        })
        new_password_edit_text.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                checkPasswordStrength()
                updateChangeAvailability()
            }
        }
        confirm_password_edit_text.addTextChangedListener(SimpleTextWatcher {
            updateChangeAvailability()
        })
        confirm_password_edit_text.onEditorAction {
            tryToChangePassword()
        }
    }

    private fun initButtons() {
        change_password_button.setOnClickListener {
            tryToChangePassword()
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
    // endregion

    // region Fingerprint
    private fun requestFingerprintAuthIfAvailable() {
        biometricAuthManager.requestAuthIfPossible(
                onSuccess = { _, password ->
                    current_password_edit_text.setText(password, 0, password.size)
                    new_password_edit_text.requestFocus()
                    password.fill('0')
                },
                onError = {
                    toastManager.short(it?.toString())
                }
        )
    }

    private fun cancelFingerprintAuth() {
        biometricAuthManager.cancelAuth()
    }
    // endregion

    private fun checkPasswordStrength() {
        if (!PasswordValidator.isValid(new_password_edit_text.text)) {
            new_password_edit_text.error = getString(R.string.error_weak_password)
        } else {
            new_password_edit_text.error = null
        }
    }

    private fun updateChangeAvailability() {
        current_password_edit_text.isEnabled = !isLoading

        canChange = !isLoading
                && !current_password_edit_text.hasError()
                && !new_password_edit_text.text.isNullOrEmpty()
                && !new_password_edit_text.hasError()
                && arePasswordsMatch()
                && !confirm_password_edit_text.hasError()
    }

    private fun arePasswordsMatch(): Boolean {
        val password = new_password_edit_text.text.getChars()
        val confirmation = confirm_password_edit_text.text.getChars()
        return password.contentEquals(confirmation).also {
            password.erase()
            confirmation.erase()
        }
    }

    private fun tryToChangePassword() {
        checkPasswordStrength()
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
                    newPassword.erase()
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

    override fun onTfaRequired(
            exception: NeedTfaException,
            verifierInterface: TfaVerifier.Interface,
    ) {
        when (exception.factorType) {
            TfaFactor.Type.PASSWORD -> verifyPasswordTfaSilently(exception, verifierInterface)
            else -> super.onTfaRequired(exception, verifierInterface)
        }
    }

    private fun verifyPasswordTfaSilently(
            exception: NeedTfaException,
            verifierInterface: TfaVerifier.Interface,
    ) {
        walletInfoProvider.getWalletInfo()?.email?.let { email ->
            val passwordChars = current_password_edit_text.text.getChars()
            Thread {
                val otp = PasswordTfaOtpGenerator().generate(exception, email, passwordChars)

                if (!isFinishing) {
                    verifierInterface.verify(otp,
                            onError = {
                                passwordChars.erase()
                                current_password_edit_text
                                        .setErrorAndFocus(R.string.error_invalid_password)
                                verifierInterface.cancelVerification()
                            },
                            onSuccess = {
                                passwordChars.erase()
                            })
                } else {
                    verifierInterface.cancelVerification()
                    passwordChars.erase()
                }
            }.start()
        }
    }

    private fun finishWithSuccess() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onPause() {
        super.onPause()
        cancelFingerprintAuth()
    }
}
