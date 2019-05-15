package org.tokend.template.activities

import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.WindowManager
import io.reactivex.disposables.CompositeDisposable
import org.tokend.sdk.tfa.NeedTfaException
import org.tokend.sdk.tfa.TfaCallback
import org.tokend.sdk.tfa.TfaVerifier
import org.tokend.template.App
import org.tokend.template.BuildConfig
import org.tokend.template.data.model.BalanceRecord
import org.tokend.template.di.providers.*
import org.tokend.template.features.kyc.storage.SubmittedKycStatePersistor
import org.tokend.template.features.tfa.view.TfaDialogFactory
import org.tokend.template.logic.AppTfaCallback
import org.tokend.template.logic.Session
import org.tokend.template.logic.persistance.CredentialsPersistor
import org.tokend.template.logic.persistance.UrlConfigPersistor
import org.tokend.template.util.errorhandler.ErrorHandlerFactory
import org.tokend.template.view.ToastManager
import org.tokend.template.view.util.formatter.AmountFormatter
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
    @Inject
    lateinit var urlConfigProvider: UrlConfigProvider
    @Inject
    lateinit var urlConfigPersistor: UrlConfigPersistor
    @Inject
    lateinit var errorHandlerFactory: ErrorHandlerFactory
    @Inject
    lateinit var toastManager: ToastManager
    @Inject
    lateinit var assetComparator: Comparator<String>
    @Inject
    lateinit var balanceComparator: Comparator<BalanceRecord>
    @Inject
    lateinit var session: Session
    @Inject
    lateinit var amountFormatter: AmountFormatter
    @Inject
    lateinit var kycStatePersistor: SubmittedKycStatePersistor

    /**
     * If set to true the activity will be operational
     * even without account in [accountProvider] or with expired [session]
     */
    protected open val allowUnauthorized = false

    /**
     * Disposable holder which will be disposed on activity destroy
     */
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
            (application as App).signOut(this, soft = true)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val enterTransition = window?.enterTransition
            if (enterTransition != null) {
                enterTransition.excludeTarget(android.R.id.statusBarBackground, true)
                enterTransition.excludeTarget(android.R.id.navigationBarBackground, true)
            }
        }
    }

    /**
     * You must implement your activity initialization here
     */
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

    override fun onResume() {
        super.onResume()

        if (session.isExpired) {
            session.reset()

            if (!allowUnauthorized) {
                (application as App).signOut(this, soft = true)
            }
        }
    }

    override fun onTfaRequired(exception: NeedTfaException,
                               verifierInterface: TfaVerifier.Interface) {
        runOnUiThread {
            val email = walletInfoProvider.getWalletInfo()?.email
            TfaDialogFactory(this, errorHandlerFactory.getDefault(),
                    credentialsPersistor, toastManager)
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