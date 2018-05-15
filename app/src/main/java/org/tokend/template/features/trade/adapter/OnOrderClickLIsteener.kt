package org.tokend.template.features.trade.adapter

import org.tokend.template.features.trade.model.Order

interface OnOrderClickLIsteener {
    fun onOrderClick(order: Order)
}