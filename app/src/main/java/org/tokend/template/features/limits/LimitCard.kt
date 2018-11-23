package org.tokend.template.features.limits

import android.content.Context
import android.support.v4.content.res.ResourcesCompat
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.layout_card_limit.view.*
import org.jetbrains.anko.*
import org.tokend.template.R
import org.tokend.template.view.util.ViewProvider
import java.math.BigDecimal

class LimitCard(private val context: Context,
                private val type: String,
                private val asset: String,
                private val paymentTotal: BigDecimal?,
                private val paymentUsed: BigDecimal?,
                private val depositTotal: BigDecimal?,
                private val depositUsed: BigDecimal?) : ViewProvider {

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

        view.payment_limit.setValues(paymentUsed?:zero, paymentTotal?:zero, asset)
        view.deposit_limit.setValues(depositUsed?:zero, depositTotal?:zero, asset)
        view.limit_type.text = type
    }

    private fun showDialog() {
        AlertDialogBuilder(context).apply {
            title(type)

            val msg = "${context.getString(R.string.payment)} \n" +
                    view.payment_limit.unformatted + "\n\n" +

                    "${context.getString(R.string.deposit)} \n" +
                    view.deposit_limit.unformatted + "\n"

            message(msg)
            positiveButton(android.R.string.ok) {
                dismiss()
            }
        }.show().dialog?.findViewById<TextView>(android.R.id.message)?.let { textView ->
            textView.textColor =
                    ResourcesCompat.getColor(context.resources, R.color.secondary_text, null)
        }
    }
}