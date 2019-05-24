package org.tokend.template.features.wallet.view

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_balance_details.*
import kotlinx.android.synthetic.main.include_appbar_elevation.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import kotlinx.android.synthetic.main.toolbar_white.*
import org.jetbrains.anko.childrenSequence
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
import org.tokend.template.view.util.*

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

        initToolbar()
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
        initFab()
    }

    private fun initToolbar() {
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.setSubtitleTextAppearance(this, R.style.HintText)

        initToolbarAnimations()
    }

    private fun initToolbarAnimations() {
        // Force toolbar to create title and subtitle views.
        toolbar.title = "*"
        toolbar.subtitle = "*"

        val fadingToolbarViews = toolbar
                .childrenSequence()
                .filter { it is TextView }

        val fadeInDuration = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
        val fadeOutDuration = collapsing_toolbar.scrimAnimationDuration

        fadingToolbarViews.forEach {
            it.visibility = View.INVISIBLE
        }

        collapsing_toolbar.scrimCallback = { scrimShown ->
            fadingToolbarViews.forEach {
                if (scrimShown) {
                    AnimationUtil.fadeInView(it, fadeInDuration)
                } else {
                    AnimationUtil.fadeOutView(it, fadeOutDuration)
                }
            }
        }
    }

    private fun initFab() {
        val asset = this.balance?.asset

        val canWithdraw = asset?.isWithdrawable == true
        val canDeposit = asset?.isBackedByExternalSystem == true
        val canSend = asset?.isTransferable == true

        if (!canWithdraw && !canDeposit && !canSend) {
            menu_fab.visibility = View.GONE
            menu_fab.isEnabled = false
            return
        } else {
            menu_fab.visibility = View.VISIBLE
            menu_fab.isEnabled = true

            withdraw_fab.isEnabled = canWithdraw
            deposit_fab.isEnabled = canDeposit
            send_fab.isEnabled = canSend
            receive_fab.isEnabled = canSend
        }

        val navigator = Navigator.from(this)

        send_fab.onClick {
            val assetCode = balance?.assetCode ?: return@onClick
            navigator.openSend(assetCode, 0)
            menu_fab.close(false)
        }
        send_fab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_send_fab))

        receive_fab.onClick {
            val walletInfo = walletInfoProvider.getWalletInfo()
                    ?: return@onClick
            navigator.openAccountQrShare(walletInfo)
            menu_fab.close(false)
        }
        receive_fab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_receive_fab))

        deposit_fab.onClick {
            val assetCode = balance?.assetCode ?: return@onClick
            navigator.openDeposit(0, assetCode)
            menu_fab.close(false)
        }
        deposit_fab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_deposit_fab))

        withdraw_fab.onClick {
            val assetCode = balance?.assetCode ?: return@onClick
            navigator.openWithdraw(0, assetCode)
            menu_fab.close(false)
        }
        withdraw_fab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_withdraw_fab))
    }

    private val hideFabScrollListener =
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 2) {
                        menu_fab.hideMenuButton(true)
                    } else if (dy < -2 && menu_fab.isEnabled) {
                        menu_fab.showMenuButton(true)
                    }
                }
            }

    private fun initHistory() {
        adapter = BalanceChangesAdapter(amountFormatter, false)
        adapter.onItemClick { _, item ->
            item.source?.let { Navigator.from(this).openBalanceChangeDetails(it) }
            menu_fab.close(false)
        }

        error_empty_view.apply {
            setEmptyDrawable(R.drawable.empty_view_wallet)
            observeAdapter(adapter, R.string.no_transaction_history)
            setEmptyViewDenial { balanceChangesRepository.isNeverUpdated }
            adjustEmptyViewHeight()
        }

        history_list.adapter = adapter
        history_list.layoutManager = LinearLayoutManager(this)
        history_list.addOnScrollListener(hideFabScrollListener)

        history_list.listenBottomReach({ adapter.getDataItemCount() }) {
            balanceChangesRepository.loadMore() || balanceChangesRepository.noMoreItems
        }

        date_text_switcher.init(history_list, adapter)

        ElevationUtil.initScrollElevation(history_list, appbar_elevation_view)
    }

    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.accent))
        swipe_refresh.setOnRefreshListener {
            update(force = true)
            menu_fab.close(true)
        }

        val offset = -dip(32)
        swipe_refresh.setProgressViewOffset(
                false,
                swipe_refresh.progressViewStartOffset + offset,
                swipe_refresh.progressViewEndOffset + offset
        )

        SwipeRefreshDependencyUtil.addDependency(swipe_refresh, appbar)
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

        val availableString =
                amountFormatter.formatAssetAmount(
                        balance.available,
                        balance.assetCode
                )
        balance_available_text_view.text = availableString
        toolbar.title = availableString

        asset_name_text_view.text = balance.asset.name ?: balance.assetCode

        AssetLogoUtil.setAssetLogo(
                asset_logo_image_view,
                balance.asset,
                resources.getDimensionPixelSize(R.dimen.asset_list_item_logo_size)
        )

        if (balance.convertedAmount != null
                && balance.conversionAssetCode != null
                && balance.conversionAssetCode != balance.assetCode) {
            val convertedString =
                    getString(
                            R.string.template_converted_amount,
                            amountFormatter.formatAssetAmount(
                                    balance.convertedAmount,
                                    balance.conversionAssetCode
                            )
                    )
            balance_converted_text_view.visibility = View.VISIBLE
            balance_converted_text_view.text = convertedString
            toolbar.subtitle = convertedString
        } else {
            balance_converted_text_view.visibility = View.GONE
            toolbar.subtitle = null
        }
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
        root_layout.post {
            error_empty_view.layoutParams = error_empty_view.layoutParams.apply {
                height = Math.max(
                        root_layout.height - appbar.height,
                        dip(256)
                )
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        adjustEmptyViewHeight()
    }

    override fun onBackPressed() {
        if (menu_fab.isOpened) {
            menu_fab.close(true)
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        const val BALANCE_ID_EXTRA = "balance_id"
    }
}