package org.tokend.template.features.kyc.capture.logic

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.view.View
import android.view.ViewGroup
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.controls.Facing
import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import org.tokend.template.R
import kotlin.math.floor

/**
 * Crops document capture frames according to the overlay.
 */
class CameraCaptureFrameCropper {
    /**
     * @param frame capture frame in size of [overlayContainer]
     * @param overlayContainer ViewGroup in size of frame that contains mask [R.id.mask_image_view]
     */
    fun crop(frame: PictureResult, overlayContainer: ViewGroup): Single<Bitmap> {
        return {
            var frameBitmap = BitmapFactory.decodeByteArray(frame.data, 0, frame.data.size)
            if (frame.facing == Facing.FRONT) {
                val matrix = Matrix().apply {
                    preScale(-1f, 1f)
                }
                frameBitmap = Bitmap.createBitmap(
                    frameBitmap, 0, 0,
                    frameBitmap.width, frameBitmap.height,
                    matrix, false
                )
            }

            frameBitmap
        }
            .toSingle()
            .flatMap { crop(it, overlayContainer) }
    }


    /**
     * @param frame capture frame in size of [overlayContainer]
     * @param overlayContainer ViewGroup in size of frame that contains mask [R.id.mask_image_view]
     */
    fun crop(frame: Bitmap, overlayContainer: ViewGroup): Single<Bitmap> = {
        val maskWidth: Int
        val maskHeight: Int
        val maskTop: Int
        overlayContainer.findViewById<View>(R.id.mask_image_view)
            .also {
                maskWidth = it.width
                maskHeight = it.height
                maskTop = IntArray(2).also(it::getLocationOnScreen)[1]
            }

        var correctRotationFrame = frame
        if (overlayContainer.height >= overlayContainer.width
            && frame.height < frame.width
        ) {
            val rotationMatrix = Matrix().apply {
                postRotate(90f)
            }
            correctRotationFrame =
                Bitmap.createBitmap(frame, 0, 0, frame.width, frame.height, rotationMatrix, true)
            frame.recycle()
        }

        val maskSizeMultiplier = correctRotationFrame.width.toDouble() / overlayContainer.width
        val cropY = floor(maskTop * maskSizeMultiplier).toInt()
        val cropWidth = floor(maskWidth * maskSizeMultiplier).toInt()
        val cropX = floor((correctRotationFrame.width - cropWidth).toDouble() / 2).toInt()
        val cropHeight = floor(maskHeight * maskSizeMultiplier).toInt() - 1

        Bitmap.createBitmap(correctRotationFrame, cropX, cropY, cropWidth, cropHeight)
    }.toSingle()
}