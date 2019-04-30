package org.tokend.template.data.repository.orderbook

import io.reactivex.Single
import org.tokend.sdk.api.base.model.DataPage
import org.tokend.sdk.api.base.params.PagingOrder
import org.tokend.template.features.offers.model.OfferRecord
import org.tokend.template.data.repository.base.RepositoryCache
import org.tokend.template.data.repository.base.pagination.PagedDataRepository
import org.tokend.template.di.providers.ApiProvider
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.base.params.PagingParamsV2
import org.tokend.sdk.api.v3.orderbook.params.OrderBookPageParams

class OrderBookRepository
(
        private val apiProvider: ApiProvider,
        private val baseAsset: String,
        private val quoteAsset: String,
        private val isBuy: Boolean,
        itemsCache: RepositoryCache<OfferRecord>
) : PagedDataRepository<OfferRecord>(itemsCache) {
    override fun getPage(nextCursor: String?): Single<DataPage<OfferRecord>> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))

        val requestParams = OrderBookPageParams(
                baseAsset = baseAsset,
                quoteAsset = quoteAsset,
                isBuy = isBuy,
                pagingParams = PagingParamsV2(
                        order = PagingOrder.DESC,
                        limit = 50,
                        page = nextCursor
                )
        )

        return signedApi.v3.orderBooks
                .getById(params = requestParams)
                .toSingle()
                .map {
                    DataPage(
                            it.nextCursor,
                            it.items.map { orderBookEntry ->
                                OfferRecord.fromResource(orderBookEntry)
                            },
                            it.isLast
                    )
                }
    }
}