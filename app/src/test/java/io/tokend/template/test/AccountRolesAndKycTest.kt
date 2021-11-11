package io.tokend.template.test

import io.tokend.template.features.account.data.model.AccountRole
import io.tokend.template.features.kyc.logic.SubmitKycRequestUseCase
import io.tokend.template.features.kyc.model.KycForm
import io.tokend.template.features.kyc.model.KycRequestState
import io.tokend.template.logic.TxManager
import io.tokend.template.logic.providers.AccountProviderFactory
import io.tokend.template.logic.providers.ApiProviderFactory
import io.tokend.template.logic.providers.RepositoryProviderImpl
import io.tokend.template.logic.providers.WalletInfoProviderFactory
import io.tokend.template.logic.session.Session
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.tokend.sdk.factory.JsonApiToolsProvider
import org.tokend.wallet.NetworkParams
import org.tokend.wallet.TransactionBuilder
import org.tokend.wallet.xdr.KeyValueEntryValue
import org.tokend.wallet.xdr.ManageKeyValueOp
import org.tokend.wallet.xdr.Operation

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class AccountRolesAndKycTest {
    @Test
    fun aSignUpUnverified() {
        val urlConfigProvider = Util.getUrlConfigProvider()
        val session = Session(
            WalletInfoProviderFactory().createWalletInfoProvider(),
            AccountProviderFactory().createAccountProvider()
        )

        val email = Util.getEmail()
        val password = Config.DEFAULT_PASSWORD

        val apiProvider =
            ApiProviderFactory().createApiProvider(urlConfigProvider, session)
        val repositoryProvider = RepositoryProviderImpl(
            apiProvider, session, urlConfigProvider,
            JsonApiToolsProvider.getObjectMapper()
        )

        Util.getVerifiedWallet(
            email, password, apiProvider, session, repositoryProvider
        )

        Assert.assertEquals(
            "A newly created account must have the 'unverified' role",
            AccountRole.UNVERIFIED,
            repositoryProvider.account.item!!.role.role
        )
    }

    @Test
    fun bBecomeGeneralAutoapprove() {
        val urlConfigProvider = Util.getUrlConfigProvider()
        val session = Session(
            WalletInfoProviderFactory().createWalletInfoProvider(),
            AccountProviderFactory().createAccountProvider()
        )

        val email = Util.getEmail()
        val password = Config.DEFAULT_PASSWORD

        val apiProvider =
            ApiProviderFactory().createApiProvider(urlConfigProvider, session)
        val repositoryProvider = RepositoryProviderImpl(
            apiProvider, session, urlConfigProvider,
            JsonApiToolsProvider.getObjectMapper()
        )

        Util.getVerifiedWallet(
            email, password, apiProvider, session, repositoryProvider
        )

        val form = KycForm.General(
            firstName = "Become",
            lastName = "General"
        )

        val txManager = TxManager(apiProvider)

        val previousChangeRoleTasks = apiProvider.getApi().v3.keyValue.getById(CHANGE_ROLE_TASKS_KEY)
            .execute().get().value.u32!!.toInt()

        setChangeRoleTasks(
            0,
            repositoryProvider.systemInfo.getNetworkParams().blockingGet(),
            txManager
        )

        try {
            SubmitKycRequestUseCase(
                form = form,
                walletInfoProvider = session,
                accountProvider = session,
                repositoryProvider = repositoryProvider,
                apiProvider = apiProvider,
                txManager = txManager
            ).perform().blockingAwait()

            setChangeRoleTasks(
                previousChangeRoleTasks,
                repositoryProvider.systemInfo.getNetworkParams().blockingGet(),
                txManager
            )

            Assert.assertTrue(
                "The request state must be Approved once the form is submitted",
                repositoryProvider.kycRequestState.item is KycRequestState.Submitted.Approved<*>
            )
            Assert.assertTrue(
                "There must be an actual role in the account repo once the form is submitted",
                repositoryProvider.account.item!!.role.role == form.getRole()
            )
            Assert.assertTrue(
                "There must be an actual form in the active KYC repo once the form is submitted",
                repositoryProvider.activeKyc.itemFormData == form
            )
        } finally {
            setChangeRoleTasks(
                previousChangeRoleTasks,
                repositoryProvider.systemInfo.getNetworkParams().blockingGet(),
                txManager
            )
        }
    }

    @Test
    fun cBecomeGeneralWithReview() {
        val urlConfigProvider = Util.getUrlConfigProvider()
        val session = Session(
            WalletInfoProviderFactory().createWalletInfoProvider(),
            AccountProviderFactory().createAccountProvider()
        )

        val email = Util.getEmail()
        val password = Config.DEFAULT_PASSWORD

        val apiProvider =
            ApiProviderFactory().createApiProvider(urlConfigProvider, session)
        val repositoryProvider = RepositoryProviderImpl(
            apiProvider, session, urlConfigProvider,
            JsonApiToolsProvider.getObjectMapper()
        )

        Util.getVerifiedWallet(
            email, password, apiProvider, session, repositoryProvider
        )

        val form = KycForm.General(
            firstName = "Become",
            lastName = "General"
        )

        val txManager = TxManager(apiProvider)

        val previousChangeRoleTasks = apiProvider.getApi().v3.keyValue.getById(CHANGE_ROLE_TASKS_KEY)
            .execute().get().value.u32!!.toInt()

        setChangeRoleTasks(
            1,
            repositoryProvider.systemInfo.getNetworkParams().blockingGet(),
            txManager
        )

        try {
            SubmitKycRequestUseCase(
                form = form,
                walletInfoProvider = session,
                accountProvider = session,
                repositoryProvider = repositoryProvider,
                apiProvider = apiProvider,
                txManager = txManager
            ).perform().blockingAwait()

            Assert.assertTrue(
                "The request state must be Pending",
                repositoryProvider.kycRequestState.item is KycRequestState.Submitted.Pending<*>
            )

            val requestIdToReview =
                (repositoryProvider.kycRequestState.item as KycRequestState.Submitted<*>)
                    .requestId

            val requestToReview = ApiProviderFactory()
                .createApiProvider(
                    urlConfigProvider,
                    Config.ADMIN_ACCOUNT,
                    Config.ADMIN_ACCOUNT.accountId
                )
                .getSignedApi()
                .v3
                .requests
                .getById(requestIdToReview.toString())
                .execute()
                .get()

            Util.reviewRequestIfNeeded(
                requestToReview,
                repositoryProvider.systemInfo.getNetworkParams().blockingGet(), txManager
            )

            repositoryProvider.kycRequestState.updateDeferred().blockingAwait()
            Assert.assertTrue(
                "The KYC request state must be Approved once the request is approved",
                repositoryProvider.kycRequestState.item is KycRequestState.Submitted.Approved<*>
            )

            repositoryProvider.account.updateDeferred().blockingAwait()
            Assert.assertTrue(
                "The account role must be actual once the request is approved",
                repositoryProvider.account.item!!.role.role == form.getRole()
            )

            repositoryProvider.activeKyc.updateDeferred().blockingAwait()
            Assert.assertTrue(
                "The active KYC must be actual once the request is approved",
                repositoryProvider.activeKyc.itemFormData == form
            )
        } finally {
            setChangeRoleTasks(
                previousChangeRoleTasks,
                repositoryProvider.systemInfo.getNetworkParams().blockingGet(),
                txManager
            )
        }
    }

    private fun setChangeRoleTasks(
        tasks: Int,
        networkParams: NetworkParams,
        txManager: TxManager
    ) {
        txManager.submit(
            TransactionBuilder(
                networkParams,
                Config.ADMIN_ACCOUNT.accountId
            )
                .addSigner(Config.ADMIN_ACCOUNT)
                .addOperation(
                    Operation.OperationBody.ManageKeyValue(
                        ManageKeyValueOp(
                            key = CHANGE_ROLE_TASKS_KEY,
                            action = ManageKeyValueOp.ManageKeyValueOpAction.Put(
                                KeyValueEntryValue.Uint32(
                                    tasks
                                )
                            ),
                            ext = ManageKeyValueOp.ManageKeyValueOpExt.EmptyVersion()
                        )
                    )
                )
                .build()
        ).blockingGet()
    }

    companion object {
        private const val CHANGE_ROLE_TASKS_KEY = "change_role_tasks:*:*"
    }
}