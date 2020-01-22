package org.tokend.template.features.balances.storage

import org.tokend.template.data.model.AssetRecord
import org.tokend.template.data.model.BalanceRecord
import org.tokend.template.data.repository.base.RepositoryCache
import org.tokend.template.extensions.equalsArithmetically
import org.tokend.template.extensions.mapSuccessful
import org.tokend.template.features.balances.model.BalanceDbEntity

class BalancesDbCache(
        private val dao: BalancesDao,
        private val assetsCache: RepositoryCache<AssetRecord>
) : RepositoryCache<BalanceRecord>() {
    override fun isContentSame(first: BalanceRecord, second: BalanceRecord): Boolean {
        return first.run {
            id == second.id
                    && available.equalsArithmetically(second.available)
                    && asset == second.asset
                    && conversionAsset == second.conversionAsset
                    && convertedAmount.equalsArithmetically(second.convertedAmount)
                    && conversionPrice.equalsArithmetically(second.conversionPrice)
        }
    }

    override fun getAllFromDb(): List<BalanceRecord> {
        assetsCache.loadFromDb().blockingAwait()
        val assets = assetsCache.items.associateBy(AssetRecord::code)
        return dao.selectAll().mapSuccessful { it.toRecord(assets) }
    }

    override fun addToDb(items: Collection<BalanceRecord>) {
        val assets = items.map(BalanceRecord::asset)
        assetsCache.add(*assets.toTypedArray())
        dao.insert(*items.map(BalanceDbEntity.Companion::fromRecord).toTypedArray())
    }

    override fun updateInDb(items: Collection<BalanceRecord>) {
        val assets = items.map(BalanceRecord::asset)
        assetsCache.update(*assets.toTypedArray())
        dao.update(*items.map(BalanceDbEntity.Companion::fromRecord).toTypedArray())
    }

    override fun deleteFromDb(items: Collection<BalanceRecord>) {
        val assets = items.map(BalanceRecord::asset)
        assetsCache.delete(*assets.toTypedArray())
        dao.delete(*items.map(BalanceDbEntity.Companion::fromRecord).toTypedArray())
    }

    override fun clearDb() =
            dao.deleteAll()
}