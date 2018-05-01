package org.tokend.template.activities

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
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.find
import org.tokend.template.R
import org.tokend.template.util.Navigator

abstract class NavigationActivity : BaseActivity() {
    companion object {
        private var counter = 0L

        val DASHBOARD_ITEM = counter++
        val WALLET_ITEM = counter++
        val DEPOSIT_ITEM = counter++
        val WITHDRAW_ITEM = counter++
        val EXPLORE_ITEM = counter++
        val TRADE_ITEM = counter++
        val SETTINGS_ITEM = counter++
        val SIGN_OUT_ITEM = counter++
    }

    protected var navigationDrawer: Drawer? = null

    protected open fun initNavigationDrawer() {
        val email = "email"
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
                .withIdentifier(DASHBOARD_ITEM)
                .withIcon(R.drawable.ic_dashboard)

        val walletItem = PrimaryDrawerItem()
                .withName(R.string.wallet_title)
                .withIdentifier(WALLET_ITEM)
                .withIcon(R.drawable.ic_balance)

        val depositItem = PrimaryDrawerItem()
                .withName(R.string.deposit_title)
                .withIdentifier(DEPOSIT_ITEM)
                .withIcon(R.drawable.ic_deposit)

        val withdrawItem = PrimaryDrawerItem()
                .withName(R.string.withdraw_title)
                .withIdentifier(WITHDRAW_ITEM)
                .withIcon(R.drawable.ic_withdraw)

        val exploreItem = PrimaryDrawerItem()
                .withName(R.string.explore_title)
                .withIdentifier(EXPLORE_ITEM)
                .withIcon(R.drawable.ic_coins)

        val tradeItem = PrimaryDrawerItem()
                .withName(R.string.trade_title)
                .withIdentifier(TRADE_ITEM)
                .withIcon(R.drawable.ic_trade)

        val settingsItem = PrimaryDrawerItem()
                .withName(R.string.settings_title)
                .withIdentifier(SETTINGS_ITEM)
                .withIcon(R.drawable.ic_settings)

        val signOutItem = PrimaryDrawerItem()
                .withName(R.string.sign_out)
                .withIdentifier(SIGN_OUT_ITEM)
                .withIcon(R.drawable.ic_sign_out)

        navigationDrawer = DrawerBuilder()
                .withActivity(this)
                .apply {
                    getToolbar()?.let {
                        withToolbar(it)
                    }
                }
                .withAccountHeader(profileHeader)
                .withHeaderDivider(false)
                .withDelayDrawerClickEvent(280)
                .withSliderBackgroundColorRes(R.color.material_drawer_background)
                .withSelectedItem(getSelectedNavigationItemId())
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

    protected open fun onNavigationItemSelected(item: IDrawerItem<Any, RecyclerView.ViewHolder>)
            : Boolean {
        when (item.identifier) {
            getSelectedNavigationItemId() -> return false
            DASHBOARD_ITEM -> {
                Navigator.toDashboard(this)
            }
            WALLET_ITEM -> {
                Navigator.toWallet(this)
            }
            DEPOSIT_ITEM -> {
            }
            WITHDRAW_ITEM -> {
            }
            EXPLORE_ITEM -> {
            }
            TRADE_ITEM -> {
            }
            SETTINGS_ITEM -> {
                Navigator.toSettings(this)
            }
            SIGN_OUT_ITEM -> {
            }
            else -> return false
        }

        return false
    }

    override fun onBackPressed() {
        if (navigationDrawer != null) {
            if (navigationDrawer?.isDrawerOpen == true) {
                navigationDrawer?.closeDrawer()
            } else {
                moveTaskToBack(true)
            }
        } else {
            super.onBackPressed()
        }
    }

    protected abstract fun getSelectedNavigationItemId(): Long
}