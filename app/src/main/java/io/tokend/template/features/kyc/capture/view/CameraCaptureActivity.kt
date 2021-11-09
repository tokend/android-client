package io.tokend.template.features.kyc.capture.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import io.reactivex.rxkotlin.addTo
import io.tokend.template.R
import io.tokend.template.activities.BaseActivity
import io.tokend.template.features.kyc.capture.model.CameraCaptureResult
import io.tokend.template.features.kyc.capture.model.CameraCaptureTarget
import io.tokend.template.util.ObservableTransformers
import io.tokend.template.util.PermissionManager
import io.tokend.template.view.util.FullscreenInsetsUtil
import io.tokend.template.view.util.UserFlowFragmentDisplayer
import kotlinx.android.synthetic.main.activity_camera_capture.*
import kotlinx.android.synthetic.main.appbar.*
import kotlinx.android.synthetic.main.toolbar.*
import java.io.File
import java.util.*

class CameraCaptureActivity : BaseActivity() {
    override val allowUnauthorized = true

    private val cameraPermission = PermissionManager(Manifest.permission.CAMERA, 404)

    private val fragmentDisplayer =
        UserFlowFragmentDisplayer(this, R.id.fragment_container_layout)

    private val target: CameraCaptureTarget by lazy {
        intent.getStringExtra(TARGET_EXTRA)!!.let(CameraCaptureTarget::valueOf)
    }

    private val canSkip: Boolean by lazy {
        intent.getBooleanExtra(CAN_SKIP_EXTRA, true)
    }

    private val captureFile: File by lazy {
        File(
            externalCacheDir, target.name.toLowerCase(Locale.ENGLISH)
                    + System.currentTimeMillis() + ".jpg"
        )
    }

    private var isFullScreen: Boolean = false
        set(value) {
            val isChanged = field != value
            if (isChanged) {
                field = value
                onOnFullScreenToggled()
            }
        }

    private var defaultStatusBarColor: Int = 0
    private var defaultLightStatusBar: Boolean = false

    private var isCapturedSuccessfully: Boolean = false

    private val backArrowDrawable: Drawable by lazy {
        ContextCompat.getDrawable(this, R.drawable.ic_arrow_back)!!.mutate()
    }

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_camera_capture)

        initToolbar()
        initFullscreenToggling()

        appbar_layout.post {
            cameraPermission.check(
                this,
                this::toCapture,
                this::finish
            )
        }
    }

    private fun initToolbar() {
        appbar.background = null
        toolbar.navigationIcon = backArrowDrawable
        toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun initFullscreenToggling() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            defaultStatusBarColor = window.statusBarColor
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                defaultLightStatusBar = window.decorView.systemUiVisibility and
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR == View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }

        isFullScreen = true
    }

    private fun toCapture() {
        val fragment = CameraCaptureFragment.newInstance(
            CameraCaptureFragment.getBundle(
                target = target,
                filePath = captureFile.path,
                canSkip = canSkip,
                topSafeZoneHeight = appbar_layout.bottom,
                bottomSafeZoneHeight = FullscreenInsetsUtil.getNavigationBarOverlayHeight(
                    appbar_layout
                )
            )
        )
        fragment.resultObservable
            .compose(ObservableTransformers.defaultSchedulers())
            .subscribe { isCaptured ->
                if (isCaptured) {
                    toPreview()
                } else {
                    finishWithSkip()
                }
            }
            .addTo(compositeDisposable)

        fragmentDisplayer.display(fragment, "capture", forward = null)

        fragment_container_layout.post {
            DrawableCompat.setTintList(backArrowDrawable, ColorStateList.valueOf(Color.WHITE))
            isFullScreen = true
        }
    }

    private fun toPreview() {
        val fragment = CapturePreviewFragment.newInstance(
            CapturePreviewFragment.getBundle(
                target = target,
                filePath = captureFile.path,
                topSafeZoneHeight = appbar_layout.height - appbar_layout.paddingTop
            )
        )
        fragment.resultObservable
            .compose(ObservableTransformers.defaultSchedulers())
            .subscribe(this::onPreviewResult)
            .addTo(compositeDisposable)

        fragmentDisplayer.display(fragment, "preview", forward = true)

        fragment_container_layout.post {
            DrawableCompat.setTintList(backArrowDrawable, null)
            isFullScreen = false
        }
    }

    private fun onPreviewResult(isAccepted: Boolean) {
        if (!isAccepted) {
            toCapture()
        } else {
            finishWithSuccess()
        }
    }

    private fun finishWithSuccess() {
        isCapturedSuccessfully = true
        setResult(
            Activity.RESULT_OK,
            Intent().putExtra(CAPTURE_RESULT_EXTRA, CameraCaptureResult.Success(captureFile))
        )
        finish()
    }

    private fun finishWithSkip() {
        setResult(
            Activity.RESULT_OK,
            Intent().putExtra(CAPTURE_RESULT_EXTRA, CameraCaptureResult.Skipped)
        )
        finish()
    }

    private fun onOnFullScreenToggled() {
        if (isFullScreen) {
            window.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    statusBarColor = Color.TRANSPARENT
                    setFlags(
                        WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
                        WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION or
                                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    )
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                }
            }
        } else {
            window.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    statusBarColor = defaultStatusBarColor
                    clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                            if (defaultLightStatusBar && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                            else
                                0
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        cameraPermission.handlePermissionResult(requestCode, permissions, grantResults)
    }

    override fun onBackPressed() {
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isCapturedSuccessfully) {
            captureFile.delete()
        }
    }

    companion object {
        private const val TARGET_EXTRA = "target"
        private const val CAN_SKIP_EXTRA = "can_skip"
        const val CAPTURE_RESULT_EXTRA = "capture_result"

        fun getBundle(
            target: CameraCaptureTarget,
            canSkip: Boolean = true
        ) = Bundle().apply {
            putString(TARGET_EXTRA, target.name)
            putBoolean(CAN_SKIP_EXTRA, canSkip)
        }
    }
}