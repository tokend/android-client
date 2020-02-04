package org.tokend.template.features.signin

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.view.View
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_sign_in.*
import kotlinx.android.synthetic.main.include_appbar_elevation.*
import kotlinx.android.synthetic.main.layout_network_field.*
import kotlinx.android.synthetic.main.layout_progress.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.browse
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.enabled
import org.jetbrains.anko.onClick
import org.tokend.sdk.api.wallets.model.EmailNotVerifiedException
import org.tokend.sdk.api.wallets.model.InvalidCredentialsException
import org.tokend.template.BuildConfig
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.data.model.AccountRecord
import org.tokend.template.extensions.getChars
import org.tokend.template.extensions.hasError
import org.tokend.template.extensions.onEditorAction
import org.tokend.template.extensions.setErrorAndFocus
import org.tokend.template.features.signin.logic.ResendVerificationEmailUseCase
import org.tokend.template.features.signin.logic.SignInMethod
import org.tokend.template.features.signin.logic.SignInUseCase
import org.tokend.template.logic.UrlConfigManager
import org.tokend.template.logic.fingerprint.FingerprintAuthManager
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.PermissionManager
import org.tokend.template.util.QrScannerUtil
import org.tokend.template.view.util.ElevationUtil
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.view.util.input.SimpleTextWatcher
import org.tokend.template.view.util.input.SoftInputUtil
import org.tokend.wallet.Account

class SignInActivity : BaseActivity() {
    override val allowUnauthorized = true

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { progress.show() },
            hideLoading = { progress.hide() }
    )

    private val cameraPermission = PermissionManager(Manifest.permission.CAMERA, 404)

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
        window.setBackgroundDrawable(ColorDrawable(
                ContextCompat.getColor(this, R.color.background)
        ))

        setContentView(R.layout.activity_sign_in)
        setSupportActionBar(toolbar)
        setTitle(R.string.sign_in)

        fingerprintAuthManager = FingerprintAuthManager(applicationContext, credentialsPersistence)
        urlConfigManager = UrlConfigManager(urlConfigProvider, urlConfigPersistor)
        urlConfigManager.onConfigUpdated {
            initNetworkField()
            password_edit_text.error = null
            updateSignInAvailability()
        }

        initVersion()
        initFields()
        initButtons()

        ElevationUtil.initScrollElevation(scroll_view, appbar_elevation_view)

        canSignIn = false

        // Does nothing but EC engine warm up.
        doAsync { Account.random() }

        when (session.lastSignInMethod) {
            SignInMethod.AUTHENTICATOR -> {
                openAuthenticatorSignIn()
            }
            SignInMethod.LOCAL_ACCOUNT -> {
                openLocalAccountSignIn()
            }
            else -> credentialsPersistence.getSavedEmail()?.let {
                Navigator.from(this).toUnlock()
            }
        }
    }

    // region Init
    private fun initVersion() {
        app_version_text_view.text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    }

    private fun initFields() {
        initNetworkField()

        email_edit_text.requestFocus()
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
            Navigator.from(this).openSignUp()
        }

        recovery_button.onClick {
            Navigator.from(this).openRecovery(email_edit_text.text.toString())
        }

        if (BuildConfig.ENABLE_AUTHENTICATOR_AUTH) {
            sign_in_with_authenticator_button.onClick {
                openAuthenticatorSignIn()
            }
        } else {
            sign_in_with_authenticator_button.visibility = View.GONE
        }

        if (BuildConfig.ENABLE_LOCAL_ACCOUNT_SIGN_IN) {
            sign_in_with_local_account_button.onClick {
                openLocalAccountSignIn()
            }
        } else {
            sign_in_with_local_account_button.visibility = View.GONE
        }
    }
    // endregion

    private fun tryOpenQrScanner() {
        cameraPermission.check(this) {
            QrScannerUtil.openScanner(this)
        }
    }

    private fun openAuthenticatorSignIn() {
        Navigator.from(this).openAuthenticatorSignIn(SIGN_IN_WITH_AUTHENTICATOR_REQUEST)
    }

    private fun openLocalAccountSignIn() {
        Navigator.from(this).openLocalAccountSignIn(SIGN_IN_WITH_LOCAL_ACCOUNT_REQUEST)
    }

    private fun updateSignInAvailability() {
        canSignIn = !isLoading
                && !email_edit_text.text.isNullOrBlank()
                && !password_edit_text.text.isNullOrEmpty()
                && !password_edit_text.hasError()
                && !email_edit_text.hasError()
                && urlConfigProvider.hasConfig()
    }

    private fun tryToSignIn() {
        when {
            email_edit_text.text.isNullOrBlank() ->
                email_edit_text.setErrorAndFocus(R.string.error_cannot_be_empty)
            password_edit_text.text.isNullOrEmpty() ->
                password_edit_text.setErrorAndFocus(R.string.error_cannot_be_empty)
            canSignIn -> {
                SoftInputUtil.hideSoftInput(this)
                signIn()
            }
        }
    }

    private fun signIn() {
        val email = email_edit_text.text.toString()
        val password = password_edit_text.text.getChars()

        SignInUseCase(
                email,
                password,
                apiProvider.getKeyServer(),
                session,
                credentialsPersistence,
                postSignInManagerFactory.get()::doPostSignIn
        )
                .perform()
                .compose(ObservableTransformers.defaultSchedulersCompletable())
                .doOnSubscribe {
                    isLoading = true
                    updateAdditionalButtonsState(false)
                }
                .doOnTerminate {
                    isLoading = false
                    updateAdditionalButtonsState(true)
                    password_edit_text.text.getChars()
                }
                .subscribeBy(
                        onComplete = this::onSignInComplete,
                        onError = this::handleSignInError
                )
                .addTo(compositeDisposable)
    }

    private fun onSignInComplete() {
        // KYC recovery check.
        val kycRecoveryStatus = repositoryProvider.account().item?.kycRecoveryStatus
        if (kycRecoveryStatus != null && kycRecoveryStatus != AccountRecord.KycRecoveryStatus.NONE) {
            showKycRecoveryStatusDialog(kycRecoveryStatus)
        } else {
            canSignIn = false
            Navigator.from(this).toMainActivity()
        }
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

    private fun showKycRecoveryStatusDialog(status: AccountRecord.KycRecoveryStatus) {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.kyc_recovery_status_dialog_title)
                .setMessage(when (status) {
                    AccountRecord.KycRecoveryStatus.PENDING ->
                        R.string.kyc_recovery_pending_message
                    AccountRecord.KycRecoveryStatus.REJECTED,
                    AccountRecord.KycRecoveryStatus.PERMANENTLY_REJECTED ->
                        R.string.kyc_recovery_rejected_message
                    else ->
                        R.string.kyc_recovery_initiated_message
                })
                .setPositiveButton(R.string.ok, null)
                .apply {
                    if (status == AccountRecord.KycRecoveryStatus.INITIATED) {
                        setNeutralButton(R.string.open_action) { _, _ ->
                            browse(urlConfigProvider.getConfig().client, true)
                        }
                    }
                }
                .show()
    }

    private fun updateAdditionalButtonsState(isEnabled: Boolean) {
        scan_qr_button.isEnabled = isEnabled
        sign_in_with_authenticator_button.isEnabled = isEnabled
        sign_up_button.isEnabled = isEnabled
        recovery_button.isEnabled = isEnabled

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
        ResendVerificationEmailUseCase(
                walletId,
                apiProvider.getApi()
        )
                .perform()
                .compose(ObservableTransformers.defaultSchedulersCompletable())
                .subscribeBy(
                        onComplete = {
                            toastManager.long(R.string.check_your_email_to_verify_account)
                        },
                        onError = {
                            errorHandlerFactory.getDefault().handle(it)
                        }
                )
                .addTo(compositeDisposable)
    }

    override fun onResume() {
        super.onResume()
        initNetworkField()
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

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                SIGN_IN_WITH_AUTHENTICATOR_REQUEST,
                SIGN_IN_WITH_LOCAL_ACCOUNT_REQUEST -> {
                    onSignInComplete()
                }
            }
        }
    }

    companion object {
        private val SIGN_IN_WITH_AUTHENTICATOR_REQUEST =
                "sign_in_with_authenticator".hashCode() and 0xffff
        private val SIGN_IN_WITH_LOCAL_ACCOUNT_REQUEST =
                "sign_in_with_local_account".hashCode() and 0xffff
    }
}
