package org.tokend.template.features.fees.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import org.tokend.template.R
import org.tokend.template.view.adapter.base.BaseRecyclerAdapter

class FeesAdapter : BaseRecyclerAdapter<List<FeeListItem>, FeesViewHolder>() {
    override fun createItemViewHolder(parent: ViewGroup): FeesViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_fees, parent, false)
        return FeesViewHolder(view)
    }
}