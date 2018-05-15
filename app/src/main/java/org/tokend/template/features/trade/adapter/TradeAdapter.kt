package org.tokend.template.features.trade.adapter

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import org.jetbrains.anko.layoutInflater
import org.jetbrains.anko.onClick
import org.tokend.template.R
import org.tokend.template.features.trade.model.Order

class TradeAdapter : RecyclerView.Adapter<TradeViewHolder>() {

    private val buyList = ArrayList<Order>()
    private val sellList = ArrayList<Order>()
    var listener: OnOrderClickLIsteener? = null

    companion object {
        private const val VIEW_ORDER_BUY = 1022
        private const val VIEW_ORDER_SELL = 1023
        private const val VIEW_STUB = 1024
    }

    fun setObjectList(orders: List<Order>) {

        buyList.clear()
        sellList.clear()

        for(order in orders) {
            when(order.type) {
                Order.OrderType.BUY -> buyList.add(order)
                Order.OrderType.SELL -> sellList.add(order)
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TradeViewHolder {
        return when(viewType) {
            VIEW_ORDER_BUY -> {
                val itemView = parent.context.layoutInflater.inflate(R.layout.item_order_buy,
                        parent, false)
                TradeViewHolder.OrderBuyViewHolder(itemView)
            }
            VIEW_ORDER_SELL -> {
                val itemView = parent.context.layoutInflater.inflate(R.layout.item_order_sell,
                        parent, false)
                TradeViewHolder.OrderSellViewHolder(itemView)
            }
            else -> TradeViewHolder.StubOrderViewHolder(View(parent.context))
        }

    }

    override fun getItemCount(): Int = Math.max(buyList.size, sellList.size) * 2

    override fun onBindViewHolder(holder: TradeViewHolder, position: Int) {
        holder.let {
            when(it) {
                is TradeViewHolder.OrderBuyViewHolder -> {
                    val item = getItemAt(position)
                    it.bind(item)
                    it.itemView.onClick({ listener?.onOrderClick(item) })
                }
                is TradeViewHolder.OrderSellViewHolder -> {
                    val item = getItemAt(position)
                    it.bind(item)
                    it.itemView.onClick({ listener?.onOrderClick(item) })
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val translatedPosition = translatePosition(position)

        return if (position % 2 == 0) {
            if (translatedPosition >= buyList.size) {
                VIEW_STUB
            } else {
                VIEW_ORDER_BUY
            }
        } else {
            if (translatedPosition >= sellList.size) {
                VIEW_STUB
            } else {
                VIEW_ORDER_SELL
            }
        }
    }

    private fun translatePosition(position: Int): Int {
        return position / 2
    }

    private fun getItemAt(position: Int): Order {
        val translatedPosition = translatePosition(position)

        return if (position % 2 == 0) {
            buyList[translatedPosition]
        } else {
            sellList[translatedPosition]
        }
    }
}