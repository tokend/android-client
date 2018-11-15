package org.tokend.template.data.repository.orderbook

import io.reactivex.Single
import org.tokend.sdk.api.trades.model.Offer
import org.tokend.template.data.repository.base.RepositoryCache

class OrderBookCache(
        private val isBuy: Boolean
): RepositoryCache<Offer>() {
    override fun isContentSame(first: Offer, second: Offer): Boolean {
        return first == second
    }

    override fun sortItems() {
        if (isBuy) {
            mItems.sortByDescending { it.price }
        } else {
            mItems.sortBy { it.price }
        }
    }

    override fun getAllFromDb(): Single<List<Offer>> = Single.just(emptyList())

    override fun addToDb(items: List<Offer>) {}

    override fun updateInDb(items: List<Offer>) {}

    override fun deleteFromDb(items: List<Offer>) {}

    override fun clearDb() {}
}