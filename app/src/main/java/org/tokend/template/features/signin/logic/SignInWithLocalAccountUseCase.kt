package org.tokend.template.features.signin.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import io.reactivex.rxkotlin.toSingle
import io.reactivex.schedulers.Schedulers
import org.tokend.rx.extensions.toCompletable
import org.tokend.sdk.keyserver.KeyServer
import org.tokend.sdk.keyserver.models.KdfAttributes
import org.tokend.sdk.keyserver.models.LoginParams
import org.tokend.sdk.keyserver.models.SignerData
import org.tokend.sdk.keyserver.models.WalletInfo
import org.tokend.sdk.utils.extentions.isConflict
import org.tokend.template.data.model.KeyValueEntryRecord
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.RepositoryProvider
import org.tokend.template.features.localaccount.logic.LocalAccountRetryDecryptor
import org.tokend.template.features.localaccount.model.LocalAccount
import org.tokend.template.features.userkey.logic.UserKeyProvider
import org.tokend.template.logic.Session
import org.tokend.template.logic.credentials.persistence.CredentialsPersistence
import org.tokend.template.util.cipher.DataCipher
import org.tokend.wallet.Account
import retrofit2.HttpException

/**
 * Performs sign in with given [Account]:
 * requests authorization result for given [Account],
 * sets up WalletInfoProvider and AccountProvider,
 * clears CredentialsPersistor.
 *
 * @param postSignInManager if set then [PostSignInManager.doPostSignIn] will be performed
 */
class SignInWithLocalAccountUseCase(
        accountCipher: DataCipher,
        userKeyProvider: UserKeyProvider,
        private val session: Session,
        private val credentialsPersistence: CredentialsPersistence?,
        private val apiProvider: ApiProvider,
        private val repositoryProvider: RepositoryProvider,
        private val postSignInManager: PostSignInManager?
) {
    private val accountDecryptor = LocalAccountRetryDecryptor(userKeyProvider, accountCipher)
    private val localAccountRepository = repositoryProvider.localAccount()

    private lateinit var localAccount: LocalAccount
    private var defaultSignerRole: Long = 0
    private lateinit var account: Account
    private lateinit var walletInfo: WalletInfo

    fun perform(): Completable {
        return getLocalAccount()
                .doOnSuccess { localAccount ->
                    this.localAccount = localAccount
                }
                .flatMap {
                    decryptLocalAccount()
                }
                .flatMap {
                    getDefaultSignerRole()
                }
                .doOnSuccess { defaultSignerRole ->
                    this.defaultSignerRole = defaultSignerRole
                }
                .flatMap {
                    ensureRemoteAccountExists()
                }
                .flatMap {
                    getAccount()
                }
                .doOnSuccess { account ->
                    this.account = account
                }
                .flatMap {
                    getWalletInfo()
                }
                .doOnSuccess { walletInfo ->
                    this.walletInfo = walletInfo
                }
                .flatMap {
                    updateProviders()
                }
                .flatMap {
                    performPostSignIn()
                }
                .ignoreElement()
    }

    private fun getLocalAccount(): Single<LocalAccount> {
        return localAccountRepository
                .item
                .toMaybe()
                .switchIfEmpty(Single.error(IllegalStateException("There is no local account in the repository")))
    }

    private fun decryptLocalAccount(): Single<LocalAccount> {
        return accountDecryptor.decrypt(localAccount)
    }

    private fun getDefaultSignerRole(): Single<Long> {
        return repositoryProvider
                .keyValueEntries()
                .ensureEntries(listOf(KeyServer.DEFAULT_SIGNER_ROLE_KEY_VALUE_KEY))
                .map {
                    it[KeyServer.DEFAULT_SIGNER_ROLE_KEY_VALUE_KEY] as KeyValueEntryRecord.Number
                }
                .map(KeyValueEntryRecord.Number::value)
    }

    private fun ensureRemoteAccountExists(): Single<Boolean> {
        return apiProvider.getApi()
                .accounts
                .createAccount(
                        accountId = localAccount.accountId,
                        signers = listOf(SignerData(localAccount.accountId, defaultSignerRole))
                )
                .toCompletable()
                .toSingleDefault(true)
                .onErrorResumeNext { error ->
                    if (error is HttpException && error.isConflict())
                        Single.just(true)
                    else
                        Single.error(error)
                }
    }

    private fun getAccount(): Single<Account> {
        return {
            localAccount.account
        }.toSingle().subscribeOn(Schedulers.computation())
    }

    private fun getWalletInfo(): Single<WalletInfo> {
        val accountId = account.accountId

        return WalletInfo(
                accountId = accountId,
                email = accountId.substring(0..3) + "..." +
                        accountId.substring(accountId.length - 4, accountId.length),
                secretSeed = charArrayOf(),
                walletIdHex = "",
                loginParams = LoginParams("", 0,
                        KdfAttributes("", 0, 0, 0, 0, byteArrayOf()))
        ).toSingle()
    }

    private fun updateProviders(): Single<Boolean> {
        session.setWalletInfo(walletInfo)
        credentialsPersistence?.clear(false)
        session.setAccount(account)
        session.signInMethod = SignInMethod.LOCAL_ACCOUNT

        return Single.just(true)
    }

    private fun performPostSignIn(): Single<Boolean> {
        return if (postSignInManager != null)
            postSignInManager
                    .doPostSignIn()
                    .toSingleDefault(true)
        else
            Single.just(false)
    }
}