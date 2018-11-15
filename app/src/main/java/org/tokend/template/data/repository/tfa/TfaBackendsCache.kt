package org.tokend.template.data.repository.tfa

import io.reactivex.Single
import org.tokend.sdk.api.tfa.model.TfaFactor
import org.tokend.template.data.repository.base.RepositoryCache

class TfaBackendsCache : RepositoryCache<TfaFactor>() {
    override fun isContentSame(first: TfaFactor, second: TfaFactor): Boolean {
        return first.id == second.id && first.type == second.type
                && first.attributes?.priority == second.attributes?.priority
    }

    override fun getAllFromDb(): Single<List<TfaFactor>> = Single.just(emptyList())

    override fun addToDb(items: List<TfaFactor>) {}

    override fun updateInDb(items: List<TfaFactor>) {}

    override fun deleteFromDb(items: List<TfaFactor>) {}

    override fun clearDb() {}
}