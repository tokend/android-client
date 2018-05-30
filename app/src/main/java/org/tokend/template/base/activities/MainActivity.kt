package org.tokend.template.base.activities

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
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
import com.trello.rxlifecycle2.android.ActivityEvent
import com.trello.rxlifecycle2.kotlin.bindUntilEvent
import io.reactivex.disposables.Disposable
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.find
import org.tokend.template.App
import org.tokend.template.R
import org.tokend.template.base.fragments.SendFragment
import org.tokend.template.base.fragments.ToolbarProvider
import org.tokend.template.base.fragments.WalletFragment
import org.tokend.template.base.fragments.settings.GeneralSettingsFragment
import org.tokend.template.features.dashboard.DashboardFragment
import org.tokend.template.features.explore.ExploreAssetsFragment
import org.tokend.template.features.deposit.DepositFragment
import org.tokend.template.features.trade.TradeFragment
import org.tokend.template.features.withdraw.WithdrawFragment

class MainActivity : BaseActivity() {
    companion object {
        private var counter = 0L

        private val DASHBOARD = counter++
        private val WALLET = counter++
        private val DEPOSIT = counter++
        private val WITHDRAW = counter++
        private val EXPLORE = counter++
        private val TRADE = counter++
        private val SETTINGS = counter++
        private val SIGN_OUT = counter++
        private val SEND = counter++

        private const val ASSET_EXTRA = "asset"
    }

    private var navigationDrawer: Drawer? = null

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_main)

        initNavigation()

        navigationDrawer?.setSelection(DASHBOARD)
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

        profileHeader.view.find<View>(R.id.material_drawer_account_header_background)
                .backgroundColor = ContextCompat.getColor(this, R.color.primary)

        val dashboardItem = PrimaryDrawerItem()
                .withName(R.string.dashboard_title)
                .withIdentifier(DASHBOARD)
                .withIcon(R.drawable.ic_dashboard)

        val walletItem = PrimaryDrawerItem()
                .withName(R.string.wallet_title)
                .withIdentifier(WALLET)
                .withIcon(R.drawable.ic_balance)

        val depositItem = PrimaryDrawerItem()
                .withName(R.string.deposit_title)
                .withIdentifier(DEPOSIT)
                .withIcon(R.drawable.ic_deposit)

        val withdrawItem = PrimaryDrawerItem()
                .withName(R.string.withdraw_title)
                .withIdentifier(WITHDRAW)
                .withIcon(R.drawable.ic_withdraw)

        val sendItem = PrimaryDrawerItem()
                .withName(R.string.send_title)
                .withIdentifier(SEND)
                .withIcon(R.drawable.ic_send)
                .withIconColorRes(R.color.icons)
                .withSelectedIconColorRes(R.color.icons)
                .withIconTintingEnabled(true)

        val exploreItem = PrimaryDrawerItem()
                .withName(R.string.explore_title_short)
                .withIdentifier(EXPLORE)
                .withIcon(R.drawable.ic_coins)

        val tradeItem = PrimaryDrawerItem()
                .withName(R.string.trade_title)
                .withIdentifier(TRADE)
                .withIcon(R.drawable.ic_trade)

        val settingsItem = PrimaryDrawerItem()
                .withName(R.string.settings_title)
                .withIdentifier(SETTINGS)
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
                        walletItem,
                        DividerDrawerItem(),
                        depositItem,
                        withdrawItem,
                        sendItem,
                        exploreItem,
                        tradeItem,
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
                DASHBOARD -> getDashboardFragment()
                WALLET -> getWalletFragment()
                WITHDRAW -> getWithdrawFragment()
                SEND -> getSendFragment()
                EXPLORE -> getExploreFragment()
                SETTINGS -> getSettingsFragment()
                TRADE -> getTradeFragment()
                DEPOSIT -> getDepositFragment()
                else -> null
            }

        if (fragment != null) {
            navigateTo(screenIdentifier, fragment)
        }
    }
    // endregion

    // region Fragments
    private fun getDashboardFragment(): Fragment {
        return DashboardFragment.newInstance()
    }

    private fun getWalletFragment(asset: String? = null): Fragment {
        return WalletFragment.newInstance(asset)
    }

    private fun getSettingsFragment(): Fragment {
        return GeneralSettingsFragment()
    }

    private fun getTradeFragment(): Fragment {
        return TradeFragment()
    }

    private fun getWithdrawFragment(asset: String? = null): Fragment {
        return WithdrawFragment.newInstance(asset)
    }

    private fun getSendFragment(asset: String? = null): Fragment {
        return SendFragment.newInstance(asset)
    }

    private fun getExploreFragment(): Fragment {
        return ExploreAssetsFragment()
    }

    private fun getDepositFragment(): Fragment {
        return DepositFragment()
    }

    private var fragmentToolbarDisposable: Disposable? = null
    private fun displayFragment(fragment: Fragment) {

        supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_layout, fragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit()

        // Bind navigation drawer to fragment's toolbar.
        fragmentToolbarDisposable?.dispose()
        if (fragment is ToolbarProvider) {
            fragmentToolbarDisposable = fragment.toolbarSubject
                    .bindUntilEvent(lifecycle(), ActivityEvent.DESTROY)
                    .subscribe { fragmentToolbar ->
                        navigationDrawer?.setToolbar(this, fragmentToolbar,
                                true)
                    }
        }
    }
    // endregion

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
            moveTaskToBack(true)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val screenId = intent!!.getLongExtra("screenId", 0L)
        val asset : String? = intent.getStringExtra(ASSET_EXTRA)

        if(asset != null){
            var fragment: Fragment? = null
            if(screenId == 1L)
                fragment = getWalletFragment(asset)
            else if(screenId == 8L){
                fragment = getSendFragment(asset)
            }
            navigateTo(screenId,fragment!!)
        }else
        navigateTo(screenId)

    }

}