package org.tokend.template.features.explore

import io.reactivex.Completable
import io.reactivex.Single
import org.tokend.template.base.logic.di.providers.AccountProvider
import org.tokend.template.base.logic.repository.SystemInfoRepository
import org.tokend.template.base.logic.repository.balances.BalancesRepository
import org.tokend.template.base.logic.transactions.TxManager

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
                .toCompletable()
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