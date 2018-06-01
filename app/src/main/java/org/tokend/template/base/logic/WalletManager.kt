package org.tokend.template.base.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import io.reactivex.schedulers.Schedulers
import org.tokend.sdk.api.models.WalletData
import org.tokend.sdk.api.models.WalletRelation
import org.tokend.sdk.keyserver.KeyStorage
import org.tokend.sdk.keyserver.models.KdfAttributes
import org.tokend.sdk.keyserver.models.LoginParamsResponse
import org.tokend.sdk.keyserver.models.WalletInfo
import org.tokend.wallet.Account

class WalletManager(
        private val keyStorage: KeyStorage
) {
    // region API interaction
    fun getWalletInfo(email: String, password: CharArray,
                      isRecovery: Boolean = false): Single<WalletInfo> {
        return {
            keyStorage.getWalletInfo(email, password, isRecovery)
        }.toSingle()
    }

    fun saveWallet(walletData: WalletData): Completable {
        return {
            keyStorage.saveWallet(walletData)
        }.toSingle().toCompletable()
    }

    fun updateWallet(walletId: String, walletData: WalletData): Completable {
        return {
            keyStorage.updateWallet(walletId, walletData)
        }.toSingle().toCompletable()
    }
    // endregion

    companion object {
        // region Creation
        /**
         * @return Fully armed and ready to go [WalletData].
         */
        fun createWallet(email: String,
                         walletId: String,
                         walletKey: ByteArray,
                         rootAccount: Account,
                         recoveryAccount: Account,
                         loginParams: LoginParamsResponse): Single<WalletData> {
            return {
                val derivationSalt = loginParams.kdfAttributes.salt
                val rootSeed = rootAccount.secretSeed
                        ?: throw IllegalStateException("Provided account has no private key")

                // Base wallet has only KDF relation, we need more.
                val wallet = KeyStorage.createBaseWallet(email, derivationSalt, walletId, walletKey,
                                rootSeed, rootAccount.accountId, loginParams.id)

                // Add recovery relation.
                // Recovery seed must be encrypted with itself.
                val recoverySeed = recoveryAccount.secretSeed
                        ?: throw IllegalStateException("Provided recovery account has no private key")
                val (recoveryWalletId, recoveryKey) =
                        deriveKeys(email, recoverySeed, loginParams.kdfAttributes).blockingGet()
                val encryptedRecovery = KeyStorage.encryptWalletKey(email, recoverySeed,
                        recoveryAccount.accountId, recoveryKey, derivationSalt)
                wallet.addRelation(WalletRelation(WalletRelation.RELATION_RECOVERY,
                        WalletRelation.RELATION_RECOVERY, recoveryWalletId,
                        recoveryAccount.accountId, encryptedRecovery))

                // Add password factor relation.
                val passwordFactorAccount = Account.random()
                val passwordFactorSeed = passwordFactorAccount.secretSeed!!
                val encryptedPasswordFactor = KeyStorage.encryptWalletKey(email, passwordFactorSeed,
                        passwordFactorAccount.accountId, walletKey, derivationSalt)
                wallet.addRelation(WalletRelation(WalletRelation.RELATION_PASSWORD_FACTOR,
                        WalletRelation.RELATION_PASSWORD, walletId,
                        passwordFactorAccount.accountId, encryptedPasswordFactor))

                wallet
            }.toSingle().subscribeOn(Schedulers.newThread())
        }

        /**
         * @return [Pair] with the HEX-encoded wallet ID as a first element
         * and the wallet key as a second.
         */
        fun deriveKeys(email: String, password: CharArray, kdfAttributes: KdfAttributes)
                : Single<Pair<String, ByteArray>> {
            return {
                Pair(
                        KeyStorage.getWalletIdHex(email, password, kdfAttributes),
                        KeyStorage.getWalletKey(email, password, kdfAttributes)
                )
            }.toSingle().subscribeOn(Schedulers.newThread())
        }
        // endregion
    }
}