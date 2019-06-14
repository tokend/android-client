package org.tokend.template.data.repository

import io.reactivex.Single
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.base.params.PagingParamsV2
import org.tokend.sdk.api.v3.atomicswap.params.AtomicSwapAskParams
import org.tokend.sdk.api.v3.atomicswap.params.AtomicSwapAsksPageParams
import org.tokend.sdk.utils.SimplePagedResourceLoader
import org.tokend.template.data.model.AtomicSwapAskRecord
import org.tokend.template.data.model.SimpleAsset
import org.tokend.template.data.repository.base.RepositoryCache
import org.tokend.template.data.repository.base.SimpleMultipleItemsRepository
import org.tokend.template.di.providers.ApiProvider
import java.math.BigDecimal

class AtomicSwapRequestsRepository(
        private val apiProvider: ApiProvider,
        private val asset: String,
        itemsCache: RepositoryCache<AtomicSwapAskRecord>
) : SimpleMultipleItemsRepository<AtomicSwapAskRecord>(itemsCache) {

    override fun getItems(): Single<List<AtomicSwapAskRecord>> {
        return Single.just(
                listOf(
                        AtomicSwapAskRecord(
                                "1",
                                SimpleAsset("MAA"),
                                BigDecimal.TEN,
                                false,
                                listOf(
                                        AtomicSwapAskRecord.QuoteAsset(
                                                "BTC",
                                                6,
                                                BigDecimal("0.015")
                                        ),
                                        AtomicSwapAskRecord.QuoteAsset(
                                                "ETH",
                                                6,
                                                BigDecimal("0.47")
                                        )
                                )
                        )
                )
        )
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))

        val loader = SimplePagedResourceLoader({ nextCursor ->
            signedApi.v3.atomicSwaps.getAtomicSwapAsks(
                    AtomicSwapAsksPageParams(
                            baseAsset = asset,
                            include = listOf(
                                    AtomicSwapAskParams.Includes.BASE_BALANCE,
                                    AtomicSwapAskParams.Includes.QUOTE_ASSETS
                            ),
                            pagingParams = PagingParamsV2(page = nextCursor)
                    )
            )
        })

        return loader.loadAll()
                .toSingle()
                .map {
                    it.map { AtomicSwapAskRecord(it) }
                }
    }
}