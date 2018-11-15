package org.tokend.template.features.qr

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.FileProvider
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_share_qr.*
import org.jetbrains.anko.doAsync
import org.tokend.template.R
import org.tokend.template.activities.BaseActivity
import org.tokend.template.view.util.AnimationUtil
import org.tokend.template.extensions.getStringExtra
import org.tokend.template.features.qr.logic.QrGenerator
import org.tokend.template.util.ObservableTransformers
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class ShareQrActivity : BaseActivity() {
    companion object {
        const val TITLE_EXTRA = "title"
        const val DATA_EXTRA = "data"
        const val SHARE_DIALOG_TEXT_EXTRA = "share_dialog_text"
        const val SHARE_TEXT_EXTRA = "share_text"
        const val TOP_TEXT_EXTRA = "top_text"
    }

    private val title: String
        get() = intent.getStringExtra(TITLE_EXTRA, "")
    private val data: String
        get() = intent.getStringExtra(DATA_EXTRA, "")
    private val shareDialogText: String
        get() = intent.getStringExtra(SHARE_DIALOG_TEXT_EXTRA, "")
    private val shareText: String
        get() = intent.getStringExtra(SHARE_TEXT_EXTRA, data)
    private val topText: String
        get() = intent.getStringExtra(TOP_TEXT_EXTRA, "")

    private var savedQrUri: Uri? = null

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_share_qr)
        setTitle(title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        displayData()
    }

    private fun shareData() {
        val sharingIntent = Intent(Intent.ACTION_SEND)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_SUBJECT,
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
        QrGenerator(this).bitmap(text, getMaxQrSize())
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
                            Toast.makeText(this,
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
            val imagesFolder = File(cacheDir, "shared")
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

                savedQrUri = FileProvider.getUriForFile(this@ShareQrActivity,
                        "$packageName.fileprovider", file)

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.share, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.share -> shareData()
        }
        return super.onOptionsItemSelected(item)
    }
}
