package org.tokend.template.features.signup

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_sign_up.*
import kotlinx.android.synthetic.main.include_appbar_elevation.*
import kotlinx.android.synthetic.main.layout_network_field.*
import kotlinx.android.synthetic.main.layout_progress.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.crypto.ecdsa.erase
import org.tokend.sdk.api.wallets.model.EmailAlreadyTakenException
import org.tokend.sdk.keyserver.KeyServer
import org.tokend.sdk.keyserver.models.WalletCreateResult
import org.tokend.template.BuildConfig
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.extensions.browse
import org.tokend.template.extensions.getChars
import org.tokend.template.extensions.hasError
import org.tokend.template.features.signin.logic.SignInUseCase
import org.tokend.template.features.signup.logic.SignUpUseCase
import org.tokend.template.features.urlconfig.logic.UrlConfigManager
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.PermissionManager
import org.tokend.template.util.QrScannerUtil
import org.tokend.template.util.errorhandler.CompositeErrorHandler
import org.tokend.template.util.errorhandler.ErrorHandler
import org.tokend.template.util.navigation.Navigator
import org.tokend.template.util.validator.PasswordValidator
import org.tokend.template.view.util.ElevationUtil
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.view.util.input.EditTextErrorHandler
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
            sign_up_button.isEnabled = value
        }

    private val cameraPermission = PermissionManager(Manifest.permission.CAMERA, 404)
    private lateinit var urlConfigManager: UrlConfigManager

    private var toSignInOnResume: Boolean = false

    private var email: String? = null
    private var password: CharArray? = null

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_sign_up)
        setSupportActionBar(toolbar)
        setTitle(R.string.sign_up)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        urlConfigManager = UrlConfigManager(urlConfigProvider, urlConfigPersistence)
        urlConfigManager.onConfigUpdated {
            initNetworkField()
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

        email_edit_text.requestFocus()
        email_edit_text.addTextChangedListener(SimpleTextWatcher {
            email_edit_text.error = null
            updateSignUpAvailability()
        })

        password_edit_text.addTextChangedListener(SimpleTextWatcher {
            password_edit_text.error = null
            updateSignUpAvailability()
        })
        password_edit_text.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                checkPasswordStrength()
                updateSignUpAvailability()
            }
        }
        confirm_password_edit_text.addTextChangedListener(SimpleTextWatcher {
            updateSignUpAvailability()
        })

        terms_of_service_checkbox.setOnCheckedChangeListener { _, _ ->
            updateSignUpAvailability()
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
        sign_up_button.setOnClickListener {
            tryToSignUp()
        }

        sign_in_text_view.setOnClickListener {
            Navigator.from(this).toSignIn(false)
        }

        terms_text_view.setOnClickListener {
            if (urlConfigProvider.hasConfig()) {
                browse(urlConfigProvider.getConfig().terms)
            } else {
                toastManager.short(R.string.error_network_not_specified)
            }
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

    private fun updateSignUpAvailability() {
        canSignUp = !isLoading
                && !email_edit_text.text.isNullOrBlank()
                && !password_edit_text.text.isNullOrEmpty()
                && !email_edit_text.hasError()
                && !password_edit_text.hasError()
                && arePasswordsMatch()
                && terms_of_service_checkbox.isChecked
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

    private fun tryToSignUp() {
        checkPasswordStrength()
        updateSignUpAvailability()

        if (canSignUp) {
            SoftInputUtil.hideSoftInput(this)
            signUp()
        }
    }

    private fun signUp() {
        val email = email_edit_text.text.toString()
        this.email = email

        val password = password_edit_text.text.getChars()
        if (this.password !== password) {
            this.password?.erase()
        }
        this.password = password

        SignUpUseCase(
            email,
            password,
            KeyServer(apiProvider.getApi().wallets),
            repositoryProvider,
            session,
            credentialsPersistence,
            walletInfoPersistence
        )
            .perform()
            .compose(ObservableTransformers.defaultSchedulersSingle())
            .doOnSubscribe {
                isLoading = true
            }
            .doOnEvent { _, _ ->
                isLoading = false
            }
            .subscribeBy(
                onSuccess = this::onSuccessfulSignUp,
                onError = signUpErrorHandler::handleIfPossible
            )
            .addTo(compositeDisposable)
    }

    private val signUpErrorHandler: ErrorHandler
        get() = CompositeErrorHandler(
            EditTextErrorHandler(email_edit_text) { error ->
                when (error) {
                    is EmailAlreadyTakenException ->
                        getString(R.string.error_email_already_taken)
                    else ->
                        null
                }
            },
            errorHandlerFactory.getDefault()
        )
            .doOnSuccessfulHandle(this::updateSignUpAvailability)

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        cameraPermission.handlePermissionResult(requestCode, permissions, grantResults)
    }

    private fun onSuccessfulSignUp(walletCreateResult: WalletCreateResult) {
        if (walletCreateResult.walletData.attributes.isVerified) {
            tryToSignIn()
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

    private fun tryToSignIn() {
        val email = this.email
        val password = this.password

        if (email == null || password == null) {
            toSignIn()
            return
        }

        SignInUseCase(
            email,
            password,
            KeyServer(apiProvider.getApi().wallets),
            session,
            credentialsPersistence,
            walletInfoPersistence,
            postSignInManagerFactory.get()::doPostSignIn
        )
            .perform()
            .compose(ObservableTransformers.defaultSchedulersCompletable())
            .doOnSubscribe {
                isLoading = true
            }
            .doOnDispose {
                isLoading = false
            }
            .subscribeBy(
                onComplete = this::onSuccessfulSignIn,
                onError = this::handleSignInError
            )
            .addTo(compositeDisposable)
    }

    private fun onSuccessfulSignIn() {
        Navigator.from(this).toMainActivity(finishAffinity = true)
    }

    private fun handleSignInError(error: Throwable) {
        errorHandlerFactory.getDefault().handle(error)
        toSignIn()
    }

    override fun onDestroy() {
        super.onDestroy()
        password?.erase()
    }
}
