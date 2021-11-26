package io.tokend.template.logic.credentials.persistence

import android.content.SharedPreferences
import io.tokend.template.data.storage.persistence.SecureStorage
import io.tokend.template.logic.credentials.model.WalletInfoRecord
import org.tokend.crypto.ecdsa.erase
import org.tokend.sdk.factory.JsonApiTools
import org.tokend.wallet.Account
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * Represents secure walletInfo storage based on SharedPreferences.
 */
class WalletInfoPersistenceImpl(
    preferences: SharedPreferences
) : WalletInfoPersistence {
    private val secureStorage = SecureStorage(preferences)

    override fun saveWalletInfo(
        walletInfo: WalletInfoRecord,
        password: CharArray
    ) {
        // Will be serialized without accounts.
        val safeWalletInfoBytes =
            JsonApiTools.objectMapper.writeValueAsBytes(walletInfo)
        val accountsBytes = serializeAccounts(walletInfo.accounts)

        secureStorage.saveWithPassword(
            data = accountsBytes,
            key = getSeedsKeyKey(walletInfo.walletId),
            password = password
        )
        secureStorage.saveWithPassword(
            data = safeWalletInfoBytes,
            key = getWalletInfoKey(walletInfo.login),
            password = password
        )
    }

    override fun loadWalletInfo(login: String, password: CharArray): WalletInfoRecord? {
        val safeWalletInfoBytes = secureStorage.loadWithPassword(
            key = getWalletInfoKey(login),
            password = password
        ) ?: return null

        val walletInfo = JsonApiTools.objectMapper.readValue(
            safeWalletInfoBytes,
            WalletInfoRecord::class.java
        )

        val accounts = secureStorage.loadWithPassword(
            key = getSeedsKeyKey(walletInfo.walletId),
            password = password
        )?.let(this::deserializeAccounts) ?: return null

        walletInfo.accounts = accounts

        return walletInfo
    }

    override fun clearWalletInfo(login: String) {
        secureStorage.clear(getWalletInfoKey(login))
    }

    private fun getWalletInfoKey(login: String): String {
        return WALLET_INFO_KEY_PREFIX + "_" + login.hashCode()
    }

    private fun getSeedsKeyKey(walletId: String): String {
        return SEEDS_KEY_PREFIX + "_" + walletId.hashCode()
    }

    private fun serializeAccounts(accounts: List<Account>): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        DataOutputStream(byteArrayOutputStream).apply {
            use {
                writeByte(ACCOUNTS_SERIALIZATION_VERSION)
                accounts.forEach { account ->
                    writeInt(account.secretSeed.size)
                    account.secretSeed.forEach { writeChar(it.toInt()) }
                }
            }
        }

        return byteArrayOutputStream.toByteArray()
    }

    private fun deserializeAccounts(serialized: ByteArray): List<Account> {
        val result = mutableListOf<CharArray>()
        val byteArrayInputStream = ByteArrayInputStream(serialized)
        DataInputStream(byteArrayInputStream).apply {
            use {
                require(readByte() == ACCOUNTS_SERIALIZATION_VERSION.toByte()) {
                    "Unknown accounts serialization version"
                }

                while (available() > 0) {
                    val buffer = CharArray(readInt())
                    (buffer.indices).forEach { i ->
                        buffer[i] = readChar()
                    }
                    result.add(buffer)
                }
            }
        }

        return result
            .map { seed ->
                Account.fromSecretSeed(seed)
                    .also { seed.erase() }
            }
    }

    companion object {
        private const val ACCOUNTS_SERIALIZATION_VERSION = 1
        private const val SEEDS_KEY_PREFIX = "(◕‿◕✿)"
        private const val WALLET_INFO_KEY_PREFIX = "ಠ_ಠ"
    }
}