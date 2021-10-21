package org.tokend.template.features.fees.adapter

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.layout_fee_info.view.*
import org.tokend.template.R
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.util.LocalizedName

class FeesViewHolder(view: View) : BaseViewHolder<List<FeeListItem>>(view) {

    private val localizedName = LocalizedName(view.context)
    private val feesInfoContainer: LinearLayout = view.findViewById(R.id.fees_info_container)
    private val feeTypeText: TextView = view.findViewById(R.id.fee_type)
    private val feeSubtypeText: TextView = view.findViewById(R.id.fee_subtype)


    override fun bind(item: List<FeeListItem>) {
        val fee = item.first()
        feeTypeText.text = localizedName.forFeeType(fee.type)
        feeSubtypeText.text = localizedName.forFeeSubtype(fee.subtype)
        setFeeList(item)
    }

    private fun setFeeList(fees: List<FeeListItem>) {
        feesInfoContainer.removeAllViews()
        fees.forEach { fee ->
            val feeFields = LayoutInflater.from(view.context)
                    .inflate(R.layout.layout_fee_info, feesInfoContainer, false)

            val lowerBound = fee.lowerBound
            val upperBound = fee.upperBound

            val boundsString = when {
                lowerBound == "0" && upperBound.isEmpty() ->
                    view.context.getString(R.string.fee_bounds_default)

                lowerBound == "0" && upperBound.isNotEmpty() ->
                    view.context.getString(R.string.template_fee_bounds_upto, upperBound)

                upperBound.isEmpty() ->
                    view.context.getString(R.string.template_fee_bounds_from, lowerBound)

                else -> view.context.getString(R.string.template_fee_bounds, lowerBound, upperBound)
            }

            feeFields.bounds_text.text = boundsString

            val valuesText = "${fee.fixed}\n${fee.percent}"
            feeFields.values_text.text = valuesText

            feesInfoContainer.addView(feeFields)
        }
    }
}