package io.tokend.template.features.fees.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import io.tokend.template.R
import io.tokend.template.view.adapter.base.BaseRecyclerAdapter

class FeesAdapter : BaseRecyclerAdapter<List<FeeListItem>, FeesViewHolder>() {
    override fun createItemViewHolder(parent: ViewGroup): FeesViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_fees, parent, false)
        return FeesViewHolder(view)
    }
}