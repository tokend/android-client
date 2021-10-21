package org.tokend.template.features.kyc.capture.view

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.controls.Facing
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.toCompletable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_camera_capture.*
import org.tokend.template.features.kyc.capture.logic.CameraCaptureFrameCropper
import org.tokend.template.features.kyc.capture.model.CameraCaptureTarget
import org.tokend.template.R
import org.tokend.template.extensions.textOrGone
import org.tokend.template.extensions.withArguments
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.util.ObservableTransformers
import java.io.File

class CameraCaptureFragment : BaseFragment() {
    private val resultSubject: PublishSubject<Boolean> = PublishSubject.create()
    val resultObservable: Observable<Boolean> = resultSubject

    private val target: CameraCaptureTarget by lazy {
        arguments?.getString(TARGET_EXTRA)?.let(CameraCaptureTarget::valueOf)
            ?: throw IllegalArgumentException("No $TARGET_EXTRA specified")
    }

    private val filePath: String by lazy {
        arguments?.getString(FILE_PATH_EXTRA)
            ?: throw IllegalArgumentException("No $FILE_PATH_EXTRA specified")
    }

    private val topSafeZoneHeight: Int by lazy {
        arguments?.getInt(TOP_SAFE_ZONE_HEIGHT_EXTRA, 0) ?: 0
    }

    private val bottomSafeZoneHeight: Int by lazy {
        arguments?.getInt(BOTTOM_SAFE_ZONE_HEIGHT_EXTRA, 0) ?: 0
    }

    private val canSkip: Boolean by lazy {
        arguments?.getBoolean(CAN_SKIP_EXTRA) ?: true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_camera_capture, container, false)
    }

    override fun onInitAllowed() {
        initCameraPreview()
        initButtons()
        initOverlay()
    }

    private fun initCameraPreview() {
        camera_preview.apply {
            facing = when (target) {
                CameraCaptureTarget.SELFIE -> Facing.FRONT
                else -> Facing.BACK
            }
            setPreviewStreamSize { availableSizes ->
                availableSizes
                    .filter { it.width >= MIN_PREVIEW_SIZE || it.height >= MIN_PREVIEW_SIZE }
                    .takeIf { it.isNotEmpty() }
                    ?: availableSizes
            }

            addCameraListener(object : CameraListener() {
                override fun onPictureTaken(result: PictureResult) {
                    cropAndSaveCapture(result)
                        .compose(ObservableTransformers.defaultSchedulersCompletable())
                        .doOnTerminate { isCapturing = false }
                        .subscribeBy(
                            onComplete = { resultSubject.onNext(true) },
                            onError = errorHandlerFactory.getDefault()::handleIfPossible
                        )
                        .addTo(compositeDisposable)
                }
            })
        }
    }

    private fun initButtons() {
        capture_button.setOnClickListener {
            capture()
        }

        if (canSkip) {
            skip_capture_button.setOnClickListener {
                skipCapture()
            }
        } else {
            skip_capture_button.visibility = View.GONE
        }
    }

    private fun initOverlay() {
        top_safe_zone_space.layoutParams = top_safe_zone_space.layoutParams.apply {
            height = topSafeZoneHeight
        }

        bottom_safe_zone_space.layoutParams = bottom_safe_zone_space.layoutParams.apply {
            height = bottomSafeZoneHeight
        }

        overlay_container.addView(
            target.getCaptureOverlay(
                context = requireContext(),
                parent = overlay_container
            )
        )

        capture_hint_text_view.textOrGone = target.getCaptureHint(requireContext())
    }

    private var isCapturing = false
    private fun capture() {
        if (isCapturing) {
            return
        }

        isCapturing = true

        camera_preview.takePictureSnapshot()
    }

    private fun cropAndSaveCapture(result: PictureResult): Completable {
        return CameraCaptureFrameCropper()
            .crop(
                frame = result,
                overlayContainer = overlay_container
            )
            .flatMapCompletable { croppedFrame ->
                {
                    File(filePath).outputStream().use {
                        croppedFrame.compress(Bitmap.CompressFormat.JPEG, 100, it)
                    }
                }.toCompletable()
            }
    }

    private fun skipCapture() {
        resultSubject.onNext(false)
    }

    override fun onResume() {
        super.onResume()
        camera_preview.open()
    }

    override fun onPause() {
        super.onPause()
        camera_preview.close()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        camera_preview.destroy()
    }

    companion object {
        private const val TARGET_EXTRA = "target"
        private const val FILE_PATH_EXTRA = "file_path"
        private const val TOP_SAFE_ZONE_HEIGHT_EXTRA = "top_safe_zone_height"
        private const val BOTTOM_SAFE_ZONE_HEIGHT_EXTRA = "bottom_safe_zone_height"
        private const val CAN_SKIP_EXTRA = "can_skip"
        private const val MIN_PREVIEW_SIZE = 700

        fun getBundle(
            target: CameraCaptureTarget,
            filePath: String,
            canSkip: Boolean = true,
            topSafeZoneHeight: Int = 0,
            bottomSafeZoneHeight: Int = 0
        ) = Bundle().apply {
            putString(TARGET_EXTRA, target.name)
            putString(FILE_PATH_EXTRA, filePath)
            putInt(TOP_SAFE_ZONE_HEIGHT_EXTRA, topSafeZoneHeight)
            putInt(BOTTOM_SAFE_ZONE_HEIGHT_EXTRA, bottomSafeZoneHeight)
            putBoolean(CAN_SKIP_EXTRA, canSkip)
        }

        fun newInstance(bundle: Bundle): CameraCaptureFragment =
            CameraCaptureFragment().withArguments(bundle)
    }
}