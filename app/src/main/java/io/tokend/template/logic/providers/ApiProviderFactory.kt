package io.tokend.template.logic.providers

import io.tokend.template.logic.credentials.model.WalletInfoRecord
import io.tokend.template.logic.session.Session
import okhttp3.CookieJar
import org.tokend.sdk.tfa.TfaCallback
import org.tokend.wallet.Account

class ApiProviderFactory {
    fun createApiProvider(
        urlConfigProvider: UrlConfigProvider,
        account: Account? = null,
        originalAccountId: String = "",
        tfaCallback: TfaCallback? = null,
        cookieJar: CookieJar? = null
    ): ApiProvider {
        return ApiProviderImpl(
            urlConfigProvider,
            AccountProviderFactory().createAccountProvider(
                account?.let { listOf(it) } ?: emptyList()),
            WalletInfoProviderFactory().createWalletInfoProvider(
                WalletInfoRecord(login = "mocked@localhost", accountId = originalAccountId)
            ),
            tfaCallback,
            cookieJar
        )
    }

    fun createApiProvider(
        urlConfigProvider: UrlConfigProvider,
        accountProvider: AccountProvider,
        walletInfoProvider: WalletInfoProvider,
        tfaCallback: TfaCallback? = null,
        cookieJar: CookieJar? = null
    ): ApiProvider {
        return ApiProviderImpl(
            urlConfigProvider,
            accountProvider,
            walletInfoProvider,
            tfaCallback,
            cookieJar
        )
    }

    fun createApiProvider(
        urlConfigProvider: UrlConfigProvider,
        session: Session,
        tfaCallback: TfaCallback? = null,
        cookieJar: CookieJar? = null
    ): ApiProvider {
        return ApiProviderImpl(
            urlConfigProvider,
            session,
            session,
            tfaCallback,
            cookieJar
        )
    }
}