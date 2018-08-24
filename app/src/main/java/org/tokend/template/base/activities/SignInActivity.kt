package org.tokend.template.base.activities

import android.Manifest
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.view.View
import com.google.zxing.integration.android.IntentIntegrator
import io.reactivex.Single
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_sign_in.*
import kotlinx.android.synthetic.main.layout_network_field.*
import kotlinx.android.synthetic.main.layout_progress.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.enabled
import org.jetbrains.anko.onClick
import org.tokend.sdk.federation.EmailNotVerifiedException
import org.tokend.sdk.federation.InvalidCredentialsException
import org.tokend.template.BuildConfig
import org.tokend.template.R
import org.tokend.template.base.logic.SignInManager
import org.tokend.template.base.logic.UrlConfigManager
import org.tokend.template.base.logic.persistance.FingerprintAuthManager
import org.tokend.template.base.view.util.AnimationUtil
import org.tokend.template.base.view.util.LoadingIndicatorManager
import org.tokend.template.base.view.util.SimpleTextWatcher
import org.tokend.template.extensions.*
import org.tokend.template.util.*
import org.tokend.template.util.error_handlers.ErrorHandlerFactory
import org.tokend.wallet.Account

class SignInActivity : BaseActivity() {
    override val allowUnauthorized = true

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { progress.show() },
            hideLoading = { progress.hide() }
    )

    private val cameraPermission = Permission(Manifest.permission.CAMERA, 404)

    private lateinit var fingerprintAuthManager: FingerprintAuthManager
    private lateinit var urlConfigManager: UrlConfigManager

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

        fingerprintAuthManager = FingerprintAuthManager(this, credentialsPersistor)
        urlConfigManager = UrlConfigManager(urlConfigProvider, urlConfigPersistor)
        urlConfigManager.onConfigUpdated {
            initNetworkField()
            password_edit_text.error = null
            updateSignInAvailability()
        }

        initVersion()
        initFields()
        initButtons()

        canSignIn = false

        // Does nothing but EC engine warm up.
        doAsync { Account.random() }
    }

    // region Init
    private fun initVersion() {
        app_version_text_view.text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    }

    private fun initFields() {
        initNetworkField()

        credentialsPersistor.getSavedEmail()?.also { savedEmail ->
            email_edit_text.setText(savedEmail)
            password_edit_text.requestFocus()
        }

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

        fingerprint_indicator.onClick {
            ToastManager(this).short(R.string.touch_sensor)
        }
    }
    // endregion

    // region Fingerprint
    private fun requestFingerprintAuthIfAvailable() {
        fingerprint_indicator.visibility = View.GONE
        fingerprintAuthManager.requestAuthIfAvailable(
                onAuthStart = { AnimationUtil.fadeInView(fingerprint_indicator) },
                onSuccess = { email, password ->
                    tryToSignInWithCredentials(email, password)
                    password.fill('0')
                },
                onError = { ToastManager(this).short(it) }
        )
    }

    private fun cancelFingerprintAuth() {
        fingerprintAuthManager.cancelAuth()
    }
    // endregion

    // region QR
    private fun tryOpenQrScanner() {
        cameraPermission.check(this) {
            openQrScanner()
        }
    }

    private fun openQrScanner() {
        IntentIntegrator(this)
                .setBeepEnabled(false)
                .setOrientationLocked(false)
                .setPrompt("")
                .initiateScan()
    }
    // endregion

    private fun updateSignInAvailability() {
        canSignIn = !isLoading
                && email_edit_text.text.isNotBlank()
                && password_edit_text.text.isNotEmpty()
                && !password_edit_text.hasError()
                && !email_edit_text.hasError()
                && urlConfigProvider.hasConfig()
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

    private fun tryToSignInWithCredentials(email: String, password: CharArray) {
        email_edit_text.setText(email)
        password_edit_text.setText(password, 0, password.size)
        password_edit_text.setSelection(password.size)

        SoftInputUtil.hideSoftInput(this)

        tryToSignIn()
    }

    private fun signIn() {
        val email = email_edit_text.text.toString()

        val signInManager = SignInManager(
                apiProvider.getKeyStorage(), walletInfoProvider, accountProvider,
                credentialsPersistor
        )

        Single.just(true)
                .flatMapCompletable {
                    // Obtain password on every flow run.
                    val password = password_edit_text.text.getChars()
                    signInManager
                            .signIn(email, password)
                            // Clear password immediately after sign in.
                            .doOnComplete { password.fill('0') }
                }
                .andThen(signInManager.doPostSignIn(repositoryProvider))
                .compose(ObservableTransformers.defaultSchedulersCompletable())
                .retry { attempt, error ->
                    error is SignInManager.InvalidPersistedCredentialsException && attempt == 1
                }
                .doOnSubscribe {
                    isLoading = true
                }
                .doOnError {
                    isLoading = false
                }
                .subscribeBy(
                        onComplete = {
                            canSignIn = false
                            isLoading = false
                            Navigator.toMainActivity(this)
                        },
                        onError = {
                            it.printStackTrace()
                            handleSignInError(it)
                        }
                )
                .addTo(compositeDisposable)
    }

    private fun handleSignInError(error: Throwable) {
        when (error) {
            is InvalidCredentialsException ->
                password_edit_text.setErrorAndFocus(R.string.error_invalid_password)
            is EmailNotVerifiedException ->
                displayEmailNotVerifiedDialog(error.walletId)
            else ->
                errorHandlerFactory.getDefault().handle(error)
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
        apiProvider.getApi()
                .requestVerificationLink(walletId)
                .toCompletable()
                .compose(ObservableTransformers.defaultSchedulersCompletable())
                .subscribeBy(
                        onComplete = {
                            ToastManager(this).long(R.string.check_your_email_to_verify_account)
                        },
                        onError = {
                            errorHandlerFactory.getDefault().handle(it)
                        }
                )
                .addTo(compositeDisposable)
    }

    override fun onResume() {
        super.onResume()
        requestFingerprintAuthIfAvailable()
        initNetworkField()
    }

    override fun onPause() {
        super.onPause()
        cancelFingerprintAuth()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        cameraPermission.handlePermissionResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val scanResult = IntentIntegrator
                .parseActivityResult(requestCode, resultCode, data)

        if (scanResult != null && scanResult.contents != null) {
            urlConfigManager.setFromJson(scanResult.contents)
        }
    }
}
