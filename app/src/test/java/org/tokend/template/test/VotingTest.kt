package org.tokend.template.test

import org.junit.Assert
import org.junit.Test
import org.tokend.sdk.factory.JsonApiToolsProvider
import org.tokend.template.di.providers.*
import org.tokend.template.features.polls.logic.AddVoteUseCase
import org.tokend.template.features.polls.logic.RemoveVoteUseCase
import org.tokend.template.logic.Session
import org.tokend.template.logic.transactions.TxManager
import org.tokend.wallet.PublicKeyFactory
import org.tokend.wallet.TransactionBuilder
import org.tokend.wallet.xdr.*
import org.tokend.wallet.xdr.utils.XdrDataOutputStream
import org.tokend.wallet.xdr.utils.toXdr
import java.util.*

class VotingTest {
    @Test
    fun aVoteInPoll() {
        val urlConfigProvider = Util.getUrlConfigProvider()
        val session = Session(
                WalletInfoProviderFactory().createWalletInfoProvider(),
                AccountProviderFactory().createAccountProvider()
        )

        val email = Util.getEmail()
        val password = Config.DEFAULT_PASSWORD

        val apiProvider =
                ApiProviderFactory().createApiProvider(urlConfigProvider, session)
        val repositoryProvider = RepositoryProviderImpl(apiProvider, session, urlConfigProvider,
                JsonApiToolsProvider.getObjectMapper())

        val (_, rootAccount, _) =
                Util.getVerifiedWallet(email, password, apiProvider, session, repositoryProvider)

        createPoll(repositoryProvider, apiProvider, session)

        val pollOwner = rootAccount.accountId

        val repo = repositoryProvider.polls(pollOwner)

        repo.updateDeferred().blockingAwait()

        val poll = repo.itemsList.first()
        val choice = 0

        val useCase = AddVoteUseCase(
                pollId = poll.id,
                pollOwnerAccountId = poll.ownerAccountId,
                accountProvider = session,
                walletInfoProvider = session,
                choiceIndex = choice,
                repositoryProvider = repositoryProvider,
                txManager = TxManager(apiProvider)
        )

        useCase.perform().blockingAwait()

        Assert.assertEquals("Poll choice must be updated locally in the repository",
                choice, repo.itemsList.first().currentChoice)

        repo.updateDeferred().blockingAwait()

        Assert.assertEquals("Remote poll choice must be the same as submitted one",
                choice, repo.itemsList.first().currentChoice)
    }

    @Test
    fun bUnvoteInPoll() {
        val urlConfigProvider = Util.getUrlConfigProvider()
        val session = Session(
                WalletInfoProviderFactory().createWalletInfoProvider(),
                AccountProviderFactory().createAccountProvider()
        )

        val email = Util.getEmail()
        val password = Config.DEFAULT_PASSWORD

        val apiProvider =
                ApiProviderFactory().createApiProvider(urlConfigProvider, session)
        val repositoryProvider = RepositoryProviderImpl(apiProvider, session, urlConfigProvider,
                JsonApiToolsProvider.getObjectMapper())

        val (_, rootAccount, _) =
                Util.getVerifiedWallet(email, password, apiProvider, session, repositoryProvider)

        createPoll(repositoryProvider, apiProvider, session)

        val pollOwner = rootAccount.accountId

        val repo = repositoryProvider.polls(pollOwner)

        repo.updateDeferred().blockingAwait()

        val poll = repo.itemsList.first()
        val choice = 0

        val voteUseCase = AddVoteUseCase(
                pollId = poll.id,
                pollOwnerAccountId = poll.ownerAccountId,
                accountProvider = session,
                walletInfoProvider = session,
                choiceIndex = choice,
                repositoryProvider = repositoryProvider,
                txManager = TxManager(apiProvider)
        )

        voteUseCase.perform().blockingAwait()

        repo.updateDeferred().blockingAwait()

        val unvoteUseCase = RemoveVoteUseCase(
                pollId = poll.id,
                pollOwnerAccountId = poll.ownerAccountId,
                accountProvider = session,
                walletInfoProvider = session,
                repositoryProvider = repositoryProvider,
                txManager = TxManager(apiProvider)
        )

        unvoteUseCase.perform().blockingAwait()

        Assert.assertNull("Poll choice must be updated locally in repository and should be null",
                repo.itemsList.first().currentChoice)

        repo.updateDeferred().blockingAwait()

        Assert.assertNull("Remote poll choice also must be null",
                repo.itemsList.first().currentChoice)
    }

    private fun createPoll(repositoryProvider: RepositoryProvider,
                           apiProvider: ApiProvider,
                           session: Session) {
        setKeyValueTasks(repositoryProvider)

        val netParams = repositoryProvider
                .systemInfo()
                .getNetworkParams()
                .blockingGet()

        val accountId = session.getWalletInfo()!!.accountId
        val account = session.getAccount()!!

        val request = CreatePollRequest(
                0,
                2,
                PollData.SingleChoice(EmptyExt.EmptyVersion()),
                "{\"subject\":\"Would you like us to do something?\"}",
                Date().time / 1000L,
                (Date().time / 1000L) + 3600,
                PublicKeyFactory.fromAccountId(accountId),
                false,
                CreatePollRequest.CreatePollRequestExt.EmptyVersion()
        )

        val tx = TransactionBuilder(netParams, accountId)
                .addOperation(Operation.OperationBody.ManageCreatePollRequest(
                        ManageCreatePollRequestOp(
                                ManageCreatePollRequestOp.ManageCreatePollRequestOpData.Create(
                                        CreatePollRequestData(
                                                request,
                                                null,
                                                CreatePollRequestData.CreatePollRequestDataExt.EmptyVersion()
                                        )
                                ),
                                ManageCreatePollRequestOp.ManageCreatePollRequestOpExt.EmptyVersion()
                        )
                ))
                .addSigner(account)
                .build()

        TxManager(apiProvider).submit(tx).blockingGet()
    }

    private fun setKeyValueTasks(repositoryProvider: RepositoryProvider) {
        val key = "create_poll_tasks:*"

        val apiProvider =
                ApiProviderFactory().createApiProvider(
                        Util.getUrlConfigProvider(),
                        AccountProviderFactory().createAccountProvider()
                )

        val op = ManageKeyValueOp(
                key = key,
                action = ManageKeyValueOp.ManageKeyValueOpAction.Put(
                        object : KeyValueEntryValue(KeyValueEntryType.UINT32) {
                            override fun toXdr(stream: XdrDataOutputStream) {
                                super.toXdr(stream)
                                0.toXdr(stream)
                            }
                        }
                ),
                ext = ManageKeyValueOp.ManageKeyValueOpExt.EmptyVersion()
        )

        val sourceAccount = Config.ADMIN_ACCOUNT

        val netParams = repositoryProvider
                .systemInfo()
                .getNetworkParams()
                .blockingGet()

        val tx = TransactionBuilder(netParams, sourceAccount.accountId)
                .addOperation(Operation.OperationBody.ManageKeyValue(op))
                .addSigner(sourceAccount)
                .build()

        TxManager(apiProvider).submit(tx).blockingGet()
    }
}