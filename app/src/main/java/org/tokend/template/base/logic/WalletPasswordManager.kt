package org.tokend.template.base.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.toSingle
import io.reactivex.schedulers.Schedulers
import org.tokend.sdk.api.base.model.DataEntity
import org.tokend.sdk.keyserver.WalletBuilder
import org.tokend.sdk.keyserver.models.WalletData
import org.tokend.sdk.keyserver.models.WalletInfo
import org.tokend.sdk.utils.extentions.encodeBase64String
import org.tokend.template.base.logic.di.providers.*
import org.tokend.template.base.logic.persistance.CredentialsPersistor
import org.tokend.template.base.logic.repository.SystemInfoRepository
import org.tokend.template.extensions.toSingle
import org.tokend.wallet.*
import org.tokend.wallet.xdr.Operation
import org.tokend.wallet.xdr.op_extensions.RemoveMasterKeyOp
import org.tokend.wallet.xdr.op_extensions.UpdateSignerOp
import retrofit2.HttpException
import java.net.HttpURLConnection

class WalletPasswordManager(
        private val systemInfoRepository: SystemInfoRepository,
        private val urlConfigProvider: UrlConfigProvider
) {
    fun restore(email: String, recoverySeed: CharArray,
                newAccount: Account, newPassword: CharArray): Completable {
        val data = object {
            lateinit var accountProvider: AccountProvider
            lateinit var apiProvider: ApiProvider
        }

        return createAccountFromSeed(recoverySeed)
                // Create API provider for recovery account.
                .map { account ->
                    data.accountProvider = AccountProviderFactory().createAccountProvider(account)
                    ApiProviderFactory()
                            .createApiProvider(urlConfigProvider, data.accountProvider)
                }
                // Create WalletManager signed with recovery account.
                .map { apiProvider ->
                    data.apiProvider = apiProvider
                    val signedKeyStorage = apiProvider.getSignedKeyStorage()
                            ?: throw IllegalStateException("Cannot obtain signed KeyStorage")
                    WalletManager(signedKeyStorage)
                }
                // Get info for wallet to recover.
                .flatMap { walletManager ->
                    walletManager.getWalletInfo(email, recoverySeed, true)
                }
                // Update wallet with new password.
                .flatMapCompletable { wallet ->
                    updateWalletWithNewPassword(
                            data.apiProvider,
                            data.accountProvider,
                            WalletInfoProviderFactory().createWalletInfoProvider(wallet),
                            null,
                            newAccount,
                            newPassword
                    )
                }
    }

    fun changePassword(apiProvider: ApiProvider, accountProvider: AccountProvider,
                       walletInfoProvider: WalletInfoProvider,
                       credentialsPersistor: CredentialsPersistor,
                       newAccount: Account, newPassword: CharArray): Completable {
        return updateWalletWithNewPassword(apiProvider, accountProvider,
                walletInfoProvider, credentialsPersistor,
                newAccount, newPassword)
    }

    private fun updateWalletWithNewPassword(apiProvider: ApiProvider,
                                            accountProvider: AccountProvider,
                                            walletInfoProvider: WalletInfoProvider,
                                            credentialsPersistor: CredentialsPersistor?,
                                            newAccount: Account,
                                            newPassword: CharArray): Completable {
        val account = accountProvider.getAccount()
                ?: return Completable.error(IllegalStateException("Cannot obtain current account"))
        val wallet = walletInfoProvider.getWalletInfo()
                ?: return Completable.error(IllegalStateException("Cannot obtain current wallet"))
        val signedKeyStorage = apiProvider.getSignedKeyStorage()
                ?: return Completable.error(IllegalStateException("Cannot obtain signed KeyStorage"))
        val signedApi = apiProvider.getSignedApi()
                ?: return Completable.error(IllegalStateException("Cannot obtain signed API"))
        val walletManager = WalletManager(signedKeyStorage)

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
                    walletManager.updateWallet(wallet.walletIdHex, newWallet)
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
    private fun createAccountFromSeed(seed: CharArray): Single<Account> {
        return {
            Account.fromSecretSeed(seed)
        }.toSingle().subscribeOn(Schedulers.newThread())
    }

    private fun createWalletForPasswordChange(networkParams: NetworkParams,
                                              currentWallet: WalletInfo,
                                              currentAccount: Account,
                                              currentSigners: Collection<org.tokend.sdk.api.accounts.model.Account.Signer>,
                                              newPassword: CharArray,
                                              newAccount: Account): Single<WalletData> {
        val currentLoginParams = currentWallet.loginParams

        val newSalt = WalletBuilder.generateKdfSalt()
        val newLoginParams = currentLoginParams.copy(
                kdfAttributes = currentLoginParams.kdfAttributes.copy(
                        encodedSalt = newSalt.encodeBase64String()
                )
        )

        return Single.zip(
                WalletManager.createWallet(
                        email = currentWallet.email,
                        password = newPassword,
                        rootAccount = newAccount,
                        recoveryAccount = Account.random(),
                        loginParams = newLoginParams
                ),

                createSignersUpdateTransaction(networkParams, currentWallet,
                        currentAccount, currentSigners, newAccount),

                BiFunction { t1: WalletData, t2: Transaction -> Pair(t1, t2) }
        )
                .map { (wallet, transaction) ->
                    wallet.relationships["transaction"] =
                            DataEntity(hashMapOf(
                                    "attributes" to hashMapOf(
                                            "envelope" to transaction.getEnvelope().toBase64()
                                    ))
                            )
                    wallet
                }
    }

    private fun createSignersUpdateTransaction(networkParams: NetworkParams,
                                               currentWallet: WalletInfo,
                                               currentAccount: Account,
                                               currentSigners: Collection<org.tokend.sdk.api.accounts.model.Account.Signer>,
                                               newAccount: Account): Single<Transaction> {
        return Single.defer {
            val operationBodies = mutableListOf<Operation.OperationBody>()

            // Add new signer.
            val currentSigner =
                    currentSigners.find {
                        it.accountId == currentAccount.accountId
                    } ?: org.tokend.sdk.api.accounts.model.Account.Signer(currentWallet.accountId)

            operationBodies.add(
                    Operation.OperationBody.SetOptions(
                            UpdateSignerOp(
                                    newAccount.accountId,
                                    currentSigner.weight,
                                    currentSigner.type,
                                    currentSigner.identity
                            )
                    )
            )

            // Remove other signers.
            currentSigners
                    .sortedBy {
                        // Remove current signer lastly, otherwise tx will be failed.
                        it.accountId == currentAccount.accountId
                    }
                    .forEach {
                        if (it.accountId != newAccount.accountId) {
                            // Master key removal is specific.
                            if (it.accountId == currentWallet.accountId) {
                                operationBodies.add(
                                        Operation.OperationBody.SetOptions(
                                                RemoveMasterKeyOp()
                                        )
                                )
                            } else {
                                // Other keys can be removed by setting 0 weight.
                                operationBodies.add(
                                        Operation.OperationBody.SetOptions(
                                                UpdateSignerOp(it.accountId,
                                                        0, 1, it.identity)
                                        )
                                )
                            }
                        }
                    }

            val transaction =
                    TransactionBuilder(networkParams,
                            PublicKeyFactory.fromAccountId(currentWallet.accountId))
                            .apply {
                                operationBodies.forEach { operationBody ->
                                    addOperation(operationBody)
                                }
                            }
                            .build()

            transaction.addSignature(currentAccount)

            Single.just(transaction)
        }.subscribeOn(Schedulers.newThread())
    }
    // endregion
}