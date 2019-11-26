package org.tokend.template.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.View
import io.reactivex.disposables.CompositeDisposable
import org.tokend.template.App
import org.tokend.template.activities.OnBackPressedListener
import org.tokend.template.data.model.Asset
import org.tokend.template.data.model.BalanceRecord
import org.tokend.template.di.providers.*
import org.tokend.template.logic.AppTfaCallback
import org.tokend.template.util.errorhandler.ErrorHandlerFactory
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
}