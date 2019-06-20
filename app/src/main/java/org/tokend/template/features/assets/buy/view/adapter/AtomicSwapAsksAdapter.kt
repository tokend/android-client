package org.tokend.template.features.assets.buy.view.adapter

import android.view.ViewGroup
import org.jetbrains.anko.layoutInflater
import org.tokend.template.R
import org.tokend.template.view.adapter.base.BaseRecyclerAdapter
import org.tokend.template.view.util.formatter.AmountFormatter

class AtomicSwapAsksAdapter(
        private val amountFormatter: AmountFormatter
): BaseRecyclerAdapter<AtomicSwapAskListItem, AtomicSwapAskItemViewHolder>() {
    override fun createItemViewHolder(parent: ViewGroup): AtomicSwapAskItemViewHolder {
        val view = parent.context.layoutInflater.inflate(
                R.layout.list_item_atomic_swap_ask, parent, false
        )
        return AtomicSwapAskItemViewHolder(view, amountFormatter)
    }

    override fun bindItemViewHolder(holder: AtomicSwapAskItemViewHolder, position: Int) {
        super.bindItemViewHolder(holder, position)
        //holder.topDividerIsVisible = position != 0
    }
}