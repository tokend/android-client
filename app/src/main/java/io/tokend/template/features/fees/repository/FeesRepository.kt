package io.tokend.template.features.fees.repository

import io.reactivex.Single
import io.tokend.template.data.storage.repository.SingleItemRepository
import io.tokend.template.di.providers.ApiProvider
import io.tokend.template.di.providers.WalletInfoProvider
import io.tokend.template.features.fees.model.FeeRecord
import io.tokend.template.features.fees.model.FeesRecords
import org.tokend.rx.extensions.toSingle

class FeesRepository(
    private val apiProvider: ApiProvider,
    private val walletInfoProvider: WalletInfoProvider
) : SingleItemRepository<FeesRecords>() {
    override fun getItem(): Single<FeesRecords> {
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
                    .groupBy { it.asset.code }
                    .let(::FeesRecords)
            }
    }
}