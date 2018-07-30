package org.tokend.template.features.deposit

import io.reactivex.Completable
import io.reactivex.Single
import org.tokend.template.base.logic.di.providers.AccountProvider
import org.tokend.template.base.logic.di.providers.WalletInfoProvider
import org.tokend.template.base.logic.repository.AccountRepository
import org.tokend.template.base.logic.repository.SystemInfoRepository
import org.tokend.template.base.logic.repository.balances.BalancesRepository
import org.tokend.template.base.logic.transactions.TransactionFailedException
import org.tokend.template.base.logic.transactions.TxManager
import org.tokend.wallet.xdr.Operation
import org.tokend.wallet.xdr.op_extensions.BindExternalAccountOp
import org.tokend.wallet.xdr.op_extensions.CreateBalanceOp

class DepositManager(
        private val walletInfoProvider: WalletInfoProvider,
        private val balancesRepository: BalancesRepository,
        private val accountRepository: AccountRepository
) {
    class NoAvailableExternalAccountsException : Exception()

    fun bindExternalAccount(accountProvider: AccountProvider,
                            systemInfoRepository: SystemInfoRepository,
                            txManager: TxManager,
                            asset: String,
                            type: Int): Completable {
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Completable.error(IllegalStateException("No wallet info found"))
        val account = accountProvider.getAccount()
                ?: return Completable.error(IllegalStateException("Cannot obtain current account"))

        var needBalanceCreation = false

        // At first update balances.
        return balancesRepository.updateIfNotFreshDeferred()
                // Then check if balance for current asset exists.
                .toSingle {
                    needBalanceCreation =
                            balancesRepository.itemsSubject.value.find { it.asset == asset } != null
                }
                .flatMap {
                    systemInfoRepository.getNetworkParams()
                }
                // Form transaction with balance creation if needed.
                .flatMap { netParams ->
                    val operations = mutableListOf<Operation.OperationBody>()

                    if (!needBalanceCreation) {
                        val createBalanceOp = CreateBalanceOp(accountId, asset)
                        operations.add(
                                Operation.OperationBody.ManageBalance(createBalanceOp)
                        )
                    }

                    val bindOp = BindExternalAccountOp(type)
                    operations
                            .add(Operation.OperationBody.BindExternalSystemAccountId(bindOp))

                    TxManager.createSignedTransaction(netParams, accountId, account,
                            *operations.toTypedArray())

                }
                .flatMap {
                    txManager.submit(it)
                }
                // Throw special error if there are no available addresses.
                .onErrorResumeNext { e: Throwable ->
                    if (e is TransactionFailedException &&
                            e.operationResultCodes.contains(
                                    TransactionFailedException.OP_NO_AVAILABLE_EXTERNAL_ACCOUNTS
                            )) {
                        Single.error(NoAvailableExternalAccountsException())
                    } else {
                        Single.error(e)
                    }
                }
                .toCompletable()
                .doOnComplete {
                    if (needBalanceCreation) {
                        balancesRepository.invalidate()
                    }
                    accountRepository.invalidate()
                }
    }
}