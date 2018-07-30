package org.tokend.template.features.trade.repository.order_book

import io.reactivex.Single
import org.tokend.sdk.api.models.Offer
import org.tokend.template.base.logic.di.providers.ApiProvider
import org.tokend.template.base.logic.repository.base.pagination.DataPage
import org.tokend.template.base.logic.repository.base.pagination.PageParams
import org.tokend.template.base.logic.repository.base.pagination.PagedDataRepository
import org.tokend.template.base.logic.repository.base.pagination.PagedRequestParams
import org.tokend.template.extensions.toSingle

class OrderBookRepository
(
        private val apiProvider: ApiProvider,
        private val baseAsset: String,
        private val quoteAsset: String,
        private val isBuy: Boolean
) : PagedDataRepository<Offer, OrderBookRepository.OrderBookRequestParams>() {
    class OrderBookRequestParams(val baseAsset: String,
                                 val quoteAsset: String,
                                 val isBuy: Boolean,
                                 pageParams: PageParams = PageParams()
    ) : PagedRequestParams(pageParams)

    override val itemsCache = OrderBookCache(isBuy)

    override fun getItems(): Single<List<Offer>> = Single.just(emptyList())

    override fun getPage(requestParams: OrderBookRequestParams): Single<DataPage<Offer>> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))

        return signedApi.getOrderBook(
                isBuy = requestParams.isBuy,
                baseAsset = requestParams.baseAsset,
                quoteAsset = requestParams.quoteAsset,
                limit = requestParams.pageParams.limit,
                cursor = requestParams.pageParams.cursor
        )
                .toSingle()
                .map {
                    DataPage.fromPage(it)
                }
    }

    override fun getNextPageRequestParams(): OrderBookRequestParams {
        return OrderBookRequestParams(baseAsset, quoteAsset, isBuy,
                PageParams(cursor = nextCursor, limit = 50))
    }
}