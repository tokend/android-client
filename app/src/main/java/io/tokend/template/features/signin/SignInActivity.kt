package io.tokend.template.features.signin

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.tokend.template.BuildConfig
import io.tokend.template.R
import io.tokend.template.activities.BaseActivity
import io.tokend.template.extensions.*
import io.tokend.template.features.account.data.model.AccountRecord
import io.tokend.template.features.signin.logic.ResendVerificationEmailUseCase
import io.tokend.template.features.signin.logic.SignInMethod
import io.tokend.template.features.signin.logic.SignInUseCase
import io.tokend.template.features.urlconfig.view.NetworkFieldWrapper
import io.tokend.template.util.ObservableTransformers
import io.tokend.template.util.PermissionManager
import io.tokend.template.util.errorhandler.CompositeErrorHandler
import io.tokend.template.util.errorhandler.ErrorHandler
import io.tokend.template.util.errorhandler.SimpleErrorHandler
import io.tokend.template.util.navigation.Navigator
import io.tokend.template.view.util.ElevationUtil
import io.tokend.template.view.util.LoadingIndicatorManager
import io.tokend.template.view.util.input.EditTextErrorHandler
import io.tokend.template.view.util.input.SimpleTextWatcher
import io.tokend.template.view.util.input.SoftInputUtil
import kotlinx.android.synthetic.main.activity_sign_in.*
import kotlinx.android.synthetic.main.include_appbar_elevation.*
import kotlinx.android.synthetic.main.layout_network_field.*
import kotlinx.android.synthetic.main.layout_progress.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.sdk.api.wallets.model.EmailNotVerifiedException
import org.tokend.sdk.api.wallets.model.InvalidCredentialsException
import org.tokend.wallet.Account

class SignInActivity : BaseActivity() {
    override val allowUnauthorized = true

    private val loadingIndicator = LoadingIndicatorManager(
        showLoading = { progress.show() },
        hideLoading = { progress.hide() }
    )

    private val cameraPermission = PermissionManager(Manifest.permission.CAMERA, 404)

    private var isLoading: Boolean = false
        set(value) {
            field = value
            loadingIndicator.setLoading(value, "main")
            updateSignInAvailability()
        }

    private var canSignIn: Boolean = false
        set(value) {
            field = value
            sign_in_button.isEnabled = value
        }

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        window.setBackgroundDrawable(
            ColorDrawable(
                ContextCompat.getColor(this, R.color.background)
            )
        )

        setContentView(R.layout.activity_sign_in)
        setSupportActionBar(toolbar)
        setTitle(R.string.sign_in)

        initVersion()
        initFields()
        initButtons()

        ElevationUtil.initScrollElevation(scroll_view, appbar_elevation_view)

        canSignIn = false

        // Does nothing but EC engine warm up.
        Thread { Account.random() }.start()

        when (session.lastSignInMethod) {
            SignInMethod.LOCAL_ACCOUNT -> {
                openLocalAccountSignIn()
            }
            else -> credentialsPersistence.getSavedLogin()?.let {
                Navigator.from(this).toUnlock()
            }
        }
    }

    // region Init
    @SuppressLint("SetTextI18n")
    private fun initVersion() {
        app_version_text_view.text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    }

    private fun initFields() {
        initNetworkField()

        email_edit_text.requestFocus()
        SimpleTextWatcher {
            password_edit_text.error = null
            updateSignInAvailability()
        }.also {
            email_edit_text.addTextChangedListener(it)
            password_edit_text.addTextChangedListener(it)
        }

        password_edit_text.onEditorAction {
            tryToSignIn()
        }
    }

    private fun initNetworkField() {
        val wrapper = NetworkFieldWrapper(
            this,
            network_field_layout,
            cameraPermission,
            compositeDisposable,
            activityRequestsBag
        )
        wrapper.onNetworkUpdated {
            password_edit_text.error = null
            updateSignInAvailability()
        }
    }

    private fun initButtons() {
        sign_in_button.setOnClickListener {
            tryToSignIn()
        }

        sign_up_button.setOnClickListener {
            Navigator.from(this).openSignUp()
        }

        recovery_button.setOnClickListener {
            Navigator.from(this).openRecovery(email_edit_text.text.toString())
        }

        if (BuildConfig.ENABLE_LOCAL_ACCOUNT_SIGN_IN) {
            sign_in_with_local_account_button.setOnClickListener {
                openLocalAccountSignIn()
            }
        } else {
            sign_in_with_local_account_button.visibility = View.GONE
        }
    }
    // endregion

    private fun openLocalAccountSignIn() {
        Navigator.from(this)
            .openLocalAccountSignIn()
            .addTo(activityRequestsBag)
            .doOnSuccess { onSignInComplete() }
    }

    private fun updateSignInAvailability() {
        canSignIn = !isLoading
                && !email_edit_text.text.isNullOrBlank()
                && !password_edit_text.text.isNullOrEmpty()
                && !password_edit_text.hasError()
                && !email_edit_text.hasError()
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
            walletInfoPersistence,
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
                onError = signInErrorHandler::handleIfPossible
            )
            .addTo(compositeDisposable)
    }

    private fun onSignInComplete() {
        // KYC recovery check.
        val kycRecoveryStatus = repositoryProvider.account.item?.kycRecoveryStatus
        if (kycRecoveryStatus != null && kycRecoveryStatus != AccountRecord.KycRecoveryStatus.NONE) {
            showKycRecoveryStatusDialog(kycRecoveryStatus)
        } else {
            canSignIn = false
            Navigator.from(this).toMainActivity()
        }
    }

    private val signInErrorHandler: ErrorHandler
        get() = CompositeErrorHandler(
            EditTextErrorHandler(password_edit_text) { error ->
                when (error) {
                    is InvalidCredentialsException ->
                        getString(R.string.error_invalid_password)
                    else ->
                        null
                }
            },
            SimpleErrorHandler { error ->
                when (error) {
                    is EmailNotVerifiedException -> {
                        displayEmailNotVerifiedDialog(error.walletId)
                        true
                    }
                    else -> false
                }
            },
            errorHandlerFactory.getDefault()
        )
            .doOnSuccessfulHandle(this::updateSignInAvailability)

    private fun showKycRecoveryStatusDialog(status: AccountRecord.KycRecoveryStatus) {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.kyc_recovery_status_dialog_title)
            .setMessage(
                when (status) {
                    AccountRecord.KycRecoveryStatus.PENDING ->
                        R.string.kyc_recovery_pending_message
                    AccountRecord.KycRecoveryStatus.REJECTED,
                    AccountRecord.KycRecoveryStatus.PERMANENTLY_REJECTED,
                    ->
                        R.string.kyc_recovery_rejected_message
                    else ->
                        R.string.kyc_recovery_initiated_message
                }
            )
            .setPositiveButton(R.string.ok, null)
            .apply {
                if (status == AccountRecord.KycRecoveryStatus.INITIATED) {
                    setNeutralButton(R.string.open_action) { _, _ ->
                        browse(urlConfigProvider.getConfig().client)
                    }
                }
            }
            .show()
    }

    private fun updateAdditionalButtonsState(isEnabled: Boolean) {
        scan_qr_button.isEnabled = isEnabled
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        cameraPermission.handlePermissionResult(requestCode, permissions, grantResults)
    }
}
