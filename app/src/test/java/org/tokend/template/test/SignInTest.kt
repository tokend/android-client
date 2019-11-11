package org.tokend.template.test

import io.reactivex.Maybe
import io.reactivex.rxkotlin.toMaybe
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.tokend.sdk.factory.JsonApiToolsProvider
import org.tokend.sdk.keyserver.models.KdfAttributes
import org.tokend.sdk.keyserver.models.LoginParams
import org.tokend.sdk.keyserver.models.WalletInfo
import org.tokend.template.di.providers.*
import org.tokend.template.features.localaccount.model.LocalAccount
import org.tokend.template.features.localaccount.storage.LocalAccountPersistor
import org.tokend.template.features.signin.logic.PostSignInManager
import org.tokend.template.features.signin.logic.SignInUseCase
import org.tokend.template.features.signin.logic.SignInWithLocalAccountUseCase
import org.tokend.template.features.userkey.logic.UserKeyProvider
import org.tokend.template.logic.Session
import org.tokend.template.logic.credentials.persistence.CredentialsPersistor
import org.tokend.template.util.cipher.Aes256GcmDataCipher
import org.tokend.wallet.Account

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SignInTest {
    @Test
    fun aRegularSignIn() {
        val urlConfigProvider = Util.getUrlConfigProvider()
        val session = Session(
                WalletInfoProviderFactory().createWalletInfoProvider(),
                AccountProviderFactory().createAccountProvider()
        )
        val apiProvider = ApiProviderFactory().createApiProvider(urlConfigProvider, session)

        val email = Util.getEmail()
        val password = Config.DEFAULT_PASSWORD

        val (walletData, rootAccount)
                = apiProvider.getKeyServer()
                .createAndSaveWallet(email, password, apiProvider.getApi().v3.keyValue)
                .execute().get()

        println("Email is $email")

        val repositoryProvider = RepositoryProviderImpl(apiProvider, session, urlConfigProvider,
                JsonApiToolsProvider.getObjectMapper())

        val credentialsPersistor = getDummyCredentialsStorage()

        val useCase = SignInUseCase(
                email,
                password,
                apiProvider.getKeyServer(),
                session,
                credentialsPersistor,
                PostSignInManager(repositoryProvider)
        )

        useCase.perform().blockingAwait()

        Assert.assertEquals("WalletInfoProvider must hold an actual wallet data",
                walletData.attributes.accountId, session.getWalletInfo()!!.accountId)
        Assert.assertArrayEquals("AccountProvider must hold an actual account",
                rootAccount.secretSeed, session.getAccount()?.secretSeed)
        Assert.assertEquals("Credentials persistor must hold actual email",
                email.toLowerCase(), credentialsPersistor.getSavedEmail()?.toLowerCase())
        Assert.assertArrayEquals("Credentials persistor must hold actual password",
                password, credentialsPersistor.getSavedPassword())

        checkRepositories(repositoryProvider)
    }

    @Test
    fun bFirstSignInWithLocalAccount() {
        val account = Account.random()

        val urlConfigProvider = Util.getUrlConfigProvider()
        val session = Session(
                WalletInfoProviderFactory().createWalletInfoProvider(),
                AccountProviderFactory().createAccountProvider()
        )
        val apiProvider = ApiProviderFactory().createApiProvider(urlConfigProvider, session)

        val repositoryProvider = RepositoryProviderImpl(apiProvider, session, urlConfigProvider,
                JsonApiToolsProvider.getObjectMapper(),
                localAccountPersistor = getDummyLocalAccountsStorage())

        val userKey = "0000".toCharArray()
        val cipher = Aes256GcmDataCipher()
        val localAccountRepository = repositoryProvider.localAccount()
        localAccountRepository.useAccount(LocalAccount.fromSecretSeed(
                account.secretSeed!!, cipher, userKey
        ))
        val userKeyProvider = object : UserKeyProvider {
            override fun getUserKey(isRetry: Boolean): Maybe<CharArray> {
                return userKey.toMaybe()
            }
        }

        val credentialsPersistor = getDummyCredentialsStorage().apply {
            saveCredentials(WalletInfo("", "", "", charArrayOf(),
                    LoginParams("", 0, KdfAttributes("", 0, 0, 0, 0, byteArrayOf()))),
                    charArrayOf())
        }

        val useCase = SignInWithLocalAccountUseCase(
                accountCipher = cipher,
                userKeyProvider = userKeyProvider,
                session = session,
                credentialsPersistor = credentialsPersistor,
                repositoryProvider = repositoryProvider,
                apiProvider = apiProvider,
                postSignInManager = PostSignInManager(repositoryProvider)
        )

        useCase.perform().blockingAwait()

        try {
            apiProvider.getApi()
                    .v3
                    .accounts
                    .getById(account.accountId)
                    .execute()
                    .get()
        } catch (e: Exception) {
            Assert.fail("Remote account must be created")
        }

        Assert.assertEquals("WalletInfoProvider must hold wallet data with actual account ID",
                account.accountId, session.getWalletInfo()!!.accountId)
        Assert.assertArrayEquals("AccountProvider must hold an actual account",
                account.secretSeed, session.getAccount()?.secretSeed)
        Assert.assertFalse("Credentials persistor must be cleaned",
                credentialsPersistor.hasSimpleCredentials() || credentialsPersistor.getSavedEmail() != null
        )

        checkRepositories(repositoryProvider)
    }

    @Test
    fun cSecondSignInWithLocalAccount() {
        val account = Account.random()

        val urlConfigProvider = Util.getUrlConfigProvider()
        val session = Session(
                WalletInfoProviderFactory().createWalletInfoProvider(),
                AccountProviderFactory().createAccountProvider()
        )
        val apiProvider = ApiProviderFactory().createApiProvider(urlConfigProvider, session)

        val repositoryProvider = RepositoryProviderImpl(apiProvider, session, urlConfigProvider,
                JsonApiToolsProvider.getObjectMapper(),
                localAccountPersistor = getDummyLocalAccountsStorage())

        val userKey = "0000".toCharArray()
        val cipher = Aes256GcmDataCipher()
        val localAccountRepository = repositoryProvider.localAccount()
        localAccountRepository.useAccount(LocalAccount.fromSecretSeed(
                account.secretSeed!!, cipher, userKey
        ))
        val userKeyProvider = object : UserKeyProvider {
            override fun getUserKey(isRetry: Boolean): Maybe<CharArray> {
                return userKey.toMaybe()
            }
        }

        val useCase = SignInWithLocalAccountUseCase(
                accountCipher = cipher,
                userKeyProvider = userKeyProvider,
                session = session,
                credentialsPersistor = null,
                repositoryProvider = repositoryProvider,
                apiProvider = apiProvider,
                postSignInManager = PostSignInManager(repositoryProvider)
        )

        useCase.perform().blockingAwait()

        try {
            useCase.perform().blockingAwait()
        } catch (e: Exception) {
            e.printStackTrace()
            Assert.fail("Second sign in with local account must be as successful as the first one")
        }
    }

    private fun getDummyCredentialsStorage() = object : CredentialsPersistor {
        private var walletInfo: WalletInfo? = null
        private var password: CharArray? = null

        override fun saveCredentials(credentials: WalletInfo, password: CharArray) {
            this.walletInfo = credentials
            this.password = password
        }

        override fun getSavedEmail(): String?=walletInfo?.email

        override fun hasSavedPassword(): Boolean = password != null

        override fun getSavedPassword(): CharArray? = password

        override fun loadCredentials(password: CharArray): WalletInfo? =
                walletInfo.takeIf { this.password?.contentEquals(password) == true }

        override fun clear(keepEmail: Boolean) {
            this.walletInfo = null
            this.password = null
        }
    }

    private fun getDummyLocalAccountsStorage() = object : LocalAccountPersistor {
        private var mAccount: LocalAccount? = null

        override fun load(): LocalAccount? = mAccount

        override fun save(localAccount: LocalAccount) {
            mAccount = localAccount
        }

        override fun clear() {
            mAccount = null
        }
    }

    private fun checkRepositories(repositoryProvider: RepositoryProvider) {
        Assert.assertTrue("Balances repository must be updated after sign in",
                repositoryProvider.balances().isFresh)
        Assert.assertTrue("Account repository must be updated after sign in",
                repositoryProvider.account().isFresh)
    }
}