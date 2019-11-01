package org.tokend.template.util.cipher

import org.tokend.crypto.cipher.Aes256GCM
import org.tokend.crypto.ecdsa.erase
import org.tokend.sdk.keyserver.models.KeychainData
import java.security.SecureRandom

class Aes256GcmDataCipher : DataCipher {
    override fun encrypt(data: ByteArray, key: ByteArray): ByteArray {
        val iv = SecureRandom().generateSeed(IV_LENGTH_BYTES)
        val cipher = Aes256GCM(iv)
        val cipherText = cipher.encrypt(data, key)
        val keychainData = KeychainData.fromRaw(iv, cipherText)
        val result = keychainData.encode().toByteArray(STRING_ENCODING)
        cipherText.erase()
        return result
    }

    override fun decrypt(encryptedData: ByteArray, key: ByteArray): ByteArray {
        val keychainData = KeychainData.fromEncoded(String(encryptedData, STRING_ENCODING))
        val iv = keychainData.iv
        val cipherText = keychainData.cipherText
        val cipher = Aes256GCM(iv)
        return cipher.decrypt(cipherText, key)
    }

    companion object {
        private const val IV_LENGTH_BYTES = 16
        private val STRING_ENCODING = Charsets.UTF_8
    }
}