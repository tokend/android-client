package org.tokend.template.base.tfa

import android.util.Base64
import org.tokend.sdk.keyserver.models.KeychainData
import org.tokend.sdk.keyserver.KeyStorage
import org.tokend.sdk.keyserver.models.KdfAttributes
import org.tokend.sdk.tfa.NeedTfaException
import org.tokend.wallet.Account

class PasswordTfaOtpGenerator() {
    fun generate(tfaException: NeedTfaException, email: String, password: CharArray): String {
        val kdfAttributes = KdfAttributes("scrypt",
                256, 4096, 1, 8, tfaException.salt)
        val key = KeyStorage.getWalletKey(email, password, kdfAttributes)
        val keychainData = KeychainData.fromJsonString(tfaException.keychainData)
        val seed = try {
            KeyStorage.decryptSecretSeed(keychainData.iv, keychainData.cipherText, key)
        } catch (e: Exception) {
            return ""
        } finally {
            key.fill(0)
        }

        val account = Account.fromSecretSeed(seed)
        seed.fill('0')
        val signature = account.sign(tfaException.token.toByteArray())

        return Base64.encodeToString(signature, Base64.NO_WRAP)
    }
}