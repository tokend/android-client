package org.tokend.template.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import org.tokend.sdk.tfa.NeedTfaException
import org.tokend.sdk.tfa.TfaCallback
import org.tokend.sdk.tfa.TfaVerifier
import org.tokend.template.App
import org.tokend.template.BuildConfig
import org.tokend.template.data.storage.persistence.ObjectPersistence
import org.tokend.template.di.providers.*
import org.tokend.template.features.assets.model.Asset
import org.tokend.template.features.balances.model.BalanceRecord
import org.tokend.template.features.kyc.storage.ActiveKycPersistence
import org.tokend.template.features.localaccount.mnemonic.logic.MnemonicCode
import org.tokend.template.features.signin.logic.PostSignInManagerFactory
import org.tokend.template.features.tfa.logic.AppTfaCallback
import org.tokend.template.features.tfa.view.TfaDialogFactory
import org.tokend.template.features.urlconfig.model.UrlConfig
import org.tokend.template.logic.Session
import org.tokend.template.logic.credentials.persistence.CredentialsPersistence
import org.tokend.template.logic.credentials.persistence.WalletInfoPersistence
import org.tokend.template.logic.persistence.BackgroundLockManager
import org.tokend.template.util.ConnectionStateUtil
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.cipher.DataCipher
import org.tokend.template.util.errorhandler.ErrorHandlerFactory
import org.tokend.template.util.locale.AppLocaleManager
import org.tokend.template.util.navigation.ActivityRequest
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
    lateinit var credentialsPersistence: CredentialsPersistence

    @Inject
    lateinit var walletInfoPersistence: WalletInfoPersistence

    @Inject
    lateinit var urlConfigProvider: UrlConfigProvider

    @Inject
    lateinit var urlConfigPersistence: ObjectPersistence<UrlConfig>

    @Inject
    lateinit var errorHandlerFactory: ErrorHandlerFactory

    @Inject
    lateinit var toastManager: ToastManager

    @Inject
    lateinit var assetCodeComparator: Comparator<String>

    @Inject
    lateinit var assetComparator: Comparator<Asset>

    @Inject
    lateinit var balanceComparator: Comparator<BalanceRecord>

    @Inject
    lateinit var session: Session

    @Inject
    lateinit var amountFormatter: AmountFormatter

    @Inject
    lateinit var activeKycPersistence: ActiveKycPersistence

    @Inject
    lateinit var localeManager: AppLocaleManager

    @Inject
    lateinit var backgroundLockManager: BackgroundLockManager

    @Inject
    lateinit var mnemonicCode: MnemonicCode

    @Inject
    lateinit var defaultDataCipher: DataCipher

    @Inject
    lateinit var postSignInManagerFactory: PostSignInManagerFactory

    @Inject
    lateinit var connectionStateUtil: ConnectionStateUtil

    /**
     * If set to true the activity will be operational
     * even without account in [accountProvider] or with expired [session]
     */
    protected open val allowUnauthorized = false

    /**
     * Disposable holder which will be disposed on activity destroy
     */
    protected val compositeDisposable: CompositeDisposable = CompositeDisposable()

    protected val activityRequestsBag: MutableCollection<ActivityRequest<*>> = mutableSetOf()

    private var baseContextWrappingDelegate: AppCompatDelegate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (application as? App)?.stateComponent?.inject(this)

        if (BuildConfig.SECURE_CONTENT) {
            try {
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (accountProvider.getAccount() != null || allowUnauthorized) {
            onCreateAllowed(savedInstanceState)
        } else {
            (application as App).signOut(this, soft = true)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val enterTransition = window?.enterTransition
            if (enterTransition != null) {
                enterTransition.excludeTarget(android.R.id.statusBarBackground, true)
                enterTransition.excludeTarget(android.R.id.navigationBarBackground, true)
            }
        }

        subscribeToLocaleChanges()
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

    override fun onTfaRequired(
        exception: NeedTfaException,
        verifierInterface: TfaVerifier.Interface
    ) {
        runOnUiThread {
            val login = session.login
            TfaDialogFactory(
                this, errorHandlerFactory.getDefault(),
                credentialsPersistence, toastManager
            )
                .getForException(exception, verifierInterface, login)
                ?.show()
                ?: verifierInterface.cancelVerification()
        }
    }

    // region Locale
    private fun subscribeToLocaleChanges() {
        localeManager
            .localeChanges
            .compose(ObservableTransformers.defaultSchedulers())
            .subscribe { recreate() }
            .addTo(compositeDisposable)
    }

    override fun getDelegate() = baseContextWrappingDelegate
        ?: App.localeManager.getLocalizeContextWrapperDelegate(super.getDelegate())
            .also { baseContextWrappingDelegate = it }
    // endregion

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    protected fun finishWithError(cause: Throwable) {
        errorHandlerFactory.getDefault().handle(cause)
        finish()
    }

    protected fun finishWithMissingArgError(argName: String) {
        errorHandlerFactory.getDefault().handle(IllegalArgumentException("No $argName specified"))
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        activityRequestsBag.forEach { request ->
            request.handleActivityResult(requestCode, resultCode, data)
        }

        activityRequestsBag.iterator().also { iterator ->
            while (iterator.hasNext()) {
                val request = iterator.next()
                if (request.isCompleted) {
                    iterator.remove()
                }
            }
        }
    }
}