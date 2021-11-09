package io.tokend.template.features.kyc.files.model

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.util.*

/**
 * Represents a file stored on the device
 */
data class LocalFile(
    /**
     * 'file://' or 'content://' URI for the file
     */
    val uri: Uri,
    /**
     * MIME type of the file
     */
    val mimeType: String,
    /**
     * Byte size of the file
     */
    val size: Long,
    /**
     * Name of the file with extension
     */
    val name: String
) {
    companion object {
        private fun getMimeTypeOfFile(fileUri: Uri): String {
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(fileUri.toString())
            return MimeTypeMap
                .getSingleton()
                .getMimeTypeFromExtension(fileExtension.toLowerCase(Locale.ENGLISH))
                ?: ""
        }

        /**
         * @return [LocalFile] instance constructed from 'file://' or 'content://' URI
         */
        fun fromUri(uri: Uri, contentResolver: ContentResolver): LocalFile {
            val mimeType = contentResolver.getType(uri)
                ?: getMimeTypeOfFile(uri)

            val size: Long
            val name: String

            val projection = arrayOf(
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE
            )
            val queryCursor = contentResolver
                .query(uri, projection, null, null, null)

            if (queryCursor?.moveToFirst() == true) {
                // Content URI
                size = queryCursor.getLong(queryCursor.getColumnIndex(OpenableColumns.SIZE))
                name =
                    queryCursor.getString(queryCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            } else {
                // File URI
                val file = File(
                    uri.path
                        ?: throw IllegalArgumentException("Not a file URI, missing path")
                )
                size = file.length()
                name = file.name
            }

            queryCursor?.close()

            return LocalFile(uri, mimeType, size, name)
        }

        fun fromFile(file: File): LocalFile {
            val uri = Uri.fromFile(file)

            return LocalFile(
                size = file.length(),
                name = file.name,
                mimeType = getMimeTypeOfFile(uri),
                uri = uri
            )
        }
    }
}