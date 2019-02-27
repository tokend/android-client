package org.tokend.template.data.repository

import io.reactivex.Observable
import io.reactivex.Single
import org.tokend.rx.extensions.toSingle
import org.tokend.template.data.repository.base.SimpleSingleItemRepository
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.WalletInfoProvider
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

        return signedApi
                .v3
                .accounts
                .getFees(accountId)
                .toSingle()
                .map { resourceList ->
                    resourceList
                            .filter { (it.fixed + it.percent).signum() > 0 }
                            .map(::FeeRecord)
                            .groupBy(FeeRecord::asset)
                            .let(::FeesRecords)
                }
    }
}