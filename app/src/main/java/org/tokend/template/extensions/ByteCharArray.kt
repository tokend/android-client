package org.tokend.template.extensions

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset

/**
 * Converts given array to the byte array using [Charset.defaultCharset]
 */
fun CharArray.toByteArray(): ByteArray {
    val charBuffer = CharBuffer.wrap(this)
    val byteBuffer = Charset.defaultCharset().encode(charBuffer)
    charBuffer.clear()
    val bytes = ByteArray(byteBuffer.remaining())
    byteBuffer.get(bytes).clear()
    return bytes
}

/**
 * Converts given array to the char array using [Charset.defaultCharset]
 */
fun ByteArray.toCharArray(): CharArray {
    val byteBuffer = ByteBuffer.wrap(this)
    val charBuffer = Charset.defaultCharset().decode(byteBuffer)
    byteBuffer.clear()
    val chars = CharArray(charBuffer.remaining())
    charBuffer.get(chars).clear()
    return chars
}