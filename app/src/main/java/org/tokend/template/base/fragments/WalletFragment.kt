package org.tokend.template.base.fragments


import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.trello.rxlifecycle2.android.FragmentEvent
import com.trello.rxlifecycle2.kotlin.bindUntilEvent
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.collapsing_balance_appbar.*
import kotlinx.android.synthetic.main.fragment_wallet.*
import org.tokend.template.R
import org.tokend.template.base.logic.repository.balances.BalancesRepository
import org.tokend.template.base.view.util.AmountFormatter
import org.tokend.template.base.view.util.LoadingIndicatorManager
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.error_handlers.ErrorHandlerFactory

class WalletFragment : BaseFragment(), ToolbarProvider {
    override val toolbarSubject: BehaviorSubject<Toolbar> = BehaviorSubject.create<Toolbar>()

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { swipe_refresh.isRefreshing = true },
            hideLoading = { swipe_refresh.isRefreshing = false }
    )

    private lateinit var balancesRepository: BalancesRepository

    private val defaultAsset: String?
        get() = arguments?.getString(ASSET_EXTRA)

    private var asset: String = ""
        set(value) {
            field = value
            onAssetChanged()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        balancesRepository = repositoryProvider.balances()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_wallet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbarSubject.onNext(toolbar)

        initAssetTabs()
        initBalance()
        initSwipeRefresh()

        subscribeToBalances()
    }

    // region init
    private fun initAssetTabs() {
        asset_tabs.onItemSelected {
            asset = it
        }
    }

    private fun initBalance() {
        converted_balance_text_view.text = "0 USD"
    }

    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(context!!, R.color.accent))
        swipe_refresh.setOnRefreshListener { update(force = true) }
    }
    // endregion

    private fun update(force: Boolean = false) {
        if (!force) {
            balancesRepository.updateIfNotFresh()
        } else {
            balancesRepository.update()
        }
    }

    private fun subscribeToBalances() {
        balancesRepository.itemsSubject
                .bindUntilEvent(lifecycle(), FragmentEvent.DESTROY)
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe {
                    displayAssetTabs(it.map { it.asset })
                    displayBalance()
                }
        balancesRepository.loadingSubject
                .bindUntilEvent(lifecycle(), FragmentEvent.DESTROY)
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe {
                    loadingIndicator.setLoading(it, "balances")
                }
        balancesRepository.errorsSubject
                .bindUntilEvent(lifecycle(), FragmentEvent.DESTROY)
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe {
                    ErrorHandlerFactory.getDefault().handle(it)
                }
    }

    private fun displayAssetTabs(assets: List<String>) {
        asset_tabs.setItems(assets, true)
    }

    private fun displayBalance() {
        balancesRepository.itemsSubject.value
                .find { it.asset == asset }
                ?.let { balanceItem ->
                    collapsing_toolbar.title = AmountFormatter.formatAssetAmount(balanceItem.balance, asset) +
                            " $asset"
                }
    }

    private fun onAssetChanged() {
        displayBalance()
    }

    companion object {
        private const val ASSET_EXTRA = "asset"

        fun newInstance(asset: String? = null): WalletFragment {
            val fragment = WalletFragment()
            fragment.arguments = Bundle().apply {
                putString(ASSET_EXTRA, asset)
            }
            return fragment
        }
    }
}
