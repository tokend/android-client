/*
 * Copyright 2013 Ken Sedgwick
 * Copyright 2014 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tokend.template.features.localaccount.mnemonic.logic

import org.tokend.wallet.utils.Hashing
import java.util.*
import kotlin.experimental.and
import kotlin.experimental.or

class MnemonicCode(private val wordList: List<String>) {

    /**
     * Convert mnemonic word list to original entropy value.
     */
    fun toEntropy(words: List<String>): ByteArray {
        if (words.size % 3 > 0)
            throw MnemonicException.MnemonicLengthException("Word list size must be multiple of three words.")

        if (words.isEmpty())
            throw MnemonicException.MnemonicLengthException("Word list is empty.")

        // Look up all the words in the list and construct the
        // concatenation of the original entropy and the checksum.
        //
        val concatLenBits = words.size * 11
        val concatBits = BooleanArray(concatLenBits)
        var wordindex = 0
        for (word in words) {
            // Find the words index in the wordlist.
            val ndx = Collections.binarySearch(this.wordList, word)
            if (ndx < 0)
                throw MnemonicException.MnemonicWordException(word)

            // Set the next 11 bits to the value of the index.
            for (ii in 0..10)
                concatBits[wordindex * 11 + ii] = ndx and (1 shl 10 - ii) != 0
            ++wordindex
        }

        val checksumLengthBits = concatLenBits / 33
        val entropyLengthBits = concatLenBits - checksumLengthBits

        // Extract original entropy as bytes.
        val entropy = ByteArray(entropyLengthBits / 8)
        for (ii in entropy.indices)
            for (jj in 0..7)
                if (concatBits[ii * 8 + jj])
                    entropy[ii] = entropy[ii] or (1 shl 7 - jj).toByte()

        // Take the digest of the entropy.
        val hash = Hashing.sha256(entropy)
        val hashBits = bytesToBits(hash)

        // Check all the checksum bits.
        for (i in 0 until checksumLengthBits)
            if (concatBits[entropyLengthBits + i] != hashBits[i])
                throw MnemonicException.MnemonicChecksumException()

        return entropy
    }

    /**
     * Convert entropy data to mnemonic word list.
     */
    @Throws(MnemonicException.MnemonicLengthException::class)
    fun toMnemonic(entropy: ByteArray): List<String> {
        if (entropy.size % 4 > 0)
            throw MnemonicException.MnemonicLengthException("Entropy length not multiple of 32 bits.")

        if (entropy.isEmpty())
            throw MnemonicException.MnemonicLengthException("Entropy is empty.")

        // We take initial entropy of ENT bits and compute its
        // checksum by taking first ENT / 32 bits of its SHA256 hash.

        val hash = Hashing.sha256(entropy)
        val hashBits = bytesToBits(hash)

        val entropyBits = bytesToBits(entropy)
        val checksumLengthBits = entropyBits.size / 32

        // We append these bits to the end of the initial entropy.
        val concatBits = BooleanArray(entropyBits.size + checksumLengthBits)
        System.arraycopy(entropyBits, 0, concatBits, 0, entropyBits.size)
        System.arraycopy(hashBits, 0, concatBits, entropyBits.size, checksumLengthBits)

        // Next we take these concatenated bits and split them into
        // groups of 11 bits. Each group encodes number from 0-2047
        // which is a position in a wordlist.  We convert numbers into
        // words and use joined words as mnemonic sentence.

        val words = mutableListOf<String>()
        val nWords = concatBits.size / 11
        for (i in 0 until nWords) {
            var index = 0
            for (j in 0..10) {
                index = index shl 1
                if (concatBits[i * 11 + j])
                    index = index or 0x1
            }
            words.add(this.wordList[index])
        }

        return words
    }

    private fun bytesToBits(data: ByteArray): BooleanArray {
        val bits = BooleanArray(data.size * 8)
        for (i in data.indices)
            for (j in 0..7)
                bits[i * 8 + j] = data[i] and (1 shl 7 - j).toByte() != 0.toByte()
        return bits
    }
}