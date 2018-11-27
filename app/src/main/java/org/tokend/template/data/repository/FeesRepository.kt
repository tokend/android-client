package org.tokend.template.data.repository

import io.reactivex.Observable
import io.reactivex.Single
import org.tokend.sdk.api.fees.model.Fees
import org.tokend.template.data.repository.base.SimpleSingleItemRepository
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.template.extensions.toSingle

class FeesRepository(private val apiProvider: ApiProvider,
                     private val walletInfoProvider: WalletInfoProvider) : SimpleSingleItemRepository<Fees>() {
    override fun getItem(): Observable<Fees> {
        return getFeesResponse().toObservable()
    }

    private fun getFeesResponse() : Single<Fees> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))

        return signedApi
                .fees
                .getExistingFees(walletInfoProvider.getWalletInfo()?.accountId)
                .toSingle()
    }
}