package org.tokend.template.data.repository.offers

import io.reactivex.Single
import org.tokend.sdk.api.trades.model.Offer
import org.tokend.template.data.repository.base.RepositoryCache

class OffersCache : RepositoryCache<Offer>() {
    override fun isContentSame(first: Offer, second: Offer): Boolean {
        return first == second
    }

    override fun sortItems() {
        mItems.sortByDescending { it.date }
    }

    override fun getAllFromDb(): Single<List<Offer>> = Single.just(emptyList())

    override fun addToDb(items: List<Offer>) {

    }

    override fun updateInDb(items: List<Offer>) {

    }

    override fun deleteFromDb(items: List<Offer>) {

    }

    override fun clearDb() {

    }
}