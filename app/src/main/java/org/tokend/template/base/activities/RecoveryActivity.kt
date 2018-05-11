package org.tokend.template.base.activities

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_recovery.*
import kotlinx.android.synthetic.main.layout_progress.*
import org.jetbrains.anko.enabled
import org.jetbrains.anko.onClick
import org.tokend.sdk.federation.EmailNotVerifiedException
import org.tokend.sdk.federation.InvalidCredentialsException
import org.tokend.template.R
import org.tokend.template.base.logic.SignUpManager
import org.tokend.template.base.logic.WalletPasswordManager
import org.tokend.template.base.view.util.EditTextHelper
import org.tokend.template.base.view.util.LoadingIndicatorManager
import org.tokend.template.base.view.util.SimpleTextWatcher
import org.tokend.template.extensions.*
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.SoftInputUtil
import org.tokend.template.util.ToastManager
import org.tokend.template.util.error_handlers.ErrorHandlerFactory
import org.tokend.wallet.Base32Check

class RecoveryActivity : BaseActivity() {
    companion object {
        const val EMAIL_EXTRA = "email"
    }

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { progress.show() },
            hideLoading = { progress.hide() }
    )
    private var isLoading: Boolean = false
        set(value) {
            field = value
            loadingIndicator.setLoading(value, "main")
            updateRecoveryAvailability()
        }

    private var canRecover: Boolean = false
        set(value) {
            field = value
            recovery_button.enabled = value
        }

    private var passwordsMatch = false

    private val email: String
        get() = intent.getStringExtra(EMAIL_EXTRA, "")

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_recovery)

        initFields()
        initButtons()

        canRecover = false
    }

    // region Init
    private fun initFields() {
        EditTextHelper.initPasswordEditText(password_edit_text)

        if (!email.isEmpty()) {
            email_edit_text.setText(email)
            email_edit_text.setSelection(email.length)
        }

        email_edit_text.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                seed_edit_text.error = null
                updateRecoveryAvailability()
            }
        })

        val passwordTextWatcher = object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                checkPasswordsMatch()
                updateRecoveryAvailability()
            }
        }

        password_edit_text.addTextChangedListener(passwordTextWatcher)
        EditTextHelper.initPasswordEditText(password_edit_text)

        confirm_password_edit_text.addTextChangedListener(passwordTextWatcher)

        seed_edit_text.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                seed_edit_text.error = null
                updateRecoveryAvailability()
            }
        })

        confirm_password_edit_text.onEditorAction {
            tryToRecover()
        }
    }

    private fun initButtons() {
        recovery_button.onClick {
            tryToRecover()
        }

        sign_in_text_view.onClick {
            Navigator.toSignIn(this)
        }
    }
    // endregion

    private fun checkPasswordsMatch() {
        val passwordChars = password_edit_text.text.getChars()
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

    private fun checkSeed() {
        val seedChars = seed_edit_text.text.getChars()
        if (!Base32Check.isValid(Base32Check.VersionByte.SEED, seedChars)) {
            seed_edit_text.setErrorAndFocus(R.string.error_invalid_seed)
        } else {
            seed_edit_text.error = null
        }
        seedChars.fill('0')
    }

    private fun updateRecoveryAvailability() {
        canRecover = !isLoading
                && !email_edit_text.text.isBlank()
                && !seed_edit_text.text.isBlank()
                && passwordsMatch
                && !password_edit_text.text.isEmpty()
                && !email_edit_text.hasError()
                && !password_edit_text.hasError()
                && !seed_edit_text.hasError()
    }

    private fun tryToRecover() {
        checkPasswordsMatch()
        checkSeed()
        updateRecoveryAvailability()
        if (canRecover) {
            SoftInputUtil.hideSoftInput(this)
            recover()
        }
    }

    private fun recover() {
        val email = email_edit_text.text.toString()
        val seed = seed_edit_text.text.getChars()
        val password = password_edit_text.text.getChars()

        SignUpManager.getRandomAccount()
                .flatMapCompletable { account ->
                    WalletPasswordManager(repositoryProvider.systemInfo())
                            .restore(email, seed, account, password)
                }
                .compose(ObservableTransformers.defaultSchedulersCompletable())
                .doOnSubscribe {
                    isLoading = true
                }
                .doOnTerminate {
                    isLoading = false
                    seed.fill('0')
                    password.fill('0')
                }
                .subscribeBy(
                        onComplete = {
                            ToastManager.long(R.string.password_was_changed)
                            finishWithSuccess()
                        },
                        onError = {
                            handleRecoveryError(it)
                            updateRecoveryAvailability()
                        }
                )
    }

    private fun finishWithSuccess() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun handleRecoveryError(error: Throwable) {
        error.printStackTrace()
        when (error) {
            is InvalidCredentialsException ->
                when (error.credential) {
                    InvalidCredentialsException.Credential.EMAIL ->
                        email_edit_text.setErrorAndFocus(R.string.error_invalid_email)
                    InvalidCredentialsException.Credential.PASSWORD ->
                        seed_edit_text.setErrorAndFocus(R.string.error_invalid_seed)
                }
            is EmailNotVerifiedException ->
                email_edit_text.setErrorAndFocus(R.string.error_email_not_verified)
            else -> ErrorHandlerFactory.getDefault().handle(error)
        }
    }
}
