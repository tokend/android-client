package org.tokend.template.base.logic.di.providers

import org.tokend.template.base.logic.repository.balances.BalancesRepository

class RepositoryProviderImpl(
        private val apiProvider: ApiProvider,
        private val walletInfoProvider: WalletInfoProvider
) : RepositoryProvider {
    private val balancesRepository: BalancesRepository by lazy {
        BalancesRepository(apiProvider, walletInfoProvider)
    }

    override fun balances(): BalancesRepository {
        return balancesRepository
    }
}