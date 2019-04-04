package org.tokend.template.data.repository.orderbook

import org.tokend.template.data.model.OfferRecord
import org.tokend.template.data.repository.base.RepositoryCache

class OrderBookCache(
        private val isBuy: Boolean
) : RepositoryCache<OfferRecord>() {
    override fun isContentSame(first: OfferRecord, second: OfferRecord): Boolean {
        return false
    }

    override fun sortItems() {
        if (isBuy) {
            mItems.sortByDescending { it.price }
        } else {
            mItems.sortBy { it.price }
        }
    }

    override fun getAllFromDb() = emptyList<OfferRecord>()

    override fun addToDb(items: List<OfferRecord>) {}

    override fun updateInDb(items: List<OfferRecord>) {}

    override fun deleteFromDb(items: List<OfferRecord>) {}

    override fun clearDb() {}
}