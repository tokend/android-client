package org.tokend.template.features.offers.repository

import org.tokend.template.data.repository.base.RepositoryCache
import org.tokend.template.features.offers.model.OfferRecord

class OffersCache : RepositoryCache<OfferRecord>() {
    override fun isContentSame(first: OfferRecord, second: OfferRecord): Boolean {
        return false
    }

    override fun sortItems() {
        mItems.sortByDescending { it.date }
    }

    override fun getAllFromDb() = emptyList<OfferRecord>()

    override fun addToDb(items: Collection<OfferRecord>) {}

    override fun updateInDb(items: Collection<OfferRecord>) {}

    override fun deleteFromDb(items: Collection<OfferRecord>) {}

    override fun clearDb() {}
}