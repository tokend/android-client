package org.tokend.template.features.localaccount.model

import org.tokend.crypto.ecdsa.erase
import org.tokend.kdf.ScryptKeyDerivation
import org.tokend.template.util.cipher.DataCipher
import org.tokend.template.features.localaccount.model.LocalAccount.Companion.DEFAULT_ENTROPY_BYTES
import org.tokend.template.features.localaccount.model.LocalAccount.Companion.fromEntropy
import org.tokend.wallet.Account
import org.tokend.wallet.Base32Check
import org.tokend.wallet.utils.toByteArray
import org.tokend.wallet.utils.toCharArray

/**
 * Local account is an [Account] stored on the device
 * without upload to Key server. It's encrypted with user key (PIN, password, etc.).
 * For easy import/export [LocalAccount] is derived not from 32 bytes salt
 * but from entropy of size [DEFAULT_ENTROPY_BYTES].
 *
 * @see fromEntropy
 */
class LocalAccount
private constructor(
        val accountId: String,
        secretSeed: CharArray?,
        entropy: ByteArray?,
        private val encryptedSource: EncryptedSource?
) {
    private sealed class EncryptedSource(val i: Byte,
                                         val encryptedData: ByteArray) {
        class Entropy(encryptedData: ByteArray) : EncryptedSource(I, encryptedData) {
            companion object {
                const val I: Byte = 0
            }
        }

        class SecretSeed(encryptedData: ByteArray) : EncryptedSource(I, encryptedData) {
            companion object {
                const val I: Byte = 1
            }
        }

        fun serialize(): ByteArray {
            return ByteArray(encryptedData.size + 1).apply {
                this[0] = i
                System.arraycopy(encryptedData, 0, this, 1, encryptedData.size)
            }
        }

        companion object {
            fun fromSerialized(serialized: ByteArray): EncryptedSource {
                val i = serialized[0]
                val data = serialized.sliceArray(1 until serialized.size)

                return when (i) {
                    Entropy.I -> Entropy(data)
                    SecretSeed.I -> SecretSeed(data)
                    else -> throw IllegalArgumentException("Unknown source type $i")
                }
            }
        }
    }

    private var mEntropy: ByteArray? = entropy
    private var mSecretSeed: CharArray? = secretSeed

    /**
     * true if account is decrypted or decryption is not required
     *
     * @see decrypt
     */
    val isDecrypted: Boolean
        get() = when (encryptedSource) {
            is EncryptedSource.Entropy -> mEntropy != null
            is EncryptedSource.SecretSeed -> mSecretSeed != null
            null -> true
        }

    /**
     * Entropy the account is derived from.
     *
     * Will be null if account was imported from secret seed.
     *
     * Requires decryption if account is not decrypted.
     *
     * @see isDecrypted
     * @see decrypt
     */
    val entropy: ByteArray?
        get() =
            if (encryptedSource is EncryptedSource.Entropy) {
                if (isDecrypted) {
                    mEntropy
                } else {
                    throw IllegalStateException("Account is not decrypted")
                }
            } else {
                mEntropy
            }

    /**
     * true if entropy can be obtained, does not matter if it's decrypted or not
     */
    val hasEntropy: Boolean
        get() = encryptedSource is EncryptedSource.Entropy || mEntropy != null

    /**
     * Secret seed of the account.
     *
     * Requires decryption if account is not decrypted.
     *
     * @see isDecrypted
     * @see decrypt
     */
    val secretSeed: CharArray
        get() = when (encryptedSource) {
            is EncryptedSource.SecretSeed -> {
                if (isDecrypted) {
                    mSecretSeed!!
                } else {
                    throw IllegalStateException("Account is not decrypted")
                }
            }
            else -> {
                val existingSeed = mSecretSeed
                val existingEntropy = entropy
                if (existingSeed != null) {
                    existingSeed
                } else if (existingEntropy != null) {
                    Base32Check.encodeSecretSeed(deriveSecretSeedFromEntropy(existingEntropy))
                            .also { mSecretSeed = it }
                } else {
                    throw IllegalStateException("Account has no encrypted source, entropy and seed")
                }
            }
        }

    private var mAccount: Account? = null
    val account
        get() = mAccount
                ?: Account.Companion.fromSecretSeed(secretSeed)
                        .also { mAccount = it }

    /**
     * @return serialized encrypted source data (entropy or seed)
     */
    fun serializeEncryptedSource(): ByteArray? {
        return encryptedSource?.serialize()
    }

    var isErased = false

    /**
     * Decrypts account source if it's encrypted
     */
    fun decrypt(cipher: DataCipher,
                userKey: CharArray) {
        val encryptionKey = deriveEncryptionKeyFromUserKey(userKey)
        when (encryptedSource) {
            is EncryptedSource.Entropy ->
                mEntropy = cipher.decrypt(encryptedSource.encryptedData, encryptionKey)
            is EncryptedSource.SecretSeed ->
                mSecretSeed = cipher.decrypt(encryptedSource.encryptedData, encryptionKey).toCharArray()
        }
    }

    companion object {
        private const val SCRYPT_N = 4096
        private const val SCRYPT_R = 8
        private const val SCRYPT_P = 1
        private val SCRYPT_PASSPHRASE = "entropy".toByteArray(Charsets.UTF_8)
        const val DEFAULT_ENTROPY_BYTES = 16

        /**
         * Creates encrypted account from given [entropy]
         */
        fun fromEntropy(entropy: ByteArray,
                        cipher: DataCipher,
                        userKey: CharArray): LocalAccount {
            val account = getAccountFromEntropy(entropy)
            val encryptionKey = deriveEncryptionKeyFromUserKey(userKey)
            val encryptedSource = EncryptedSource.Entropy(
                    cipher.encrypt(entropy, encryptionKey)
            )
            encryptionKey.erase()
            return LocalAccount(
                    accountId = account.accountId,
                    encryptedSource = encryptedSource,
                    entropy = entropy,
                    secretSeed = account.secretSeed!!
            )
        }

        /**
         * Creates encrypted account from given [secretSeed]
         */
        fun fromSecretSeed(secretSeed: CharArray,
                           cipher: DataCipher,
                           userKey: CharArray): LocalAccount {
            val account = Account.fromSecretSeed(secretSeed)
            val encryptionKey = deriveEncryptionKeyFromUserKey(userKey)
            val encryptedSource = EncryptedSource.SecretSeed(
                    cipher.encrypt(secretSeed.toByteArray(), encryptionKey)
            )
            encryptionKey.erase()
            return LocalAccount(
                    accountId = account.accountId,
                    secretSeed = secretSeed,
                    encryptedSource = encryptedSource,
                    entropy = null
            )
        }

        /**
         * Creates encrypted account from it's serialized encrypted source
         */
        fun fromSerializedEncryptedSource(accountId: String,
                                          serializedEncryptedSource: ByteArray): LocalAccount {
            val encryptedSource = EncryptedSource.fromSerialized(serializedEncryptedSource)
            return LocalAccount(
                    accountId = accountId,
                    encryptedSource = encryptedSource,
                    secretSeed = null,
                    entropy = null
            )
        }

        private fun getAccountFromEntropy(entropy: ByteArray): Account {
            val seed = deriveSecretSeedFromEntropy(entropy)
            return Account.fromSecretSeed(seed)
        }

        private fun deriveSecretSeedFromEntropy(entropy: ByteArray): ByteArray {
            return ScryptKeyDerivation(SCRYPT_N, SCRYPT_R, SCRYPT_P)
                    .derive(SCRYPT_PASSPHRASE, entropy, 32)
        }

        private fun deriveEncryptionKeyFromUserKey(userKey: CharArray): ByteArray {
            return ScryptKeyDerivation(SCRYPT_N, SCRYPT_R, SCRYPT_P)
                    .derive(SCRYPT_PASSPHRASE, userKey.toByteArray(), 32)
        }
    }
}