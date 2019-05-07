package org.tokend.template.features.dashboard.view

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.activity_multiple_fragments.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.template.R
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.fragments.ToolbarProvider

class DashboardFragment : BaseFragment(), ToolbarProvider {
    override val toolbarSubject: BehaviorSubject<Toolbar> = BehaviorSubject.create<Toolbar>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_multiple_fragments, container, false)
    }

    override fun onInitAllowed() {
        toolbarSubject.onNext(toolbar)

        toolbar.title = getString(R.string.dashboard_title)

        initViewPager()
    }

    // region Init
    private fun initViewPager() {
        val adapter = DashboardPagerAdapter(requireContext(), childFragmentManager)
        pager.adapter = adapter
        pager.offscreenPageLimit = adapter.count
        pager.background = ColorDrawable(
                ContextCompat.getColor(requireContext(), R.color.colorDefaultBackground)
        )
        toolbar_tabs.setupWithViewPager(pager)
    }
    // endregion

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
