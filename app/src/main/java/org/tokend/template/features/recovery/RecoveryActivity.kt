package org.tokend.template.features.recovery

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.View
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_recovery.*
import kotlinx.android.synthetic.main.include_appbar_elevation.*
import kotlinx.android.synthetic.main.layout_network_field.*
import kotlinx.android.synthetic.main.layout_progress.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.enabled
import org.jetbrains.anko.onClick
import org.tokend.sdk.api.wallets.model.EmailNotVerifiedException
import org.tokend.sdk.api.wallets.model.InvalidCredentialsException
import org.tokend.template.BuildConfig
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.extensions.*
import org.tokend.template.features.recovery.logic.RecoveryUseCase
import org.tokend.template.logic.UrlConfigManager
import org.tokend.template.logic.wallet.WalletUpdateManager
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.PermissionManager
import org.tokend.template.util.QrScannerUtil
import org.tokend.template.view.util.ElevationUtil
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.view.util.input.EditTextHelper
import org.tokend.template.view.util.input.SimpleTextWatcher
import org.tokend.template.view.util.input.SoftInputUtil
import org.tokend.wallet.Base32Check

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
            recovery_button.enabled = value
        }

    private var passwordsMatch = false

    private val cameraPermission = PermissionManager(Manifest.permission.CAMERA, 404)
    private lateinit var urlConfigManager: UrlConfigManager

    private val email: String
        get() = intent.getStringExtra(EMAIL_EXTRA, "")

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_recovery)
        setSupportActionBar(toolbar)
        setTitle(R.string.recover_account)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        urlConfigManager = UrlConfigManager(urlConfigProvider, urlConfigPersistor)
        urlConfigManager.onConfigUpdated {
            initNetworkField()

            seed_edit_text.error = null
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

        EditTextHelper.initPasswordEditText(password_edit_text)

        if (email.isNotEmpty()) {
            email_edit_text.setText(email)
            email_edit_text.setSelection(email.length)
            seed_edit_text.requestFocus()
        } else {
            email_edit_text.requestFocus()
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

    private fun initNetworkField() {
        if (BuildConfig.IS_NETWORK_SPECIFIED_BY_USER) {
            network_field_layout.visibility = View.VISIBLE
            urlConfigManager.get()?.also { network_edit_text.setText(it.apiDomain) }

            scan_qr_button.onClick {
                tryOpenQrScanner()
            }
        } else {
            network_field_layout.visibility = View.GONE
        }
    }

    private fun initButtons() {
        recovery_button.onClick {
            tryToRecover()
        }

        sign_in_text_view.onClick {
            Navigator.from(this).toSignIn(false)
        }
    }
    // endregion

    private fun tryOpenQrScanner() {
        cameraPermission.check(this) {
            QrScannerUtil.openScanner(this)
        }
    }

    private fun checkPasswordsMatch() {
        val passwordChars = password_edit_text.text.getChars()
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
                && !email_edit_text.text.isNullOrBlank()
                && !seed_edit_text.text.isNullOrBlank()
                && passwordsMatch
                && !password_edit_text.text.isNullOrEmpty()
                && !email_edit_text.hasError()
                && !password_edit_text.hasError()
                && !seed_edit_text.hasError()
                && urlConfigProvider.hasConfig()
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

        val walletUpdateManager = WalletUpdateManager(repositoryProvider.systemInfo())

        RecoveryUseCase(
                email,
                seed,
                password,
                walletUpdateManager,
                urlConfigProvider
        )
                .perform()
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
                            toastManager.long(R.string.password_was_changed)
                            finishWithSuccess()
                        },
                        onError = {
                            handleRecoveryError(it)
                            updateRecoveryAvailability()
                        }
                )
                .addTo(compositeDisposable)
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
            else -> errorHandlerFactory.getDefault().handle(error)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        cameraPermission.handlePermissionResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        QrScannerUtil.getStringFromResult(requestCode, resultCode, data)?.also {
            urlConfigManager.setFromJson(it)
        }
    }
}
