package org.tokend.template.features.assets.storage

import io.reactivex.Single
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.utils.extentions.isNotFound
import org.tokend.template.data.repository.base.SingleItemRepository
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.features.assets.model.AssetChartData
import retrofit2.HttpException

class AssetChartRepository private constructor(
        private val apiProvider: ApiProvider
) : SingleItemRepository<AssetChartData>() {
    private lateinit var baseAssetCode: String
    private var quoteAssetCode: String? = null

    constructor(assetCode: String,
                apiProvider: ApiProvider) : this(apiProvider) {
        this.baseAssetCode = assetCode
    }

    constructor(baseAssetCode: String,
                quoteAssetCode: String,
                apiProvider: ApiProvider) : this(baseAssetCode, apiProvider) {
        this.quoteAssetCode = quoteAssetCode
    }

    override fun getItem(): Single<AssetChartData> {
        val quoteAssetCode = quoteAssetCode

        return apiProvider
                .getApi()
                .charts
                .let {
                    if (quoteAssetCode != null)
                        it.getChart(baseAssetCode, quoteAssetCode)
                    else
                        it.getChart(baseAssetCode)
                }
                .map(::AssetChartData)
                .toSingle()
                .onErrorResumeNext{ error ->
                    if (error is HttpException && error.isNotFound())
                        Single.just(AssetChartData())
                    else
                        Single.error(error)
                }
    }
}