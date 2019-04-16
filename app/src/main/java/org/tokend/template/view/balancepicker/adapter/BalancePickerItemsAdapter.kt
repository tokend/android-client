package org.tokend.template.view.balancepicker.adapter

import android.support.v7.util.DiffUtil
import android.view.ViewGroup
import org.jetbrains.anko.layoutInflater
import org.tokend.template.R
import org.tokend.template.view.adapter.base.BaseRecyclerAdapter
import org.tokend.template.view.util.formatter.AmountFormatter

class BalancePickerItemsAdapter(
        private val amountFormatter: AmountFormatter
) : BaseRecyclerAdapter<BalancePickerListItem, BalancePickerItemViewHolder>() {
    override fun createItemViewHolder(parent: ViewGroup): BalancePickerItemViewHolder {
        val view = parent.context.layoutInflater
                .inflate(R.layout.list_item_balance_picker_item, parent, false)
        return BalancePickerItemViewHolder(view, amountFormatter)
    }

    override fun bindItemViewHolder(holder: BalancePickerItemViewHolder, position: Int) {
        super.bindItemViewHolder(holder, position)
        holder.dividerIsVisible = position < itemCount - 1
    }

    override fun getDiffCallback(newItems: List<BalancePickerListItem>): DiffUtil.Callback? {
        return object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val first = items[oldItemPosition]
                val second = newItems[newItemPosition]

                return first.source != null && first.source.id == second.source?.id
            }

            override fun getOldListSize(): Int {
                return items.size
            }

            override fun getNewListSize(): Int {
                return newItems.size
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val first = items[oldItemPosition]
                val second = newItems[newItemPosition]

                // Divider.
                if (oldItemPosition == items.size - 1 && newItemPosition != newItems.size - 1
                        || oldItemPosition != items.size - 1 && newItemPosition == newItems.size - 1) {
                    return false
                }

                return first == second
            }
        }
    }
}