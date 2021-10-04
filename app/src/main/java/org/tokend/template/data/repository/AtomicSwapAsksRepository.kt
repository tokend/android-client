package org.tokend.template.data.repository

import io.reactivex.Single
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.base.params.PagingOrder
import org.tokend.sdk.api.base.params.PagingParamsV2
import org.tokend.sdk.api.v3.atomicswap.params.AtomicSwapAskParams
import org.tokend.sdk.api.v3.atomicswap.params.AtomicSwapAsksPageParams
import org.tokend.sdk.utils.SimplePagedResourceLoader
import org.tokend.template.data.model.AtomicSwapAskRecord
import org.tokend.template.data.storage.repository.MultipleItemsRepository
import org.tokend.template.data.storage.repository.RepositoryCache
import org.tokend.template.di.providers.ApiProvider

class AtomicSwapAsksRepository(
        private val apiProvider: ApiProvider,
        private val asset: String,
        itemsCache: RepositoryCache<AtomicSwapAskRecord>
) : MultipleItemsRepository<AtomicSwapAskRecord>(itemsCache) {

    override fun getItems(): Single<List<AtomicSwapAskRecord>> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))

        val loader = SimplePagedResourceLoader({ nextCursor ->
            signedApi.v3.atomicSwaps.getAtomicSwapAsks(
                    AtomicSwapAsksPageParams(
                            baseAsset = asset,
                            include = listOf(
                                    AtomicSwapAskParams.Includes.BASE_BALANCE,
                                    AtomicSwapAskParams.Includes.QUOTE_ASSETS,
                                    AtomicSwapAskParams.Includes.BASE_ASSET
                            ),
                            pagingParams = PagingParamsV2(
                                    page = nextCursor,
                                    order = PagingOrder.DESC
                            )
                    )
            )
        }, distinct = true)

        return loader.loadAll()
                .toSingle()
                .map {
                    it.map { AtomicSwapAskRecord(it) }
                }
    }
}