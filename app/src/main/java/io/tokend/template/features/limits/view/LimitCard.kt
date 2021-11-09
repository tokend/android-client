package io.tokend.template.features.limits.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import io.tokend.template.R
import io.tokend.template.extensions.layoutInflater
import io.tokend.template.view.util.ViewProvider
import io.tokend.template.view.util.formatter.AmountFormatter
import kotlinx.android.synthetic.main.layout_card_limit.view.*
import org.tokend.sdk.api.accounts.model.limits.LimitEntry

class LimitCard(
    private val context: Context,
    private val type: String,
    private val asset: String,
    private val entry: LimitEntry,
    private val amountFormatter: AmountFormatter
) : ViewProvider {

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