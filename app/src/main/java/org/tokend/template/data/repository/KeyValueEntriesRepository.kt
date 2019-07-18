package org.tokend.template.data.repository

import io.reactivex.Single
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.base.params.PagingOrder
import org.tokend.sdk.api.base.params.PagingParamsV2
import org.tokend.sdk.utils.SimplePagedResourceLoader
import org.tokend.template.data.model.KeyValueEntryRecord
import org.tokend.template.data.repository.base.RepositoryCache
import org.tokend.template.data.repository.base.SimpleMultipleItemsRepository
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.extensions.mapSuccessful

class KeyValueEntriesRepository(
        private val apiProvider: ApiProvider,
        itemsCache: RepositoryCache<KeyValueEntryRecord>
) : SimpleMultipleItemsRepository<KeyValueEntryRecord>(itemsCache) {
    override fun getItems(): Single<List<KeyValueEntryRecord>> {
        val loader = SimplePagedResourceLoader({ nextCursor ->
            apiProvider
                    .getApi()
                    .v3
                    .keyValue
                    .get(
                            PagingParamsV2(
                                    order = PagingOrder.ASC,
                                    limit = 20
                            )
                    )
        }, distinct = false)

        return loader
                .loadAll()
                .toSingle()
                .map { entries ->
                    entries.mapSuccessful(KeyValueEntryRecord.Companion::fromResource)
                }
    }
}