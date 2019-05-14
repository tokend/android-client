package org.tokend.template.features.signin

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.text.Editable
import android.view.View
import com.google.gson.JsonParser
import com.squareup.picasso.Picasso
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_unlock_app.*
import org.jetbrains.anko.onClick
import org.tokend.sdk.api.wallets.model.InvalidCredentialsException
import org.tokend.sdk.factory.GsonFactory
import org.tokend.template.App
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.extensions.getChars
import org.tokend.template.extensions.hasError
import org.tokend.template.extensions.onEditorAction
import org.tokend.template.extensions.setErrorAndFocus
import org.tokend.template.features.assets.LogoFactory
import org.tokend.template.features.kyc.model.form.SimpleKycForm
import org.tokend.template.features.kyc.storage.SubmittedKycStatePersistor
import org.tokend.template.features.signin.logic.PostSignInManager
import org.tokend.template.features.signin.logic.SignInUseCase
import org.tokend.template.logic.persistance.FingerprintAuthManager
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.imagetransform.CircleTransform
import org.tokend.template.view.util.AnimationUtil
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.view.util.SignOutDialogFactory
import org.tokend.template.view.util.input.SimpleTextWatcher
import org.tokend.template.view.util.input.SoftInputUtil

class UnlockAppActivity : BaseActivity() {
    override val allowUnauthorized: Boolean = true

    private lateinit var fingerprintAuthManager: FingerprintAuthManager

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { updateFieldsLoadingState(true) },
            hideLoading = { updateFieldsLoadingState(false) }
    )

    private var usePassword = true
        set(value) {
            field = value
            updateSignInMethod()
        }

    private var canSignIn = false
        set(value) {
            field = value
            updateSignInButtonState()
        }

    private lateinit var email: String

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_unlock_app)

        email = credentialsPersistor.getSavedEmail() ?: return

        fingerprintAuthManager = FingerprintAuthManager(applicationContext, credentialsPersistor)

        initViews()
        initDefaultState()
        updateSignInAvailability()
        requestFingerprintAuthIfAvailable()
    }

    private fun initViews() {
        user_email_text.text = email
        initAccountLogo()
        initButtons()
    }

    private fun initAccountLogo() {
        val placeholderImage = LogoFactory(this)
                .getForValue(
                        email.toUpperCase(),
                        resources.getDimensionPixelSize(R.dimen.hepta_margin),
                        ContextCompat.getColor(this, R.color.avatar_placeholder_background),
                        Color.WHITE
                )
        val placeholderDrawable = BitmapDrawable(resources, placeholderImage)
        user_logo.setImageDrawable(placeholderDrawable)

        updateProfileImage(placeholderDrawable)
    }

    //Temporary solution
    private fun updateProfileImage(placeHolder: BitmapDrawable) {
        val prefs = getSharedPreferences("KycStatePersistence", Context.MODE_PRIVATE)

        val gson = GsonFactory().getBaseGson()

        val kycForm = prefs.getString(SubmittedKycStatePersistor.KEY, null)
                ?.let {
                    try {
                        val remoteFileJson = JsonParser()
                                .parse(it).asJsonObject.getAsJsonObject("state").get("formData")
                        gson.fromJson(remoteFileJson, SimpleKycForm::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }

        kycForm?.avatar?.let {
            Picasso.with(this)
                    .load(it.getUrl(urlConfigProvider.getConfig().storage))
                    .placeholder(placeHolder)
                    .transform(CircleTransform())
                    .fit()
                    .centerCrop()
                    .into(user_logo)
        }
    }

    private fun initButtons() {
        use_password_text_button.onClick {
            usePassword = true
        }

        password_edit_text.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(p0: Editable?) {
                password_edit_text.error = null
                updateSignInAvailability()
            }
        })

        password_edit_text.onEditorAction {
            tryToSignIn()
        }

        unlock_button.onClick {
            tryToSignIn()
        }

        sign_out_button.onClick {
            SignOutDialogFactory.getTunedDialog(this) {
                (application as App).signOut(this)
            }.show()
        }

        recovery_button.onClick {
            Navigator.from(this).openRecovery(email)
        }
    }

    private fun initDefaultState() {
        if (fingerprintAuthManager.isAuthAvailable) {
            fingerprint_layout.visibility = View.VISIBLE
            password_layout.visibility = View.GONE
        }
    }

    private fun updateSignInMethod() {
        password_edit_text.text.clear()
        password_edit_text.error = null
        if (usePassword) {
            cancelFingerprintAuth()
            AnimationUtil.fadeOutView(fingerprint_layout, ANIMATION_DURATION) {
                AnimationUtil.fadeInView(password_layout, ANIMATION_DURATION)
                password_edit_text.requestFocus()
                SoftInputUtil.showSoftInputOnView(password_edit_text)
                fingerprint_layout.clearAnimation()
            }
        } else {
            AnimationUtil.fadeOutView(password_layout, ANIMATION_DURATION) {
                AnimationUtil.fadeInView(fingerprint_layout, ANIMATION_DURATION)
                password_layout.clearAnimation()
            }
        }

    }

    private fun updateSignInAvailability() {
        canSignIn = password_edit_text.text.isNotEmpty()
                && !password_edit_text.hasError()
                && email.isNotEmpty()
    }

    private fun updateSignInButtonState() {
        unlock_button.isEnabled = canSignIn
    }

    private fun updateFieldsLoadingState(isLoading: Boolean) {
        val animatedView =
                if (usePassword) password_layout
                else fingerprint_layout

        if (isLoading) {
            AnimationUtil.fadeOutView(sign_out_button)
            AnimationUtil.fadeOutView(
                    view = animatedView,
                    viewGone = false)
            progress.show()
        } else {
            AnimationUtil.fadeInView(animatedView)
            AnimationUtil.fadeInView(sign_out_button)
            progress.hide()
        }
    }

    private fun requestFingerprintAuthIfAvailable() {
        fingerprintAuthManager.requestAuthIfAvailable(
                onAuthStart = { usePassword = false },
                onSuccess = { email, password ->
                    signIn(email, password)
                },
                onError = {
                    toastManager.short(it)
                }
        )
    }

    private fun cancelFingerprintAuth() {
        fingerprintAuthManager.cancelAuth()
    }

    private fun tryToSignIn() {
        when {
            password_edit_text.text.isEmpty() ->
                password_edit_text.setErrorAndFocus(R.string.error_cannot_be_empty)
            canSignIn -> {
                SoftInputUtil.hideSoftInput(this)
                signIn(email, password_edit_text.text.getChars())
            }
        }
    }

    private fun signIn(email: String, password: CharArray) {
        SignInUseCase(
                email,
                password,
                apiProvider.getKeyServer(),
                session,
                credentialsPersistor,
                PostSignInManager(repositoryProvider)
        )
                .perform()
                .compose(ObservableTransformers.defaultSchedulersCompletable())
                .doOnSubscribe {
                    loadingIndicator.setLoading(true)
                    SoftInputUtil.hideSoftInput(this)
                }
                .doOnEvent {
                    password.fill('0')
                }
                .doOnDispose { password.fill('0') }
                .subscribeBy(
                        onComplete = {
                            Navigator.from(this).toMainActivity()
                        },
                        onError = {
                            it.printStackTrace()
                            handleUnlockError(it)
                            loadingIndicator.setLoading(false)
                        }
                )
                .addTo(compositeDisposable)
    }

    private fun handleUnlockError(error: Throwable) {
        when (error) {
            is InvalidCredentialsException ->
                password_edit_text.setErrorAndFocus(R.string.error_invalid_password)
            else ->
                errorHandlerFactory.getDefault().handle(error)
        }
        updateSignInAvailability()
    }

    override fun onResume() {
        super.onResume()
        if (!usePassword) {
            requestFingerprintAuthIfAvailable()
        }
    }

    override fun onPause() {
        super.onPause()
        cancelFingerprintAuth()
    }

    override fun onBackPressed() {
        if (usePassword && fingerprintAuthManager.isAuthAvailable) {
            requestFingerprintAuthIfAvailable()
        } else super.onBackPressed()
    }

    companion object {
        private const val ANIMATION_DURATION = 200L
    }
}
