package org.tokend.template.features.signup

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.view.View
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_sign_up.*
import kotlinx.android.synthetic.main.include_appbar_elevation.*
import kotlinx.android.synthetic.main.layout_network_field.*
import kotlinx.android.synthetic.main.layout_progress.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.browse
import org.jetbrains.anko.enabled
import org.jetbrains.anko.onCheckedChange
import org.jetbrains.anko.onClick
import org.tokend.sdk.api.wallets.model.EmailAlreadyTakenException
import org.tokend.sdk.keyserver.models.WalletCreateResult
import org.tokend.template.BuildConfig
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.extensions.getChars
import org.tokend.template.extensions.hasError
import org.tokend.template.extensions.setErrorAndFocus
import org.tokend.template.features.signup.logic.SignUpUseCase
import org.tokend.template.logic.UrlConfigManager
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.PermissionManager
import org.tokend.template.util.QrScannerUtil
import org.tokend.template.view.util.ElevationUtil
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.view.util.input.EditTextHelper
import org.tokend.template.view.util.input.SimpleTextWatcher
import org.tokend.template.view.util.input.SoftInputUtil

class SignUpActivity : BaseActivity() {
    override val allowUnauthorized = true

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

    private val cameraPermission = PermissionManager(Manifest.permission.CAMERA, 404)
    private lateinit var urlConfigManager: UrlConfigManager

    private var toSignInOnResume: Boolean = false

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_sign_up)
        setSupportActionBar(toolbar)
        setTitle(R.string.sign_up)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        urlConfigManager = UrlConfigManager(urlConfigProvider, urlConfigPersistor)
        urlConfigManager.onConfigUpdated {
            initNetworkField()

            // Recall validation
            email_edit_text.text = email_edit_text.text

            updateSignUpAvailability()
        }

        initFields()
        initButtons()
        ElevationUtil.initScrollElevation(scroll_view, appbar_elevation_view)

        canSignUp = false
    }

    // region Init
    private fun initFields() {
        initNetworkField()

        EditTextHelper.initEmailEditText(email_edit_text)
        EditTextHelper.initPasswordEditText(password_edit_text)

        email_edit_text.requestFocus()
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
        sign_up_button.onClick {
            tryToSignUp()
        }

        sign_in_text_view.onClick {
            Navigator.from(this).toSignIn(false)
        }

        terms_text_view.onClick {
            if (urlConfigProvider.hasConfig()) {
                browse(urlConfigProvider.getConfig().terms, true)
            } else {
                toastManager.short(R.string.error_network_not_specified)
            }
        }
    }
    // endregion

    private fun tryOpenQrScanner() {
        cameraPermission.check(this) {
            QrScannerUtil.openScanner(this)
        }
    }

    private fun updateSignUpAvailability() {
        canSignUp = !isLoading
                && !email_edit_text.text.isNullOrBlank()
                && passwordsMatch && !password_edit_text.text.isNullOrEmpty()
                && !email_edit_text.hasError()
                && !password_edit_text.hasError()
                && terms_of_service_checkbox.isChecked
                && urlConfigProvider.hasConfig()
    }

    private fun checkPasswordsMatch() {
        passwordsMatch = password_edit_text.text.toString() ==
                confirm_password_edit_text.text.toString()

        if (!passwordsMatch && !confirm_password_edit_text.text.isNullOrEmpty()) {
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
        val password = password_edit_text.text.getChars()

        SoftInputUtil.hideSoftInput(this)

        SignUpUseCase(
                email,
                password,
                apiProvider.getKeyServer()
        )
                .perform()
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .doOnSubscribe {
                    isLoading = true
                }
                .doOnEvent { _, _ ->
                    isLoading = false
                    password.fill('0')
                }
                .subscribeBy(
                        onSuccess = this::onSuccessfulSignUp,
                        onError = this::handleSignUpError
                )
                .addTo(compositeDisposable)
    }

    private fun handleSignUpError(error: Throwable) {
        error.printStackTrace()

        when (error) {
            is EmailAlreadyTakenException ->
                email_edit_text.setErrorAndFocus(R.string.error_email_already_taken)
            else ->
                errorHandlerFactory.getDefault().handle(error)
        }

        updateSignUpAvailability()
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

    private fun onSuccessfulSignUp(walletCreateResult: WalletCreateResult) {
        if (walletCreateResult.walletData.attributes?.isVerified == true) {
            toastManager.long(R.string.account_created_successfully)
        } else {
            showNotVerifiedEmailDialogAndFinish()
        }
    }

    private fun showNotVerifiedEmailDialogAndFinish() {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.almost_done)
                .setMessage(R.string.check_your_email_to_verify_account)
                .setPositiveButton(R.string.ok) { _, _ ->
                    toSignIn()
                }
                .setNeutralButton(R.string.open_email_app) { _, _ ->
                    toSignInOnResume = true
                    startActivity(
                            Intent.createChooser(
                                    Intent(Intent.ACTION_MAIN)
                                            .addCategory(Intent.CATEGORY_APP_EMAIL),
                                    getString(R.string.open_email_app)
                            )
                    )
                }
                .setOnCancelListener {
                    toSignIn()
                }
                .show()
    }

    private fun toSignIn() {
        Navigator.from(this).toSignIn(false)
    }

    override fun onResume() {
        super.onResume()
        if (toSignInOnResume) {
            toSignIn()
        }
    }
}
