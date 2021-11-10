package io.tokend.template.features.fees.repository

import io.reactivex.Single
import io.tokend.template.data.storage.repository.SingleItemRepository
import io.tokend.template.logic.providers.ApiProvider
import io.tokend.template.logic.providers.WalletInfoProvider
import io.tokend.template.features.fees.model.FeeRecord
import io.tokend.template.features.fees.model.FeesRecords
import org.tokend.rx.extensions.toSingle

class FeesRepository(
    private val apiProvider: ApiProvider,
    private val walletInfoProvider: WalletInfoProvider
) : SingleItemRepository<FeesRecords>() {
    override fun getItem(): Single<FeesRecords> {
        val signedApi = apiProvider.getSignedApi()
        val accountId = walletInfoProvider.getWalletInfo().accountId

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