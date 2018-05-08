package org.tokend.template.features.dashboard


import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.trello.rxlifecycle2.android.FragmentEvent
import com.trello.rxlifecycle2.kotlin.bindUntilEvent
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_dashboard.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.template.R
import org.tokend.template.base.fragments.BaseFragment
import org.tokend.template.base.fragments.ToolbarProvider
import org.tokend.template.base.logic.repository.balances.BalancesRepository
import org.tokend.template.base.view.util.AmountFormatter
import org.tokend.template.base.view.util.LoadingIndicatorManager
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.error_handlers.ErrorHandlerFactory

class DashboardFragment : BaseFragment(), ToolbarProvider {
    override val toolbarSubject: BehaviorSubject<Toolbar> = BehaviorSubject.create<Toolbar>()

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { activity_progress.show() },
            hideLoading = { activity_progress.hide() }
    )

    private var asset: String = ""
        set(value) {
            field = value
            onAssetChanged()
        }

    private lateinit var balancesRepository: BalancesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        balancesRepository = repositoryProvider.balances()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbarSubject.onNext(toolbar)

        toolbar.title = getString(R.string.dashboard_title)

        initAssetTabs()
        initBalance()
        initRecentActivity()

        subscribeToBalances()

        update()
    }

    // region Init
    private fun initAssetTabs() {
        asset_tabs.onItemSelected {
            asset = it
        }
    }

    private fun initBalance() {
        converted_balance_text_view.text = "0 USD"
    }

    private fun initRecentActivity() {
        empty_view.visibility = View.VISIBLE
        empty_view.text = getString(R.string.loading_data)
        view_more_button.visibility = View.GONE
    }
    // endregion

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
        asset_tabs.setItems(assets)
    }

    private fun displayBalance() {
        balancesRepository.itemsSubject.value
                .find { it.asset == asset }
                ?.let { balanceItem ->
                    balance_text_view.text = AmountFormatter.formatAssetAmount(
                            balanceItem.balance, balanceItem.asset
                    ) + " $asset"
                }
    }

    private fun update(force: Boolean = false) {
        if (!force) {
            balancesRepository.updateIfNotFresh()
        } else {
            balancesRepository.update()
        }
    }

    private fun onAssetChanged() {
        displayBalance()
    }

    companion object {
        fun newInstance(): DashboardFragment {
            val fragment = DashboardFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}
