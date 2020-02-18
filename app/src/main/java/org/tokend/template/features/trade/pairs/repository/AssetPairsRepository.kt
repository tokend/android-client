package org.tokend.template.features.trade.pairs.repository

import com.fasterxml.jackson.databind.ObjectMapper
import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.base.params.PagingParamsV2
import org.tokend.sdk.api.v3.assetpairs.params.AssetPairParams
import org.tokend.sdk.api.v3.assetpairs.params.AssetPairsPageParams
import org.tokend.sdk.utils.SimplePagedResourceLoader
import org.tokend.template.features.trade.pairs.model.AssetPairRecord
import org.tokend.template.data.repository.base.RepositoryCache
import org.tokend.template.data.repository.base.MultipleItemsRepository
import org.tokend.template.features.trade.pairs.logic.AmountConverter
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.UrlConfigProvider
import java.math.BigDecimal
import java.math.MathContext

class AssetPairsRepository(
        private val apiProvider: ApiProvider,
        private val urlConfigProvider: UrlConfigProvider,
        private val objectMapper: ObjectMapper,
        private val defaultConversionAssetCode: String?,
        itemsCache: RepositoryCache<AssetPairRecord>
) : MultipleItemsRepository<AssetPairRecord>(itemsCache), AmountConverter {

    override fun getItems(): Single<List<AssetPairRecord>> {
        val urlConfig = urlConfigProvider.getConfig()

        val loader = SimplePagedResourceLoader(
                { nextCursor ->
                    apiProvider.getApi().v3.assetPairs.get(
                            AssetPairsPageParams(
                                    include = listOf(
                                            AssetPairParams.Includes.BASE_ASSET,
                                            AssetPairParams.Includes.QUOTE_ASSET
                                    ),
                                    pagingParams = PagingParamsV2(
                                            limit = PAGE_LIMIT,
                                            page = nextCursor
                                    )
                            )
                    )
                }
        )

        return loader
                .loadAll()
                .toSingle()
                .map { pairsList ->
                    pairsList.map { AssetPairRecord.fromResource(it, urlConfig, objectMapper) }
                }
    }

    /***
     * @return single asset pair info
     */
    fun getSingle(code: String): Single<AssetPairRecord> {
        val urlConfig = urlConfigProvider.getConfig()

        return itemsCache.items
                .find { it.id == code }
                .toMaybe()
                .switchIfEmpty(
                        apiProvider.getApi()
                                .v3
                                .assetPairs
                                .getById(code)
                                .toSingle()
                                .map { AssetPairRecord.fromResource(it, urlConfig, objectMapper) }
                                .doOnSuccess {
                                    itemsCache.updateOrAdd(it)
                                }
                )
    }

    fun getSingle(base: String, quote: String): Single<AssetPairRecord> {
        val code = "$base:$quote"
        return getSingle(code)
    }

    override fun getRate(sourceAsset: String, destAsset: String): BigDecimal? {
        if (sourceAsset == destAsset) {
            return BigDecimal.ONE
        }

        val pairs = itemsCache.items

        val mainPairPrice = pairs.find {
            it.quote.code == destAsset && it.base.code == sourceAsset
        }?.price

        val quotePair = pairs.find {
            it.quote.code == sourceAsset && it.base.code == destAsset
        }
        val quotePairPrice =
                if (quotePair?.price != null)
                    BigDecimal.ONE.divide(quotePair.price, MathContext.DECIMAL128)
                else
                    null
        val throughDefaultAssetPrice =
                if (defaultConversionAssetCode == null
                        || destAsset == defaultConversionAssetCode
                        || sourceAsset == defaultConversionAssetCode)
                    null
                else
                    getRateOrOne(sourceAsset, defaultConversionAssetCode)
                            .multiply(getRateOrOne(defaultConversionAssetCode, destAsset))

        return mainPairPrice ?: quotePairPrice ?: throughDefaultAssetPrice
    }

    override fun getRateOrOne(sourceAsset: String, destAsset: String): BigDecimal {
        return getRate(sourceAsset, destAsset) ?: BigDecimal.ONE
    }

    override fun convert(amount: BigDecimal, sourceAsset: String, destAsset: String): BigDecimal? {
        val rate = getRate(sourceAsset, destAsset)
        return if (rate != null)
            amount.multiply(rate, MathContext.DECIMAL128)
        else null
    }

    companion object {
        private val PAGE_LIMIT = 20
    }
}