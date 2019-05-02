package org.tokend.template.features.fees.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.layout_fee_card.view.*
import kotlinx.android.synthetic.main.layout_fee_info.view.*
import org.tokend.template.R
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
        fees.forEach { fee ->
            val feeFields = layoutInflater.inflate(R.layout.layout_fee_info, view, false)

            val lowerBound = fee.lowerBound
            val upperBound = fee.upperBound

            val boundsString = when {
                lowerBound == "0" && upperBound.isEmpty() ->
                    context.getString(R.string.fee_bounds_default)

                lowerBound == "0" && upperBound.isNotEmpty() ->
                    context.getString(R.string.template_fee_bounds_upto, upperBound)

                upperBound.isEmpty() ->
                context.getString(R.string.template_fee_bounds_from, lowerBound)

                else -> context.getString(R.string.template_fee_bounds, lowerBound, upperBound)
            }

            feeFields.bounds_text.text = boundsString

            val valuesText = "${fee.fixed}\n${fee.percent}"
            feeFields.values_text.text = valuesText

            view.fees_info_container.addView(feeFields)
        }
    }
}