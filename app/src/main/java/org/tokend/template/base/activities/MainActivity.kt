package org.tokend.template.base.activities

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.support.v4.content.ContextCompat
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
import org.tokend.template.R
import org.tokend.template.base.fragments.ToolbarProvider
import org.tokend.template.base.fragments.WalletFragment
import org.tokend.template.base.fragments.settings.GeneralSettingsFragment
import org.tokend.template.features.dashboard.DashboardFragment

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

        val exploreItem = PrimaryDrawerItem()
                .withName(R.string.explore_title)
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
        navigateTo(item.identifier)
        return false
    }

    private fun navigateTo(screenIdentifier: Long) {
        when (screenIdentifier) {
            DASHBOARD -> displayFragment(getDashboardFragment())
            WALLET -> displayFragment(getWalletFragment())
            SETTINGS -> displayFragment(getSettingsFragment())
        }
    }
    // endregion

    // region Fragments
    private fun getDashboardFragment(): Fragment {
        return DashboardFragment.newInstance()
    }

    private fun getWalletFragment(): Fragment {
        return WalletFragment.newInstance()
    }

    private fun getSettingsFragment(): Fragment {
        return GeneralSettingsFragment()
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

    override fun onBackPressed() {
        if (navigationDrawer?.isDrawerOpen == true) {
            navigationDrawer?.closeDrawer()
        } else {
            moveTaskToBack(true)
        }
    }
}
