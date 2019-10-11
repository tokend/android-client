package org.tokend.template.util.cipher

interface DataCipher {
    fun encrypt(data: ByteArray, key: ByteArray): ByteArray

    fun decrypt(encryptedData: ByteArray, key: ByteArray): ByteArray
}