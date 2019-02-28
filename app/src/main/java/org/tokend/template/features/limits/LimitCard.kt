package org.tokend.template.features.limits

import android.content.Context
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.app.AlertDialog
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.layout_card_limit.view.*
import org.jetbrains.anko.layoutInflater
import org.jetbrains.anko.onClick
import org.jetbrains.anko.textColor
import org.tokend.template.R
import org.tokend.template.view.util.ViewProvider
import org.tokend.template.view.util.formatter.AmountFormatter
import java.math.BigDecimal

class LimitCard(private val context: Context,
                private val type: String,
                private val asset: String,
                private val paymentTotal: BigDecimal?,
                private val paymentUsed: BigDecimal?,
                private val depositTotal: BigDecimal?,
                private val depositUsed: BigDecimal?,
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

                    onClick {
                        showDialog()
                    }
                }
        setLimit()
        return view
    }

    private fun setLimit() {
        val zero = BigDecimal.ZERO

        view.payment_limit.setValues(paymentUsed ?: zero, paymentTotal
                ?: zero, asset, amountFormatter)
        view.deposit_limit.setValues(depositUsed ?: zero, depositTotal
                ?: zero, asset, amountFormatter)
        view.limit_type.text = type
    }

    private fun showDialog() {
        AlertDialog.Builder(context, R.style.AlertDialogStyle)
                .setTitle(type)
                .setMessage(
                        "${context.getString(R.string.payment)} \n" +
                                view.payment_limit.unformatted + "\n\n" +

                                "${context.getString(R.string.deposit)} \n" +
                                view.deposit_limit.unformatted + "\n"
                )
                .setPositiveButton(R.string.ok, null)
                .show()
                .apply {
                    findViewById<TextView>(android.R.id.message)?.let { textView ->
                        textView.textColor = ResourcesCompat.getColor(context.resources,
                                R.color.secondary_text, null)
                    }
                }
    }
}