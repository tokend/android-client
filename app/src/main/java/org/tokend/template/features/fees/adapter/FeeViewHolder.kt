package org.tokend.template.features.fees.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_fee.view.*
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.util.LocalizedName

class FeeViewHolder(itemView: View) : BaseViewHolder<FeeItem>(itemView) {

    init {
        setIsRecyclable(false)
    }

    private val localizedName = LocalizedName(view.context)

    override fun bind(item: FeeItem) {
        view.fee_type.text = localizedName.forFeeType(item.type)
        view.fee_subtype.text = localizedName.forFeeSubtype(item.subtype)
        view.fixed_value.text = item.fixed
        view.percent_value.text = item.percent
        view.lower_bound_value.text = item.lowerBound
        view.upper_bound_value.text = item.upperBound
    }
}