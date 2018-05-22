package org.tokend.template.features.trade.repository.pairs

import io.reactivex.Single
import org.tokend.sdk.api.models.AssetPair
import org.tokend.template.base.logic.di.providers.ApiProvider
import org.tokend.template.base.logic.repository.base.SimpleMultipleItemsRepository
import org.tokend.template.extensions.toSingle

class AssetPairsRepository(
        private val apiProvider: ApiProvider
) : SimpleMultipleItemsRepository<AssetPair>() {
    override val itemsCache = AssetPairsCache()

    override fun getItems(): Single<List<AssetPair>> {
        return apiProvider.getApi()
                .getAssetPairs()
                .toSingle()
    }
}