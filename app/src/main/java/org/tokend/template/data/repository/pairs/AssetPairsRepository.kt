package org.tokend.template.data.repository.pairs

import io.reactivex.Single
import org.tokend.template.data.model.AssetPairRecord
import org.tokend.template.data.repository.base.SimpleMultipleItemsRepository
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.extensions.toSingle
import java.math.BigDecimal
import java.math.MathContext

class AssetPairsRepository(
        private val apiProvider: ApiProvider
) : SimpleMultipleItemsRepository<AssetPairRecord>(), AmountConverter {
    override val itemsCache = AssetPairsCache()

    override fun getItems(): Single<List<AssetPairRecord>> {
        return apiProvider.getApi()
                .assets
                .getPairs()
                .toSingle()
                .map { pairsList ->
                    pairsList.map { AssetPairRecord(it) }
                }
    }

    override fun getRate(sourceAsset: String, destAsset: String): BigDecimal? {
        if (sourceAsset == destAsset) {
            return BigDecimal.ONE
        }

        val pairs = itemsCache.items
        val conversionAsset = "USD"

        val mainPairPrice = pairs.find {
            it.quote == destAsset && it.base == sourceAsset
        }?.price

        val quotePair = pairs.find {
            it.quote == sourceAsset && it.base == destAsset
        }
        val quotePairPrice =
                if (quotePair?.price != null)
                    BigDecimal.ONE.divide(quotePair.price, MathContext.DECIMAL128)
                else
                    null
        val throughDefaultAssetPrice =
                if (destAsset == conversionAsset || sourceAsset == conversionAsset)
                    null
                else
                    getRateOrOne(sourceAsset, conversionAsset)
                            .multiply(getRateOrOne(conversionAsset, destAsset))

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
}