package io.tokend.template.features.recovery

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.view.View
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.tokend.template.BuildConfig
import io.tokend.template.R
import io.tokend.template.activities.BaseActivity
import io.tokend.template.extensions.getChars
import io.tokend.template.extensions.getStringExtra
import io.tokend.template.extensions.hasError
import io.tokend.template.extensions.onEditorAction
import io.tokend.template.features.recovery.logic.RecoverPasswordUseCase
import io.tokend.template.features.urlconfig.logic.UrlConfigManager
import io.tokend.template.util.ObservableTransformers
import io.tokend.template.util.PermissionManager
import io.tokend.template.util.QrScannerUtil
import io.tokend.template.util.errorhandler.CompositeErrorHandler
import io.tokend.template.util.errorhandler.ErrorHandler
import io.tokend.template.util.navigation.Navigator
import io.tokend.template.util.validator.PasswordValidator
import io.tokend.template.view.util.ElevationUtil
import io.tokend.template.view.util.LoadingIndicatorManager
import io.tokend.template.view.util.input.EditTextErrorHandler
import io.tokend.template.view.util.input.SimpleTextWatcher
import io.tokend.template.view.util.input.SoftInputUtil
import kotlinx.android.synthetic.main.activity_recovery.*
import kotlinx.android.synthetic.main.include_appbar_elevation.*
import kotlinx.android.synthetic.main.layout_network_field.*
import kotlinx.android.synthetic.main.layout_progress.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.crypto.ecdsa.erase
import org.tokend.sdk.api.wallets.model.EmailNotVerifiedException
import org.tokend.sdk.api.wallets.model.InvalidCredentialsException

class RecoveryActivity : BaseActivity() {
    override val allowUnauthorized = true

    companion object {
        private const val EMAIL_EXTRA = "email"

        fun getBundle(email: String?) = Bundle().apply {
            putString(EMAIL_EXTRA, email)
        }
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
            recovery_button.isEnabled = value
        }

    private val cameraPermission = PermissionManager(Manifest.permission.CAMERA, 404)
    private lateinit var urlConfigManager: UrlConfigManager

    private val email: String
        get() = intent.getStringExtra(EMAIL_EXTRA, "")

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_recovery)
        setSupportActionBar(toolbar)
        setTitle(R.string.recover_account)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        urlConfigManager = UrlConfigManager(urlConfigProvider, urlConfigPersistence)
        urlConfigManager.onConfigUpdated {
            initNetworkField()

            email_edit_text.text = email_edit_text.text

            updateRecoveryAvailability()
        }

        initFields()
        initButtons()
        ElevationUtil.initScrollElevation(scroll_view, appbar_elevation_view)

        canRecover = false
    }

    // region Init
    private fun initFields() {
        initNetworkField()

        if (email.isNotEmpty()) {
            email_edit_text.setText(email)
            email_edit_text.setSelection(email.length)
            password_edit_text.requestFocus()
        } else {
            email_edit_text.requestFocus()
        }

        email_edit_text.addTextChangedListener(SimpleTextWatcher {
            email_edit_text.error = null
            updateRecoveryAvailability()
        })

        password_edit_text.addTextChangedListener(SimpleTextWatcher {
            password_edit_text.error = null
            updateRecoveryAvailability()
        })
        password_edit_text.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                checkPasswordStrength()
                updateRecoveryAvailability()
            }
        }
        confirm_password_edit_text.addTextChangedListener(SimpleTextWatcher {
            updateRecoveryAvailability()
        })
        confirm_password_edit_text.onEditorAction {
            tryToRecover()
        }
    }

    private fun initNetworkField() {
        if (BuildConfig.IS_NETWORK_SPECIFIED_BY_USER) {
            network_field_layout.visibility = View.VISIBLE
            urlConfigManager.get()?.also { network_edit_text.setText(it.apiDomain) }

            scan_qr_button.setOnClickListener {
                tryOpenQrScanner()
            }
        } else {
            network_field_layout.visibility = View.GONE
        }
    }

    private fun initButtons() {
        recovery_button.setOnClickListener {
            tryToRecover()
        }

        sign_in_text_view.setOnClickListener {
            Navigator.from(this).toSignIn(false)
        }
    }
    // endregion

    private fun tryOpenQrScanner() {
        cameraPermission.check(this) {
            QrScannerUtil.openScanner(this)
                .addTo(activityRequestsBag)
                .doOnSuccess { urlConfigManager.setFromJson(it) }
        }
    }

    private fun checkPasswordStrength() {
        if (!PasswordValidator.isValid(password_edit_text.text)) {
            password_edit_text.error = getString(R.string.error_weak_password)
        } else {
            password_edit_text.error = null
        }
    }

    private fun updateRecoveryAvailability() {
        canRecover = !isLoading
                && !email_edit_text.text.isNullOrBlank()
                && !password_edit_text.text.isNullOrEmpty()
                && !email_edit_text.hasError()
                && !password_edit_text.hasError()
                && arePasswordsMatch()
                && urlConfigProvider.hasConfig()
    }

    private fun arePasswordsMatch(): Boolean {
        val password = password_edit_text.text.getChars()
        val confirmation = confirm_password_edit_text.text.getChars()
        return password.contentEquals(confirmation).also {
            password.erase()
            confirmation.erase()
        }
    }

    private fun tryToRecover() {
        checkPasswordStrength()
        updateRecoveryAvailability()

        if (canRecover) {
            SoftInputUtil.hideSoftInput(this)
            recover()
        }
    }

    private fun recover() {
        val email = email_edit_text.text.toString()
        val password = password_edit_text.text.getChars()

        RecoverPasswordUseCase(
            email,
            password,
            apiProvider
        )
            .perform()
            .compose(ObservableTransformers.defaultSchedulersCompletable())
            .doOnSubscribe {
                isLoading = true
            }
            .doOnTerminate {
                isLoading = false
                password.erase()
            }
            .subscribeBy(
                onComplete = {
                    toastManager.long(R.string.kyc_recovery_initiated_message)
                    finishWithSuccess()
                },
                onError = recoveryErrorHandler::handleIfPossible
            )
            .addTo(compositeDisposable)
    }

    private fun finishWithSuccess() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    private val recoveryErrorHandler: ErrorHandler
        get() = CompositeErrorHandler(
            EditTextErrorHandler(email_edit_text) { error ->
                when (error) {
                    is InvalidCredentialsException ->
                        getString(R.string.error_invalid_email)
                    is EmailNotVerifiedException ->
                        getString(R.string.error_email_not_verified)
                    else ->
                        null
                }
            },
            errorHandlerFactory.getDefault()
        )
            .doOnSuccessfulHandle(this::updateRecoveryAvailability)

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        cameraPermission.handlePermissionResult(requestCode, permissions, grantResults)
    }
}
