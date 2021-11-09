package io.tokend.template.view.details.adapter

import android.view.ViewGroup
import io.tokend.template.R
import io.tokend.template.extensions.layoutInflater
import io.tokend.template.view.adapter.base.BaseRecyclerAdapter
import io.tokend.template.view.details.DetailsItem

class DetailsItemsAdapter : BaseRecyclerAdapter<DetailsItem, DetailsItemViewHolder>() {
    override fun createItemViewHolder(parent: ViewGroup): DetailsItemViewHolder {
        val view =
            parent.context.layoutInflater.inflate(R.layout.list_item_details_row, parent, false)
        return DetailsItemViewHolder(view)
    }

    override fun bindItemViewHolder(holder: DetailsItemViewHolder, position: Int) {
        super.bindItemViewHolder(holder, position)
        val lastInGroup = position == itemCount - 1
                || getItemAt(position + 1)?.hasHeader == true
        val nextHasNoIcon = position == itemCount - 1
                || getItemAt(position + 1)?.icon == null

        holder.dividerIsVisible = !lastInGroup && !nextHasNoIcon
    }

    fun addOrUpdateItem(newItem: DetailsItem) {
        val index = items.indexOf(newItem)
        if (index > -1) {
            items[index] = newItem
            notifyItemChanged(index)
        } else {
            addData(newItem)
        }
    }
}