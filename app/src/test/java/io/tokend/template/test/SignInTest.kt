package io.tokend.template.test

import io.reactivex.Maybe
import io.reactivex.rxkotlin.toMaybe
import io.tokend.template.data.storage.persistence.MemoryOnlyObjectPersistence
import io.tokend.template.di.providers.*
import io.tokend.template.features.localaccount.model.LocalAccount
import io.tokend.template.features.signin.logic.PostSignInManager
import io.tokend.template.features.signin.logic.SignInUseCase
import io.tokend.template.features.signin.logic.SignInWithLocalAccountUseCase
import io.tokend.template.features.userkey.logic.UserKeyProvider
import io.tokend.template.logic.Session
import io.tokend.template.logic.credentials.model.WalletInfoRecord
import io.tokend.template.logic.credentials.persistence.CredentialsPersistence
import io.tokend.template.logic.credentials.persistence.WalletInfoPersistence
import io.tokend.template.util.cipher.Aes256GcmDataCipher
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.tokend.sdk.factory.JsonApiToolsProvider
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

        val repositoryProvider = RepositoryProviderImpl(
            apiProvider, session, urlConfigProvider,
            JsonApiToolsProvider.getObjectMapper()
        )

        val credentialsPersistor = getDummyCredentialsStorage()
        val walletInfoPersistor = getDummyWalletInfoStorage()

        val useCase = SignInUseCase(
            email,
            password,
            apiProvider.getKeyServer(),
            session,
            credentialsPersistor,
            walletInfoPersistor,
            PostSignInManager(repositoryProvider)::doPostSignIn
        )

        useCase.perform().blockingAwait()

        Assert.assertEquals(
            "WalletInfoProvider must hold an actual wallet data",
            walletData.attributes.accountId, session.getWalletInfo()!!.accountId
        )
        Assert.assertArrayEquals(
            "AccountProvider must hold an actual account",
            rootAccount.secretSeed, session.getAccount()?.secretSeed
        )
        Assert.assertNotEquals(
            "WalletInfo must be saved for the actual email",
            walletInfoPersistor.loadWalletInfo(email.toLowerCase(), password), null
        )
        Assert.assertEquals(
            "Credentials persistor must hold actual email",
            email.toLowerCase(), credentialsPersistor.getSavedLogin()?.toLowerCase()
        )
        Assert.assertArrayEquals(
            "Credentials persistor must hold actual password",
            password, credentialsPersistor.getSavedPassword()
        )

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

        val repositoryProvider = RepositoryProviderImpl(
            apiProvider, session, urlConfigProvider,
            JsonApiToolsProvider.getObjectMapper(),
            localAccountPersistence = getDummyLocalAccountsStorage()
        )

        val userKey = "0000".toCharArray()
        val cipher = Aes256GcmDataCipher()
        val localAccountRepository = repositoryProvider.localAccount
        localAccountRepository.useAccount(
            LocalAccount.fromSecretSeed(
                account.secretSeed!!, cipher, userKey
            )
        )
        val userKeyProvider = object : UserKeyProvider {
            override fun getUserKey(isRetry: Boolean): Maybe<CharArray> {
                return userKey.toMaybe()
            }
        }

        val login = ""

        val dummyPassword = charArrayOf()
        val credentialsPersistor = getDummyCredentialsStorage().apply {
            saveCredentials(login, dummyPassword)
        }

        val useCase = SignInWithLocalAccountUseCase(
            accountCipher = cipher,
            userKeyProvider = userKeyProvider,
            session = session,
            credentialsPersistence = credentialsPersistor,
            repositoryProvider = repositoryProvider,
            apiProvider = apiProvider,
            connectionStateProvider = null,
            postSignInActions = PostSignInManager(repositoryProvider)::doPostSignIn
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

        Assert.assertEquals(
            "WalletInfoProvider must hold wallet data with actual account ID",
            account.accountId, session.getWalletInfo()!!.accountId
        )
        Assert.assertArrayEquals(
            "AccountProvider must hold an actual account",
            account.secretSeed, session.getAccount()?.secretSeed
        )
        Assert.assertFalse(
            "Credentials persistor must be cleaned",
            credentialsPersistor.hasCredentials() || credentialsPersistor.getSavedLogin() != null
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

        val repositoryProvider = RepositoryProviderImpl(
            apiProvider, session, urlConfigProvider,
            JsonApiToolsProvider.getObjectMapper(),
            localAccountPersistence = getDummyLocalAccountsStorage()
        )

        val userKey = "0000".toCharArray()
        val cipher = Aes256GcmDataCipher()
        val localAccountRepository = repositoryProvider.localAccount
        localAccountRepository.useAccount(
            LocalAccount.fromSecretSeed(
                account.secretSeed!!, cipher, userKey
            )
        )
        val userKeyProvider = object : UserKeyProvider {
            override fun getUserKey(isRetry: Boolean): Maybe<CharArray> {
                return userKey.toMaybe()
            }
        }

        val useCase = SignInWithLocalAccountUseCase(
            accountCipher = cipher,
            userKeyProvider = userKeyProvider,
            session = session,
            credentialsPersistence = null,
            repositoryProvider = repositoryProvider,
            apiProvider = apiProvider,
            connectionStateProvider = null,
            postSignInActions = PostSignInManager(repositoryProvider)::doPostSignIn
        )

        useCase.perform().blockingAwait()

        try {
            useCase.perform().blockingAwait()
        } catch (e: Exception) {
            e.printStackTrace()
            Assert.fail("Second sign in with local account must be as successful as the first one")
        }
    }

    private fun getDummyCredentialsStorage() = object : CredentialsPersistence {
        private var email: String? = null
        private var password: CharArray? = null

        override fun saveCredentials(login: String, password: CharArray) {
            this.email = login
            this.password = password
        }

        override fun getSavedLogin(): String? = email

        override fun hasSavedPassword(): Boolean = password != null

        override fun getSavedPassword(): CharArray? = password

        override fun clear(keepLogin: Boolean) {
            this.email = null
            this.password = null
        }
    }

    private fun getDummyWalletInfoStorage() = object : WalletInfoPersistence {
        private var walletInfo: WalletInfoRecord? = null
        private var password: CharArray? = null

        override fun saveWalletInfo(
            walletInfo: WalletInfoRecord,
            password: CharArray
        ) {
            this.walletInfo = walletInfo
            this.password = password
        }

        override fun loadWalletInfo(login: String, password: CharArray): WalletInfoRecord? {
            return walletInfo.takeIf { this.password?.contentEquals(password) == true && this.walletInfo?.login == login }
        }

        override fun clearWalletInfo(login: String) {
            this.walletInfo = null
        }
    }

    private fun getDummyLocalAccountsStorage() = MemoryOnlyObjectPersistence<LocalAccount>()

    private fun checkRepositories(repositoryProvider: RepositoryProvider) {
        Assert.assertTrue(
            "Balances repository must be updated after sign in",
            repositoryProvider.balances.isFresh
        )
        Assert.assertTrue(
            "Account repository must be updated after sign in",
            repositoryProvider.account.isFresh
        )
    }
}