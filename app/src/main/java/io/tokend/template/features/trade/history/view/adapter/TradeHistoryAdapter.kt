package io.tokend.template.features.trade.history.view.adapter

import android.view.View
import android.view.ViewGroup
import io.tokend.template.R
import io.tokend.template.extensions.layoutInflater
import io.tokend.template.features.trade.history.model.TradeHistoryRecord
import io.tokend.template.view.adapter.base.BaseViewHolder
import io.tokend.template.view.adapter.base.PaginationRecyclerAdapter
import io.tokend.template.view.util.formatter.AmountFormatter
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