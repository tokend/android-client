package org.tokend.template.data.repository

import io.reactivex.Observable
import io.reactivex.Single
import org.tokend.template.data.repository.base.SimpleSingleItemRepository
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.rx.extensions.toSingle
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

        return signedApi
                .fees
                .getExistingFees(walletInfoProvider.getWalletInfo()?.accountId)
                .toSingle()
                .map {
                    val feesMap = it.feesAssetMap.mapValues { entry ->
                        entry.value.map { fee ->
                            FeeRecord(fee)
                        }
                    }
                    FeesRecords(feesMap)
                }
    }
}