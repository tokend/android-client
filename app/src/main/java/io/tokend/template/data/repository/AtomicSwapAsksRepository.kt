package io.tokend.template.data.repository

import io.reactivex.Single
import io.tokend.template.data.model.AtomicSwapAskRecord
import io.tokend.template.data.storage.repository.MultipleItemsRepository
import io.tokend.template.data.storage.repository.RepositoryCache
import io.tokend.template.logic.providers.ApiProvider
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.base.params.PagingOrder
import org.tokend.sdk.api.base.params.PagingParamsV2
import org.tokend.sdk.api.v3.atomicswap.params.AtomicSwapAskParams
import org.tokend.sdk.api.v3.atomicswap.params.AtomicSwapAsksPageParams
import org.tokend.sdk.utils.SimplePagedResourceLoader

class AtomicSwapAsksRepository(
    private val apiProvider: ApiProvider,
    private val asset: String,
    itemsCache: RepositoryCache<AtomicSwapAskRecord>
) : MultipleItemsRepository<AtomicSwapAskRecord>(itemsCache) {

    override fun getItems(): Single<List<AtomicSwapAskRecord>> {
        val signedApi = apiProvider.getSignedApi()

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