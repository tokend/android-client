package org.tokend.template.data.repository

import io.reactivex.Single
import org.jetbrains.anko.collections.forEachReversedByIndex
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.base.model.DataPage
import org.tokend.sdk.api.base.params.PagingOrder
import org.tokend.sdk.api.base.params.PagingParams
import org.tokend.sdk.api.trades.params.OrdersParams
import org.tokend.template.data.model.TradeHistoryRecord
import org.tokend.template.data.repository.base.RepositoryCache
import org.tokend.template.data.repository.base.pagination.PagedDataRepository
import org.tokend.template.di.providers.ApiProvider
import java.math.BigDecimal

class TradeHistoryRepository(
        private val baseAsset: String,
        private val quoteAsset: String,
        private val apiProvider: ApiProvider,
        itemsCache: RepositoryCache<TradeHistoryRecord>
) : PagedDataRepository<TradeHistoryRecord>(itemsCache) {

    override fun getPage(nextCursor: String?): Single<DataPage<TradeHistoryRecord>> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))

        val requestParams = OrdersParams(
                baseAsset = baseAsset,
                quoteAsset = quoteAsset,
                orderBookId = 0L,
                pagingParams = PagingParams(
                        order = PagingOrder.DESC,
                        limit = LIMIT,
                        cursor = nextCursor
                )
        )

        return signedApi.trades.getMatchedOrders(requestParams)
                .toSingle()
                .map {
                    val items = it.items.map { matchedOrder ->
                        TradeHistoryRecord.fromMatchedOrder(matchedOrder)
                    }

                    DataPage(
                            it.nextCursor,
                            items,
                            it.isLast
                    )
                }
    }

    override fun cacheNewItems(newItems: List<TradeHistoryRecord>) {
        val allItems = itemsCache
                .items
                .toMutableList()
                .also { it.addAll(newItems) }

        var currentTrendIsPositive = true
        var previousPrice = BigDecimal.ZERO

        allItems.forEachReversedByIndex {
            it.hasPositiveTrend =
                    when {
                        it.price > previousPrice -> true
                        it.price < previousPrice -> false
                        else -> currentTrendIsPositive
                    }
            previousPrice = it.price
            currentTrendIsPositive = it.hasPositiveTrend
        }

        itemsCache.transform(allItems)
    }

    companion object {
        const val LIMIT = 30
    }
}