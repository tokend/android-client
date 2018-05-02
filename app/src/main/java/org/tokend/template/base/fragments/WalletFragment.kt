package org.tokend.template.base.fragments


import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.collapsing_balance_appbar.*
import kotlinx.android.synthetic.main.include_error_empty_view.*

import org.tokend.template.R

class WalletFragment : Fragment(), ToolbarProvider {
    override val toolbarSubject: BehaviorSubject<Toolbar> = BehaviorSubject.create<Toolbar>()

    private val defaultAsset: String?
        get() = arguments?.getString(ASSET_EXTRA)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_wallet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbarSubject.onNext(toolbar)

        error_empty_view.showEmpty("Empty wallet")

        toolbar.title = "0 TKD"
        converted_balance_text_view.text = "0 USD"
    }

    companion object {
        private const val ASSET_EXTRA = "asset"

        fun newInstance(asset: String? = null): WalletFragment {
            val fragment = WalletFragment()
            fragment.arguments = Bundle().apply {
                putString(ASSET_EXTRA, asset)
            }
            return fragment
        }
    }
}
