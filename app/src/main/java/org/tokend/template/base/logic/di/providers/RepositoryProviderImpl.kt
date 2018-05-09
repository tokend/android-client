package org.tokend.template.base.logic.di.providers

import org.tokend.template.base.logic.repository.TxRepository
import org.tokend.template.base.logic.repository.balances.BalancesRepository

class RepositoryProviderImpl(
        private val apiProvider: ApiProvider,
        private val walletInfoProvider: WalletInfoProvider
) : RepositoryProvider {
    private val balancesRepository: BalancesRepository by lazy {
        BalancesRepository(apiProvider, walletInfoProvider)
    }
    private val transactionsRepositoriesByAsset = mutableMapOf<String, TxRepository>()

    override fun balances(): BalancesRepository {
        return balancesRepository
    }

    override fun transactions(asset: String): TxRepository {
        return transactionsRepositoriesByAsset.getOrPut(asset) {
            TxRepository(apiProvider, walletInfoProvider, asset)
        }
    }
}