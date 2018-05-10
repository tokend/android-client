package org.tokend.template.base.activities

import android.os.Bundle
import android.view.MenuItem
import android.view.WindowManager
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity
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
import org.tokend.template.base.tfa.TfaDialogFactory
import javax.inject.Inject

abstract class BaseActivity : RxAppCompatActivity(), TfaCallback {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (application as App).stateComponent.inject(this)

        if (BuildConfig.SECURE_CONTENT) {
            try {
                window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE)
            } catch(e: Exception) {
                e.printStackTrace()
            }
        }

        onCreateAllowed(savedInstanceState)
    }

    abstract fun onCreateAllowed(savedInstanceState: Bundle?)

    override fun onStart() {
        super.onStart()
        appTfaCallback.registerHandler(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        appTfaCallback.unregisterHandler(this)
    }

    override fun onTfaRequired(exception: NeedTfaException,
                               verifierInterface: TfaVerifier.Interface) {
        runOnUiThread {
            TfaDialogFactory(this).getForException(exception, verifierInterface)
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