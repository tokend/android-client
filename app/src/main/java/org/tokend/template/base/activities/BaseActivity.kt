package org.tokend.template.base.activities

import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.WindowManager
import io.reactivex.disposables.CompositeDisposable
import org.tokend.sdk.api.tfa.TfaCallback
import org.tokend.sdk.api.tfa.TfaVerifier
import org.tokend.sdk.federation.NeedTfaException
import org.tokend.template.App
import org.tokend.template.BuildConfig
import org.tokend.template.base.logic.AppTfaCallback
import org.tokend.template.base.logic.di.providers.AccountProvider
import org.tokend.template.base.logic.di.providers.ApiProvider
import org.tokend.template.base.logic.di.providers.RepositoryProvider
import org.tokend.template.base.logic.di.providers.WalletInfoProvider
import org.tokend.template.base.logic.persistance.CredentialsPersistor
import org.tokend.template.base.tfa.TfaDialogFactory
import org.tokend.template.util.Navigator
import javax.inject.Inject

abstract class BaseActivity : AppCompatActivity(), TfaCallback {
    @Inject
    lateinit var appTfaCallback: AppTfaCallback
    @Inject
    lateinit var accountProvider: AccountProvider
    @Inject
    lateinit var apiProvider: ApiProvider
    @Inject
    lateinit var walletInfoProvider: WalletInfoProvider
    @Inject
    lateinit var repositoryProvider: RepositoryProvider
    @Inject
    lateinit var credentialsPersistor: CredentialsPersistor

    protected open val allowUnauthorized = false

    protected val compositeDisposable: CompositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (application as? App)?.stateComponent?.inject(this)

        if (BuildConfig.SECURE_CONTENT) {
            try {
                window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (accountProvider.getAccount() != null || allowUnauthorized) {
            onCreateAllowed(savedInstanceState)
        } else {
            Navigator.toSignIn(this)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val enterTransition = window?.enterTransition
            if (enterTransition != null) {
                enterTransition.excludeTarget(android.R.id.statusBarBackground, true)
                enterTransition.excludeTarget(android.R.id.navigationBarBackground, true)
            }
        }
    }

    abstract fun onCreateAllowed(savedInstanceState: Bundle?)

    override fun onStart() {
        super.onStart()
        appTfaCallback.registerHandler(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        appTfaCallback.unregisterHandler(this)
        compositeDisposable.dispose()
    }

    override fun onTfaRequired(exception: NeedTfaException,
                               verifierInterface: TfaVerifier.Interface) {
        runOnUiThread {
            val email = walletInfoProvider.getWalletInfo()?.email
            TfaDialogFactory(this, credentialsPersistor)
                    .getForException(exception, verifierInterface, email)
                    ?.show()
                    ?: verifierInterface.cancelVerification()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }
}