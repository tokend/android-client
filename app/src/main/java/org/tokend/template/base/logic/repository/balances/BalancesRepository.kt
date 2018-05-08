package org.tokend.template.base.logic.repository.balances

import io.reactivex.Single
import org.tokend.sdk.api.models.BalanceDetails
import org.tokend.template.base.logic.di.providers.ApiProvider
import org.tokend.template.base.logic.di.providers.WalletInfoProvider
import org.tokend.template.base.logic.repository.base.SimpleMultipleItemsRepository
import org.tokend.template.extensions.toSingle

class BalancesRepository(
        private val apiProvider: ApiProvider,
        private val walletInfoProvider: WalletInfoProvider
) : SimpleMultipleItemsRepository<BalanceDetails>() {
    override val itemsCache = BalancesCache()

    override fun getItems(): Single<List<BalanceDetails>> {
        val signedApi = apiProvider.getSignedApi()
                ?: return Single.error(IllegalStateException("No signed API instance found"))
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Single.error(IllegalStateException("No wallet info found"))
        return signedApi.getBalancesDetails(accountId).toSingle()
    }
}