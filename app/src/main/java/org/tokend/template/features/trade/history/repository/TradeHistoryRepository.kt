package org.tokend.template.features.trade.history.repository

import io.reactivex.Single
import org.jetbrains.anko.collections.forEachReversedByIndex
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.base.model.DataPage
import org.tokend.sdk.api.base.params.PagingOrder
import org.tokend.sdk.api.base.params.PagingParamsV2
import org.tokend.sdk.api.v3.orderbook.params.MatchesPageParams
import org.tokend.template.data.repository.base.pagination.PagedDataRepository
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.features.trade.history.model.TradeHistoryRecord
import java.math.BigDecimal

class TradeHistoryRepository(
        private val baseAsset: String,
        private val quoteAsset: String,
        private val apiProvider: ApiProvider
) : PagedDataRepository<TradeHistoryRecord>(PagingOrder.DESC, null) {
    override val pageLimit: Int = LIMIT

    override fun getRemotePage(nextCursor: Long?,
                               requiredOrder: PagingOrder): Single<DataPage<TradeHistoryRecord>> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))

        val requestParams = MatchesPageParams(
                baseAsset = baseAsset,
                quoteAsset = quoteAsset,
                orderBookId = "0",
                pagingParams = PagingParamsV2(
                        order = requiredOrder,
                        limit = LIMIT,
                        page = nextCursor?.toString()
                )
        )

        return signedApi.v3.orderBooks.getMatches(requestParams)
                .toSingle()
                .map { page ->
                    page.mapItems(::TradeHistoryRecord)
                }
    }

    override fun onNewPage(page: DataPage<TradeHistoryRecord>) {
        val allItems = mItems + page.items

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

        super.onNewPage(page)
    }

    companion object {
        const val LIMIT = 30
    }
}