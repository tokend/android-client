package io.tokend.template.features.balances.storage

import io.tokend.template.data.storage.repository.RepositoryCache
import io.tokend.template.extensions.mapSuccessful
import io.tokend.template.features.assets.model.AssetRecord
import io.tokend.template.features.balances.model.BalanceDbEntity
import io.tokend.template.features.balances.model.BalanceRecord

class BalancesDbCache(
    private val dao: BalancesDao,
    private val assetsCache: RepositoryCache<AssetRecord>
) : RepositoryCache<BalanceRecord>() {
    override fun isContentSame(first: BalanceRecord, second: BalanceRecord): Boolean =
        first.contentEquals(second)

    override fun getAllFromDb(): List<BalanceRecord> {
        assetsCache.loadFromDb().blockingAwait()
        val assets = assetsCache.items.associateBy(AssetRecord::code)
        return dao.selectAll().mapSuccessful { it.toRecord(assets) }
    }

    override fun addToDb(items: Collection<BalanceRecord>) {
        val assets = items.map(BalanceRecord::asset).distinct()
        assetsCache.add(*assets.toTypedArray())
        dao.insert(*items.map(BalanceDbEntity.Companion::fromRecord).toTypedArray())
    }

    override fun updateInDb(items: Collection<BalanceRecord>) {
        val assets = items.map(BalanceRecord::asset).distinct()
        assetsCache.update(*assets.toTypedArray())
        dao.update(*items.map(BalanceDbEntity.Companion::fromRecord).toTypedArray())
    }

    override fun deleteFromDb(items: Collection<BalanceRecord>) {
        val assets = items.map(BalanceRecord::asset).distinct()
        assetsCache.delete(*assets.toTypedArray())
        dao.delete(*items.map(BalanceDbEntity.Companion::fromRecord).toTypedArray())
    }

    override fun clearDb() =
        dao.deleteAll()
}