package org.tokend.template.util

import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.fragment.app.Fragment
import android.webkit.MimeTypeMap
import org.tokend.sdk.api.base.model.RemoteFile
import org.tokend.template.R
import org.tokend.template.view.ToastManager

/**
 * Manages [RemoteFile] downloading.
 */
class FileDownloader(
        private val context: Context,
        private val storageUrl: String,
        private val toastManager: ToastManager?
) {
    private val storagePermission =
            PermissionManager(Manifest.permission.WRITE_EXTERNAL_STORAGE, 403)

    /**
     * Downloads given file to the downloads folder with permission ask.
     * Make sure to call [handlePermissionResult]
     * inside [Activity.onRequestPermissionsResult] for correct work
     */
    fun download(activity: Activity, file: RemoteFile) {
        storagePermission.check(activity) { downloadFile(context, file) }
    }

    /**
     * Downloads given file to the downloads folder with permission ask
     * Make sure to call [handlePermissionResult]
     * inside [Fragment.onRequestPermissionsResult] for correct work
     */
    fun download(fragment: androidx.fragment.app.Fragment, file: RemoteFile) {
        storagePermission.check(fragment) { downloadFile(context, file) }
    }

    /**
     * Handles write permission grant result.
     * If permission has been granted the downloading will be performed
     */
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

        toastManager?.long(context.getString(R.string.template_file_download_location,
                Environment.DIRECTORY_DOWNLOADS))
    }
}