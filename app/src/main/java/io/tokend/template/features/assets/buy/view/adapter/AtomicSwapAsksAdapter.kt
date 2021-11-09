package io.tokend.template.features.assets.buy.view.adapter

import android.view.ViewGroup
import io.tokend.template.R
import io.tokend.template.extensions.layoutInflater
import io.tokend.template.view.adapter.base.BaseRecyclerAdapter
import io.tokend.template.view.util.formatter.AmountFormatter

class AtomicSwapAsksAdapter(
    private val amountFormatter: AmountFormatter
) : BaseRecyclerAdapter<AtomicSwapAskListItem, AtomicSwapAskItemViewHolder>() {
    override fun createItemViewHolder(parent: ViewGroup): AtomicSwapAskItemViewHolder {
        val view = parent.context.layoutInflater.inflate(
            R.layout.list_item_atomic_swap_ask, parent, false
        )
        return AtomicSwapAskItemViewHolder(view, amountFormatter)
    }

}