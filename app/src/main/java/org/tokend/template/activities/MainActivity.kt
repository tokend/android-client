package org.tokend.template.activities

import android.content.res.Configuration
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.ImageView
import com.mikepenz.materialdrawer.AccountHeader
import com.mikepenz.materialdrawer.AccountHeaderBuilder
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.find
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.onClick
import org.tokend.template.BuildConfig
import org.tokend.template.R
import org.tokend.template.features.assets.ExploreAssetsFragment
import org.tokend.template.features.dashboard.DashboardFragment
import org.tokend.template.features.deposit.DepositFragment
import org.tokend.template.features.invest.view.SalesFragment
import org.tokend.template.features.send.SendFragment
import org.tokend.template.features.send.model.PaymentRequest
import org.tokend.template.features.send.view.SasActivity
import org.tokend.template.features.settings.SettingsFragment
import org.tokend.template.features.trade.orderbook.view.OrderBookFragment
import org.tokend.template.features.trade.pairs.view.TradeAssetPairsFragment
import org.tokend.template.features.wallet.WalletFragment
import org.tokend.template.features.withdraw.WithdrawFragment
import org.tokend.template.features.withdraw.model.WithdrawalRequest
import org.tokend.template.fragments.FragmentFactory
import org.tokend.template.fragments.ToolbarProvider
import org.tokend.template.logic.wallet.WalletEventsListener
import org.tokend.template.util.Navigator

class MainActivity : BaseActivity(), WalletEventsListener {
    companion object {
        private val DEFAULT_FRAGMENT_ID = DashboardFragment.ID
    }

    private var navigationDrawer: Drawer? = null
    private var landscapeNavigationDrawer: Drawer? = null
    private var onBackPressedListener: OnBackPressedListener? = null
    private val factory = FragmentFactory()
    private val tablet by lazy {
        resources.getBoolean(R.bool.isTablet)
    }
    private val orientation: Int
        get() = resources.configuration.orientation

    private var toolbar: Toolbar? = null

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_main)

        initNavigation()

        navigationDrawer?.setSelection(DEFAULT_FRAGMENT_ID)

        startActivity(intentFor<SasActivity>())
    }

    // region Init
    private fun initNavigation() {
        val email = walletInfoProvider.getWalletInfo()?.email
        val items = HashMap<Long, PrimaryDrawerItem>()

        PrimaryDrawerItem()
                .withName(R.string.dashboard_title)
                .withIdentifier(DashboardFragment.ID)
                .withIcon(R.drawable.ic_dashboard)
                .also { items[DashboardFragment.ID] = it }

        PrimaryDrawerItem()
                .withName(R.string.wallet_title)
                .withIdentifier(WalletFragment.ID)
                .withIcon(R.drawable.ic_balance)
                .also { items[WalletFragment.ID] = it }

        PrimaryDrawerItem()
                .withName(R.string.deposit_title)
                .withIdentifier(DepositFragment.ID)
                .withIcon(R.drawable.ic_deposit)
                .also { items[DepositFragment.ID] = it }

        PrimaryDrawerItem()
                .withName(R.string.withdraw_title)
                .withIdentifier(WithdrawFragment.ID)
                .withIcon(R.drawable.ic_withdraw)
                .also { items[WithdrawFragment.ID] = it }

        PrimaryDrawerItem()
                .withName(R.string.send_title)
                .withIdentifier(SendFragment.ID)
                .withIcon(R.drawable.ic_send)
                .withIconColorRes(R.color.icons)
                .withSelectedIconColorRes(R.color.icons)
                .withIconTintingEnabled(true)
                .also { items[SendFragment.ID] = it }

        PrimaryDrawerItem()
                .withName(R.string.explore_sales_title)
                .withIdentifier(SalesFragment.ID)
                .withIcon(R.drawable.ic_invest)
                .also { items[SalesFragment.ID] = it }

        PrimaryDrawerItem()
                .withName(R.string.explore_assets_title)
                .withIdentifier(ExploreAssetsFragment.ID)
                .withIcon(R.drawable.ic_coins)
                .also { items[ExploreAssetsFragment.ID] = it }

        PrimaryDrawerItem()
                .withName(R.string.trade_title)
                .withIdentifier(TradeAssetPairsFragment.ID)
                .withIcon(R.drawable.ic_trade)
                .also { items[OrderBookFragment.ID] = it }

        PrimaryDrawerItem()
                .withName(R.string.settings_title)
                .withIdentifier(SettingsFragment.ID)
                .withIcon(R.drawable.ic_settings)
                .also { items[SettingsFragment.ID] = it }

        navigationDrawer = initDrawerBuilder(items, getHeaderInstance(email)).build()
        landscapeNavigationDrawer = initDrawerBuilder(items, getHeaderInstance(email)).buildView()
        nav_tablet.addView(landscapeNavigationDrawer?.slider, 0)
    }

    private fun getHeaderInstance(email: String?): AccountHeader {
        val profileHeader = AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackgroundScaleType(ImageView.ScaleType.FIT_START)
                .withHeaderBackground(ContextCompat.getDrawable(this,
                        R.drawable.navigation_header_background))
                .withSelectionListEnabledForSingleProfile(false)
                .withProfileImagesVisible(false)
                .addProfiles(ProfileDrawerItem()
                        .withEmail(email))
                .build()

        profileHeader.view.find<View>(R.id.material_drawer_account_header_background).apply {
            backgroundColor = ContextCompat.getColor(this@MainActivity, R.color.primary)
            onClick {
                val accountId = walletInfoProvider.getWalletInfo()?.accountId
                        ?: getString(R.string.error_try_again)
                Navigator.from(this@MainActivity).openQrShare(
                        data = accountId,
                        title = getString(R.string.account_id_title),
                        shareLabel = getString(R.string.share_account_id)
                )
                navigationDrawer?.closeDrawer()
            }
        }

        return profileHeader
    }

    private fun initDrawerBuilder(items: Map<Long, PrimaryDrawerItem>,
                                  profileHeader: AccountHeader): DrawerBuilder {
        return DrawerBuilder()
                .withActivity(this)
                .withAccountHeader(profileHeader)
                .withHeaderDivider(false)
                .withSliderBackgroundColorRes(R.color.material_drawer_background)
                .addDrawerItems(
                        items[DashboardFragment.ID],
                        items[WalletFragment.ID]
                )
                .apply {

                    if (BuildConfig.IS_DEPOSIT_ALLOWED) {
                        addDrawerItems(items[DepositFragment.ID])
                    }

                    if (BuildConfig.IS_WITHDRAW_ALLOWED) {
                        addDrawerItems(items[WithdrawFragment.ID])
                    }

                    if (BuildConfig.IS_SEND_ALLOWED) {
                        addDrawerItems(items[SendFragment.ID])
                    }

                    if (BuildConfig.IS_INVEST_ALLOWED) {
                        addDrawerItems(items[SalesFragment.ID])
                    }

                    if (BuildConfig.IS_EXPLORE_ALLOWED) {
                        addDrawerItems(items[ExploreAssetsFragment.ID])
                    }

                    if (BuildConfig.IS_TRADE_ALLOWED) {
                        addDrawerItems(items[OrderBookFragment.ID])
                    }
                }
                .addDrawerItems(
                        items[SettingsFragment.ID]
                )
                .withOnDrawerItemClickListener { _, _, item ->
                    return@withOnDrawerItemClickListener onNavigationItemSelected(item)
                }
    }
    // endregion

    // region Navigation
    private fun onNavigationItemSelected(item: IDrawerItem<Any, RecyclerView.ViewHolder>)
            : Boolean {
        navigateTo(item.identifier)
        return false
    }

    private fun navigateTo(screenIdentifier: Long, fragment: Fragment) {
        navigationDrawer?.setSelection(screenIdentifier, false)
        landscapeNavigationDrawer?.setSelection(screenIdentifier, false)
        displayFragment(fragment)
    }

    private fun navigateTo(screenIdentifier: Long) {
        val fragment =
                when (screenIdentifier) {
                    DashboardFragment.ID -> factory.getDashboardFragment()
                    WalletFragment.ID -> factory.getWalletFragment()
                    WithdrawFragment.ID -> factory.getWithdrawFragment()
                    SendFragment.ID -> factory.getSendFragment()
                    ExploreAssetsFragment.ID -> factory.getExploreFragment()
                    SettingsFragment.ID -> factory.getSettingsFragment()
                    DepositFragment.ID -> factory.getDepositFragment()
                    SalesFragment.ID -> factory.getSalesFragment()
                    TradeAssetPairsFragment.ID -> factory.getTradeAssetPairsFragment()
                    else -> return
                }

        onBackPressedListener = fragment as? OnBackPressedListener
        navigateTo(screenIdentifier, fragment)
    }
    // endregion

    private var fragmentToolbarDisposable: Disposable? = null
    private fun displayFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
                .disallowAddToBackStack()
                .setCustomAnimations(R.anim.stay_visible, R.anim.activity_fade_out)
                .replace(R.id.fragment_container_layout, fragment)
                .commit()

        // Bind navigation drawer to fragment's toolbar.
        fragmentToolbarDisposable?.dispose()
        if (fragment is ToolbarProvider) {
            fragmentToolbarDisposable = fragment.toolbarSubject
                    .subscribe { fragmentToolbar ->
                        toolbar = fragmentToolbar
                        fragmentToolbar.apply {
                            setNavigationContentDescription(
                                    com.mikepenz.materialdrawer.R.string.material_drawer_open
                            )
                            setNavigationOnClickListener {
                                navigationDrawer?.openDrawer()
                            }
                        }
                        updateDrawerVisibility()
                    }
                    .addTo(compositeDisposable)
        }
    }

    private fun updateDrawerVisibility() {
        if (tablet && orientation == Configuration.ORIENTATION_LANDSCAPE) {
            navigationDrawer?.drawerLayout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            nav_tablet.visibility = View.VISIBLE
            toolbar?.navigationIcon = null
            side_shadow_view.visibility = View.VISIBLE

        } else {
            navigationDrawer?.drawerLayout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            nav_tablet.visibility = View.GONE
            toolbar?.setNavigationIcon(R.drawable.ic_menu)
            side_shadow_view.visibility = View.GONE
        }
    }

    override fun onBackPressed() {
        if (navigationDrawer?.isDrawerOpen == true) {
            navigationDrawer?.closeDrawer()
        } else {
            if (navigationDrawer?.currentSelection == DEFAULT_FRAGMENT_ID) {
                moveTaskToBack(true)
            } else {
                if (onBackPressedListener?.onBackPressed() != false)
                    navigateTo(DEFAULT_FRAGMENT_ID)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        updateDrawerVisibility()
    }

    override fun onPaymentRequestConfirmed(paymentRequest: PaymentRequest) {
        navigateTo(WalletFragment.ID, factory.getWalletFragment(paymentRequest.asset))
    }

    override fun onWithdrawalRequestConfirmed(withdrawalRequest: WithdrawalRequest) {
        navigateTo(WalletFragment.ID, factory.getWalletFragment(withdrawalRequest.asset))
    }
}