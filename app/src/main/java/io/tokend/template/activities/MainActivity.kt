package io.tokend.template.activities

import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.materialdrawer.AccountHeader
import com.mikepenz.materialdrawer.AccountHeaderBuilder
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import com.mikepenz.materialdrawer.util.DrawerImageLoader
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.tokend.template.BuildConfig
import io.tokend.template.R
import io.tokend.template.features.account.data.model.AccountRole
import io.tokend.template.features.account.data.storage.AccountRepository
import io.tokend.template.features.assets.view.ExploreAssetsFragment
import io.tokend.template.features.dashboard.view.DashboardFragment
import io.tokend.template.features.deposit.view.DepositFragment
import io.tokend.template.features.invest.view.SalesFragment
import io.tokend.template.features.kyc.model.ActiveKyc
import io.tokend.template.features.kyc.storage.ActiveKycRepository
import io.tokend.template.features.polls.view.PollsFragment
import io.tokend.template.features.send.model.PaymentRequest
import io.tokend.template.features.settings.GeneralSettingsFragment
import io.tokend.template.features.trade.pairs.view.TradeAssetPairsFragment
import io.tokend.template.features.withdraw.WithdrawFragment
import io.tokend.template.features.withdraw.model.WithdrawalRequest
import io.tokend.template.fragments.ToolbarProvider
import io.tokend.template.logic.WalletEventsListener
import io.tokend.template.util.ObservableTransformers
import io.tokend.template.util.ProfileUtil
import io.tokend.template.util.navigation.Navigator
import io.tokend.template.view.util.GlideDrawerImageLoader
import io.tokend.template.view.util.LocalizedName
import io.tokend.template.view.util.input.SoftInputUtil
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.HashMap

class MainActivity : BaseActivity(), WalletEventsListener {
    companion object {
        private val DEFAULT_FRAGMENT_ID = DashboardFragment.ID
    }

    private var navigationDrawer: Drawer? = null
    private var landscapeNavigationDrawer: Drawer? = null
    private var onBackPressedListener: OnBackPressedListener? = null
    private val tablet by lazy {
        resources.getBoolean(R.bool.isTablet)
    }
    private val orientation: Int
        get() = resources.configuration.orientation
    private var accountHeader: AccountHeader? = null
    private var landscapeAccountHeader: AccountHeader? = null

    private var toolbar: Toolbar? = null

    private val activeKycRepository: ActiveKycRepository
        get() = repositoryProvider.activeKyc

    private val accountRepository: AccountRepository
        get() = repositoryProvider.account

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_main)
        window.setBackgroundDrawable(null)

        initNavigation()

        subscribeToKycChanges()

        navigationDrawer?.setSelection(DEFAULT_FRAGMENT_ID)
    }

    // region Init
    private fun initNavigation() {
        val login = session.login

        val placeholderValue = login.toUpperCase(Locale.ENGLISH)
        val placeholderSize =
            resources.getDimensionPixelSize(R.dimen.material_drawer_item_profile_icon_width)
        val placeholderBackground =
            ContextCompat.getColor(this, R.color.avatar_placeholder_background)
        val placeholderDrawable =
            ProfileUtil.getAvatarPlaceholder(placeholderValue, this, placeholderSize)
        DrawerImageLoader.init(
            GlideDrawerImageLoader(this, placeholderDrawable, placeholderBackground)
        )

        val items = HashMap<Long, PrimaryDrawerItem>()

        PrimaryDrawerItem()
            .withName(R.string.dashboard_title)
            .withIdentifier(DashboardFragment.ID)
            .withIcon(R.drawable.ic_dashboard)
            .also { items[DashboardFragment.ID] = it }

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
            .withName(R.string.explore_sales_title)
            .withIdentifier(SalesFragment.ID)
            .withIcon(R.drawable.ic_invest)
            .also { items[SalesFragment.ID] = it }

        PrimaryDrawerItem()
            .withName(R.string.trade_title)
            .withIdentifier(TradeAssetPairsFragment.ID)
            .withIcon(R.drawable.ic_trade)
            .also { items[TradeAssetPairsFragment.ID] = it }

        PrimaryDrawerItem()
            .withName(R.string.polls_title)
            .withIdentifier(PollsFragment.ID)
            .withIcon(R.drawable.ic_poll)
            .also { items[PollsFragment.ID] = it }

        PrimaryDrawerItem()
            .withName(R.string.settings_title)
            .withIdentifier(GeneralSettingsFragment.ID)
            .withIcon(R.drawable.ic_settings)
            .also { items[GeneralSettingsFragment.ID] = it }

        val accountHeader = getHeaderInstance(login)
        val landscapeAccountHeader = getHeaderInstance(login)

        navigationDrawer = initDrawerBuilder(items, accountHeader).build()
        landscapeNavigationDrawer = initDrawerBuilder(items, landscapeAccountHeader).buildView()
        nav_tablet.addView(landscapeNavigationDrawer?.slider, 0)

        this.accountHeader = accountHeader
        this.landscapeAccountHeader = landscapeAccountHeader
    }

    private fun getHeaderInstance(login: String?): AccountHeader {
        return AccountHeaderBuilder()
            .withActivity(this)
            .withAccountHeader(R.layout.navigation_drawer_header)
            .withHeaderBackground(
                ColorDrawable(ContextCompat.getColor(this, R.color.material_drawer_background))
            )
            .withTextColor(ContextCompat.getColor(this, R.color.primary_text))
            .withSelectionListEnabledForSingleProfile(false)
            .withProfileImagesVisible(true)
            .withDividerBelowHeader(true)
            .addProfiles(getProfileHeaderItem(login, null, accountRepository.item!!.role.role))
            .withOnAccountHeaderListener { _, _, _ ->
                openAccountIdShare()
                true
            }
            .build()
            .apply {
                view.findViewById<View>(R.id.material_drawer_account_header_current)
                    .setOnClickListener { openAccountIdShare() }
            }
    }

    private fun getProfileHeaderItem(
        login: String?,
        activeKyc: ActiveKyc?,
        accountRole: AccountRole
    ): ProfileDrawerItem {
        val avatarUrl = ProfileUtil.getAvatarUrl(activeKyc, urlConfigProvider)
        val roleName = LocalizedName(this).forAccountRole(accountRole)

        return ProfileDrawerItem()
            .withIdentifier(1)
            .withName(login)
            .withEmail(roleName)
            .apply {
                avatarUrl?.also { withIcon(it) }
            }
    }

    private fun initDrawerBuilder(
        items: Map<Long, PrimaryDrawerItem>,
        profileHeader: AccountHeader
    ): DrawerBuilder {
        return DrawerBuilder()
            .withActivity(this)
            .withAccountHeader(profileHeader)
            .withHeaderDivider(false)
            .withTranslucentStatusBar(false)
            .withSliderBackgroundColorRes(R.color.material_drawer_background)
            .addDrawerItems(
                items[DashboardFragment.ID]
            )
            .apply {

                if (BuildConfig.IS_DEPOSIT_ALLOWED) {
                    addDrawerItems(items[DepositFragment.ID])
                }

                if (BuildConfig.IS_WITHDRAW_ALLOWED) {
                    addDrawerItems(items[WithdrawFragment.ID])
                }

                if (BuildConfig.IS_INVEST_ALLOWED) {
                    addDrawerItems(items[SalesFragment.ID])
                }

                if (BuildConfig.IS_TRADE_ALLOWED) {
                    addDrawerItems(items[TradeAssetPairsFragment.ID])
                }

                if (BuildConfig.ARE_POLLS_ALLOWED) {
                    addDrawerItems(items[PollsFragment.ID])
                }
            }
            .addDrawerItems(
                items[GeneralSettingsFragment.ID]
            )
            .withOnDrawerItemClickListener { _, _, item ->
                return@withOnDrawerItemClickListener onNavigationItemSelected(item)
            }
    }
    // endregion

    private fun subscribeToKycChanges() {
        activeKycRepository
            .itemSubject
            .compose(ObservableTransformers.defaultSchedulers())
            .subscribe { updateProfileHeader() }
            .addTo(compositeDisposable)
    }

    // region Navigation
    private fun onNavigationItemSelected(item: IDrawerItem<Any, RecyclerView.ViewHolder>)
            : Boolean {
        navigateTo(item.identifier)
        return false
    }

    private fun navigateTo(screenIdentifier: Long, fragment: Fragment) {
        navigationDrawer?.setSelection(screenIdentifier, false)
        landscapeNavigationDrawer?.setSelection(screenIdentifier, false)

        onBackPressedListener = fragment as? OnBackPressedListener

        SoftInputUtil.hideSoftInput(this)

        displayFragment(fragment)
    }

    private fun navigateTo(screenIdentifier: Long) {
        val fragment: Fragment =
            when (screenIdentifier) {
                DashboardFragment.ID -> DashboardFragment.newInstance()
                WithdrawFragment.ID -> WithdrawFragment.newInstance(WithdrawFragment.getBundle())
                ExploreAssetsFragment.ID -> ExploreAssetsFragment.newInstance()
                GeneralSettingsFragment.ID -> GeneralSettingsFragment.newInstance()
                DepositFragment.ID -> DepositFragment.newInstance(DepositFragment.getBundle())
                SalesFragment.ID -> SalesFragment.newInstance()
                TradeAssetPairsFragment.ID -> TradeAssetPairsFragment.newInstance()
                PollsFragment.ID -> PollsFragment.newInstance(
                    PollsFragment.getBundle(
                        allowToolbar = true,
                        ownerAccountId = null
                    )
                )
                else -> return
            }

        navigateTo(screenIdentifier, fragment)
    }
    // endregion

    private fun updateProfileHeader() {
        val login = session.login
        val activeKyc = activeKycRepository.item
        val accountRole = accountRepository.item!!.role.role

        val h = getProfileHeaderItem(login, activeKyc, accountRole)
        accountHeader?.updateProfile(h)
        landscapeAccountHeader?.updateProfile(h)
    }

    private fun openAccountIdShare() {
        val walletInfo = walletInfoProvider.getWalletInfo()

        Navigator.from(this@MainActivity).openAccountQrShare(walletInfo)

        navigationDrawer?.closeDrawer()
    }

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
                if (onBackPressedListener?.onBackPressed() != false)
                    moveTaskToBack(true)
            } else {
                if (onBackPressedListener?.onBackPressed() != false)
                    navigateTo(DEFAULT_FRAGMENT_ID)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateDrawerVisibility()
    }

    override fun onPaymentRequestConfirmed(paymentRequest: PaymentRequest) {
        navigateTo(DashboardFragment.ID)
    }

    override fun onWithdrawalRequestConfirmed(withdrawalRequest: WithdrawalRequest) {
        navigateTo(DashboardFragment.ID)
    }
}