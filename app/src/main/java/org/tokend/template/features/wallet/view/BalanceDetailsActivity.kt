package org.tokend.template.features.wallet.view

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_balance_details.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import org.jetbrains.anko.dip
import org.jetbrains.anko.onClick
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.data.model.BalanceRecord
import org.tokend.template.data.repository.balancechanges.BalanceChangesRepository
import org.tokend.template.data.repository.balances.BalancesRepository
import org.tokend.template.features.wallet.adapter.BalanceChangeListItem
import org.tokend.template.features.wallet.adapter.BalanceChangesAdapter
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.util.AssetLogoUtil
import org.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.template.view.util.LocalizedName

class BalanceDetailsActivity : BaseActivity() {
    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = { swipe_refresh.isRefreshing = true },
            hideLoading = { swipe_refresh.isRefreshing = false }
    )

    private lateinit var balanceId: String

    private val balancesRepository: BalancesRepository
        get() = repositoryProvider.balances()

    private val balanceChangesRepository: BalanceChangesRepository
        get() = repositoryProvider.balanceChanges(balanceId)

    private val balance: BalanceRecord?
        get() = balancesRepository.itemsList.find { it.id == balanceId }

    private lateinit var adapter: BalanceChangesAdapter

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_balance_details)

        val balanceId = intent.getStringExtra(BALANCE_ID_EXTRA)

        if (balanceId == null) {
            finish()
            return
        }

        this.balanceId = balanceId

        initBackground()
        initButtons()
        initHistory()
        initSwipeRefresh()

        subscribeToBalances()
        subscribeToBalanceChanges()

        update()
    }

    // region Init
    private fun initBackground() {
        window.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = Color.WHITE
        }
    }

    private fun initButtons() {
        close_button.onClick { finish() }
    }

    private fun initHistory() {
        adapter = BalanceChangesAdapter(amountFormatter, false)
        adapter.onItemClick { _, item ->
            item.source?.let { Navigator.from(this).openBalanceChangeDetails(it) }
//            menu_fab.close(false)
        }

        error_empty_view.apply {
            setEmptyDrawable(R.drawable.empty_view_wallet)
            observeAdapter(adapter, R.string.no_transaction_history)
            setEmptyViewDenial { balanceChangesRepository.isNeverUpdated }
            adjustEmptyViewHeight()
        }

        history_list.adapter = adapter
        history_list.layoutManager = LinearLayoutManager(this)

        history_list.listenBottomReach({ adapter.getDataItemCount() }) {
            balanceChangesRepository.loadMore() || balanceChangesRepository.noMoreItems
        }
    }

    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.accent))
        swipe_refresh.setOnRefreshListener {
            update(force = true)
//            menu_fab.close(true)
        }
    }
    // endregion

    private fun update(force: Boolean = false) {
        if (!force) {
            balancesRepository.updateIfNotFresh()
            balanceChangesRepository.updateIfNotFresh()
        } else {
            balancesRepository.update()
            balanceChangesRepository.update()
        }
    }

    private fun subscribeToBalances() {
        balancesRepository
                .itemsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { displayBalance() }
                .addTo(compositeDisposable)

        balancesRepository
                .loadingSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { loadingIndicator.setLoading(it, "balances") }
                .addTo(compositeDisposable)
    }

    private fun subscribeToBalanceChanges() {
        balanceChangesRepository
                .itemsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { displayHistory() }
                .addTo(compositeDisposable)

        balanceChangesRepository.loadingSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { loading ->
                    if (loading) {
                        if (balanceChangesRepository.isOnFirstPage) {
                            loadingIndicator.show("history")
                        } else {
                            adapter.showLoadingFooter()
                        }
                    } else {
                        loadingIndicator.hide("history")
                        adapter.hideLoadingFooter()
                    }
                }
                .addTo(compositeDisposable)

        balanceChangesRepository.errorsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { error ->
                    if (!adapter.hasData) {
                        error_empty_view.showError(error, errorHandlerFactory.getDefault()) {
                            update(true)
                        }
                    } else {
                        errorHandlerFactory.getDefault().handle(error)
                    }
                }
                .addTo(compositeDisposable)
    }

    // region Display
    private fun displayBalance() {
        val balance = this.balance
                ?: return

        balance_available_text_view.text =
                amountFormatter.formatAssetAmount(
                        balance.available,
                        balance.assetCode
                )

        asset_name_text_view.text = balance.asset.name ?: balance.assetCode

        AssetLogoUtil.setAssetLogo(
                asset_logo_image_view,
                balance.asset,
                resources.getDimensionPixelSize(R.dimen.asset_list_item_logo_size)
        )
    }

    private fun displayHistory() {
        val localizedName = LocalizedName(this)
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return

        adapter.setData(balanceChangesRepository.itemsList.map { balanceChange ->
            BalanceChangeListItem(balanceChange, accountId, localizedName)
        })
    }
    // endregion

    private fun adjustEmptyViewHeight() {
        swipe_refresh.apply {
            measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            post {
                layoutParams = layoutParams.apply {
                    height = Math.max(
                            swipe_refresh.measuredHeight - appbar.measuredHeight,
                            dip(256)
                    )
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        adjustEmptyViewHeight()
    }

    companion object {
        const val BALANCE_ID_EXTRA = "balance_id"
    }
}