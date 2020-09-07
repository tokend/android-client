package org.tokend.template.features.qr

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.core.content.FileProvider
import androidx.appcompat.widget.Toolbar
import android.view.*
import android.widget.Toast
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_share_qr.*
import kotlinx.android.synthetic.main.include_appbar_elevation.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.doAsync
import org.tokend.template.R
import org.tokend.template.extensions.withArguments
import org.tokend.template.features.qr.logic.QrGenerator
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.fragments.ToolbarProvider
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.util.AnimationUtil
import org.tokend.template.view.util.ElevationUtil
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.min

open class ShareQrFragment : BaseFragment(), ToolbarProvider {
    companion object {
        val ID = "share_qr".hashCode().toLong() and 0xffff
        const val TITLE_EXTRA = "title"
        const val DATA_EXTRA = "data"
        const val SHARE_DIALOG_TEXT_EXTRA = "share_dialog_text"
        const val SHARE_TEXT_EXTRA = "share_text"
        const val TOP_TEXT_EXTRA = "top_text"
        const val BOTTOM_TEXT_EXTRA = "bottom_text"

        fun newInstance(bundle: Bundle): ShareQrFragment = ShareQrFragment().withArguments(bundle)

        fun getBundle(data: String,
                      title: String?,
                      shareDialogText: String?,
                      shareText: String?,
                      topText: String?,
                      bottomText: String?) = Bundle().apply {
            putString(TITLE_EXTRA, title)
            putString(DATA_EXTRA, data)
            putString(SHARE_DIALOG_TEXT_EXTRA, shareDialogText)
            putString(SHARE_TEXT_EXTRA, shareText)
            putString(TOP_TEXT_EXTRA, topText)
            putString(BOTTOM_TEXT_EXTRA, bottomText)
        }
    }

    override val toolbarSubject: BehaviorSubject<Toolbar> = BehaviorSubject.create()

    open val title: String
        get() = arguments?.getString(TITLE_EXTRA) ?: ""
    open val data: String
        get() = arguments?.getString(DATA_EXTRA) ?: ""
    open val shareDialogText: String
        get() = arguments?.getString(SHARE_DIALOG_TEXT_EXTRA) ?: ""
    open val shareText: String
        get() = arguments?.getString(SHARE_TEXT_EXTRA) ?: data
    open val topText: String
        get() = arguments?.getString(TOP_TEXT_EXTRA) ?: ""
    open val bottomText: String
        get() = arguments?.getString(BOTTOM_TEXT_EXTRA) ?: ""

    private var savedQrUri: Uri? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_share_qr, container, false)
    }

    override fun onInitAllowed() {
        toolbarSubject.onNext(toolbar)
        toolbar.title = title
        initBrightness()
        initAutoRedraw()
        ElevationUtil.initScrollElevation(scroll_view, appbar_elevation_view)
    }

    private fun initBrightness() {
        requireActivity().window.apply {
            attributes.screenBrightness = 1f
            setType(attributes.type)
        }
    }

    private fun initAutoRedraw() {
        val sizeSubject = PublishSubject.create<Boolean>()

        scroll_view.addOnLayoutChangeListener { _, left, top, right, bottom,
                                                oldLeft, oldTop, oldRight, oldBottom ->
            val width = right - left
            val height = bottom - top
            val oldWidth = oldRight - oldLeft
            val oldHeight = oldBottom - oldTop

            if (oldWidth != width || oldHeight != height) {
                sizeSubject.onNext(true)
            }
        }

        sizeSubject
                .debounce(100, TimeUnit.MILLISECONDS)
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe {
                    displayData()
                }
                .addTo(compositeDisposable)
    }

    private fun shareData() {
        val sharingIntent = Intent(Intent.ACTION_SEND)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT,
                            getString(R.string.app_name))
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }

        savedQrUri?.also {
            sharingIntent.putExtra(Intent.EXTRA_STREAM, savedQrUri)
            sharingIntent.type = "image/png"
        }

        startActivity(Intent.createChooser(sharingIntent, shareDialogText))
    }

    private fun getMaxQrSize(): Int {
        val space = 2 * resources.getDimensionPixelSize(R.dimen.double_margin)
        return min(scroll_view.height - space, scroll_view.width - space)
    }

    private fun displayData() {
        scroll_view.post {
            displayQrCode(data)
        }

        data_text_view.text = data
        data_text_view.setSizeToFit()

        if (topText.isNotBlank()) {
            top_text_view.visibility = View.VISIBLE
            top_text_view.text = topText
        } else {
            top_text_view.visibility = View.GONE
        }

        if (bottomText.isNotBlank()) {
            bottom_text_view.visibility = View.VISIBLE
            bottom_text_view.text = bottomText
            bottom_text_view.setSizeToFit()
        } else {
            bottom_text_view.visibility = View.GONE
        }
    }

    private var displayQrDisposable: Disposable? = null
    private fun displayQrCode(text: String) {
        displayQrDisposable?.dispose()
        displayQrDisposable = QrGenerator().bitmap(text, getMaxQrSize())
                .delay(300, TimeUnit.MILLISECONDS)
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .doOnSubscribe {
                    qr_code_image_view.visibility = View.INVISIBLE
                }
                .subscribeBy(
                        onSuccess = {
                            qr_code_image_view.setImageBitmap(it)
                            animateQrCode()
                            saveQrCode(it)
                        },
                        onError = {
                            Toast.makeText(requireContext(),
                                    R.string.error_try_again, Toast.LENGTH_SHORT)
                                    .show()
                        }
                )
                .addTo(compositeDisposable)
    }

    private fun animateQrCode() {
        AnimationUtil.fadeInView(qr_code_image_view)
    }

    private fun saveQrCode(bitmap: Bitmap) {
        doAsync {
            val imagesFolder = File(requireContext().cacheDir, "shared")
            try {
                val borderSize = resources.getDimensionPixelSize(R.dimen.standard_padding)
                val borderedBitmap = Bitmap.createBitmap(
                        bitmap.width + borderSize * 2,
                        bitmap.height + borderSize * 2,
                        bitmap.config
                )

                val canvas = Canvas(borderedBitmap)
                canvas.drawColor(Color.WHITE)
                canvas.drawBitmap(bitmap, borderSize.toFloat(), borderSize.toFloat(), null)

                imagesFolder.mkdirs()
                val file = File(imagesFolder, "shared_qr.png")

                FileOutputStream(file).use {
                    borderedBitmap.compress(Bitmap.CompressFormat.PNG, 1, it)
                    it.flush()
                }

                savedQrUri = FileProvider.getUriForFile(requireContext(),
                        "${requireActivity().packageName}.fileprovider", file)

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.share, menu)

        menu.findItem(R.id.share).apply {
            setOnMenuItemClickListener {
                shareData()
                true
            }
        }
    }
}
