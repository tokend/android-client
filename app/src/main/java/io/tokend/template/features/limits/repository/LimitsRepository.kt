package io.tokend.template.features.limits.repository

import io.reactivex.Single
import io.tokend.template.data.storage.repository.SingleItemRepository
import io.tokend.template.logic.providers.ApiProvider
import io.tokend.template.logic.providers.WalletInfoProvider
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.accounts.model.limits.Limits

class LimitsRepository(
    private val apiProvider: ApiProvider,
    private val walletInfoProvider: WalletInfoProvider
) : SingleItemRepository<Limits>() {
    override fun getItem(): Single<Limits> {
        val accountId = walletInfoProvider.getWalletInfo().accountId
        val signedApi = apiProvider.getSignedApi()

        return signedApi
            .accounts
            .getLimits(accountId)
            .toSingle()
    }
}