package org.tokend.template.data.repository.orderbook

import io.reactivex.Single
import org.tokend.sdk.api.base.model.DataPage
import org.tokend.sdk.api.base.params.PagingOrder
import org.tokend.sdk.api.base.params.PagingParams
import org.tokend.sdk.api.trades.model.Offer
import org.tokend.sdk.api.trades.params.OrderBookParams
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.data.repository.base.pagination.PagedDataRepository
import org.tokend.template.extensions.toSingle

class OrderBookRepository
(
        private val apiProvider: ApiProvider,
        private val baseAsset: String,
        private val quoteAsset: String,
        private val isBuy: Boolean
) : PagedDataRepository<Offer, OrderBookParams>() {
    override val itemsCache = OrderBookCache(isBuy)

    override fun getItems(): Single<List<Offer>> = Single.just(emptyList())

    override fun getPage(requestParams: OrderBookParams): Single<DataPage<Offer>> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))

        return signedApi.trades
                .getOrderBook(requestParams)
                .toSingle()
    }

    override fun getNextPageRequestParams(): OrderBookParams {
        return OrderBookParams(
                baseAsset = baseAsset,
                quoteAsset = quoteAsset,
                isBuy = isBuy,
                pagingParams = PagingParams(
                        order = PagingOrder.DESC,
                        limit = 50,
                        cursor = nextCursor
                )
        )
    }
}