package org.tokend.template.activities

import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.View
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
import kotlinx.android.synthetic.main.activity_main.*
import org.tokend.template.BuildConfig
import org.tokend.template.R
import org.tokend.template.features.assets.view.ExploreAssetsFragment
import org.tokend.template.features.dashboard.view.DashboardFragment
import org.tokend.template.features.deposit.DepositFragment
import org.tokend.template.features.invest.view.SalesFragment
import org.tokend.template.features.kyc.model.ActiveKyc
import org.tokend.template.features.kyc.model.KycRequestState
import org.tokend.template.features.kyc.storage.ActiveKycRepository
import org.tokend.template.features.polls.view.PollsFragment
import org.tokend.template.features.send.model.PaymentRequest
import org.tokend.template.features.settings.GeneralSettingsFragment
import org.tokend.template.features.trade.pairs.view.TradeAssetPairsFragment
import org.tokend.template.features.withdraw.WithdrawFragment
import org.tokend.template.features.withdraw.model.WithdrawalRequest
import org.tokend.template.fragments.ToolbarProvider
import org.tokend.template.logic.WalletEventsListener
import org.tokend.template.util.Navigator
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.util.ProfileUtil
import org.tokend.template.view.util.LocalizedName
import org.tokend.template.view.util.PicassoDrawerImageLoader
import org.tokend.template.view.util.input.SoftInputUtil

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
        get() = repositoryProvider.activeKyc()

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_main)
        window.setBackgroundDrawable(null)

        initNavigation()

        subscribeToKycChanges()

        navigationDrawer?.setSelection(DEFAULT_FRAGMENT_ID)
    }

    // region Init
    private fun initNavigation() {
        val email = walletInfoProvider.getWalletInfo()?.email

        val placeholderValue = (email ?: getString(R.string.app_name)).toUpperCase()
        val placeholderSize =
                resources.getDimensionPixelSize(R.dimen.material_drawer_item_profile_icon_width)
        val placeholderBackground =
                ContextCompat.getColor(this, R.color.avatar_placeholder_background)
        val placeholderDrawable =
                ProfileUtil.getAvatarPlaceholder(placeholderValue, this, placeholderSize)
        DrawerImageLoader.init(
                PicassoDrawerImageLoader(this, placeholderDrawable, placeholderBackground)
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

        val accountHeader = getHeaderInstance(email)
        val landscapeAccountHeader = getHeaderInstance(email)

        navigationDrawer = initDrawerBuilder(items, accountHeader).build()
        landscapeNavigationDrawer = initDrawerBuilder(items, landscapeAccountHeader).buildView()
        nav_tablet.addView(landscapeNavigationDrawer?.slider, 0)

        this.accountHeader = accountHeader
        this.landscapeAccountHeader = landscapeAccountHeader
    }

    private fun getHeaderInstance(email: String?): AccountHeader {
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
                .addProfiles(getProfileHeaderItem(email, null))
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

    private fun getProfileHeaderItem(email: String?,
                                     activeKyc: ActiveKyc?): ProfileDrawerItem {
        val form = (activeKyc as? ActiveKyc.Form)?.formData
        val avatarUrl = ProfileUtil.getAvatarUrl(activeKyc, urlConfigProvider)

        return ProfileDrawerItem()
                .withIdentifier(1)
                .withName(email)
                .withEmail(
                        when {
                            activeKyc == null -> getString(R.string.loading_data)
                            form == null -> getString(R.string.unverified_account)
                            else -> LocalizedName(this).forKycForm(form)
                        }
                )
                .apply {
                    avatarUrl?.also { withIcon(it) }
                }
    }

    private fun initDrawerBuilder(items: Map<Long, PrimaryDrawerItem>,
                                  profileHeader: AccountHeader): DrawerBuilder {
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
                    PollsFragment.ID -> PollsFragment.newInstance(PollsFragment.getBundle(
                            allowToolbar = true,
                            ownerAccountId = null
                    ))
                    else -> return
                }

        navigateTo(screenIdentifier, fragment)
    }
    // endregion

    private fun updateProfileHeader() {
        val email = walletInfoProvider.getWalletInfo()?.email
        val activeKyc = activeKycRepository.item

        val h = getProfileHeaderItem(email, activeKyc)
        accountHeader?.updateProfile(h)
        landscapeAccountHeader?.updateProfile(h)
    }

    private fun openAccountIdShare() {
        val walletInfo = walletInfoProvider.getWalletInfo() ?: return

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

    override fun onConfigurationChanged(newConfig: Configuration?) {
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