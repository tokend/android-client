package org.tokend.template.base.activities.signup

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.View
import com.google.zxing.integration.android.IntentIntegrator
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_sign_up.*
import kotlinx.android.synthetic.main.layout_network_field.*
import kotlinx.android.synthetic.main.layout_progress.*
import org.jetbrains.anko.browse
import org.jetbrains.anko.enabled
import org.jetbrains.anko.onCheckedChange
import org.jetbrains.anko.onClick
import org.tokend.sdk.federation.EmailAlreadyTakenException
import org.tokend.template.BuildConfig
import org.tokend.template.R
import org.tokend.template.base.activities.BaseActivity
import org.tokend.template.base.logic.SignUpManager
import org.tokend.template.base.logic.UrlConfigManager
import org.tokend.template.base.view.util.EditTextHelper
import org.tokend.template.base.view.util.LoadingIndicatorManager
import org.tokend.template.base.view.util.SimpleTextWatcher
import org.tokend.template.extensions.hasError
import org.tokend.template.extensions.setErrorAndFocus
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.Permission
import org.tokend.template.util.ToastManager
import org.tokend.template.util.error_handlers.ErrorHandlerFactory
import org.tokend.wallet.Account

class SignUpActivity : BaseActivity() {
    companion object {
        private val SAVE_SEED_REQUEST = "save_recovery_seed".hashCode() and 0xffff
    }

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

    private val cameraPermission = Permission(Manifest.permission.CAMERA, 404)
    private lateinit var urlConfigManager: UrlConfigManager

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_sign_up)

        urlConfigManager = UrlConfigManager(urlConfigProvider, urlConfigPersistor)
        urlConfigManager.onConfigUpdated {
            initNetworkField()

            // Recall validation
            email_edit_text.text = email_edit_text.text

            updateSignUpAvailability()
        }

        initFields()
        initButtons()

        canSignUp = false
    }

    // region Init
    private fun initFields() {
        initNetworkField()

        EditTextHelper.initEmailEditText(email_edit_text)
        EditTextHelper.initPasswordEditText(password_edit_text)

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
            Navigator.toSignIn(this, false)
        }

        terms_text_view.onClick {
            if (urlConfigProvider.hasConfig()) {
                browse(urlConfigProvider.getConfig().terms, true)
            } else {
                ToastManager.short(R.string.error_network_not_specified)
            }
        }
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

    private fun updateSignUpAvailability() {
        canSignUp = !isLoading
                && !email_edit_text.text.isBlank()
                && passwordsMatch && !password_edit_text.text.isEmpty()
                && !email_edit_text.hasError()
                && !password_edit_text.hasError()
                && terms_of_service_checkbox.isChecked
                && urlConfigProvider.hasConfig()
    }

    private fun checkPasswordsMatch() {
        passwordsMatch = password_edit_text.text.toString() ==
                confirm_password_edit_text.text.toString()

        if (!passwordsMatch && !confirm_password_edit_text.text.isEmpty()) {
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
        val passwordLength = password_edit_text.text.length
        val password = CharArray(passwordLength)
        password_edit_text.text.getChars(0, passwordLength, password, 0)

        var recoverySeed: String? = null
        Single.zip(SignUpManager.getRandomAccount(), SignUpManager.getRandomAccount(),
                BiFunction { key1: Account, key2: Account -> Pair(key1, key2) })
                .flatMapCompletable { (rootAccount, recoveryAccount) ->
                    recoverySeed = recoveryAccount.secretSeed?.joinToString("")
                    SignUpManager(urlConfigProvider)
                            .signUp(email, password, rootAccount, recoveryAccount)
                }
                .doOnComplete { password.fill('0') }
                .compose(ObservableTransformers.defaultSchedulersCompletable())
                .doOnSubscribe {
                    isLoading = true
                }
                .doOnTerminate {
                    isLoading = false
                }
                .subscribeBy(
                        onComplete = {
                            recoverySeed.let {
                                if (it != null) {
                                    Navigator.openRecoverySeedSaving(this,
                                            SAVE_SEED_REQUEST, it)
                                } else {
                                    onSuccessfulSignUp()
                                }
                            }
                        },
                        onError = {
                            it.printStackTrace()
                            handleSignUpError(it)
                        }
                )
                .addTo(compositeDisposable)
    }

    private fun handleSignUpError(error: Throwable) {
        when (error) {
            is EmailAlreadyTakenException ->
                email_edit_text.setErrorAndFocus(R.string.error_email_already_taken)
            else ->
                ErrorHandlerFactory.getDefault().handle(error)
        }
        updateSignUpAvailability()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        cameraPermission.handlePermissionResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SAVE_SEED_REQUEST) {
            onSuccessfulSignUp()
        } else {
            val scanResult = IntentIntegrator
                    .parseActivityResult(requestCode, resultCode, data)

            if (scanResult != null && scanResult.contents != null) {
                urlConfigManager.setFromJson(scanResult.contents)
            }
        }
    }

    private fun onSuccessfulSignUp() {
        ToastManager.long(R.string.check_your_email_to_verify_account)
        Navigator.toSignIn(this, false)
    }
}
