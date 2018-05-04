package org.tokend.template.base.activities.signup

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import com.trello.rxlifecycle2.android.ActivityEvent
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity
import com.trello.rxlifecycle2.kotlin.bindUntilEvent
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.toSingle
import kotlinx.android.synthetic.main.activity_sign_up.*
import kotlinx.android.synthetic.main.layout_progress.*
import org.jetbrains.anko.browse
import org.jetbrains.anko.enabled
import org.jetbrains.anko.onCheckedChange
import org.jetbrains.anko.onClick
import org.tokend.sdk.federation.EmailAlreadyTakenException
import org.tokend.template.BuildConfig
import org.tokend.template.R
import org.tokend.template.base.logic.SignUpManager
import org.tokend.template.base.view.util.EditTextHelper
import org.tokend.template.base.view.util.LoadingIndicatorManager
import org.tokend.template.base.view.util.SimpleTextWatcher
import org.tokend.template.extensions.hasError
import org.tokend.template.extensions.setErrorAndFocus
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.ToastManager
import org.tokend.template.util.error_handlers.ErrorHandlerFactory
import org.tokend.wallet.Account

class SignUpActivity : RxAppCompatActivity() {
    companion object {
        private val SAVE_SEED_REQUEST = "save_recovery_seed".hashCode() and 0xffff
    }

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { progress.show() },
            hideLoading = { progress.hide() }
    )
    private var isLoading: Boolean = false
        set(value) {
            field = value
            loadingIndicator.setLoading(value, "main")
            updateSignUpAvailability()
        }

    private var canSignUp: Boolean = false
        set(value) {
            field = value
            sign_up_button.enabled = value
        }

    private var passwordsMatch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        initFields()
        initButtons()

        canSignUp = false
    }

    // region Init
    private fun initFields() {
        EditTextHelper.initEmailEditText(email_edit_text)
        EditTextHelper.initPasswordEditText(password_edit_text)

        email_edit_text.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                updateSignUpAvailability()
            }
        })

        val passwordTextWatcher = object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                checkPasswordsMatch()
                updateSignUpAvailability()
            }
        }

        password_edit_text.addTextChangedListener(passwordTextWatcher)
        confirm_password_edit_text.addTextChangedListener(passwordTextWatcher)

        terms_of_service_checkbox.onCheckedChange { _, _ ->
            updateSignUpAvailability()
        }
    }

    private fun initButtons() {
        sign_up_button.onClick {
            tryToSignUp()
        }

        sign_in_text_view.onClick {
            Navigator.toSignIn(this)
        }

        terms_text_view.onClick {
            browse(BuildConfig.TERMS_URL, true)
        }
    }
    // endregion

    private fun updateSignUpAvailability() {
        canSignUp = !isLoading
                && !email_edit_text.text.isBlank()
                && passwordsMatch && !password_edit_text.text.isEmpty()
                && !email_edit_text.hasError()
                && !password_edit_text.hasError()
                && terms_of_service_checkbox.isChecked
    }

    private fun checkPasswordsMatch() {
        passwordsMatch = password_edit_text.text.toString() ==
                confirm_password_edit_text.text.toString()

        if (!passwordsMatch && !confirm_password_edit_text.text.isEmpty()) {
            confirm_password_edit_text.error = getString(R.string.error_passwords_mismatch)
        } else {
            confirm_password_edit_text.error = null
        }
    }

    private fun tryToSignUp() {
        checkPasswordsMatch()
        updateSignUpAvailability()
        if (canSignUp) {
            signUp()
        }
    }

    private fun signUp() {
        val email = email_edit_text.text.toString()
        val password = password_edit_text.text.toString()

        var recoverySeed: String? = null
        Single.zip(getRandomAccount(), getRandomAccount(),
                BiFunction { key1: Account, key2: Account -> Pair(key1, key2) })
                .flatMapCompletable { (rootAccount, recoveryAccount) ->
                    recoverySeed = recoveryAccount.secretSeed
                    SignUpManager.signUp(email, password, rootAccount, recoveryAccount)
                }
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
                            recoverySeed.let {
                                if (it != null) {
                                    Navigator.openRecoverySeedSaving(this,
                                            SAVE_SEED_REQUEST, it)
                                } else {
                                    onSuccessfulSignUp()
                                }
                            }
                        },
                        onError = {
                            it.printStackTrace()
                            handleSignUpError(it)
                        }
                )
    }

    private fun getRandomAccount(): Single<Account> {
        return {
            Account.random()
        }.toSingle()
    }

    private fun handleSignUpError(error: Throwable) {
        when (error) {
            is EmailAlreadyTakenException ->
                email_edit_text.setErrorAndFocus(R.string.error_email_already_taken)
            else ->
                ErrorHandlerFactory.getDefault().handle(error)
        }
        updateSignUpAvailability()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SAVE_SEED_REQUEST) {
            onSuccessfulSignUp()
        }
    }

    private fun onSuccessfulSignUp() {
        ToastManager.long(R.string.check_your_email_to_verify_account)
        Navigator.toSignIn(this)
    }
}
