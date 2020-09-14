package org.tokend.template.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import io.reactivex.disposables.CompositeDisposable
import org.tokend.template.App
import org.tokend.template.activities.OnBackPressedListener
import org.tokend.template.di.providers.*
import org.tokend.template.features.assets.model.Asset
import org.tokend.template.features.balances.model.BalanceRecord
import org.tokend.template.features.tfa.logic.AppTfaCallback
import org.tokend.template.util.errorhandler.ErrorHandlerFactory
import org.tokend.template.util.navigation.ActivityRequest
import org.tokend.template.view.ToastManager
import org.tokend.template.view.util.formatter.AmountFormatter
import javax.inject.Inject

abstract class BaseFragment : Fragment(), OnBackPressedListener {
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
    lateinit var urlConfigProvider: UrlConfigProvider
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
    lateinit var amountFormatter: AmountFormatter

    override fun onBackPressed() = true

    /**
     * Disposable holder which will be disposed on fragment destroy
     */
    protected lateinit var compositeDisposable: CompositeDisposable

    protected val activityRequestsBag: MutableCollection<ActivityRequest<*>> = mutableSetOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity?.application as? App)?.stateComponent?.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        compositeDisposable = CompositeDisposable()
        if (savedInstanceState == null) {
            onInitAllowed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        compositeDisposable.dispose()
        compositeDisposable = CompositeDisposable()
    }

    /**
     * You must implement your fragment initialization here
     */
    abstract fun onInitAllowed()

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