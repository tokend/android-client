package org.tokend.template.features.trade.history.repository

import io.reactivex.Single
import org.jetbrains.anko.collections.forEachReversedByIndex
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.base.model.DataPage
import org.tokend.sdk.api.base.params.PagingOrder
import org.tokend.sdk.api.base.params.PagingParamsV2
import org.tokend.sdk.api.v3.orderbook.params.MatchesPageParams
import org.tokend.template.data.storage.repository.pagination.SimplePagedDataRepository
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.features.trade.history.model.TradeHistoryRecord
import java.math.BigDecimal

class TradeHistoryRepository(
        private val baseAsset: String,
        private val quoteAsset: String,
        private val apiProvider: ApiProvider,
) : SimplePagedDataRepository<TradeHistoryRecord>(
        pagingOrder = PagingOrder.DESC,
        pageLimit = 30
) {
    override fun getPage(
            limit: Int,
            page: String?,
            order: PagingOrder,
    ): Single<DataPage<TradeHistoryRecord>> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))

        val requestParams = MatchesPageParams(
                baseAsset = baseAsset,
                quoteAsset = quoteAsset,
                orderBookId = "0",
                pagingParams = PagingParamsV2(
                        order = order,
                        limit = limit,
                        page = page
                )
        )

        return signedApi.v3.orderBooks.getMatches(requestParams)
                .toSingle()
                .map { dataPage ->
                    dataPage.mapItems(::TradeHistoryRecord)
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
}