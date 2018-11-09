package org.tokend.template.base.logic.persistance

import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.support.annotation.RequiresApi
import org.tokend.crypto.cipher.Aes256GCM
import org.tokend.kdf.KeyDerivationFunction
import org.tokend.kdf.ScryptKeyDerivation
import org.tokend.sdk.factory.GsonFactory
import org.tokend.sdk.keyserver.models.KdfAttributes
import org.tokend.sdk.keyserver.models.KeychainData
import org.tokend.template.extensions.toByteArray
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

/**
 * Represents secure storage based on SharedPreferences.
 */
class SecureStorage(
        private val preferences: SharedPreferences
) {
    /**
     * Encrypts data using secure key from Android [KeyStore]
     * and saves cipher text in [SharedPreferences].
     * @return true on success, false otherwise
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun save(data: ByteArray, key: String): Boolean {
        val secretKey = getSecretKey(key)
                ?: createSecretKey(key)
                ?: return false

        try {
            val encryptCipher = getEncryptCipher(secretKey)!!
            val encryptedData = encryptCipher.doFinal(data)
            val keychainData = KeychainData.fromRaw(encryptCipher.iv, encryptedData)
            saveKeychainData(keychainData, key)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        return true
    }

    /**
     * Loads data by given key and decrypts it with secure key from Android [KeyStore]
     * @return decrypted data or [null] if it is not exists or decryption failed
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun load(key: String): ByteArray? {
        val secretKey = getSecretKey(key)
                ?: return null

        try {
            val keychainData = loadKeychainData(key)!!
            val decryptCipher = getDecryptCipher(secretKey, keychainData.iv)!!
            return decryptCipher.doFinal(keychainData.cipherText)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Encrypts data with given password
     * and saves cipher text in [SharedPreferences].
     */
    fun saveWithPassword(data: ByteArray, key: String, password: CharArray): Boolean {
        var keyBytes = ByteArray(0)
        val passwordBytes = password.toByteArray()
        return try {
            val seed = SecureRandom.getSeed(16)
            keyBytes = getKeyDerivation()
                    .derive(passwordBytes, seed, KDF_PARAMS.bytes)
            val encryptedData = Aes256GCM(seed).encrypt(data, keyBytes)
            keyBytes.fill(0)
            val keychainData = KeychainData.fromRaw(seed, encryptedData)
            saveKeychainData(keychainData, key)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            keyBytes.fill(0)
            passwordBytes.fill(0)
        }
    }

    /**
     * Loads data by given key and decrypts it with password.
     * @return decrypted data or null if it is not exists or decryption failed
     */
    fun loadWithPassword(key: String, password: CharArray): ByteArray? {
        var keyBytes = ByteArray(0)
        val passwordBytes = password.toByteArray()
        return try {
            val keychainData = loadKeychainData(key)!!
            val seed = keychainData.iv
            keyBytes = getKeyDerivation()
                    .derive(passwordBytes, seed, KDF_PARAMS.bytes)
            return Aes256GCM(seed).decrypt(keychainData.cipherText, keyBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            keyBytes.fill(0)
            passwordBytes.fill(0)
        }
    }

    /**
     * Clears encrypted data entry for given key.
     */
    fun clear(key: String) {
        preferences.edit().remove(key).apply()
    }

    private fun saveKeychainData(data: KeychainData, key: String) {
        preferences
                .edit()
                .putString(
                        key,
                        GsonFactory().getBaseGson().toJson(data)
                )
                .apply()
    }

    private fun loadKeychainData(key: String): KeychainData? {
        val json = preferences.getString(key, "")
                .takeIf { it.isNotEmpty() }
                ?: return null
        return try {
            KeychainData.fromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun createSecretKey(name: String): SecretKey? {
        return try {
            val keyGenerator = KeyGenerator.getInstance(SECRET_KEY_ALG, KEYSTORE_NAME)
            keyGenerator.init(KeyGenParameterSpec.Builder(
                    "${SECRET_KEY_NAME_PREFIX}_$name",
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(false)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build())
            keyGenerator.generateKey()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getSecretKey(name: String): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_NAME)
            keyStore.load(null)
            return keyStore.getKey("${SECRET_KEY_NAME_PREFIX}_$name", null) as? SecretKey
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getEncryptCipher(key: SecretKey): Cipher? {
        return try {
            val cipher = Cipher.getInstance(CIPHER)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            return cipher
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getDecryptCipher(key: SecretKey, iv: ByteArray): Cipher? {
        return try {
            val cipher = Cipher.getInstance(CIPHER)
            cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
            return cipher
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getKeyDerivation(): KeyDerivationFunction {
        return ScryptKeyDerivation(KDF_PARAMS.n, KDF_PARAMS.r, KDF_PARAMS.p)
    }

    companion object {
        private const val SECRET_KEY_NAME_PREFIX = "ss_"
        private const val SECRET_KEY_ALG = "AES"
        private const val KEYSTORE_NAME = "AndroidKeyStore"
        private const val CIPHER = "AES/CBC/PKCS7Padding"
        private val KDF_PARAMS = KdfAttributes(
                "scrypt",
                256,
                2048,
                1,
                4,
                ""
        )
    }
}