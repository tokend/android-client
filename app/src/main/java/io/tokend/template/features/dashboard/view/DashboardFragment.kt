package io.tokend.template.features.dashboard.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.SupportMenuInflater
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import io.reactivex.subjects.BehaviorSubject
import io.tokend.template.BuildConfig
import io.tokend.template.R
import io.tokend.template.activities.OnBackPressedListener
import io.tokend.template.features.dashboard.balances.view.BalancesFragment
import io.tokend.template.features.dashboard.movements.view.AccountMovementsFragment
import io.tokend.template.fragments.BaseFragment
import io.tokend.template.fragments.ToolbarProvider
import io.tokend.template.util.navigation.Navigator
import io.tokend.template.view.util.UserFlowFragmentDisplayer
import io.tokend.template.view.util.input.SoftInputUtil
import kotlinx.android.synthetic.main.fragment_dashboard.*
import kotlinx.android.synthetic.main.toolbar.*

class DashboardFragment : BaseFragment(), ToolbarProvider {
    override val toolbarSubject: BehaviorSubject<Toolbar> = BehaviorSubject.create()

    private val fragmentDisplayer = UserFlowFragmentDisplayer(this, R.id.fragment_container_layout)
    private var backPressedListener: OnBackPressedListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onInitAllowed() {
        initToolbar()
        initTabs()

        navigateToPage(R.id.balances)
    }

    // region Init
    private fun initToolbar() {
        toolbarSubject.onNext(toolbar)
        toolbar.title = getString(R.string.dashboard_title)
    }

    private fun initTabs() {
        if (!BuildConfig.IS_SEND_ALLOWED) {
            bottom_tabs.menu.removeItem(R.id.send)
            bottom_tabs.menu.removeItem(R.id.receive)
        }

        bottom_tabs.setOnNavigationItemSelectedListener {
            navigateToPage(it.itemId)
        }
    }
    // endregion

    // region Navigation
    private fun navigateToPage(pageId: Int): Boolean {
        return when (pageId) {
            R.id.send -> {
                openSend()
                false
            }
            R.id.receive -> {
                openReceive()
                false
            }
            else -> {
                val fragment = when (pageId) {
                    R.id.balances -> BalancesFragment.newInstance()
                    R.id.movements -> AccountMovementsFragment.newInstance()
                    else -> null
                }

                if (fragment != null) {
                    displayFragment(fragment)
                    true
                } else {
                    false
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun displayFragment(fragment: Fragment) {
        // Menu.
        toolbar.menu.clear()
        fragment.onCreateOptionsMenu(
            toolbar.menu,
            SupportMenuInflater(requireContext())
        )

        if (fragment is OnBackPressedListener) {
            backPressedListener = fragment
        }

        fragmentDisplayer.display(fragment, "", null)

        SoftInputUtil.hideSoftInput(requireActivity())
    }

    private fun openSend() {
        Navigator.from(this).openSend()
    }

    private fun openReceive() {
        val walletInfo = walletInfoProvider.getWalletInfo()
        Navigator.from(this).openAccountQrShare(walletInfo)
    }
    // endregion

    override fun onBackPressed(): Boolean {
        return backPressedListener?.onBackPressed() ?: true
    }

    companion object {
        val ID = "dashboard".hashCode().toLong()

        fun newInstance() = DashboardFragment()
    }
}
