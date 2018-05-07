package org.tokend.template.features.dashboard


import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_dashboard.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.template.R
import org.tokend.template.base.fragments.BaseFragment
import org.tokend.template.base.fragments.ToolbarProvider

class DashboardFragment : BaseFragment(), ToolbarProvider {
    override val toolbarSubject: BehaviorSubject<Toolbar> = BehaviorSubject.create<Toolbar>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbarSubject.onNext(toolbar)

        toolbar.title = getString(R.string.dashboard_title)

        balance_text_view.text = "0 TKD"
        converted_balance_text_view.text = "0 USD"
        activity_layout.visibility = View.GONE
        empty_view.visibility = View.VISIBLE
    }

    companion object {
        fun newInstance(): DashboardFragment {
            val fragment = DashboardFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}
