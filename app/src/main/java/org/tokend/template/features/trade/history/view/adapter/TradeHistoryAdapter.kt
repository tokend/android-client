package org.tokend.template.features.trade.history.view.adapter

import android.view.View
import android.view.ViewGroup
import org.tokend.template.R
import org.tokend.template.extensions.layoutInflater
import org.tokend.template.features.trade.history.model.TradeHistoryRecord
import org.tokend.template.view.adapter.base.BaseViewHolder
import org.tokend.template.view.adapter.base.PaginationRecyclerAdapter
import org.tokend.template.view.util.formatter.AmountFormatter
import java.text.DateFormat

class TradeHistoryAdapter(
        private val amountFormatter: AmountFormatter,
        private val dateFormat: DateFormat
) : PaginationRecyclerAdapter<TradeHistoryRecord, BaseViewHolder<TradeHistoryRecord>>() {

    private class FooterViewHolder(view: View) : BaseViewHolder<TradeHistoryRecord>(view) {
        override fun bind(item: TradeHistoryRecord) {}
    }

    override fun createFooterViewHolder(parent: ViewGroup): BaseViewHolder<TradeHistoryRecord> {
        val view = parent.context
                .layoutInflater.inflate(R.layout.list_item_loading_footer, parent, false)
        return FooterViewHolder(view)
    }

    override fun createItemViewHolder(parent: ViewGroup): TradeHistoryItemViewHolder {
        val view = parent.context
                .layoutInflater.inflate(R.layout.list_item_trade_history, parent, false)
        return TradeHistoryItemViewHolder(view, amountFormatter, dateFormat)
    }

    override fun bindFooterViewHolder(holder: BaseViewHolder<TradeHistoryRecord>) {}

}