package org.tokend.template.data.repository

import io.reactivex.Single
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.accounts.model.limits.Limits
import org.tokend.template.data.repository.base.SingleItemRepository
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.WalletInfoProvider

class LimitsRepository(private val apiProvider: ApiProvider,
                       private val walletInfoProvider: WalletInfoProvider
) : SingleItemRepository<Limits>() {
    override fun getItem(): Single<Limits> {
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Single.error(IllegalStateException("No wallet info found"))
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))

        return signedApi
                .accounts
                .getLimits(accountId)
                .toSingle()
    }
}