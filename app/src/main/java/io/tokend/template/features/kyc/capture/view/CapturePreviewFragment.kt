package io.tokend.template.features.kyc.capture.view

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.toSingle
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.tokend.template.R
import io.tokend.template.extensions.withArguments
import io.tokend.template.features.kyc.capture.model.CameraCaptureTarget
import io.tokend.template.fragments.BaseFragment
import io.tokend.template.util.ObservableTransformers
import io.tokend.template.view.util.AnimationUtil
import kotlinx.android.synthetic.main.fragment_capture_preview.*
import kotlinx.android.synthetic.main.layout_primary_and_secondary_main_buttons.*

class CapturePreviewFragment : BaseFragment() {
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_capture_preview, container, false)
    }

    override fun onInitAllowed() {
        initOverlay()
        initPreview()
        initButtons()
    }

    private fun initPreview() {
        { BitmapDrawable(resources, BitmapFactory.decodeFile(filePath)) }
            .toSingle()
            .subscribeOn(Schedulers.io())
            .compose(ObservableTransformers.defaultSchedulersSingle())
            .subscribe { captureBitmap ->
                val maskView = overlay_container.findViewById<View>(R.id.mask_image_view)
                AnimationUtil.fadeInView(
                    view = maskView,
                    duration = resources
                        .getInteger(android.R.integer.config_shortAnimTime)
                        .toLong()
                )
                maskView.background = captureBitmap
            }
            .addTo(compositeDisposable)
    }

    private fun initButtons() {
        secondary_action_button.text = target.getRetryLabel(requireContext())
        secondary_action_button.setOnClickListener {
            resultSubject.onNext(false)
        }

        primary_action_button.text = target.getAcceptLabel(requireContext())
        primary_action_button.setOnClickListener {
            resultSubject.onNext(true)
        }
    }

    private fun initOverlay() {
        top_safe_zone_space.layoutParams = top_safe_zone_space.layoutParams.apply {
            height = topSafeZoneHeight
        }

        overlay_container.addView(
            target.getPreviewOverlay(
                context = requireContext(),
                parent = overlay_container,
                backgroundColor = ContextCompat.getColor(requireContext(), R.color.background)
            )
        )
    }

    companion object {
        private const val TARGET_EXTRA = "target"
        private const val FILE_PATH_EXTRA = "file_path"
        private const val TOP_SAFE_ZONE_HEIGHT_EXTRA = "top_safe_zone_height"

        fun getBundle(
            target: CameraCaptureTarget,
            filePath: String,
            topSafeZoneHeight: Int = 0
        ) = Bundle().apply {
            putString(TARGET_EXTRA, target.name)
            putString(FILE_PATH_EXTRA, filePath)
            putInt(TOP_SAFE_ZONE_HEIGHT_EXTRA, topSafeZoneHeight)
        }

        fun newInstance(bundle: Bundle): CapturePreviewFragment =
            CapturePreviewFragment().withArguments(bundle)
    }
}