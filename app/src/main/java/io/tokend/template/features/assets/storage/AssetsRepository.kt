package io.tokend.template.features.assets.storage

import com.fasterxml.jackson.databind.ObjectMapper
import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import io.tokend.template.data.storage.repository.MultipleItemsRepository
import io.tokend.template.data.storage.repository.RepositoryCache
import io.tokend.template.logic.providers.ApiProvider
import io.tokend.template.logic.providers.UrlConfigProvider
import io.tokend.template.extensions.mapSuccessful
import io.tokend.template.features.assets.model.AssetRecord
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.base.params.PagingParamsV2
import org.tokend.sdk.api.v3.assets.params.AssetsPageParams
import org.tokend.sdk.utils.SimplePagedResourceLoader

class AssetsRepository(
    private val apiProvider: ApiProvider,
    private val urlConfigProvider: UrlConfigProvider,
    private val mapper: ObjectMapper,
    itemsCache: RepositoryCache<AssetRecord>
) : MultipleItemsRepository<AssetRecord>(itemsCache) {
    override fun getItems(): Single<List<AssetRecord>> {

        val loader = SimplePagedResourceLoader(
            { nextCursor ->
                apiProvider.getApi().v3.assets.get(
                    AssetsPageParams(
                        pagingParams = PagingParamsV2(page = nextCursor)
                    )
                )
            }
        )

        return loader
            .loadAll()
            .toSingle()
            .map { assetResources ->
                assetResources.mapSuccessful {
                    AssetRecord.fromResource(it, urlConfigProvider.getConfig(), mapper)
                }
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
                    .v3
                    .assets
                    .getById(code)
                    .toSingle()
                    .map { AssetRecord.fromResource(it, urlConfigProvider.getConfig(), mapper) }
                    .doOnSuccess {
                        itemsCache.updateOrAdd(it)
                    }
            )
    }
}