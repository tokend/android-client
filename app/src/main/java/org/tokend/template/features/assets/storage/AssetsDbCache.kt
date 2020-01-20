package org.tokend.template.features.assets.storage

import org.tokend.template.data.model.AssetRecord
import org.tokend.template.data.repository.base.RepositoryCache
import org.tokend.template.extensions.mapSuccessful
import org.tokend.template.features.assets.model.AssetDbEntity

class AssetsDbCache(
        private val dao: AssetsDao
): RepositoryCache<AssetRecord>() {
    override fun isContentSame(first: AssetRecord, second: AssetRecord): Boolean {
        return first.run {
            policy == second.policy
                    && name == second.name
                    && logoUrl == second.logoUrl
                    && description == second.description
                    && externalSystemType == second.externalSystemType
                    && issued == second.issued
                    && available == second.available
                    && maximum == second.maximum
                    && ownerAccountId == second.ownerAccountId
                    && trailingDigits == second.trailingDigits
                    && state == second.state
        }
    }

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