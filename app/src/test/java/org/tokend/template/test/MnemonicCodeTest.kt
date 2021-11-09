package org.tokend.template.test

import org.junit.Assert
import org.junit.Test
import org.tokend.sdk.utils.extentions.decodeHex
import org.tokend.sdk.utils.extentions.encodeHexString
import org.tokend.template.features.localaccount.mnemonic.logic.EnglishMnemonicWords
import org.tokend.template.features.localaccount.mnemonic.logic.MnemonicCode
import org.tokend.template.features.localaccount.mnemonic.logic.MnemonicException

class MnemonicCodeTest {
    val englishMnemonic = MnemonicCode(EnglishMnemonicWords.LIST)

    @Test
    fun entropyToMnemonic() {
        val entropy = "e48a6da6bcd758d1f37d49e2b89e0019"
        val expected = "tonight fat have keen intact happy social powder tired shaft length cram"

        Assert.assertEquals(
            expected,
            englishMnemonic.toMnemonic(entropy.decodeHex()).joinToString(" ")
        )
    }

    @Test
    fun mnemonicToEntropy() {
        val mnemonic =
            "panel inspire unfold trap blanket carry drum giraffe soda practice spider path"
        val expected = "9f8eabb4f3a17445d0e311ce352f46d0"

        Assert.assertEquals(
            expected,
            englishMnemonic.toEntropy(mnemonic.split(" ")).encodeHexString()
        )
    }

    @Test
    fun badLength() {
        val mnemonic = "unfold trap"

        try {
            englishMnemonic.toEntropy(mnemonic.split(" "))
        } catch (e: MnemonicException.MnemonicLengthException) {
            // OK
        } catch (e: Exception) {
            Assert.fail("Expected MnemonicLengthException but $e is thrown")
        }
    }

    @Test
    fun badWords() {
        val mnemonic = "panel inspire oleg"

        try {
            englishMnemonic.toEntropy(mnemonic.split(" "))
        } catch (e: MnemonicException.MnemonicWordException) {
            // OK
        } catch (e: Exception) {
            Assert.fail("Expected MnemonicWordException but $e is thrown")
        }
    }

    @Test
    fun badChecksum() {
        val mnemonic = "legal winner thank year wave sausage worth useful legal winner thank thank"

        try {
            englishMnemonic.toEntropy(mnemonic.split(" "))
        } catch (e: MnemonicException.MnemonicChecksumException) {
            // OK
        } catch (e: Exception) {
            Assert.fail("Expected MnemonicChecksumException but $e is thrown")
        }
    }
}