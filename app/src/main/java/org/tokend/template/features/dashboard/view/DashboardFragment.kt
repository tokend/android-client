package org.tokend.template.features.dashboard.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.support.v7.view.SupportMenuInflater
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_dashboard.*
import kotlinx.android.synthetic.main.toolbar_white.*
import org.tokend.template.BuildConfig
import org.tokend.template.R
import org.tokend.template.activities.OnBackPressedListener
import org.tokend.template.extensions.disableShifting
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.fragments.ToolbarProvider
import org.tokend.template.util.Navigator
import org.tokend.template.view.util.input.SoftInputUtil

class DashboardFragment : BaseFragment(), ToolbarProvider {
    override val toolbarSubject: BehaviorSubject<Toolbar> = BehaviorSubject.create<Toolbar>()

    private lateinit var adapter: DashboardPagerAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onInitAllowed() {
        toolbarSubject.onNext(toolbar)

        toolbar.title = getString(R.string.dashboard_title)

        initViewPager()
        initTabs()
    }

    // region Init
    @SuppressLint("RestrictedApi")
    private fun initViewPager() {
        adapter = DashboardPagerAdapter(requireContext(), childFragmentManager)
        pager.adapter = adapter
        pager.offscreenPageLimit = adapter.count
        pager.swipesEnabled = false

        // Menu.
        val inflatePageMenu = { pagePosition: Int ->
            toolbar.menu.clear()
            adapter
                    .getItem(pagePosition)
                    ?.onCreateOptionsMenu(
                            toolbar.menu,
                            SupportMenuInflater(requireContext())
                    )
        }

        val onPageSelected = { pagePosition: Int ->
            inflatePageMenu(pagePosition)
            bottom_tabs.selectedItemId = adapter.getItemId(pagePosition).toInt()
            SoftInputUtil.hideSoftInput(requireActivity())
        }

        pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageSelected(position: Int) {
                onPageSelected(position)
            }

            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
        })

        onPageSelected(0)
    }

    private fun initTabs() {
        bottom_tabs.disableShifting()

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
                val index = adapter.getIndexOf(pageId.toLong())
                if (index >= 0) {
                    pager.currentItem = index
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun openSend() {
        Navigator.from(this).openSend(requestCode = 0)
    }

    private fun openReceive() {
        val walletInfo = walletInfoProvider.getWalletInfo()
                ?: return
        Navigator.from(this).openAccountQrShare(walletInfo)
    }
    // endregion

    override fun onBackPressed(): Boolean {
        val currentPage = adapter.getItem(pager.currentItem)
        return if (currentPage is OnBackPressedListener) {
            currentPage.onBackPressed()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        const val ID = 1110L

        fun newInstance(): DashboardFragment {
            val fragment = DashboardFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}
