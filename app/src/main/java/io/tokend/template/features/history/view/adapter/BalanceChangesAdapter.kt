package io.tokend.template.features.history.view.adapter

import android.view.View
import android.view.ViewGroup
import io.tokend.template.R
import io.tokend.template.extensions.layoutInflater
import io.tokend.template.view.adapter.base.BaseViewHolder
import io.tokend.template.view.adapter.base.PaginationRecyclerAdapter
import io.tokend.template.view.history.HistoryItemView
import io.tokend.template.view.util.formatter.AmountFormatter
import java.text.DateFormat

class BalanceChangesAdapter(
    private val amountFormatter: AmountFormatter,
    private val dateFormat: DateFormat
) :
    PaginationRecyclerAdapter<BalanceChangeListItem, BaseViewHolder<BalanceChangeListItem>>() {

    class FooterViewHolder(v: View) : BaseViewHolder<BalanceChangeListItem>(v) {
        override fun bind(item: BalanceChangeListItem) {}
    }

    override fun createItemViewHolder(parent: ViewGroup): BalanceChangeItemViewHolder {
        val view = parent.context
            .layoutInflater.inflate(R.layout.list_item_tx, parent, false)
        return BalanceChangeItemViewHolder(view, amountFormatter, dateFormat)
    }

    override fun createFooterViewHolder(parent: ViewGroup): BaseViewHolder<BalanceChangeListItem> {
        val view = parent.context
            .layoutInflater.inflate(R.layout.list_item_loading_footer, parent, false)
        return FooterViewHolder(view)
    }

    override fun bindItemViewHolder(holder: BaseViewHolder<BalanceChangeListItem>, position: Int) {
        super.bindItemViewHolder(holder, position)
        val isLastInSection =
            position == itemCount - (if (needLoadingFooter) 2 else 1)
        (holder as? HistoryItemView)?.dividerIsVisible = !isLastInSection
    }

    override fun bindFooterViewHolder(holder: BaseViewHolder<BalanceChangeListItem>) {}
}