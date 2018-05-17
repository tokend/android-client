package org.tokend.template.base.logic.repository.balances

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.tokend.sdk.api.models.BalanceDetails
import org.tokend.template.base.logic.di.providers.AccountProvider
import org.tokend.template.base.logic.di.providers.ApiProvider
import org.tokend.template.base.logic.di.providers.WalletInfoProvider
import org.tokend.template.base.logic.repository.SystemInfoRepository
import org.tokend.template.base.logic.repository.base.SimpleMultipleItemsRepository
import org.tokend.template.base.logic.transactions.TxManager
import org.tokend.template.extensions.toSingle
import org.tokend.wallet.*
import org.tokend.wallet.xdr.Operation
import org.tokend.wallet.xdr.op_extensions.CreateBalanceOp

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

    fun create(asset: String,
               accountProvider: AccountProvider,
               systemInfoRepository: SystemInfoRepository,
               txManager: TxManager): Completable {
        val accountId = walletInfoProvider.getWalletInfo()?.accountId
                ?: return Completable.error(IllegalStateException("No wallet info found"))
        val account = accountProvider.getAccount()
                ?: return Completable.error(IllegalStateException("Cannot obtain current account"))

        return systemInfoRepository.getNetworkParams()
                .flatMap { netParams ->
                    createBalanceCreationTransaction(netParams, accountId, account, asset)
                }
                .flatMap { transition ->
                    txManager.submit(transition)
                }
                .flatMapCompletable {
                    updateDeferred()
                }
                .doOnSubscribe {
                    isLoading = true
                }
                .doOnTerminate {
                    isLoading = false
                }
    }

    private fun createBalanceCreationTransaction(networkParams: NetworkParams,
                                                 sourceAccountId: String,
                                                 signer: Account,
                                                 asset: String): Single<Transaction> {
        return Single.defer {
            val operation = CreateBalanceOp(sourceAccountId, asset)

            val transaction =
                    TransactionBuilder(networkParams, PublicKeyFactory.fromAccountId(sourceAccountId))
                            .addOperation(Operation.OperationBody.ManageBalance(operation))
                            .build()

            transaction.addSignature(signer)

            Single.just(transaction)
        }.subscribeOn(Schedulers.computation())
    }
}