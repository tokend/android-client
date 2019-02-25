package org.tokend.template.data.repository

import io.reactivex.Observable
import io.reactivex.Single
import org.tokend.template.data.repository.base.SimpleSingleItemRepository
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.base.params.PagingParamsV2
import org.tokend.sdk.api.v3.fees.params.FeesPageParamsV3
import org.tokend.sdk.utils.SimplePagedResourceLoader
import org.tokend.template.features.fees.model.FeeRecord
import org.tokend.template.features.fees.model.FeesRecords

class FeesRepository(private val apiProvider: ApiProvider,
                     private val walletInfoProvider: WalletInfoProvider
) : SimpleSingleItemRepository<FeesRecords>() {
    override fun getItem(): Observable<FeesRecords> {
        return getFeesResponse().toObservable()
    }

    private fun getFeesResponse(): Single<FeesRecords> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))

        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Single.error(IllegalStateException("No wallet info found"))

        val loader = SimplePagedResourceLoader({ nextCursor ->
            signedApi.v3.fees.get(
                    FeesPageParamsV3(
                            account = accountId,
                            pagingParams = PagingParamsV2(
                                    page = nextCursor
                            )
                    )
            )
        })

        return loader.loadAll()
                .toSingle()
                .map { resourceList ->
                    val records = resourceList.map { FeeRecord.fromResource(it) }
                    FeesRecords(records.groupBy { it.asset })
                }
    }
}