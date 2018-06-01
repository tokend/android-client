package org.tokend.template.base.activities.qr

import android.content.Intent
import android.graphics.Point
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.widget.Toast
import com.trello.rxlifecycle2.android.ActivityEvent
import com.trello.rxlifecycle2.kotlin.bindUntilEvent
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_share_qr.*
import org.jetbrains.anko.dimen
import org.tokend.template.R
import org.tokend.template.base.activities.BaseActivity
import org.tokend.template.extensions.getStringExtra
import org.tokend.template.util.ObservableTransformers
import java.util.concurrent.TimeUnit

class ShareQrActivity : BaseActivity() {
    companion object {
        const val TITLE_EXTRA = "title"
        const val DATA_EXTRA = "data"
        const val SHARE_DIALOG_TEXT_EXTRA = "share_dialog_text"
        const val SHARE_TEXT_EXTRA = "share_text"
        const val TOP_TEXT_EXTRA = "top_text"
    }

    private var maxQrSize: Int = 0

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

    override fun onCreateAllowed(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_share_qr)
        setTitle(title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initQrHolder()

        displayData()
    }

    private fun initQrHolder() {
        maxQrSize = calculateMaxQrCodeSize(this)
        qr_code_layout.layoutParams.height = (maxQrSize * 1.1).toInt()

        qr_code_image_view.layoutParams.width = maxQrSize
        qr_code_image_view.layoutParams.height = maxQrSize
    }

    private fun shareData() {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                getString(R.string.app_name))
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareText)
        startActivity(Intent.createChooser(sharingIntent, shareDialogText))
    }

    private fun calculateMaxQrCodeSize(activity: AppCompatActivity): Int {
        val display = activity.windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val width = size.x
        val qrPadding = dimen(R.dimen.standard_padding)
        return width - 2 * qrPadding
    }

    private fun displayData() {
        displayQrCode(data)
        data_text_view.text = data

        if (topText.isNotBlank()) {
            top_text_view.visibility = View.VISIBLE
            top_text_view.text = topText
        } else {
            top_text_view.visibility = View.GONE
        }
    }

    private fun displayQrCode(text: String) {

        QrGenerator(this).bitmap(text, maxQrSize)
                .delay(300, TimeUnit.MILLISECONDS)
                .compose(ObservableTransformers.defaultSchedulers())
                .bindUntilEvent(lifecycle(), ActivityEvent.DESTROY)
                .doOnSubscribe {
                    qr_code_image_view.visibility = View.INVISIBLE
                }
                .subscribeBy(
                        onNext = {
                            qr_code_image_view.setImageBitmap(it)
                            qr_code_image_view.visibility = View.VISIBLE
                            animateQrCode()
                        },
                        onError = {
                            Toast.makeText(this,
                                    R.string.error_try_again, Toast.LENGTH_SHORT)
                                    .show()
                        }
                )
    }

    private fun animateQrCode() {
        val alphaAnimation = AlphaAnimation(0f, 1f)
        alphaAnimation.interpolator = AccelerateDecelerateInterpolator()
        alphaAnimation.duration =
                resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        alphaAnimation.fillAfter = true

        qr_code_image_view.clearAnimation()
        qr_code_image_view.startAnimation(alphaAnimation)
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
