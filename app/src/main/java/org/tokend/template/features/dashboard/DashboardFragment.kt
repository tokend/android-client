package org.tokend.template.features.dashboard


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_dashboard.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.childrenSequence
import org.tokend.template.R
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.fragments.ToolbarProvider

class DashboardFragment : BaseFragment(), ToolbarProvider {
    override val toolbarSubject: BehaviorSubject<Toolbar> = BehaviorSubject.create<Toolbar>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onInitAllowed() {
        toolbarSubject.onNext(toolbar)

        toolbar.title = getString(R.string.dashboard_title)

        initViews()
        update()
    }

    // region Init

    private fun initViews() {
        initAssetTabsCard()
                .addTo(cards_layout)
                .initViewMoreButton(this)

        initPendingOffersCard()
                .addTo(cards_layout)
                .initViewMoreButton(this)

        addSpacesBetweenViews()
    }

    private fun initAssetTabsCard(): AssetTabsCard {
        return  AssetTabsCard(
                activity!!,
                repositoryProvider,
                errorHandlerFactory,
                assetComparator,
                compositeDisposable
        )
    }

    private fun initPendingOffersCard(): PendingOffersCard {
        return PendingOffersCard(
                context,
                repositoryProvider,
                compositeDisposable)
    }

    private fun addSpacesBetweenViews() {
        cards_layout.childrenSequence().forEach { view ->
            view.layoutParams.also { params ->
                params as ViewGroup.MarginLayoutParams
                params.bottomMargin = this.resources.getDimensionPixelSize(R.dimen.half_standard_margin)
                view.layoutParams = params
            }
        }
    }
    // endregion

    private fun update() {
        repositoryProvider.balances().updateIfNotFresh()
        repositoryProvider.offers().updateIfNotFresh()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PendingOffersCard.CANCEL_OFFER_REQUEST,
                AssetTabsCard.SEND_REQUEST -> update()
            }
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
