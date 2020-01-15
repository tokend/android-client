package org.tokend.template.features.polls.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.tokend.template.di.providers.AccountProvider
import org.tokend.template.di.providers.RepositoryProvider
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.template.logic.TxManager
import org.tokend.wallet.NetworkParams
import org.tokend.wallet.Transaction
import org.tokend.wallet.TransactionBuilder
import org.tokend.wallet.xdr.ManageVoteOp
import org.tokend.wallet.xdr.Operation
import org.tokend.wallet.xdr.RemoveVoteData


/**
 * Submits removing user's vote in the poll.
 *
 * Updates polls repository on success.
 */
class RemoveVoteUseCase(
        private val pollId: String,
        private val pollOwnerAccountId: String,
        private val accountProvider: AccountProvider,
        private val walletInfoProvider: WalletInfoProvider,
        private val repositoryProvider: RepositoryProvider,
        private val txManager: TxManager
) {
    private lateinit var networkParams: NetworkParams

    fun perform(): Completable {
        return getNetworkParams()
                .doOnSuccess { networkParams ->
                    this.networkParams = networkParams
                }
                .flatMap {
                    getTransaction()
                }
                .flatMap { transaction ->
                    txManager.submit(transaction, waitForIngest = false)
                }
                .doOnSuccess {
                    updateRepository()
                }
                .ignoreElement()
    }

    private fun getNetworkParams(): Single<NetworkParams> {
        return repositoryProvider
                .systemInfo()
                .getNetworkParams()
    }

    private fun getTransaction(): Single<Transaction> {
        return Single.defer {
            val operation = ManageVoteOp(
                    data = ManageVoteOp.ManageVoteOpData.Remove(
                            RemoveVoteData(
                                    pollId.toLong(),
                                    RemoveVoteData.RemoveVoteDataExt.EmptyVersion()
                            )
                    ),
                    ext = ManageVoteOp.ManageVoteOpExt.EmptyVersion()
            )

            val account = accountProvider.getAccount()
                    ?: return@defer Single.error<Transaction>(
                            IllegalStateException("Cannot obtain current account")
                    )

            val sourceAccountId = walletInfoProvider.getWalletInfo()?.accountId
                    ?: return@defer Single.error<Transaction>(
                            IllegalStateException("No wallet info found")
                    )

            val transaction =
                    TransactionBuilder(networkParams, sourceAccountId)
                            .addOperation(Operation.OperationBody.ManageVote(operation))
                            .addSigner(account)
                            .build()

            Single.just(transaction)
        }.subscribeOn(Schedulers.newThread())
    }

    private fun updateRepository() {
        repositoryProvider
                .polls(pollOwnerAccountId)
                .updatePollChoiceLocally(pollId, null)
    }
}