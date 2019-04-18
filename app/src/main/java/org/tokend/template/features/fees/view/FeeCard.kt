package org.tokend.template.features.fees.view

import android.content.Context
import android.support.v4.content.ContextCompat
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.layout_fee_card.view.*
import kotlinx.android.synthetic.main.layout_fee_info.view.*
import org.tokend.template.R
import org.tokend.template.extensions.highlight
import org.tokend.template.view.util.LocalizedName

class FeeCard(private val context: Context,
              private val fees: List<FeeItem>) {

    private lateinit var view: ViewGroup
    private val localizedName = LocalizedName(context)
    private val layoutInflater = LayoutInflater.from(context)

    fun addTo(rootView: ViewGroup) {
        rootView.addView(getView(rootView))
    }

    private fun getView(rootView: ViewGroup): View {
        view = layoutInflater.inflate(R.layout.layout_fee_card, rootView, false) as ViewGroup

        val fee = fees.first()
        view.fee_type.text = localizedName.forFeeType(fee.type)
        view.fee_subtype.text = localizedName.forFeeSubtype(fee.subtype)
        setFeeList()
        return view
    }

    private fun setFeeList() {
        val highlightColor = ContextCompat.getColor(context, R.color.primary)
        fees.forEach { fee ->
            val feeFields = layoutInflater.inflate(R.layout.layout_fee_info, view, false)

            val lowerBoundAmountString = fee.lowerBound
            val lowerBoundString =
                    context.getString(R.string.template_lower_bound, lowerBoundAmountString)
            val lowerBoundSpannableString = SpannableString(lowerBoundString)
            lowerBoundSpannableString.highlight(lowerBoundAmountString, highlightColor)

            feeFields.from_text.text = lowerBoundSpannableString

            if (fee.upperBound.isNotEmpty()) {
                val upperBoundAmountString = fee.upperBound
                val upperBoundString =
                        context.getString(R.string.template_upper_bound, upperBoundAmountString)
                val upperBoundSpannableString = SpannableString(upperBoundString)
                upperBoundSpannableString.highlight(upperBoundAmountString, highlightColor)

                feeFields.to_text.text = upperBoundSpannableString
            }

            val valuesText = "${fee.fixed}\n${fee.percent}"
            feeFields.values_text.text = valuesText

            view.fees_info_container.addView(feeFields)
        }
    }
}