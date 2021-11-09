package io.tokend.template.features.offers.view

import android.view.View
import android.view.ViewGroup
import io.tokend.template.R
import io.tokend.template.extensions.layoutInflater
import io.tokend.template.view.adapter.base.BaseViewHolder
import io.tokend.template.view.adapter.base.PaginationRecyclerAdapter
import io.tokend.template.view.history.HistoryItemView
import io.tokend.template.view.util.formatter.AmountFormatter
import java.text.DateFormat

class PendingOffersAdapter(
    private val amountFormatter: AmountFormatter,
    private val dateFormat: DateFormat
) : PaginationRecyclerAdapter<PendingOfferListItem, BaseViewHolder<PendingOfferListItem>>() {
    private class FooterViewHolder(view: View) : BaseViewHolder<PendingOfferListItem>(view) {
        override fun bind(item: PendingOfferListItem) {}
    }

    override fun createFooterViewHolder(parent: ViewGroup): BaseViewHolder<PendingOfferListItem> {
        val view = parent.context
            .layoutInflater.inflate(R.layout.list_item_loading_footer, parent, false)
        return FooterViewHolder(view)
    }

    override fun createItemViewHolder(parent: ViewGroup): PendingOfferItemViewHolder {
        val view = parent.context
            .layoutInflater.inflate(R.layout.list_item_tx, parent, false)
        return PendingOfferItemViewHolder(view, amountFormatter, dateFormat)
    }

    override fun bindItemViewHolder(holder: BaseViewHolder<PendingOfferListItem>, position: Int) {
        super.bindItemViewHolder(holder, position)
        val isLastInSection =
            position == itemCount - (if (needLoadingFooter) 2 else 1)
        (holder as? HistoryItemView)?.dividerIsVisible = !isLastInSection
    }

    override fun bindFooterViewHolder(holder: BaseViewHolder<PendingOfferListItem>) {}
}