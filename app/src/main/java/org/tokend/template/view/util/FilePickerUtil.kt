package org.tokend.template.view.util

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import org.tokend.template.features.kyc.files.model.LocalFile
import org.tokend.template.util.navigation.ActivityRequest
import java.io.File

object FilePickerUtil {

    private fun getCameraOutputFile(context: Context): File {
        return File(context.externalCacheDir, "photo_${context.hashCode() and 0xffff}.jpg")
    }

    fun pickFile(
        fragment: Fragment,
        allowedMimeTypes: Array<out String>,
        withCamera: Boolean
    ): ActivityRequest<LocalFile> {
        val context = fragment.requireContext()

        val cameraOutputFile = getCameraOutputFile(context)

        val request = getRequest(context, cameraOutputFile)

        val intent = getFilePickerIntent(context, allowedMimeTypes, withCamera, cameraOutputFile)

        fragment.startActivityForResult(intent, request.code)

        return request
    }

    fun pickFile(
        activity: Activity,
        allowedMimeTypes: Array<out String>,
        withCamera: Boolean
    ): ActivityRequest<LocalFile> {
        val context = activity

        val cameraOutputFile = getCameraOutputFile(context)

        val request = getRequest(context, cameraOutputFile)

        val intent = getFilePickerIntent(context, allowedMimeTypes, withCamera, cameraOutputFile)

        activity.startActivityForResult(intent, request.code)

        return request
    }

    private fun getRequest(context: Context, cameraOutputFile: File): ActivityRequest<LocalFile> {

        val request = ActivityRequest { intent ->
            val intentUri = intent?.data
            val resultUriString =
                (intentUri ?: Uri.fromFile(cameraOutputFile))
                    .toString()
                    .replace("file%3A/", "")
            LocalFile.fromUri(Uri.parse(resultUriString), context.contentResolver)
        }

        return request
    }

    private fun getFilePickerIntent(
        context: Context,
        allowedMimeTypes: Array<out String>,
        withCamera: Boolean,
        cameraOutputFile: File
    ): Intent {
        val contentIntent = Intent(Intent.ACTION_GET_CONTENT)

        contentIntent.type =
            if (allowedMimeTypes.isEmpty())
                "*/*"
            else
                allowedMimeTypes.joinToString(",")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
            && allowedMimeTypes.isNotEmpty()
        ) {
            contentIntent.putExtra(Intent.EXTRA_MIME_TYPES, allowedMimeTypes)
        }

        val chooserIntent = Intent(Intent.ACTION_CHOOSER)
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentIntent)

        if (withCamera) {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val captureIntents = mutableListOf<Intent>()
            val outputFileUri = Uri.fromFile(cameraOutputFile)

            val cameraActivities = context.packageManager.queryIntentActivities(cameraIntent, 0)
            for (camera in cameraActivities) {
                val intent = Intent(cameraIntent)
                intent.component =
                    ComponentName(camera.activityInfo.packageName, camera.activityInfo.name)
                intent.`package` = camera.activityInfo.packageName
                if (outputFileUri != null) {
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri)
                }
                captureIntents.add(intent)
            }

            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, captureIntents.toTypedArray())
        }

        return chooserIntent
    }

    fun removeTemporaryFile(fragment: Fragment) {
        getCameraOutputFile(fragment.requireContext()).delete()
    }

    fun removeTemporaryFile(activity: Activity) {
        getCameraOutputFile(activity).delete()
    }
}