package io.tokend.template.features.signin.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import io.reactivex.rxkotlin.toSingle
import io.reactivex.schedulers.Schedulers
import io.tokend.template.di.providers.ApiProvider
import io.tokend.template.di.providers.RepositoryProvider
import io.tokend.template.features.keyvalue.model.KeyValueEntryRecord
import io.tokend.template.features.localaccount.logic.LocalAccountRetryDecryptor
import io.tokend.template.features.localaccount.model.LocalAccount
import io.tokend.template.features.userkey.logic.UserKeyProvider
import io.tokend.template.logic.Session
import io.tokend.template.logic.credentials.model.WalletInfoRecord
import io.tokend.template.logic.credentials.persistence.CredentialsPersistence
import io.tokend.template.util.cipher.DataCipher
import org.tokend.rx.extensions.toCompletable
import org.tokend.sdk.keyserver.KeyServer
import org.tokend.sdk.keyserver.models.KdfAttributes
import org.tokend.sdk.keyserver.models.LoginParams
import org.tokend.sdk.keyserver.models.SignerData
import org.tokend.sdk.utils.extentions.isConflict
import org.tokend.wallet.Account
import retrofit2.HttpException

/**
 * Performs sign in with given [Account]:
 * requests authorization result for given [Account],
 * sets up [Session],
 * clears [CredentialsPersistence].
 *
 * @see PostSignInManager
 */
class SignInWithLocalAccountUseCase(
    accountCipher: DataCipher,
    userKeyProvider: UserKeyProvider,
    private val session: Session,
    private val credentialsPersistence: CredentialsPersistence?,
    private val apiProvider: ApiProvider,
    private val repositoryProvider: RepositoryProvider,
    private val connectionStateProvider: (() -> Boolean)?,
    private val postSignInActions: (() -> Completable)?
) {
    private val isOnline: Boolean
        get() = connectionStateProvider?.invoke() ?: true

    private val accountDecryptor = LocalAccountRetryDecryptor(userKeyProvider, accountCipher)
    private val localAccountRepository = repositoryProvider.localAccount

    private lateinit var localAccount: LocalAccount
    private var defaultSignerRole: Long = 0
    private lateinit var account: Account
    private lateinit var walletInfo: WalletInfoRecord

    fun perform(): Completable {
        return getLocalAccount()
            .doOnSuccess { localAccount ->
                this.localAccount = localAccount
            }
            .flatMap {
                decryptLocalAccount()
            }
            .flatMap {
                getDefaultSignerRoleIfOnline()
            }
            .doOnSuccess { defaultSignerRole ->
                this.defaultSignerRole = defaultSignerRole
            }
            .flatMap {
                ensureRemoteAccountExistsIfOnline()
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
            .presentAccount
            .toMaybe()
            .switchIfEmpty(Single.error(IllegalStateException("There is no local account in the repository")))
    }

    private fun decryptLocalAccount(): Single<LocalAccount> {
        return accountDecryptor.decrypt(localAccount)
    }

    private fun getDefaultSignerRoleIfOnline(): Single<Long> {
        if (!isOnline) {
            return Single.just(0)
        }

        return repositoryProvider
            .keyValueEntries
            .ensureEntries(listOf(KeyServer.DEFAULT_SIGNER_ROLE_KEY_VALUE_KEY))
            .map {
                it[KeyServer.DEFAULT_SIGNER_ROLE_KEY_VALUE_KEY] as KeyValueEntryRecord.Number
            }
            .map(KeyValueEntryRecord.Number::value)
    }

    private fun ensureRemoteAccountExistsIfOnline(): Single<Boolean> {
        if (!isOnline) {
            return Single.just(false)
        }

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

    private fun getWalletInfo(): Single<WalletInfoRecord> {
        val accountId = account.accountId

        return WalletInfoRecord(
            walletId = "",
            accountId = accountId,
            login = accountId.substring(0..3) + "..." +
                    accountId.substring(accountId.length - 4, accountId.length),
            loginParams = LoginParams(
                "", 0,
                KdfAttributes("", 0, 0, 0, 0, byteArrayOf())
            ),
            seeds = emptyList()
        ).toSingle()
    }

    private fun updateProviders(): Single<Boolean> {
        SignInUseCase.updateProviders(
            walletInfo = walletInfo,
            accounts = arrayListOf(account),
            password = charArrayOf(),
            session = session,
            credentialsPersistence = null,
            walletInfoPersistence = null,
            signInMethod = SignInMethod.LOCAL_ACCOUNT
        )
        credentialsPersistence?.clear(false)

        return Single.just(true)
    }

    private fun performPostSignIn(): Single<Boolean> {
        return postSignInActions
            ?.invoke()
            ?.toSingleDefault(true)
            ?: Single.just(false)
    }
}