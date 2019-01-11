package org.tokend.template.data.repository.assets

import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import org.tokend.template.data.repository.base.SimpleMultipleItemsRepository
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.UrlConfigProvider
import org.tokend.template.extensions.toSingle
import org.tokend.template.features.assets.model.AssetRecord

class AssetsRepository(
        private val apiProvider: ApiProvider,
        private val urlConfigProvider: UrlConfigProvider
) : SimpleMultipleItemsRepository<AssetRecord>() {
    override val itemsCache = AssetsRepositoryCache()

    override fun getItems(): Single<List<AssetRecord>> {
        return apiProvider.getApi()
                .assets
                .get()
                .toSingle()
                .map { simpleAssets ->
                    simpleAssets.map { AssetRecord(it, urlConfigProvider.getConfig()) }
                }
    }

    /**
     * @return single asset info
     */
    fun getSingle(code: String): Single<AssetRecord> {
        return itemsCache.items
                .find { it.code == code }
                .toMaybe()
                .switchIfEmpty(
                        apiProvider.getApi()
                                .assets
                                .getByCode(code)
                                .toSingle()
                                .map { AssetRecord(it, urlConfigProvider.getConfig()) }
                                .doOnSuccess { _ ->
                                    itemsCache.items.find { it.code == code }?.also {
                                        itemsCache.updateOrAdd(it)
                                    }
                                }
                )
    }
}