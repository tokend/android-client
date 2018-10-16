package org.tokend.template.util

import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.support.v4.app.Fragment
import android.webkit.MimeTypeMap
import org.tokend.sdk.api.base.model.RemoteFile
import org.tokend.template.R

/**
 * Manages [RemoteFile] downloading.
 */
class FileDownloader(
        private val context: Context,
        private val storageUrl: String
) {
    private val storagePermission =
            Permission(Manifest.permission.WRITE_EXTERNAL_STORAGE, 403)

    fun download(activity: Activity, file: RemoteFile) {
        storagePermission.check(activity, { downloadFile(context, file) })
    }

    fun download(fragment: Fragment, file: RemoteFile) {
        storagePermission.check(fragment, { downloadFile(context, file) })
    }

    fun handlePermissionResult(requestCode: Int, permissions: Array<out String>,
                               grantResults: IntArray) {
        storagePermission.handlePermissionResult(requestCode, permissions, grantResults)
    }

    private fun downloadFile(context: Context, file: RemoteFile) {
        val name = file.name ?: "document"
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(file.mimeType)

        val saveName =
                if (extension == null || name.endsWith(extension))
                    name
                else
                    "$name.$extension"

        val request = DownloadManager.Request(Uri.parse(file.getUrl(storageUrl)))
        request.setTitle(file.name)
        if (file.mimeType != null) {
            request.setMimeType(file.mimeType)
        }
        request.allowScanningByMediaScanner()
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                saveName)
        request.setVisibleInDownloadsUi(true)

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)

        ToastManager(context).long(context.getString(R.string.template_file_download_location,
                Environment.DIRECTORY_DOWNLOADS))
    }
}