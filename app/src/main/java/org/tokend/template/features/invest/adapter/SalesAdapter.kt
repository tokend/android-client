package org.tokend.template.features.invest.adapter

import android.support.v7.util.DiffUtil
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.tokend.template.R
import org.tokend.template.base.view.adapter.base.BaseViewHolder
import org.tokend.template.base.view.adapter.base.PaginationRecyclerAdapter
import org.tokend.template.extensions.Sale

class SalesAdapter : PaginationRecyclerAdapter<Sale, BaseViewHolder<Sale>>() {
    class FooterViewHolder(v: View) : BaseViewHolder<Sale>(v) {
        override fun bind(item: Sale) {}
    }

    override fun createFooterViewHolder(parent: ViewGroup): BaseViewHolder<Sale> {
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_loading_footer, parent, false)
        return FooterViewHolder(v)
    }

    override fun bindFooterViewHolder(holder: BaseViewHolder<Sale>) {}

    override fun createItemViewHolder(parent: ViewGroup): SaleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_sale,
                parent, false)
        return SaleViewHolder(view)
    }

    override fun getDiffCallback(newItems: List<Sale>): DiffUtil.Callback? {
        return object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return items[oldItemPosition].id == newItems[newItemPosition].id
            }

            override fun getOldListSize(): Int {
                return items.size
            }

            override fun getNewListSize(): Int {
                return newItems.size
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return items[oldItemPosition] == newItems[newItemPosition]
            }
        }
    }
}