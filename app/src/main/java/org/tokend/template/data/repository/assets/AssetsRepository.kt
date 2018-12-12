package org.tokend.template.data.repository.assets

import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import org.tokend.template.data.repository.base.SimpleMultipleItemsRepository
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.extensions.Asset
import org.tokend.template.extensions.toSingle

class AssetsRepository(
        private val apiProvider: ApiProvider
) : SimpleMultipleItemsRepository<Asset>() {
    override val itemsCache = AssetsRepositoryCache()

    override fun getItems(): Single<List<Asset>> {
        return apiProvider.getApi()
                .assets
                .get()
                .toSingle()
    }

    /**
     * @return single asset info
     */
    fun getSingle(code: String): Single<Asset> {
        return itemsCache.items
                .find { it.code == code }
                .toMaybe()
                .switchIfEmpty(
                        apiProvider.getApi()
                                .assets
                                .getByCode(code)
                                .toSingle()
                                .doOnSuccess { _ ->
                                    itemsCache.items.find { it.code == code }?.also {
                                        itemsCache.updateOrAdd(it)
                                    }
                                }
                )
    }
}