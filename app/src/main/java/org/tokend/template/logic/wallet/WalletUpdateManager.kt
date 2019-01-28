package org.tokend.template.logic.wallet

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import org.tokend.rx.extensions.createWalletSingle
import org.tokend.rx.extensions.updateWalletCompletable
import org.tokend.sdk.keyserver.KeyServer
import org.tokend.sdk.keyserver.WalletKeyDerivation
import org.tokend.sdk.keyserver.models.WalletCreateResult
import org.tokend.sdk.keyserver.models.WalletData
import org.tokend.sdk.keyserver.models.WalletInfo
import org.tokend.template.data.repository.SystemInfoRepository
import org.tokend.template.di.providers.AccountProvider
import org.tokend.template.di.providers.ApiProvider
import org.tokend.template.di.providers.WalletInfoProvider
import org.tokend.rx.extensions.toSingle
import org.tokend.template.logic.persistance.CredentialsPersistor
import org.tokend.wallet.Account
import org.tokend.wallet.NetworkParams
import org.tokend.wallet.Transaction
import retrofit2.HttpException
import java.net.HttpURLConnection

/**
 * Holds wallet update logic.
 *
 * @param credentialsPersistor if set credentials will be updated on wallet update complete
 */
class WalletUpdateManager(
        private val systemInfoRepository: SystemInfoRepository,
        private val credentialsPersistor: CredentialsPersistor? = null
) {
    /**
     * Updates keychain data and account signers in order to
     * change password (or recover it).
     * Also updates data in [accountProvider] and [walletInfoProvider] on complete.
     *
     * @param apiProvider must be able to provide API and KeyServer signed by active account signer
     * @param accountProvider must provide active account signer, well be updated on complete
     * @param walletInfoProvider must provide actual wallet info, will be updated on complete
     */
    fun updateWalletWithNewPassword(apiProvider: ApiProvider,
                                    accountProvider: AccountProvider,
                                    walletInfoProvider: WalletInfoProvider,
                                    newAccount: Account,
                                    newPassword: CharArray): Completable {
        val account = accountProvider.getAccount()
                ?: return Completable.error(IllegalStateException("Cannot obtain current account"))
        val wallet = walletInfoProvider.getWalletInfo()
                ?: return Completable.error(IllegalStateException("Cannot obtain current wallet"))
        val signedKeyServer = apiProvider.getSignedKeyServer()
                ?: return Completable.error(IllegalStateException("Cannot obtain signed KeyServer"))
        val signedApi = apiProvider.getSignedApi()
                ?: return Completable.error(IllegalStateException("Cannot obtain signed API"))

        val data = object {
            lateinit var newSalt: String
            lateinit var newWalletId: String
        }

        // Simultaneously get network params and original account signers.
        return Single.zip(
                systemInfoRepository.getNetworkParams(),

                signedApi.accounts.getSigners(wallet.accountId)
                        .toSingle()
                        .onErrorResumeNext { error ->
                            // When account is not yet exists return empty signers list.
                            return@onErrorResumeNext if (error is HttpException
                                    && error.code() == HttpURLConnection.HTTP_NOT_FOUND) {
                                Single.just(emptyList())
                            } else {
                                Single.error(error)
                            }
                        },

                BiFunction { t1: NetworkParams, t2: Collection<org.tokend.sdk.api.accounts.model.Account.Signer> ->
                    Pair(t1, t2)
                }
        )
                // Create new wallet with signers update transaction inside.
                .flatMap { (netParams, signers) ->
                    createWalletForPasswordChange(
                            networkParams = netParams,
                            currentWallet = wallet,
                            currentAccount = account,
                            currentSigners = signers,
                            newAccount = newAccount,
                            newPassword = newPassword
                    )
                }
                // Update current wallet with it.
                .flatMapCompletable { newWallet ->
                    data.newSalt = newWallet.attributes?.salt ?: ""
                    data.newWalletId = newWallet.id ?: ""
                    signedKeyServer.updateWalletCompletable(wallet.walletIdHex, newWallet)
                }
                // Update current credentials.
                .doOnComplete {
                    val currentWalletInfo = walletInfoProvider.getWalletInfo()
                            ?: return@doOnComplete

                    // Update in memory.
                    currentWalletInfo.apply {
                        loginParams.kdfAttributes.encodedSalt = data.newSalt
                        walletIdHex = data.newWalletId
                    }
                    accountProvider.setAccount(newAccount)

                    // Update in persistent storage.
                    val walletInfoToSave = currentWalletInfo.copy(
                            secretSeed = newAccount.secretSeed ?: CharArray(0)
                    )
                    credentialsPersistor?.saveCredentials(walletInfoToSave, newPassword)
                    walletInfoToSave.secretSeed.fill('0')
                }
    }

    // region Creation
    private fun createWalletForPasswordChange(networkParams: NetworkParams,
                                              currentWallet: WalletInfo,
                                              currentAccount: Account,
                                              currentSigners: Collection<org.tokend.sdk.api.accounts.model.Account.Signer>,
                                              newPassword: CharArray,
                                              newAccount: Account): Single<WalletData> {
        val loginParams = currentWallet.loginParams
        loginParams.kdfAttributes.salt = WalletKeyDerivation.generateKdfSalt()

        return Single.zip(
                KeyServer.createWalletSingle(
                        email = currentWallet.email,
                        password = newPassword,
                        rootAccount = newAccount,
                        recoveryAccount = Account.random(),
                        loginParams = loginParams
                ),

                createSignersUpdateTransaction(networkParams, currentWallet,
                        currentAccount, currentSigners, newAccount),

                BiFunction { t1: WalletCreateResult, t2: Transaction -> Pair(t1.walletData, t2) }
        )
                .map { (wallet, transaction) ->
                    wallet.addTransactionRelation(transaction)
                    wallet
                }
    }

    private fun createSignersUpdateTransaction(networkParams: NetworkParams,
                                               currentWallet: WalletInfo,
                                               currentAccount: Account,
                                               currentSigners: Collection<org.tokend.sdk.api.accounts.model.Account.Signer>,
                                               newAccount: Account): Single<Transaction> {
        return Single.defer {
            val transaction = KeyServer.createSignersUpdateTransaction(
                    networkParams,
                    currentWallet.accountId,
                    currentAccount,
                    currentSigners,
                    newAccount
            )

            Single.just(transaction)
        }.subscribeOn(Schedulers.newThread())
    }
    // endregion
}