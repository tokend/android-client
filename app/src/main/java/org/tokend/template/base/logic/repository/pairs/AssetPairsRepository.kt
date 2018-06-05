package org.tokend.template.base.logic.repository.pairs

import io.reactivex.Single
import org.tokend.sdk.api.models.AssetPair
import org.tokend.template.BuildConfig
import org.tokend.template.base.logic.di.providers.ApiProvider
import org.tokend.template.base.logic.repository.base.SimpleMultipleItemsRepository
import org.tokend.template.extensions.toSingle
import java.math.BigDecimal
import java.math.MathContext

class AssetPairsRepository(
        private val apiProvider: ApiProvider
) : SimpleMultipleItemsRepository<AssetPair>(), AmountConverter {
    override val itemsCache = AssetPairsCache()

    override fun getItems(): Single<List<AssetPair>> {
        return apiProvider.getApi()
                .getAssetPairs()
                .toSingle()
    }

    override fun getRate(sourceAsset: String, destAsset: String): BigDecimal {
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
                    getRate(sourceAsset, conversionAsset)
                            .multiply(getRate(conversionAsset, destAsset))

        return mainPairPrice ?: quotePairPrice ?: throughDefaultAssetPrice ?: BigDecimal.ONE
    }

    override fun convertAmount(amount: BigDecimal?, sourceAsset: String, destAsset: String): BigDecimal {
        return if (amount == null) {
            BigDecimal.ZERO
        } else {
            amount.multiply(getRate(sourceAsset, destAsset), MathContext.DECIMAL128)
        }
    }
}