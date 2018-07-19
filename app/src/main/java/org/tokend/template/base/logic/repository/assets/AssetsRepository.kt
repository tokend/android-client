package org.tokend.template.base.logic.repository.assets

import io.reactivex.Single
import org.tokend.template.base.logic.di.providers.ApiProvider
import org.tokend.template.base.logic.repository.base.SimpleMultipleItemsRepository
import org.tokend.template.extensions.Asset
import org.tokend.template.extensions.toSingle

class AssetsRepository(
        private val apiProvider: ApiProvider
) : SimpleMultipleItemsRepository<Asset>() {
    override val itemsCache = AssetsRepositoryCache()

    override fun getItems(): Single<List<Asset>> {
        return apiProvider.getApi()
                .getAssetsDetails()
                .toSingle()
    }
}