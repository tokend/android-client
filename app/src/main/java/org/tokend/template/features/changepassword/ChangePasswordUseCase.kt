package org.tokend.template.features.changepassword

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.functions.Function3
import io.reactivex.rxkotlin.toMaybe
import org.tokend.rx.extensions.randomSingle
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.keyserver.KeyServer
import org.tokend.sdk.keyserver.models.SignerData
import org.tokend.template.di.providers.AccountProvider
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.RepositoryProvider
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.template.features.keyvalue.model.KeyValueEntryRecord
import org.tokend.template.logic.credentials.model.WalletInfoRecord
import org.tokend.template.logic.credentials.persistence.CredentialsPersistence
import org.tokend.template.logic.credentials.persistence.WalletInfoPersistence
import org.tokend.template.logic.Session
import org.tokend.wallet.Account
import org.tokend.wallet.NetworkParams

/**
 * Changes user's password:
 * updates wallet keychain data and account signers.
 * [accountProvider] and [walletInfoProvider] will be used to obtain
 * actual wallet info and will be updated with the new info on complete
 */
class ChangePasswordUseCase(
    private val newPassword: CharArray,
    private val apiProvider: ApiProvider,
    private val accountProvider: AccountProvider,
    private val walletInfoProvider: WalletInfoProvider,
    private val repositoryProvider: RepositoryProvider,
    private val credentialsPersistence: CredentialsPersistence?,
    private val walletInfoPersistence: WalletInfoPersistence?,
    private val session: Session
) {
    private lateinit var currentWalletInfo: WalletInfoRecord
    private lateinit var currentAccount: Account
    private lateinit var newAccount: Account
    private var defaultSignerRole: Long = 0L
    private lateinit var currentSigners: List<SignerData>
    private lateinit var networkParams: NetworkParams
    private lateinit var newWalletInfo: WalletInfoRecord

    fun perform(): Completable {
        return getCurrentWalletInfo()
            .doOnSuccess { currentWalletInfo ->
                this.currentWalletInfo = currentWalletInfo
            }
            .flatMap {
                getCurrentAccount()
            }
            .doOnSuccess { currentAccount ->
                this.currentAccount = currentAccount
            }
            .flatMap {
                generateNewAccount()
            }
            .doOnSuccess { newAccount ->
                this.newAccount = newAccount
            }
            .flatMap {
                Single.zip(
                    getDefaultSignerRole(),
                    getCurrentSigners(),
                    getNetworkParams(),
                    Function3 { defaultSignerRole: Long,
                                currentSigners: List<SignerData>,
                                networkParams: NetworkParams ->
                        this.defaultSignerRole = defaultSignerRole
                        this.currentSigners = currentSigners
                        this.networkParams = networkParams
                        true
                    }
                )
            }
            .flatMap {
                updateWallet()
            }
            .doOnSuccess { newWalletInfo ->
                this.newWalletInfo = newWalletInfo
            }
            .doOnSuccess {
                updateProviders()
            }
            .ignoreElement()
    }

    private fun getCurrentWalletInfo(): Single<WalletInfoRecord> {
        return walletInfoProvider.getWalletInfo()
            .toMaybe()
            .switchIfEmpty(Single.error(IllegalStateException("No wallet info found")))
    }

    private fun getCurrentAccount(): Single<Account> {
        return accountProvider.getAccount()
            .toMaybe()
            .switchIfEmpty(Single.error(IllegalStateException("No account found")))
    }

    private fun generateNewAccount(): Single<Account> {
        return Account.randomSingle()
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

    private fun getCurrentSigners(): Single<List<SignerData>> {
        return apiProvider.getApi()
            .v3
            .signers
            .get(currentWalletInfo.accountId)
            .map { signers ->
                signers.map { SignerData(it) }
            }
            .toSingle()
    }

    private fun getNetworkParams(): Single<NetworkParams> {
        return repositoryProvider
            .systemInfo()
            .getNetworkParams()
    }

    private fun updateWallet(): Single<WalletInfoRecord> {
        val signedApi = apiProvider.getSignedApi()
            ?: return Single.error(IllegalStateException("No signed API instance found"))

        return KeyServer(signedApi.wallets)
            .updateWalletPassword(
                currentWalletInfo = currentWalletInfo.toSdkWalletInfo(session.login),
                currentAccount = currentAccount,
                currentSigners = currentSigners,
                defaultSignerRole = defaultSignerRole,
                newAccount = newAccount,
                newPassword = newPassword,
                networkParams = networkParams
            )
            .toSingle()
            .map {
                WalletInfoRecord(it)
            }
    }

    private fun updateProviders() {
        walletInfoProvider.setWalletInfo(newWalletInfo)
        val accountsList = accountProvider.getAccounts().toMutableList()
        accountsList.remove(currentAccount)
        accountsList.add(newAccount)
        accountProvider.setAccounts(accountsList.toList())

        // Update in persistent storage
        credentialsPersistence?.saveCredentials(session.login, newPassword)
        walletInfoPersistence?.saveWalletInfo(newWalletInfo, session.login, newPassword)
        newWalletInfo.eraseSeeds()
    }
}