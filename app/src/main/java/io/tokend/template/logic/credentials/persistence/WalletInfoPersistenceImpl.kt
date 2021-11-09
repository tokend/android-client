package io.tokend.template.logic.credentials.persistence

import android.content.SharedPreferences
import io.tokend.template.logic.credentials.model.WalletInfoRecord
import io.tokend.template.data.storage.persistence.SecureStorage
import org.tokend.sdk.factory.GsonFactory
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
        val safeWalletInfoBytes = GsonFactory().getBaseGson().toJson(walletInfo.withoutSeeds())
            .toByteArray(Charsets.UTF_8)
        val seedsBytes = serializeSeeds(walletInfo.seeds)

        secureStorage.saveWithPassword(
            data = seedsBytes,
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

        val walletInfo = GsonFactory().getBaseGson().fromJson(
            String(safeWalletInfoBytes, Charsets.UTF_8),
            WalletInfoRecord::class.java
        )

        val seeds = secureStorage.loadWithPassword(
            key = getSeedsKeyKey(walletInfo.walletId),
            password = password
        )?.let(this::deserializeSeeds) ?: return null

        walletInfo.seeds = seeds

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

    private fun serializeSeeds(seeds: List<CharArray>): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        DataOutputStream(byteArrayOutputStream).apply {
            use {
                writeByte(SEEDS_SERIALIZATION_VERSION)
                seeds.forEach { seed ->
                    writeInt(seed.size)
                    seed.forEach { writeChar(it.toInt()) }
                }
            }
        }
        return byteArrayOutputStream.toByteArray()
    }

    private fun deserializeSeeds(serialized: ByteArray): List<CharArray> {
        val result = mutableListOf<CharArray>()
        val byteArrayInputStream = ByteArrayInputStream(serialized)
        DataInputStream(byteArrayInputStream).apply {
            use {
                require(readByte() == SEEDS_SERIALIZATION_VERSION.toByte()) {
                    "Unknown seeds serialization version"
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
    }

    companion object {
        private const val SEEDS_SERIALIZATION_VERSION = 1
        private const val SEEDS_KEY_PREFIX = "(◕‿◕✿)"
        private const val WALLET_INFO_KEY_PREFIX = "ಠ_ಠ"
    }
}