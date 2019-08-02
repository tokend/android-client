package org.tokend.template.data.repository.assets

import io.reactivex.Observable
import io.reactivex.Single
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.utils.extentions.isNotFound
import org.tokend.template.data.model.AssetChartData
import org.tokend.template.data.repository.base.SimpleSingleItemRepository
import org.tokend.template.di.providers.ApiProvider
import retrofit2.HttpException

class AssetChartRepository private constructor(
        private val apiProvider: ApiProvider
) : SimpleSingleItemRepository<AssetChartData>() {
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

    override fun getItem(): Observable<AssetChartData> {
        val quoteAssetCode = quoteAssetCode

        return apiProvider
                .getApi()
                .assets
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
                .toObservable()
    }
}