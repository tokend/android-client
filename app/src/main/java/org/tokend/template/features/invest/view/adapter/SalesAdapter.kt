package org.tokend.template.features.invest.view.adapter

import androidx.recyclerview.widget.DiffUtil
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.tokend.template.R
import org.tokend.template.features.invest.model.SaleRecord
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.adapter.base.PaginationRecyclerAdapter
import org.tokend.template.view.util.formatter.AmountFormatter

class SalesAdapter(
        private val storageUrl: String,
        private val amountFormatter: AmountFormatter
) : PaginationRecyclerAdapter<SaleRecord, BaseViewHolder<SaleRecord>>() {
    class FooterViewHolder(v: View) : BaseViewHolder<SaleRecord>(v) {
        override fun bind(item: SaleRecord) {}
    }

    override fun createFooterViewHolder(parent: ViewGroup): BaseViewHolder<SaleRecord> {
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_loading_footer, parent, false)
        return FooterViewHolder(v)
    }

    override fun bindFooterViewHolder(holder: BaseViewHolder<SaleRecord>) {}

    override fun createItemViewHolder(parent: ViewGroup): SaleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_sale,
                parent, false)
        return SaleViewHolder(view, amountFormatter)
    }

    override fun getDiffCallback(newItems: List<SaleRecord>): DiffUtil.Callback? {
        return object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return items[oldItemPosition] == newItems[newItemPosition]
            }

            override fun getOldListSize(): Int {
                return items.size
            }

            override fun getNewListSize(): Int {
                return newItems.size
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val new = newItems[newItemPosition]
                val old = items[oldItemPosition]
                return new.state == old.state
                        && new.currentCap == old.currentCap
                        && new.description == old.description
            }
        }
    }
}