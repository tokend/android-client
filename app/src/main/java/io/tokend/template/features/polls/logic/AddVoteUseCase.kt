package io.tokend.template.features.polls.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.tokend.template.logic.TxManager
import io.tokend.template.logic.providers.AccountProvider
import io.tokend.template.logic.providers.RepositoryProvider
import io.tokend.template.logic.providers.WalletInfoProvider
import org.tokend.wallet.NetworkParams
import org.tokend.wallet.Transaction
import org.tokend.wallet.TransactionBuilder
import org.tokend.wallet.xdr.*

/**
 * Submits user's vote in the poll.
 *
 * Updates polls repository on success.
 *
 * @param choiceIndex index of choice, starts from 0
 */
class AddVoteUseCase(
    private val pollId: String,
    private val pollOwnerAccountId: String,
    private val choiceIndex: Int,
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
            .systemInfo
            .getNetworkParams()
    }

    private fun getTransaction(): Single<Transaction> {
        return Single.defer {
            val operation = ManageVoteOp(
                data = ManageVoteOp.ManageVoteOpData.Create(
                    CreateVoteData(
                        pollId.toLong(),
                        VoteData.SingleChoice(
                            SingleChoiceVote(
                                choiceIndex + 1,
                                EmptyExt.EmptyVersion()
                            )
                        ),
                        CreateVoteData.CreateVoteDataExt.EmptyVersion()
                    )
                ),
                ext = ManageVoteOp.ManageVoteOpExt.EmptyVersion()
            )

            val account = accountProvider.getDefaultAccount()

            val sourceAccountId = walletInfoProvider.getWalletInfo().accountId

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
            .updatePollChoiceLocally(pollId, choiceIndex)
    }
}