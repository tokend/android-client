package io.tokend.template.features.signup

import android.Manifest
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.tokend.template.R
import io.tokend.template.activities.BaseActivity
import io.tokend.template.extensions.browse
import io.tokend.template.extensions.getChars
import io.tokend.template.extensions.hasError
import io.tokend.template.features.signup.logic.AfterSignUpSignInUseCase
import io.tokend.template.features.signup.logic.SignUpUseCase
import io.tokend.template.features.urlconfig.view.NetworkFieldWrapper
import io.tokend.template.util.ObservableTransformers
import io.tokend.template.util.PermissionManager
import io.tokend.template.util.errorhandler.CompositeErrorHandler
import io.tokend.template.util.errorhandler.ErrorHandler
import io.tokend.template.util.navigation.Navigator
import io.tokend.template.util.validator.PasswordValidator
import io.tokend.template.view.util.ElevationUtil
import io.tokend.template.view.util.LoadingIndicatorManager
import io.tokend.template.view.util.input.EditTextErrorHandler
import io.tokend.template.view.util.input.SimpleTextWatcher
import io.tokend.template.view.util.input.SoftInputUtil
import kotlinx.android.synthetic.main.activity_sign_up.*
import kotlinx.android.synthetic.main.include_appbar_elevation.*
import kotlinx.android.synthetic.main.layout_network_field.*
import kotlinx.android.synthetic.main.layout_progress.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.crypto.ecdsa.erase
import org.tokend.sdk.api.wallets.model.EmailAlreadyTakenException
import org.tokend.sdk.keyserver.KeyServer
import org.tokend.sdk.keyserver.models.WalletCreateResult

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

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_sign_up)
        setSupportActionBar(toolbar)
        setTitle(R.string.sign_up)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initFields()
        initButtons()
        ElevationUtil.initScrollElevation(scroll_view, appbar_elevation_view)

        canSignUp = false
    }

    // region Init
    private fun initFields() {
        initNetworkField()

        login_edit_text.requestFocus()
        login_edit_text.addTextChangedListener(SimpleTextWatcher {
            login_edit_text.error = null
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
        val wrapper = NetworkFieldWrapper(
            this,
            network_field_layout,
            cameraPermission,
            compositeDisposable,
            activityRequestsBag
        )
        wrapper.onNetworkUpdated {
            updateSignUpAvailability()
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
            browse(urlConfigProvider.getConfig().terms)
        }
    }
    // endregion

    private fun checkPasswordStrength() {
        if (!PasswordValidator.isValid(password_edit_text.text)) {
            password_edit_text.error = getString(R.string.error_weak_password)
        } else {
            password_edit_text.error = null
        }
    }

    private fun updateSignUpAvailability() {
        canSignUp = !isLoading
                && !login_edit_text.text.isNullOrBlank()
                && !password_edit_text.text.isNullOrEmpty()
                && !login_edit_text.hasError()
                && !password_edit_text.hasError()
                && arePasswordsMatch()
                && terms_of_service_checkbox.isChecked
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
        val password = password_edit_text.text.getChars()

        SignUpUseCase(
            login = login_edit_text.text.toString().trim(),
            password = password,
            keyServer = KeyServer(apiProvider.getApi().wallets),
            repositoryProvider = repositoryProvider,
            session = session,
        )
            .perform()
            .compose(ObservableTransformers.defaultSchedulersSingle())
            .doOnSubscribe {
                isLoading = true
            }
            .doOnEvent { _, _ ->
                password.erase()
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
            EditTextErrorHandler(login_edit_text) { error ->
                when (error) {
                    is EmailAlreadyTakenException ->
                        getString(R.string.error_login_already_taken)
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
            showRegistrationNotVerifiedDialogAndFinish()
        }
    }

    private fun showRegistrationNotVerifiedDialogAndFinish() {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.almost_done)
            .setMessage(R.string.check_confirmation_message_to_verify_account)
            .setPositiveButton(R.string.ok) { _, _ ->
                toSignIn()
            }
            .setOnCancelListener {
                toSignIn()
            }
            .show()
    }

    private fun toSignIn() {
        Navigator.from(this).toSignIn(false)
    }

    private fun tryToSignIn() {
        val password = password_edit_text.text.getChars()

        AfterSignUpSignInUseCase(
            login = login_edit_text.text!!.toString(),
            password = password,
            session = session,
            credentialsPersistence = credentialsPersistence,
            walletInfoPersistence = walletInfoPersistence,
            postSignInActions = postSignInManagerFactory.get()::doPostSignIn
        )
            .perform()
            .compose(ObservableTransformers.defaultSchedulersCompletable())
            .doOnSubscribe {
                isLoading = true
            }
            .doOnDispose {
                isLoading = false
                password.erase()
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
}
