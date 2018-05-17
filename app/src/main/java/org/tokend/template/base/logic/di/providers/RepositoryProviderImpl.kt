package org.tokend.template.base.logic.di.providers

import org.tokend.template.base.logic.repository.AccountDetailsRepository
import org.tokend.template.base.logic.repository.SystemInfoRepository
import org.tokend.template.base.logic.repository.assets.AssetsRepository
import org.tokend.template.base.logic.repository.balances.BalancesRepository
import org.tokend.template.base.logic.repository.tfa.TfaBackendsRepository
import org.tokend.template.base.logic.repository.transactions.TxRepository

class RepositoryProviderImpl(
        private val apiProvider: ApiProvider,
        private val walletInfoProvider: WalletInfoProvider
) : RepositoryProvider {
    private val balancesRepository: BalancesRepository by lazy {
        BalancesRepository(apiProvider, walletInfoProvider)
    }
    private val transactionsRepositoriesByAsset = mutableMapOf<String, TxRepository>()
    private val accountDetails: AccountDetailsRepository by lazy {
        AccountDetailsRepository(apiProvider)
    }
    private val systemInfoRepository: SystemInfoRepository by lazy {
        SystemInfoRepository(apiProvider)
    }
    private val tfaBackendsRepository: TfaBackendsRepository by lazy {
        TfaBackendsRepository(apiProvider, walletInfoProvider)
    }
    private val assetsRepository: AssetsRepository by lazy {
        AssetsRepository(apiProvider)
    }

    override fun balances(): BalancesRepository {
        return balancesRepository
    }

    override fun transactions(asset: String): TxRepository {
        return transactionsRepositoriesByAsset.getOrPut(asset) {
            TxRepository(apiProvider, walletInfoProvider, asset, accountDetails())
        }
    }

    override fun accountDetails(): AccountDetailsRepository {
        return accountDetails
    }

    override fun systemInfo(): SystemInfoRepository {
        return systemInfoRepository
    }

    override fun tfaBackends(): TfaBackendsRepository {
        return tfaBackendsRepository
    }

    override fun assets(): AssetsRepository {
        return assetsRepository
    }
}