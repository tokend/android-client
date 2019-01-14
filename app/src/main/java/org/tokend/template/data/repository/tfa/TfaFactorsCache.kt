package org.tokend.template.data.repository.tfa

import io.reactivex.Single
import org.tokend.template.data.repository.base.RepositoryCache
import org.tokend.template.features.tfa.model.TfaFactorRecord

class TfaFactorsCache : RepositoryCache<TfaFactorRecord>() {
    override fun isContentSame(first: TfaFactorRecord, second: TfaFactorRecord): Boolean {
        return false
    }

    override fun getAllFromDb(): Single<List<TfaFactorRecord>> = Single.just(emptyList())

    override fun addToDb(items: List<TfaFactorRecord>) {}

    override fun updateInDb(items: List<TfaFactorRecord>) {}

    override fun deleteFromDb(items: List<TfaFactorRecord>) {}

    override fun clearDb() {}
}