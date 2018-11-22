package org.tokend.template.features.limits

import android.content.Context
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.layout_card_limit.view.*
import org.jetbrains.anko.layoutInflater
import org.tokend.sdk.utils.BigDecimalUtil
import org.tokend.template.R
import org.tokend.template.view.util.ViewProvider
import org.tokend.template.view.util.formatter.AmountFormatter
import java.math.BigDecimal

class LimitCard(private val context: Context,
                private val type: String,
                private val total: BigDecimal,
                private val used: BigDecimal) : ViewProvider {

    private lateinit var view: View

    override fun addTo(rootView: ViewGroup): ViewProvider {
        rootView.addView(getView(rootView))
        return this
    }

    override fun getView(rootView: ViewGroup): View {
        view = context.layoutInflater
                .inflate(R.layout.layout_card_limit, rootView, false)
        setLimit()
        return view
    }

    private fun setLimit() {
        val totalString: String
        val leftString: String

        if(total == BigDecimal.ZERO) {
            totalString = "—"
            leftString = "—"
        } else {
            totalString = BigDecimalUtil.scaleAmount(total, AmountFormatter.ASSET_DECIMAL_DIGITS).toString()
            leftString = BigDecimalUtil.scaleAmount(used, AmountFormatter.ASSET_DECIMAL_DIGITS).toString()
        }
        val limitString =
                context.getString(R.string.template_limit, leftString, totalString)
        view.payment_limit.text = limitString
        view.deposit_limit.text = context.getString(R.string.template_limit, "—", "—")
        view.limit_type.text = type
    }
}