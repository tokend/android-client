package org.tokend.template.features.qr

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.FileProvider
import android.support.v7.widget.Toolbar
import android.view.*
import android.widget.Toast
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.activity_share_qr.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.doAsync
import org.tokend.template.R
import org.tokend.template.features.qr.logic.QrGenerator
import org.tokend.template.fragments.BaseFragment
import org.tokend.template.fragments.ToolbarProvider
import org.tokend.template.util.ObservableTransformers
import org.tokend.template.view.util.AnimationUtil
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

open class ShareQrFragment : BaseFragment(), ToolbarProvider {
    companion object {
        val ID = "share_qr".hashCode().toLong() and 0xffff
        const val TITLE_EXTRA = "title"
        const val DATA_EXTRA = "data"
        const val SHARE_DIALOG_TEXT_EXTRA = "share_dialog_text"
        const val SHARE_TEXT_EXTRA = "share_text"
        const val TOP_TEXT_EXTRA = "top_text"

        fun newInstance(
                title: String? = null,
                data: String? = null,
                shareDialogText: String? = null,
                shareText: String? = null,
                topText: String? = null
        ): ShareQrFragment {
            val fragment = ShareQrFragment()
            fragment.arguments = Bundle().apply {
                putString(TITLE_EXTRA, title)
                putString(DATA_EXTRA, data)
                putString(SHARE_DIALOG_TEXT_EXTRA, shareDialogText)
                putString(SHARE_TEXT_EXTRA, shareText)
                putString(TOP_TEXT_EXTRA, topText)
            }
            return fragment
        }
    }

    override val toolbarSubject: BehaviorSubject<Toolbar> = BehaviorSubject.create<Toolbar>()

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

    private var savedQrUri: Uri? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_share_qr, container, false)
    }

    override fun onInitAllowed() {
        toolbarSubject.onNext(toolbar)
        toolbar.title = title
        displayData()
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
        val height = scroll_view.measuredHeight -
                2 * resources.getDimensionPixelSize(R.dimen.standard_margin)
        return Math.min(height, scroll_view.measuredWidth)
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
    }

    private fun displayQrCode(text: String) {
        QrGenerator().bitmap(text, getMaxQrSize())
                .delay(300, TimeUnit.MILLISECONDS)
                .compose(ObservableTransformers.defaultSchedulers())
                .doOnSubscribe {
                    qr_code_image_view.visibility = View.INVISIBLE
                }
                .subscribeBy(
                        onNext = {
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
