package org.tokend.template.features.fees.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import org.tokend.template.R
import org.tokend.template.view.adapter.base.BaseRecyclerAdapter

class FeeAdapter : BaseRecyclerAdapter<FeeItem, FeeViewHolder>() {

    override fun createItemViewHolder(parent: ViewGroup): FeeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_fee, parent, false)
        return  FeeViewHolder(view)
    }
}