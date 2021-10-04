package org.tokend.template.features.invest.repository

import com.fasterxml.jackson.databind.ObjectMapper
import io.reactivex.Single
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.base.model.DataPage
import org.tokend.sdk.api.base.params.PagingOrder
import org.tokend.sdk.api.base.params.PagingParamsV2
import org.tokend.sdk.api.v3.sales.model.SaleState
import org.tokend.sdk.api.v3.sales.params.SaleParamsV3
import org.tokend.sdk.api.v3.sales.params.SalesPageParamsV3
import org.tokend.template.data.storage.repository.pagination.SimplePagedDataRepository
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.UrlConfigProvider
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.template.extensions.tryOrNull
import org.tokend.template.features.invest.model.SaleRecord

class SalesRepository(
        private val walletInfoProvider: WalletInfoProvider,
        private val apiProvider: ApiProvider,
        private val urlConfigProvider: UrlConfigProvider,
        private val mapper: ObjectMapper,
) : SimplePagedDataRepository<SaleRecord>(
        pagingOrder = PagingOrder.DESC,
        pageLimit = 10
) {
    private var baseAsset: String? = null

    override fun getPage(
            limit: Int,
            page: String?,
            order: PagingOrder,
    ): Single<DataPage<SaleRecord>> {
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Single.error(IllegalStateException("No wallet info found"))

        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))

        val requestParams = SalesPageParamsV3(
                pagingParams = PagingParamsV2(
                        page = page,
                        order = order,
                        limit = limit
                ),
                state = SaleState.OPEN,
                baseAsset = baseAsset,
                includes = listOf(
                        SaleParamsV3.Includes.BASE_ASSET,
                        SaleParamsV3.Includes.QUOTE_ASSET,
                        SaleParamsV3.Includes.DEFAULT_QUOTE_ASSET
                )
        )

        return signedApi
                .v3
                .sales
                .getForAccount(accountId, requestParams)
                .toSingle()
                .map { dataPage ->
                    dataPage.mapItemsNotNull {
                        tryOrNull {
                            SaleRecord.fromResource(it, urlConfigProvider.getConfig(), mapper)
                        }
                    }
                }
    }

    fun getSingle(id: Long): Single<SaleRecord> {
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Single.error(IllegalStateException("No wallet info found"))

        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))

        val params = SaleParamsV3(
                include = listOf(
                        SaleParamsV3.Includes.BASE_ASSET,
                        SaleParamsV3.Includes.QUOTE_ASSET,
                        SaleParamsV3.Includes.DEFAULT_QUOTE_ASSET
                )
        )

        return signedApi
                .v3
                .sales
                .getByIdForAccount(accountId, id.toString(), params)
                .toSingle()
                .map { SaleRecord.fromResource(it, urlConfigProvider.getConfig(), mapper) }
    }

    fun forQuery(baseAsset: String? = null): SalesRepository {
        this.baseAsset = baseAsset
        return this
    }
}