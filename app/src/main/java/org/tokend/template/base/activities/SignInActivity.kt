package org.tokend.template.base.activities

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.text.Editable
import com.trello.rxlifecycle2.android.ActivityEvent
import com.trello.rxlifecycle2.kotlin.bindUntilEvent
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_sign_in.*
import kotlinx.android.synthetic.main.layout_progress.*
import org.jetbrains.anko.enabled
import org.jetbrains.anko.onClick
import org.tokend.sdk.api.ApiFactory
import org.tokend.sdk.federation.EmailNotVerifiedException
import org.tokend.sdk.federation.InvalidCredentialsException
import org.tokend.template.BuildConfig
import org.tokend.template.R
import org.tokend.template.base.logic.SignInManager
import org.tokend.template.base.view.util.LoadingIndicatorManager
import org.tokend.template.base.view.util.SimpleTextWatcher
import org.tokend.template.extensions.hasError
import org.tokend.template.extensions.onEditorAction
import org.tokend.template.extensions.setErrorAndFocus
import org.tokend.template.extensions.toCompletable
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.SoftInputUtil
import org.tokend.template.util.ToastManager
import org.tokend.template.util.error_handlers.ErrorHandlerFactory

class SignInActivity : BaseActivity() {
    override val allowUnauthorized = true

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { progress.show() },
            hideLoading = { progress.hide() }
    )
    private var isLoading: Boolean = false
        set(value) {
            field = value
            loadingIndicator.setLoading(value, "main")
            updateSignInAvailability()
        }

    private var canSignIn: Boolean = false
        set(value) {
            field = value
            sign_in_button.enabled = value
        }

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_sign_in)
        window.setBackgroundDrawable(
                ColorDrawable(ContextCompat.getColor(this, R.color.white)))
        setTitle(R.string.sign_in)

        initVersion()
        initFields()
        initButtons()

        canSignIn = false
    }

    // region Init
    private fun initVersion() {
        app_version_text_view.text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    }

    private fun initFields() {
        object : SimpleTextWatcher() {
            override fun afterTextChanged(p0: Editable?) {
                password_edit_text.error = null
                updateSignInAvailability()
            }
        }.also {
            email_edit_text.addTextChangedListener(it)
            password_edit_text.addTextChangedListener(it)
        }

        password_edit_text.onEditorAction {
            tryToSignIn()
        }
    }

    private fun initButtons() {
        sign_in_button.onClick {
            tryToSignIn()
        }

        sign_up_button.onClick {
            Navigator.openSignUp(this)
        }

        recovery_button.onClick {
            Navigator.openRecovery(this,
                    email_edit_text.text.toString())
        }
    }
    // endregion

    private fun updateSignInAvailability() {
        canSignIn = !isLoading
                && email_edit_text.text.isNotBlank()
                && password_edit_text.text.isNotEmpty()
                && !password_edit_text.hasError()
                && !email_edit_text.hasError()
    }

    private fun tryToSignIn() {
        if (email_edit_text.text.isBlank()) {
            email_edit_text.setErrorAndFocus(R.string.error_cannot_be_empty)
        } else if (password_edit_text.text.isEmpty()) {
            password_edit_text.setErrorAndFocus(R.string.error_cannot_be_empty)
        } else if (canSignIn) {
            SoftInputUtil.hideSoftInput(this)
            signIn()
        }
    }

    private fun signIn() {
        val email = email_edit_text.text.toString()
        val passwordLength = password_edit_text.text.length
        val password = CharArray(passwordLength)
        password_edit_text.text.getChars(0, passwordLength, password, 0)

        val signInManager = SignInManager(
                apiProvider.getKeyStorage(), walletInfoProvider, accountProvider
        )

        signInManager
                .signIn(email, password)
                .doOnComplete { password.fill('0') }
                .andThen(signInManager.doPostSignIn(repositoryProvider))
                .bindUntilEvent(lifecycle(), ActivityEvent.DESTROY)
                .compose(ObservableTransformers.defaultSchedulersCompletable())
                .doOnSubscribe {
                    isLoading = true
                }
                .doOnTerminate {
                    isLoading = false
                }
                .subscribeBy(
                        onComplete = {
                            canSignIn = false
                            Navigator.toMainActivity(this)
                        },
                        onError = {
                            it.printStackTrace()
                            handleSignInError(it)
                        }
                )
    }

    private fun handleSignInError(error: Throwable) {
        when (error) {
            is InvalidCredentialsException ->
                password_edit_text.setErrorAndFocus(R.string.error_invalid_password)
            is EmailNotVerifiedException ->
                displayEmailNotVerifiedDialog(error.walletId)
            else ->
                ErrorHandlerFactory.getDefault().handle(error)
        }
        updateSignInAvailability()
    }

    private fun displayEmailNotVerifiedDialog(walletId: String) {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.error_email_not_verified)
                .setMessage(R.string.email_not_verified_explanation)
                .setPositiveButton(R.string.ok, null)
                .setNeutralButton(R.string.resend_letter) { _, _ ->
                    resendVerificationEmail(walletId)
                }
                .show()
    }

    private fun resendVerificationEmail(walletId: String) {
        ApiFactory.getApiService(BuildConfig.API_URL)
                .requestVerificationLink(walletId)
                .toCompletable()
                .bindUntilEvent(lifecycle(), ActivityEvent.DESTROY)
                .compose(ObservableTransformers.defaultSchedulersCompletable())
                .subscribeBy(
                        onComplete = {
                            ToastManager.long(R.string.check_your_email_to_verify_account)
                        },
                        onError = {
                            ErrorHandlerFactory.getDefault().handle(it)
                        }
                )
    }
}
