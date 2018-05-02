package org.tokend.template.base.view.adapter.history

import android.view.View
import android.view.ViewGroup
import org.jetbrains.anko.layoutInflater
import org.tokend.template.R
import org.tokend.template.base.view.adapter.base.BaseViewHolder
import org.tokend.template.base.view.adapter.base.PaginationRecyclerAdapter

class TxHistoryAdapter : PaginationRecyclerAdapter<TxHistoryItem, BaseViewHolder<TxHistoryItem>>() {
    class FooterViewHolder(v: View) : BaseViewHolder<TxHistoryItem>(v) {
        override fun bind(item: TxHistoryItem) {}
    }

    override fun createItemViewHolder(parent: ViewGroup): BaseViewHolder<TxHistoryItem> {
        val view = parent.context
                .layoutInflater.inflate(R.layout.list_item_tx, parent, false)
        return TxHistoryItemViewHolder(view)
    }

    override fun createFooterViewHolder(parent: ViewGroup): BaseViewHolder<TxHistoryItem> {
        val view = parent.context
                .layoutInflater.inflate(R.layout.list_item_loading_footer, parent, false)
        return FooterViewHolder(view)
    }

    override fun bindItemViewHolder(holder: BaseViewHolder<TxHistoryItem>, position: Int) {
        super.bindItemViewHolder(holder, position)
        val isLastInSection =
                position == itemCount - (if (needLoadingFooter) 2 else 1)
        (holder as? TxHistoryItemViewHolder)?.dividerIsVisible = !isLastInSection
    }

    override fun bindFooterViewHolder(holder: BaseViewHolder<TxHistoryItem>) {}
}