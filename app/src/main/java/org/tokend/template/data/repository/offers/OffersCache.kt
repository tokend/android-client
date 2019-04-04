package org.tokend.template.data.repository.offers

import org.tokend.template.data.model.OfferRecord
import org.tokend.template.data.repository.base.RepositoryCache

class OffersCache : RepositoryCache<OfferRecord>() {
    override fun isContentSame(first: OfferRecord, second: OfferRecord): Boolean {
        return false
    }

    override fun sortItems() {
        mItems.sortByDescending { it.date }
    }

    override fun getAllFromDb() = emptyList<OfferRecord>()

    override fun addToDb(items: List<OfferRecord>) {}

    override fun updateInDb(items: List<OfferRecord>) {}

    override fun deleteFromDb(items: List<OfferRecord>) {}

    override fun clearDb() {}
}