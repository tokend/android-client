package org.tokend.template.features.trade.model

import java.io.Serializable
import java.math.BigDecimal

class Order(
        val type: OrderType,
        val amount: BigDecimal,
        val price: BigDecimal,
        val asset: String) : Serializable {

    enum class OrderType {
        BUY,
        SELL
    }
}