package org.tokend.template.data.repository

import io.reactivex.Maybe
import org.tokend.rx.extensions.toSingle
import org.tokend.template.data.repository.base.SingleItemRepository
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.template.features.fees.model.FeeRecord
import org.tokend.template.features.fees.model.FeesRecords

class FeesRepository(private val apiProvider: ApiProvider,
                     private val walletInfoProvider: WalletInfoProvider
) : SingleItemRepository<FeesRecords>() {
    override fun getItem(): Maybe<FeesRecords> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Maybe.error(IllegalStateException("No signed API instance found"))
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Maybe.error(IllegalStateException("No wallet info found"))

        return signedApi
                .v3
                .accounts
                .getFees(accountId)
                .toSingle()
                .map { resourceList ->
                    resourceList
                            .filter { (it.fixed + it.percent).signum() > 0 }
                            .map(::FeeRecord)
                            .groupBy { it.asset.code }
                            .let(::FeesRecords)
                }
                .toMaybe()
    }
}