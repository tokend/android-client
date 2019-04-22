package org.tokend.template.features.signin

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.toSingle
import kotlinx.android.synthetic.main.activity_authenticator_sign_in.*
import org.jetbrains.anko.browse
import org.jetbrains.anko.onClick
import org.tokend.sdk.api.authenticator.model.AuthRequest
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.features.qr.logic.QrGenerator
import org.tokend.template.features.signin.logic.AuthResultPoller
import org.tokend.template.features.signin.logic.PostSignInManager
import org.tokend.template.features.signin.logic.SignInWithAccountUseCase
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.util.AnimationUtil
import org.tokend.wallet.Account
import java.util.concurrent.TimeUnit

class AuthenticatorSignInActivity : BaseActivity() {
    override val allowUnauthorized = true

    private lateinit var account: Account
    private var needAuthResultSubscription = false

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_authenticator_sign_in)

        setTitle(R.string.sign_in)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        showLoading()

        generateNewAccount()
    }

    private fun generateNewAccount() {
        {
            Account.random()
        }
                .toSingle()
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .subscribeBy(
                        onSuccess = { account ->
                            this.account = account

                            showAuthContent()
                            displayAccountQr()
                            updateOpenAuthenticatorButton()
                            subscribeToAuthResult()
                        },
                        onError = {
                            errorHandlerFactory.getDefault().handle(it)
                            finish()
                        }
                )
                .addTo(compositeDisposable)
    }

    // region QR
    private fun getMaxQrSize(): Int {
        val height = root_layout.measuredHeight -
                2 * resources.getDimensionPixelSize(R.dimen.standard_margin)
        return Math.min(height, root_layout.measuredWidth)
    }

    private fun displayQrCode(text: String) {
        QrGenerator().bitmap(text, getMaxQrSize())
                .delay(300, TimeUnit.MILLISECONDS)
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribeBy(
                        onNext = {
                            qr_code_image_view.setImageBitmap(it)
                            AnimationUtil.fadeInView(qr_code_image_view)
                        },
                        onError = {
                            errorHandlerFactory.getDefault().handle(it)
                        }
                )
                .addTo(compositeDisposable)
    }

    private fun displayAccountQr() {
        displayQrCode(getAuthUri(account))
    }
    // endregion

    // region View switch
    private fun showLoading() {
        progress_layout.visibility = View.VISIBLE
        auth_content_layout.visibility = View.GONE
    }

    private fun showAuthContent() {
        progress_layout.visibility = View.GONE
        auth_content_layout.visibility = View.VISIBLE
    }

    private fun isAuthContentShown(): Boolean {
        return auth_content_layout.visibility == View.VISIBLE
    }
    // endregion

    private fun getAuthUri(account: Account): String {
        return AuthRequest(
                urlConfigProvider.getConfig().api,
                getString(R.string.authenticator_app_name),
                Int.MAX_VALUE,
                account.accountId,
                null
        ).uriString
    }

    private fun updateOpenAuthenticatorButton() {
        open_authenticator_button.onClick {
            openAuthenticatorOrGooglePlay(getAuthUri(account))
        }
    }

    private fun openAuthenticatorOrGooglePlay(uri: String) {
        try {
            startActivityForResult(
                    Intent.parseUri(uri, 0),
                    OPEN_AUTHENTICATOR_REQUEST
            )
        } catch (e: ActivityNotFoundException) {
            browse(GOOGLE_PLAY_AUTHENTICATOR_URI)
        }
    }

    private fun checkIsAppInstalled() {
        val isAppInstalled = try {
            packageManager.getApplicationInfo(AUTHENTICATOR_URI, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
        updateButtonTitle(isAppInstalled)
    }

    private fun updateButtonTitle(isAppInstalled: Boolean) {
        val buttonTitle = when (isAppInstalled) {
            true -> getString(R.string.open_authenticator_action)
            false -> getString(R.string.install_authenticator_action)
        }
        open_authenticator_button.text = buttonTitle
    }

    // region Auth result polling
    private var authResultSubscriptionDisposable: Disposable? = null

    private fun subscribeToAuthResultIfNeeded() {
        if (needAuthResultSubscription) {
            subscribeToAuthResult()
        }
    }

    private fun subscribeToAuthResult() {
        needAuthResultSubscription = true

        unsubscribeFromAuthResult()

        authResultSubscriptionDisposable =
                AuthResultPoller(account.accountId, apiProvider.getApi().authResults)
                        .getObservable()
                        .compose(ObservableTransformers.defaultSchedulers())
                        .subscribeBy(
                                onNext = { authResult ->
                                    if (authResult.isSuccessful) {
                                        unsubscribeFromAuthResult()
                                        signInWithAccount(account)
                                    }
                                }
                        )
                        .addTo(compositeDisposable)
    }

    private fun unsubscribeFromAuthResult() {
        authResultSubscriptionDisposable
                ?.also { compositeDisposable.remove(it) }
                ?.dispose()
    }

    override fun onResume() {
        super.onResume()
        checkIsAppInstalled()
        subscribeToAuthResultIfNeeded()
    }

    override fun onPause() {
        super.onPause()
        unsubscribeFromAuthResult()
    }
    // endregion

    private fun signInWithAccount(account: Account) {
        SignInWithAccountUseCase(
                account,
                apiProvider.getKeyServer(),
                apiProvider.getApi().authResults,
                session,
                credentialsPersistor,
                PostSignInManager(repositoryProvider)
        )
                .perform()
                .compose(ObservableTransformers.defaultSchedulersCompletable())
                .doOnSubscribe {
                    showLoading()
                }
                .subscribeBy(
                        onComplete = {
                            setResult(Activity.RESULT_OK)
                            Navigator(this).toMainActivity()
                        },
                        onError = {
                            errorHandlerFactory.getDefault().handle(it)

                            if (it is SignInWithAccountUseCase.AuthResultNotFoundException
                                    || it is SignInWithAccountUseCase.AuthResultUnsuccessfulException) {
                                finish()
                            } else {
                                showAuthContent()
                                subscribeToAuthResult()
                            }
                        }
                )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                OPEN_AUTHENTICATOR_REQUEST -> onAuthenticatorSuccess()
            }
        }
    }

    private fun onAuthenticatorSuccess() {
        needAuthResultSubscription = false
        signInWithAccount(account)
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)

        qr_code_image_view.postDelayed({
            if (isAuthContentShown()) {
                displayAccountQr()
            }
        }, 300)
    }

    companion object {
        private const val AUTHENTICATOR_URI = "org.tokend.authenticator"
        private const val GOOGLE_PLAY_AUTHENTICATOR_URI =
                "https://play.google.com/store/apps/cause?id=org.tokend.authenticator"
        private val OPEN_AUTHENTICATOR_REQUEST = "open_authenticator".hashCode() and 0xffff
    }

}