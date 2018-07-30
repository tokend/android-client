package org.tokend.template.base.logic.repository.tfa

import io.reactivex.Single
import org.tokend.sdk.api.tfa.TfaBackend
import org.tokend.template.base.logic.repository.base.RepositoryCache

class TfaBackendsCache : RepositoryCache<TfaBackend>() {
    override fun isContentSame(first: TfaBackend, second: TfaBackend): Boolean {
        return first.id == second.id && first.type == second.type
                && first.attributes?.priority == second.attributes?.priority
    }

    override fun getAllFromDb(): Single<List<TfaBackend>> = Single.just(emptyList())

    override fun addToDb(items: List<TfaBackend>) {}

    override fun updateInDb(items: List<TfaBackend>) {}

    override fun deleteFromDb(items: List<TfaBackend>) {}

    override fun clearDb() {}
}