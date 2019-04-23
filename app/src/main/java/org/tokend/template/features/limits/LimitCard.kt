package org.tokend.template.features.limits

import android.content.Context
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.layout_card_limit.view.*
import org.jetbrains.anko.layoutInflater
import org.tokend.sdk.api.accounts.model.limits.LimitEntry
import org.tokend.template.R
import org.tokend.template.view.util.ViewProvider
import org.tokend.template.view.util.formatter.AmountFormatter

class LimitCard(private val context: Context,
                private val type: String,
                private val asset: String,
                private val entry: LimitEntry,
                private val amountFormatter: AmountFormatter) : ViewProvider {

    private lateinit var view: View

    override fun addTo(rootView: ViewGroup): ViewProvider {
        rootView.addView(getView(rootView))
        return this
    }

    override fun getView(rootView: ViewGroup): View {
        view = context.layoutInflater
                .inflate(R.layout.layout_card_limit, rootView, false).apply {
                    val params = layoutParams as ViewGroup.MarginLayoutParams
                    params.bottomMargin =
                            this.resources.getDimensionPixelSize(R.dimen.half_standard_margin)
                    layoutParams = params
                }
        setLimit()
        return view
    }

    private fun setLimit() {
        view.limit_type.text = type
        LimitProgressWrapper(view, amountFormatter).displayProgress(entry, asset)
    }
}