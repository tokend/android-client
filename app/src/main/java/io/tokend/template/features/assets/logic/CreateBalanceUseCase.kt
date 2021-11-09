package io.tokend.template.features.assets.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.tokend.template.di.providers.AccountProvider
import io.tokend.template.features.balances.storage.BalancesRepository
import io.tokend.template.features.systeminfo.storage.SystemInfoRepository
import io.tokend.template.logic.TxManager

/**
 * Creates balance of given asset
 */
class CreateBalanceUseCase(
    private val asset: String,
    private val balancesRepository: BalancesRepository,
    private val systemInfoRepository: SystemInfoRepository,
    private val accountProvider: AccountProvider,
    private val txManager: TxManager
) {
    fun perform(): Completable {
        // Well, quite simple (¬_¬).
        return createBalance()
            .ignoreElement()
    }

    private fun createBalance(): Single<Boolean> {
        return balancesRepository
            .create(
                accountProvider,
                systemInfoRepository,
                txManager,
                asset
            )
            .toSingleDefault(true)
    }
}