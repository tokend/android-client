package io.tokend.template.features.deposit.logic

import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import io.tokend.template.data.model.AccountRecord
import io.tokend.template.data.repository.AccountRepository
import io.tokend.template.logic.providers.AccountProvider
import io.tokend.template.logic.providers.WalletInfoProvider
import io.tokend.template.extensions.tryOrNull
import io.tokend.template.features.balances.storage.BalancesRepository
import io.tokend.template.features.systeminfo.storage.SystemInfoRepository
import io.tokend.template.logic.TxManager
import org.tokend.sdk.api.transactions.model.TransactionFailedException
import org.tokend.wallet.NetworkParams
import org.tokend.wallet.Transaction
import org.tokend.wallet.xdr.LedgerEntry
import org.tokend.wallet.xdr.LedgerEntryChange
import org.tokend.wallet.xdr.Operation
import org.tokend.wallet.xdr.TransactionMeta
import org.tokend.wallet.xdr.op_extensions.BindExternalAccountOp
import org.tokend.wallet.xdr.op_extensions.CreateBalanceOp

/**
 * Binds deposit account from pool with [BindExternalAccountOp]
 */
class BindExternalSystemDepositAccountUseCase(
    private val externalSystemType: Int,
    private val systemInfoRepository: SystemInfoRepository,
    private val accountProvider: AccountProvider,
    private val txManager: TxManager,
    asset: String,
    walletInfoProvider: WalletInfoProvider,
    balancesRepository: BalancesRepository,
    accountRepository: AccountRepository
) : BindDepositAccountUseCase(asset, walletInfoProvider, balancesRepository, accountRepository) {
    private lateinit var networkParams: NetworkParams
    private lateinit var transaction: Transaction
    private lateinit var transactionResultMetaXdr: String

    override fun getBoundDepositAccount(): Single<AccountRecord.DepositAccount> {
        return getNetworkParams()
            .doOnSuccess { networkParams ->
                this.networkParams = networkParams
            }
            .flatMap {
                getTransaction()
            }
            .doOnSuccess { transaction ->
                this.transaction = transaction
            }
            .flatMap {
                getSubmitTransactionResult()
            }
            .doOnSuccess { transactionResultMetaXdr ->
                this.transactionResultMetaXdr = transactionResultMetaXdr
            }
            .flatMap {
                getDepositAccountFromTransactionResult()
            }
    }

    private fun getNetworkParams(): Single<NetworkParams> {
        return systemInfoRepository
            .getNetworkParams()
    }

    private fun getTransaction(): Single<Transaction> {
        val operations = mutableListOf<Operation.OperationBody>()

        if (isBalanceCreationRequired) {
            val createBalanceOp = CreateBalanceOp(accountId, asset)
            operations.add(Operation.OperationBody.ManageBalance(createBalanceOp))
        }

        val bindOp = BindExternalAccountOp(externalSystemType)
        operations.add(Operation.OperationBody.BindExternalSystemAccountId(bindOp))

        val account = accountProvider.getDefaultAccount()

        return TxManager.createSignedTransaction(
            networkParams, accountId, account,
            *operations.toTypedArray()
        )
    }

    private fun getSubmitTransactionResult(): Single<String> {
        return txManager
            .submit(transaction)
            // Throw special error if there are no available addresses.
            .onErrorResumeNext { e: Throwable ->
                if (e is TransactionFailedException &&
                    e.operationResultCodes.contains(
                        TransactionFailedException.OP_NO_AVAILABLE_EXTERNAL_ACCOUNTS
                    )
                ) {
                    Single.error(NoAvailableDepositAccountException())
                } else {
                    Single.error(e)
                }
            }
            .map { it.resultMetaXdr!! }
    }

    private fun getDepositAccountFromTransactionResult(): Single<AccountRecord.DepositAccount> {
        val meta = tryOrNull {
            TransactionMeta.fromBase64(transactionResultMetaXdr) as TransactionMeta.EmptyVersion
        } ?: return Single.error(IllegalStateException("Unable to parse TX meta"))

        val affectedEntry = meta
            .operations
            .asSequence()
            .map { it.changes.toList() }
            .flatten()
            .mapNotNull {
                when (it) {
                    is LedgerEntryChange.Updated -> it.updated.data
                    is LedgerEntryChange.Created -> it.created.data
                    else -> null
                }
            }
            .filterIsInstance<LedgerEntry.LedgerEntryData.ExternalSystemAccountIdPoolEntry>()
            .map { it.externalSystemAccountIDPoolEntry }
            .firstOrNull()
            ?: return Single.error(
                IllegalStateException(
                    "Unable to get affected external system account ID entry"
                )
            )

        return {
            AccountRecord.DepositAccount(affectedEntry)
        }.toSingle()
    }
}