package org.tokend.template.activities

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import com.mikepenz.materialdrawer.AccountHeaderBuilder
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.find
import org.jetbrains.anko.onClick
import org.tokend.template.App
import org.tokend.template.BuildConfig
import org.tokend.template.R
import org.tokend.template.features.send.SendFragment
import org.tokend.template.fragments.ToolbarProvider
import org.tokend.template.features.wallet.WalletFragment
import org.tokend.template.features.settings.SettingsFragment
import org.tokend.template.features.send.model.PaymentRequest
import org.tokend.template.features.dashboard.DashboardFragment
import org.tokend.template.features.deposit.DepositFragment
import org.tokend.template.features.assets.ExploreAssetsFragment
import org.tokend.template.features.fees.FeesFragment
import org.tokend.template.features.invest.SalesFragment
import org.tokend.template.features.limits.LimitsFragment
import org.tokend.template.features.trade.TradeFragment
import org.tokend.template.features.withdraw.WithdrawFragment
import org.tokend.template.features.withdraw.model.WithdrawalRequest
import org.tokend.template.logic.wallet.WalletEventsListener
import org.tokend.template.fragments.FragmentFactory
import org.tokend.template.util.Navigator

class MainActivity : BaseActivity(), WalletEventsListener {
    companion object {
        private const val SIGN_OUT = 7L
    }

    private var navigationDrawer: Drawer? = null
    private var onBackPressedListener: OnBackPressedListener? = null
    private val factory = FragmentFactory()

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_main)

        initNavigation()

        navigationDrawer?.setSelection(DashboardFragment.ID)
    }

    // region Init
    private fun initNavigation() {
        val email = walletInfoProvider.getWalletInfo()?.email
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
                    Navigator.openQrShare(this@MainActivity,
                            data = accountId,
                            title = getString(R.string.account_id_title),
                            shareLabel = getString(R.string.share_account_id)
                    )
                navigationDrawer?.closeDrawer()
            }
        }

        val dashboardItem = PrimaryDrawerItem()
                .withName(R.string.dashboard_title)
                .withIdentifier(DashboardFragment.ID)
                .withIcon(R.drawable.ic_dashboard)

        val walletItem = PrimaryDrawerItem()
                .withName(R.string.wallet_title)
                .withIdentifier(WalletFragment.ID)
                .withIcon(R.drawable.ic_balance)

        val depositItem = PrimaryDrawerItem()
                .withName(R.string.deposit_title)
                .withIdentifier(DepositFragment.ID)
                .withIcon(R.drawable.ic_deposit)

        val withdrawItem = PrimaryDrawerItem()
                .withName(R.string.withdraw_title)
                .withIdentifier(WithdrawFragment.ID)
                .withIcon(R.drawable.ic_withdraw)

        val sendItem = PrimaryDrawerItem()
                .withName(R.string.send_title)
                .withIdentifier(SendFragment.ID)
                .withIcon(R.drawable.ic_send)
                .withIconColorRes(R.color.icons)
                .withSelectedIconColorRes(R.color.icons)
                .withIconTintingEnabled(true)

        val limitsItem = PrimaryDrawerItem()
                .withName(R.string.limits)
                .withIdentifier(LimitsFragment.ID)
                .withIcon(R.drawable.ic_insert_chart)
                .withIconColorRes(R.color.icons)
                .withSelectedIconColorRes(R.color.icons)
                .withIconTintingEnabled(true)

        val investItem = PrimaryDrawerItem()
                .withName(R.string.explore_sales_title)
                .withIdentifier(SalesFragment.ID)
                .withIcon(R.drawable.ic_invest)

        val exploreItem = PrimaryDrawerItem()
                .withName(R.string.explore_tokens_title)
                .withIdentifier(ExploreAssetsFragment.ID)
                .withIcon(R.drawable.ic_coins)

        val tradeItem = PrimaryDrawerItem()
                .withName(R.string.trade_title)
                .withIdentifier(TradeFragment.ID)
                .withIcon(R.drawable.ic_trade)

        val feesItem = PrimaryDrawerItem()
                .withName(getString(R.string.my_fees))
                .withIdentifier(FeesFragment.ID)
                .withIcon(R.drawable.ic_flash)

        val settingsItem = PrimaryDrawerItem()
                .withName(R.string.settings_title)
                .withIdentifier(SettingsFragment.ID)
                .withIcon(R.drawable.ic_settings)

        val signOutItem = PrimaryDrawerItem()
                .withName(R.string.sign_out)
                .withIdentifier(SIGN_OUT)
                .withSelectable(false)
                .withIcon(R.drawable.ic_sign_out)

        navigationDrawer = DrawerBuilder()
                .withActivity(this)
                .withAccountHeader(profileHeader)
                .withHeaderDivider(false)
                .withSliderBackgroundColorRes(R.color.material_drawer_background)
                .addDrawerItems(
                        dashboardItem,
                        walletItem
                )
                .apply {
                    if (BuildConfig.IS_DEPOSIT_ALLOWED
                            || BuildConfig.IS_WITHDRAW_ALLOWED
                            || BuildConfig.IS_SEND_ALLOWED
                            || BuildConfig.IS_LIMITS_ALLOWED
                            || BuildConfig.IS_EXPLORE_ALLOWED
                            || BuildConfig.IS_TRADE_ALLOWED
                            || BuildConfig.IS_FEES_ALLOWED
                            || BuildConfig.IS_INVEST_ALLOWED) {
                        addDrawerItems(DividerDrawerItem())

                        if (BuildConfig.IS_DEPOSIT_ALLOWED) {
                            addDrawerItems(depositItem)
                        }

                        if (BuildConfig.IS_WITHDRAW_ALLOWED) {
                            addDrawerItems(withdrawItem)
                        }

                        if (BuildConfig.IS_SEND_ALLOWED) {
                            addDrawerItems(sendItem)
                        }

                        if(BuildConfig.IS_LIMITS_ALLOWED) {
                            addDrawerItems(limitsItem)
                        }

                        if (BuildConfig.IS_INVEST_ALLOWED) {
                            addDrawerItems(investItem)
                        }

                        if (BuildConfig.IS_EXPLORE_ALLOWED) {
                            addDrawerItems(exploreItem)
                        }

                        if (BuildConfig.IS_TRADE_ALLOWED) {
                            addDrawerItems(tradeItem)
                        }

                        if (BuildConfig.IS_FEES_ALLOWED) {
                            addDrawerItems(feesItem)
                        }
                     }
                }
                .addDrawerItems(
                        DividerDrawerItem(),
                        settingsItem,
                        signOutItem
                )
                .withOnDrawerItemClickListener { _, _, item ->
                    return@withOnDrawerItemClickListener onNavigationItemSelected(item)
                }
                .build()
    }
    // endregion

    // region Navigation
    private fun onNavigationItemSelected(item: IDrawerItem<Any, RecyclerView.ViewHolder>)
            : Boolean {
        if (item.identifier == SIGN_OUT) {
            signOutWithConfirmation()
        } else {
            navigateTo(item.identifier)
        }
        return false
    }

    private fun navigateTo(screenIdentifier: Long, fragment: Fragment) {
        navigationDrawer?.setSelection(screenIdentifier, false)
        displayFragment(fragment)
    }

    private fun navigateTo(screenIdentifier: Long) {
        val fragment =
                when (screenIdentifier) {
                    DashboardFragment.ID -> factory.getDashboardFragment()
                    WalletFragment.ID -> factory.getWalletFragment()
                    WithdrawFragment.ID -> factory.getWithdrawFragment()
                    SendFragment.ID -> factory.getSendFragment()
                    LimitsFragment.ID -> factory.getLimitsFragment()
                    ExploreAssetsFragment.ID -> factory.getExploreFragment()
                    SettingsFragment.ID -> factory.getSettingsFragment()
                    TradeFragment.ID -> factory.getTradeFragment()
                    DepositFragment.ID -> factory.getDepositFragment()
                    SalesFragment.ID -> factory.getSalesFragment()
                    FeesFragment.ID -> factory.getFeesFragment()
                    else -> return
                }

        onBackPressedListener = fragment as? OnBackPressedListener
        navigateTo(screenIdentifier, fragment)
    }
    // endregion

    private var fragmentToolbarDisposable: Disposable? = null
    private fun displayFragment(fragment: Fragment) {

        supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.stay_visible, R.anim.activity_fade_out)
                .replace(R.id.fragment_container_layout, fragment)
                .commit()

        // Bind navigation drawer to fragment's toolbar.
        fragmentToolbarDisposable?.dispose()
        if (fragment is ToolbarProvider) {
            fragmentToolbarDisposable = fragment.toolbarSubject
                    .subscribe { fragmentToolbar ->
                        navigationDrawer?.setToolbar(this, fragmentToolbar,
                                true)
                    }
                    .addTo(compositeDisposable)
        }
    }

    private fun signOutWithConfirmation() {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setMessage(R.string.sign_out_confirmation)
                .setPositiveButton(R.string.yes) { _, _ ->
                    (application as App).signOut(this)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    override fun onBackPressed() {
        if (navigationDrawer?.isDrawerOpen == true) {
            navigationDrawer?.closeDrawer()
        } else {
            if (navigationDrawer?.currentSelection == DashboardFragment.ID) {
                moveTaskToBack(true)
            } else {
                if (onBackPressedListener?.onBackPressed() != false)
                    navigateTo(DashboardFragment.ID)
            }
        }
    }

    override fun onPaymentRequestConfirmed(paymentRequest: PaymentRequest) {
        navigateTo(WalletFragment.ID, factory.getWalletFragment(paymentRequest.asset))
    }

    override fun onWithdrawalRequestConfirmed(withdrawalRequest: WithdrawalRequest) {
        navigateTo(WalletFragment.ID, factory.getWalletFragment(withdrawalRequest.asset))
    }
}