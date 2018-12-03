package org.tokend.template.test

import org.tokend.sdk.keyserver.models.WalletCreateResult
import org.tokend.template.data.model.UrlConfig
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.RepositoryProvider
import org.tokend.template.di.providers.UrlConfigProvider
import org.tokend.template.di.providers.UrlConfigProviderFactory
import org.tokend.template.features.signin.logic.PostSignInManager
import org.tokend.template.features.signin.logic.SignInUseCase
import org.tokend.template.logic.Session
import org.tokend.template.logic.transactions.TxManager
import org.tokend.wallet.Account
import org.tokend.wallet.PublicKeyFactory
import org.tokend.wallet.TransactionBuilder
import org.tokend.wallet.xdr.CreateIssuanceRequestOp
import org.tokend.wallet.xdr.Fee
import org.tokend.wallet.xdr.IssuanceRequest
import org.tokend.wallet.xdr.Operation
import java.math.BigDecimal

object Util {
    fun getUrlConfigProvider(url: String = Config.API): UrlConfigProvider {
        return UrlConfigProviderFactory().createUrlConfigProvider(
                UrlConfig(url, "", "", "")
        )
    }

    fun getVerifiedWallet(email: String,
                          password: CharArray,
                          apiProvider: ApiProvider,
                          session: Session,
                          repositoryProvider: RepositoryProvider?): WalletCreateResult {
        val createResult = apiProvider.getKeyStorage().createAndSaveWallet(email, password)

        System.out.println("Email is $email")
        System.out.println("Recovery seed is " +
                createResult.recoveryAccount.secretSeed!!.joinToString(""))

        SignInUseCase(
                email,
                password,
                apiProvider.getKeyStorage(),
                session,
                null,
                repositoryProvider?.let { PostSignInManager(it) }
        ).perform().blockingAwait()

        return createResult
    }

    fun getSomeMoney(asset: String,
                             amount: BigDecimal,
                             repositoryProvider: RepositoryProvider,
                             txManager: TxManager): BigDecimal {
        val netParams = repositoryProvider.systemInfo().getNetworkParams().blockingGet()

        val balanceId = repositoryProvider.balances()
                .itemsSubject.value
                .find { it.asset == asset }!!
                .balanceId

        val issuance = IssuanceRequest(
                asset,
                netParams.amountToPrecised(amount),
                PublicKeyFactory.fromBalanceId(balanceId),
                "{}",
                Fee(0, 0, Fee.FeeExt.EmptyVersion()),
                IssuanceRequest.IssuanceRequestExt.EmptyVersion()
        )

        val op = CreateIssuanceRequestOp(
                issuance,
                "${System.currentTimeMillis()}",
                CreateIssuanceRequestOp.CreateIssuanceRequestOpExt.EmptyVersion()
        )

        val sourceAccount = Account.fromSecretSeed(Config.ADMIN_SEED)

        val tx = TransactionBuilder(netParams, sourceAccount.accountId)
                .addOperation(Operation.OperationBody.CreateIssuanceRequest(op))
                .build()
        tx.addSignature(sourceAccount)

        txManager.submit(tx).blockingGet()

        return amount
    }

}