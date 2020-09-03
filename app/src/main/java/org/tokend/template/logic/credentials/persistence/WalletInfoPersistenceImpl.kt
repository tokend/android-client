package org.tokend.template.logic.credentials.persistence

import android.content.SharedPreferences
import org.tokend.sdk.factory.GsonFactory
import org.tokend.sdk.keyserver.models.WalletInfo
import org.tokend.template.logic.persistence.SecureStorage
import org.tokend.wallet.utils.toByteArray
import org.tokend.wallet.utils.toCharArray

/**
 * Represents secure walletInfo storage based on SharedPreferences.
 */
class WalletInfoPersistenceImpl(
        preferences: SharedPreferences
) : WalletInfoPersistence {
    private val secureStorage = SecureStorage(preferences)

    override fun saveWalletInfo(data: WalletInfo, password: CharArray) {
        val nonSensitiveData =
                GsonFactory().getBaseGson().toJson(
                        data.copy(secretSeed = CharArray(0))
                ).toByteArray()
        val sensitiveData = data.secretSeed.toByteArray()
        secureStorage.saveWithPassword(sensitiveData, SEED_KEY, password)
        secureStorage.saveWithPassword(nonSensitiveData, WALLET_INFO_KEY, password)
    }



    override fun loadWalletInfo(email: String, password: CharArray): WalletInfo? {
        try {
            val walletInfoBytes = secureStorage.loadWithPassword(WALLET_INFO_KEY, password)
                    ?: return null
            val walletInfo = GsonFactory().getBaseGson()
                    .fromJson(String(walletInfoBytes), WalletInfo::class.java)
                    .also {
                        // Will fall with NPE on failed parsing.
                        it.accountId.length
                    }

            val seedBytes = secureStorage.loadWithPassword(SEED_KEY, password)
                    ?: return null
            walletInfo.secretSeed = seedBytes.toCharArray()
            seedBytes.fill(0)

            return walletInfo
                    ?.takeIf { email == it.email }

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    override fun clear() {
        secureStorage.clear(SEED_KEY)
        secureStorage.clear(WALLET_INFO_KEY)
    }

    companion object {
        private const val SEED_KEY = "(◕‿◕✿)"
        private const val WALLET_INFO_KEY = "ಠ_ಠ"
    }
}