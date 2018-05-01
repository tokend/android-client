package org.tokend.template.activities

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import io.reactivex.Observable
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_sign_in.*
import kotlinx.android.synthetic.main.layout_progress.*
import org.jetbrains.anko.enabled
import org.jetbrains.anko.onClick
import org.tokend.template.BuildConfig
import org.tokend.template.R
import org.tokend.template.extensions.hasError
import org.tokend.template.extensions.onEditorAction
import org.tokend.template.extensions.setErrorAndFocus
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.SoftInputUtil
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.view.util.SimpleTextWatcher
import java.util.concurrent.TimeUnit

class SignInActivity : AppCompatActivity() {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        Observable.just(true)
                .delay(3, TimeUnit.SECONDS)
                .compose(ObservableTransformers.defaultSchedulers())
                .doOnSubscribe {
                    isLoading = true
                }
                .doOnTerminate {
                    isLoading = false
                }
                .subscribeBy {
                    Navigator.toWallet(this)
                }
    }
}
