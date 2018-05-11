package org.tokend.template.base.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.toSingle
import io.reactivex.schedulers.Schedulers
import org.tokend.sdk.api.ApiService
import org.tokend.sdk.api.models.WalletData
import org.tokend.sdk.api.requests.DataEntity
import org.tokend.sdk.api.responses.AccountResponse
import org.tokend.sdk.keyserver.KeyStorage
import org.tokend.sdk.keyserver.models.WalletInfo
import org.tokend.template.base.logic.di.providers.ApiProviderFactory
import org.tokend.template.base.logic.repository.SystemInfoRepository
import org.tokend.template.extensions.toSingle
import org.tokend.wallet.*
import org.tokend.wallet.xdr.Operation
import org.tokend.wallet.xdr.op_extensions.RemoveMasterKeyOp
import org.tokend.wallet.xdr.op_extensions.UpdateSignerOp
import retrofit2.HttpException
import java.net.HttpURLConnection

class WalletPasswordManager(
        private val systemInfoRepository: SystemInfoRepository
) {
    fun restore(email: String, recoverySeed: CharArray,
                newAccount: Account, newPassword: CharArray): Completable {
        val data = object {
            lateinit var account: Account
            lateinit var signedKeyStorage: KeyStorage
            lateinit var signedApi: ApiService
            lateinit var walletManager: WalletManager
            lateinit var wallet: WalletInfo
        }

        return createAccountFromSeed(recoverySeed)
                // Create API provider for recovery account.
                .map { account ->
                    data.account = account
                    ApiProviderFactory().createApiProvider(account)
                }
                // Create WalletManager signed with recovery account.
                .map { apiProvider ->
                    data.signedKeyStorage = apiProvider.getSignedKeyStorage()
                            ?: throw IllegalStateException("Cannot obtain signed KeyStorage")
                    data.signedApi = apiProvider.getSignedApi()
                            ?: throw IllegalStateException("Cannot obtain signed API")

                    WalletManager(data.signedKeyStorage)
                }
                // Get info for wallet to recover.
                .flatMap { walletManager ->
                    data.walletManager = walletManager
                    walletManager.getWalletInfo(email, recoverySeed, true)
                }
                // Simultaneously get network params and original account signers.
                .flatMap { wallet ->
                    data.wallet = wallet
                    Single.zip(
                            systemInfoRepository.getNetworkParams(),
                            data.signedApi.getAccountSigners(wallet.accountId)
                                    .toSingle()
                                    .map { it.signers ?: emptyList() }
                                    .onErrorResumeNext { error ->
                                        // When account is not yet exists return empty signers list.
                                        return@onErrorResumeNext if (error is HttpException
                                                && error.code() == HttpURLConnection.HTTP_NOT_FOUND) {
                                            Single.just(emptyList())
                                        } else {
                                            Single.error(error)
                                        }
                                    },

                            BiFunction { t1: NetworkParams, t2: Collection<AccountResponse.Signer> ->
                                Pair(t1, t2)
                            }
                    )
                }
                // Create new wallet with signers update transaction inside.
                .flatMap { (netParams, signers) ->
                    createWalletForPasswordChange(
                            networkParams = netParams,
                            currentWallet = data.wallet,
                            currentAccount = data.account,
                            currentSigners = signers,
                            newAccount = newAccount,
                            newPassword = newPassword
                    )
                }
                // Update current wallet with it.
                .flatMapCompletable { newWallet ->
                    data.walletManager.updateWallet(data.wallet.walletIdHex,
                            newWallet)
                }
    }

    // region Creation
    private fun createAccountFromSeed(seed: CharArray): Single<Account> {
        return {
            Account.fromSecretSeed(seed)
        }.toSingle().subscribeOn(Schedulers.computation())
    }

    private fun createWalletForPasswordChange(networkParams: NetworkParams,
                                              currentWallet: WalletInfo,
                                              currentAccount: Account,
                                              currentSigners: Collection<AccountResponse.Signer>,
                                              newPassword: CharArray,
                                              newAccount: Account): Single<WalletData> {
        return WalletManager.deriveKeys(currentWallet.email, newPassword,
                currentWallet.loginParams.kdfAttributes)

                .flatMap { (walletId, walletKey) ->
                    Single.zip(
                            WalletManager.createWallet(
                                    email = currentWallet.email,
                                    walletId = walletId,
                                    walletKey = walletKey,
                                    rootAccount = newAccount,
                                    recoveryAccount = Account.random(),
                                    loginParams = currentWallet.loginParams
                            ),

                            createSignersUpdateTransaction(networkParams, currentWallet,
                                    currentAccount, currentSigners, newAccount),

                            BiFunction { t1: WalletData, t2: Transaction -> Pair(t1, t2) }
                    )
                }

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
                                               currentSigners: Collection<AccountResponse.Signer>,
                                               newAccount: Account): Single<Transaction> {
        return Single.defer {
            val operationBodies = mutableListOf<Operation.OperationBody>()

            // Add new signer.
            val currentSigner =
                    currentSigners.find {
                        it.accountId == currentAccount.accountId
                    } ?: AccountResponse.Signer(currentWallet.accountId)

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
        }.subscribeOn(Schedulers.computation())
    }
    // endregion
}