package org.tokend.template.features.assets.storage

import org.tokend.template.features.assets.model.AssetRecord
import org.tokend.template.data.repository.base.RepositoryCache
import org.tokend.template.extensions.mapSuccessful
import org.tokend.template.features.assets.model.AssetDbEntity

class AssetsDbCache(
        private val dao: AssetsDao
) : RepositoryCache<AssetRecord>() {
    override fun isContentSame(first: AssetRecord, second: AssetRecord): Boolean =
            first.contentEquals(second)

    override fun getAllFromDb(): List<AssetRecord> =
            dao.selectAll().mapSuccessful(AssetDbEntity::toRecord)

    override fun addToDb(items: Collection<AssetRecord>) =
            dao.insert(*items.map(AssetDbEntity.Companion::fromRecord).toTypedArray())

    override fun updateInDb(items: Collection<AssetRecord>) =
            dao.update(*items.map(AssetDbEntity.Companion::fromRecord).toTypedArray())

    override fun deleteFromDb(items: Collection<AssetRecord>) =
            dao.delete(*items.map(AssetDbEntity.Companion::fromRecord).toTypedArray())

    override fun clearDb() =
            dao.deleteAll()
}