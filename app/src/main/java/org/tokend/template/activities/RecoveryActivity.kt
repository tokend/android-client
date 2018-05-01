package org.tokend.template.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import kotlinx.android.synthetic.main.activity_recovery.*
import kotlinx.android.synthetic.main.layout_progress.*
import org.jetbrains.anko.enabled
import org.jetbrains.anko.onClick
import org.tokend.template.R
import org.tokend.template.extensions.getStringExtra
import org.tokend.template.extensions.hasError
import org.tokend.template.util.Navigator
import org.tokend.template.view.util.EditTextHelper
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.view.util.SimpleTextWatcher

class RecoveryActivity : AppCompatActivity() {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        passwordsMatch = password_edit_text.text.toString() ==
                confirm_password_edit_text.text.toString()

        if (!passwordsMatch && !confirm_password_edit_text.text.isEmpty()) {
            confirm_password_edit_text.error = getString(R.string.error_passwords_mismatch)
        } else {
            confirm_password_edit_text.error = null
        }
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
        updateRecoveryAvailability()
        if (canRecover) {
            recover()
        }
    }

    private fun recover() {

    }
}
