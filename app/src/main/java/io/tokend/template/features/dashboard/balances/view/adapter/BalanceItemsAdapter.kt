package io.tokend.template.features.dashboard.balances.view.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import io.tokend.template.R
import io.tokend.template.extensions.layoutInflater
import io.tokend.template.view.adapter.base.BaseRecyclerAdapter
import io.tokend.template.view.util.formatter.AmountFormatter

class BalanceItemsAdapter(
    private val amountFormatter: AmountFormatter
) : BaseRecyclerAdapter<BalanceListItem, BalanceItemViewHolder>() {
    private var dividersWasDrawn = true

    var drawDividers: Boolean = true
        set(value) {
            field = value
            notifyDataSetChanged()
            dividersWasDrawn = value
        }

    override fun createItemViewHolder(parent: ViewGroup): BalanceItemViewHolder {
        val view = parent.context.layoutInflater.inflate(
            R.layout.list_item_balance,
            parent, false
        )
        return BalanceItemViewHolder(view, amountFormatter)
    }

    override fun onBindViewHolder(holder: BalanceItemViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        holder.dividerIsVisible = drawDividers && position < itemCount - 1
    }

    override fun getDiffCallback(
        newItems: List<BalanceListItem>
    ): DiffUtil.Callback? = object : DiffUtil.Callback() {
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
            if (dividersWasDrawn != drawDividers
                || oldItemPosition == items.size - 1 && newItemPosition != newItems.size - 1
                || oldItemPosition != items.size - 1 && newItemPosition == newItems.size - 1
            ) {
                return false
            }

            return first == second
        }
    }
}