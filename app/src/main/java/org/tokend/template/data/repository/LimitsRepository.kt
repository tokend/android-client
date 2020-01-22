package org.tokend.template.data.repository

import io.reactivex.Maybe
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.accounts.model.limits.Limits
import org.tokend.template.data.repository.base.SingleItemRepository
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.WalletInfoProvider

class LimitsRepository(private val apiProvider: ApiProvider,
                       private val walletInfoProvider: WalletInfoProvider
) : SingleItemRepository<Limits>() {
    override fun getItem(): Maybe<Limits> {
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Maybe.error(IllegalStateException("No wallet info found"))
        val signedApi = apiProvider.getSignedApi()
                ?: return Maybe.error(IllegalStateException("No signed API instance found"))

        return signedApi
                .accounts
                .getLimits(accountId)
                .toSingle()
                .toMaybe()
    }
}