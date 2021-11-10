package io.tokend.template.features.trade.orderbook.repository

import io.reactivex.Single
import io.tokend.template.data.storage.repository.SingleItemRepository
import io.tokend.template.logic.providers.ApiProvider
import io.tokend.template.features.trade.orderbook.model.OrderBook
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.v3.orderbook.params.OrderBookParamsV3

class OrderBookRepository
    (
    private val apiProvider: ApiProvider,
    private val baseAsset: String,
    private val quoteAsset: String
) : SingleItemRepository<OrderBook>() {
    override fun getItem(): Single<OrderBook> {
        val api = apiProvider.getApi()

        val params = OrderBookParamsV3.Builder()
            .withMaxEntries(25)
            .withInclude(
                OrderBookParamsV3.Includes.BUY_ENTRIES,
                OrderBookParamsV3.Includes.SELL_ENTRIES,
                OrderBookParamsV3.Includes.BASE_ASSET,
                OrderBookParamsV3.Includes.QUOTE_ASSET
            )
            .build()

        return api
            .v3
            .orderBooks
            .getById(
                baseAssetCode = baseAsset,
                quoteAssetCode = quoteAsset,
                params = params
            )
            .toSingle()
            .map(::OrderBook)
    }
}