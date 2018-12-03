package org.tokend.template.test

import junit.framework.Assert
import org.junit.Test
import org.tokend.template.di.providers.AccountProviderFactory
import org.tokend.template.di.providers.ApiProviderFactory
import org.tokend.template.di.providers.RepositoryProviderImpl
import org.tokend.template.di.providers.WalletInfoProviderFactory
import org.tokend.template.features.withdraw.logic.CreateWithdrawalRequestUseCase
import org.tokend.template.logic.FeeManager
import org.tokend.template.logic.Session
import org.tokend.wallet.xdr.FeeType
import java.math.BigDecimal

class WithdrawTest {
    private val asset = "ETH"
    private val amount = BigDecimal.TEN
    private val destAddress = "0x6a7a43be33ba930fe58f34e07d0ad6ba7adb9b1f"

    @Test
    fun createWithdrawal() {
        val urlConfigProvider = Util.getUrlConfigProvider()
        val session = Session(
                WalletInfoProviderFactory().createWalletInfoProvider(),
                AccountProviderFactory().createAccountProvider()
        )

        val email = "${System.currentTimeMillis()}@mail.com"
        val password = "qwe123".toCharArray()

        val apiProvider =
                ApiProviderFactory().createApiProvider(urlConfigProvider, session)
        val repositoryProvider = RepositoryProviderImpl(apiProvider, session)

        Util.getVerifiedWallet(
                email, password, apiProvider, session, repositoryProvider
        )

        val useCase = CreateWithdrawalRequestUseCase(
                amount,
                asset,
                destAddress,
                session,
                FeeManager(apiProvider)
        )

        val request = useCase.perform().blockingGet()

        Assert.assertEquals(0, amount.compareTo(request.amount))
        Assert.assertEquals(asset, request.asset)
        Assert.assertEquals(destAddress, request.destinationAddress)
        Assert.assertEquals(session.getWalletInfo()!!.accountId, request.accountId)
        Assert.assertEquals(FeeType.WITHDRAWAL_FEE.value, request.fee.feeType)
    }
}