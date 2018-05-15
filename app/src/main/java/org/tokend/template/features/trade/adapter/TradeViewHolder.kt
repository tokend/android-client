package org.tokend.template.features.trade.adapter

import android.support.v7.widget.RecyclerView
import android.view.View
import kotlinx.android.synthetic.main.item_order_buy.view.*
import org.tokend.template.base.view.util.AmountFormatter
import org.tokend.template.features.trade.model.Order

sealed class TradeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    class OrderBuyViewHolder(itemView: View) : TradeViewHolder(itemView) {
        fun bind(order: Order) {
            itemView.price_text_view.text = AmountFormatter.formatAssetAmount(order.price,
                    minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS)

            itemView.amount_text_view.text = AmountFormatter.formatAssetAmount(order.amount,
                    minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS)
        }
    }

    class OrderSellViewHolder(itemView: View) : TradeViewHolder(itemView) {
        fun bind(order: Order) {
            itemView.price_text_view.text = AmountFormatter.formatAssetAmount(order.price,
                    minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS)

            itemView.amount_text_view.text = AmountFormatter.formatAssetAmount(order.amount,
                    minDecimalDigits = AmountFormatter.ASSET_DECIMAL_DIGITS)
        }
    }

    class StubOrderViewHolder(itemView: View) : TradeViewHolder(itemView)
}