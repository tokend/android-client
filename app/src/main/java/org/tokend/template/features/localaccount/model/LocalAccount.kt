package org.tokend.template.features.localaccount.model

import org.tokend.kdf.ScryptKeyDerivation
import org.tokend.template.features.localaccount.model.LocalAccount.Companion.DEFAULT_ENTROPY_BYTES
import org.tokend.template.features.localaccount.model.LocalAccount.Companion.fromEntropy
import org.tokend.wallet.Account

/**
 * Local account is an [Account] stored on the device
 * without upload to Key server.
 * For easy import/export [LocalAccount] is derived not from 32 bytes salt
 * but from entropy of size [DEFAULT_ENTROPY_BYTES]
 *
 * @see fromEntropy
 */
class LocalAccount
constructor(
        val accountId: String,
        val entropy: ByteArray,
        account: Account?
) {
    private var mAccount: Account? = account
    val account
        get() = mAccount
                ?: getAccountFromEntropy(entropy)
                        .also { mAccount = it }

    companion object {
        private const val SCRYPT_N = 4096
        private const val SCRYPT_R = 8
        private const val SCRYPT_P = 1
        private val SCRYPT_PASSPHRASE = "entropy".toByteArray(Charsets.UTF_8)
        const val DEFAULT_ENTROPY_BYTES = 16

        fun fromEntropy(entropy: ByteArray): LocalAccount {
            val account = getAccountFromEntropy(entropy)
            return LocalAccount(
                    accountId = account.accountId,
                    entropy = entropy,
                    account = null
            )
        }

        fun getAccountFromEntropy(entropy: ByteArray): Account {
            val seed = deriveSecretSeedFromEntropy(entropy)
            return Account.fromSecretSeed(seed)
        }

        private fun deriveSecretSeedFromEntropy(entropy: ByteArray): ByteArray {
            return ScryptKeyDerivation(SCRYPT_N, SCRYPT_R, SCRYPT_P)
                    .derive(SCRYPT_PASSPHRASE, entropy, 32)
        }
    }
}