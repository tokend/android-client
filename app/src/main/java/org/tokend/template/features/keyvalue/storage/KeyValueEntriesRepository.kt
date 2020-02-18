package org.tokend.template.features.keyvalue.storage

import io.reactivex.Single
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.base.params.PagingOrder
import org.tokend.sdk.api.base.params.PagingParamsV2
import org.tokend.sdk.utils.SimplePagedResourceLoader
import org.tokend.template.features.keyvalue.model.KeyValueEntryRecord
import org.tokend.template.data.repository.base.RepositoryCache
import org.tokend.template.data.repository.base.MultipleItemsRepository
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.extensions.mapSuccessful

class KeyValueEntriesRepository(
        private val apiProvider: ApiProvider,
        itemsCache: RepositoryCache<KeyValueEntryRecord>
) : MultipleItemsRepository<KeyValueEntryRecord>(itemsCache) {
    private val entriesMap = mutableMapOf<String, KeyValueEntryRecord>()

    override fun getItems(): Single<List<KeyValueEntryRecord>> {
        val loader = SimplePagedResourceLoader({ nextCursor ->
            apiProvider
                    .getApi()
                    .v3
                    .keyValue
                    .get(
                            PagingParamsV2(
                                    order = PagingOrder.ASC,
                                    limit = 20,
                                    page = nextCursor
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

    private fun getItem(key: String): Single<KeyValueEntryRecord> {
        return apiProvider
                .getApi()
                .v3
                .keyValue
                .getById(key)
                .map(KeyValueEntryRecord.Companion::fromResource)
                .toSingle()
                .doOnSuccess { entriesMap[key] = it }
    }

    override fun cacheNewItems(newItems: List<KeyValueEntryRecord>) {
        super.cacheNewItems(newItems)
        entriesMap.clear()
        newItems.associateByTo(entriesMap, KeyValueEntryRecord::key)
    }

    fun getEntry(key: String): KeyValueEntryRecord? {
        return entriesMap[key]
    }

    fun ensureEntries(keys: Collection<String>): Single<Map<String, KeyValueEntryRecord>> {
        return if (entriesMap.keys.containsAll(keys))
            Single.just(entriesMap)
        else
            (if (keys.size == 1)
                getItem(keys.first())
            else
                updateDeferred()
                        .toSingleDefault(true)
                    ).map { entriesMap }
    }
}